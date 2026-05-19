package com.hrm.breeze.data.network

import com.hrm.breeze.data.llm.LlmMessage
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import io.ktor.utils.io.readUTF8Line
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

interface OpenAiCompatibleChatApi {
    fun streamChat(
        endpoint: String,
        apiToken: String?,
        modelId: String,
        messages: List<LlmMessage>,
        reasoningEnabled: Boolean = false,
    ): Flow<String>
}

class KtorOpenAiCompatibleChatApi(
    private val httpClient: HttpClient,
) : OpenAiCompatibleChatApi {
    override fun streamChat(
        endpoint: String,
        apiToken: String?,
        modelId: String,
        messages: List<LlmMessage>,
        reasoningEnabled: Boolean,
    ): Flow<String> = flow {
        val response = httpClient.post(endpoint.toChatCompletionsUrl()) {
            if (!apiToken.isNullOrBlank()) {
                header(HttpHeaders.Authorization, "Bearer $apiToken")
            }
            header(HttpHeaders.Accept, ContentType.Text.EventStream.toString())
            setBody(
                OpenAiCompatibleChatRequest(
                    model = modelId,
                    messages = messages.map(LlmMessage::toNetwork),
                    stream = true,
                    reasoning = if (reasoningEnabled) OpenAiCompatibleReasoningRequest(enabled = true) else null,
                )
            )
        }

        if (!response.status.isSuccess()) {
            val responseText = response.bodyAsText()
            throw OpenAiCompatibleApiException(
                statusCode = response.status.value,
                statusDescription = response.status.description,
                serviceMessage = responseText.extractServiceMessage(),
            )
        }

        val channel = response.bodyAsChannel()
        val eventPayload = mutableListOf<String>()

        while (true) {
            val line = channel.readUTF8Line() ?: break
            when {
                line.startsWith("data:") -> {
                    val data = line.removePrefix("data:").trim()
                    if (data == "[DONE]") {
                        break
                    }
                    if (data.isNotEmpty()) {
                        eventPayload += data
                    }
                }

                line.isBlank() -> {
                    eventPayload.consumeAsDeltaOrNull()?.let { emit(it) }
                }
            }
        }

        eventPayload.consumeAsDeltaOrNull()?.let { emit(it) }
    }
}

private fun String.toChatCompletionsUrl(): String {
    val normalized = trim().trimEnd('/')
    return if (normalized.endsWith("/chat/completions")) {
        normalized
    } else {
        "$normalized/chat/completions"
    }
}

private fun LlmMessage.toNetwork(): OpenAiCompatibleMessage =
    OpenAiCompatibleMessage(
        role = role.name.lowercase(),
        content = content,
    )

private fun String.extractServiceMessage(): String? {
    if (isBlank()) return null
    return runCatching {
        val root = BreezeJson.parseToJsonElement(this).jsonObject
        root["error"].extractNestedMessage()
            ?: root["message"]?.jsonPrimitive?.contentOrNull
            ?: root["detail"]?.jsonPrimitive?.contentOrNull
    }.getOrNull() ?: trim()
}

private fun JsonElement?.extractNestedMessage(): String? {
    val obj = this?.jsonObject ?: return null
    return obj["message"]?.jsonPrimitive?.contentOrNull
        ?: obj["metadata"]?.jsonObject?.get("raw")?.jsonPrimitive?.contentOrNull
}

private fun MutableList<String>.consumeAsDeltaOrNull(): String? {
    if (isEmpty()) return null
    val payload = joinToString(separator = "\n")
    clear()
    return payload.extractStreamDeltaOrNull()
}

private fun String.extractStreamDeltaOrNull(): String? =
    runCatching {
        BreezeJson.decodeFromString<OpenAiCompatibleChatStreamResponse>(this)
            .choices
            .joinToString(separator = "") { it.delta?.content.orEmpty() }
            .takeIf(String::isNotEmpty)
    }.getOrNull()

class OpenAiCompatibleApiException(
    val statusCode: Int,
    val statusDescription: String,
    val serviceMessage: String?,
) : IllegalStateException(buildMessage(statusCode, statusDescription, serviceMessage))

private fun buildMessage(
    statusCode: Int,
    statusDescription: String,
    serviceMessage: String?,
): String = buildString {
    append(statusCode)
    append(' ')
    append(statusDescription)
    if (!serviceMessage.isNullOrBlank()) {
        append(": ")
        append(serviceMessage)
    }
}

@Serializable
private data class OpenAiCompatibleChatRequest(
    val model: String,
    val messages: List<OpenAiCompatibleMessage>,
    val stream: Boolean = false,
    val reasoning: OpenAiCompatibleReasoningRequest? = null,
)

@Serializable
private data class OpenAiCompatibleMessage(
    val role: String,
    val content: String,
)

@Serializable
private data class OpenAiCompatibleReasoningRequest(
    val enabled: Boolean,
)

@Serializable
private data class OpenAiCompatibleChatStreamResponse(
    val choices: List<OpenAiCompatibleStreamChoice> = emptyList(),
)

@Serializable
private data class OpenAiCompatibleStreamChoice(
    val delta: OpenAiCompatibleStreamDelta? = null,
)

@Serializable
private data class OpenAiCompatibleStreamDelta(
    val content: String? = null,
)
