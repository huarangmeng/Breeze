package com.hrm.breeze.data.network

import com.hrm.breeze.data.llm.LlmMessage
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

interface OpenAiCompatibleChatApi {
    suspend fun completeChat(
        endpoint: String,
        apiToken: String?,
        modelId: String,
        messages: List<LlmMessage>,
    ): String
}

class KtorOpenAiCompatibleChatApi(
    private val httpClient: HttpClient,
) : OpenAiCompatibleChatApi {
    override suspend fun completeChat(
        endpoint: String,
        apiToken: String?,
        modelId: String,
        messages: List<LlmMessage>,
    ): String {
        val response = httpClient.post(endpoint.toChatCompletionsUrl()) {
            if (!apiToken.isNullOrBlank()) {
                header(HttpHeaders.Authorization, "Bearer $apiToken")
            }
            setBody(
                OpenAiCompatibleChatRequest(
                    model = modelId,
                    messages = messages.map(LlmMessage::toNetwork),
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

        val responseBody = response.body<OpenAiCompatibleChatResponse>()

        return responseBody.choices.firstOrNull()?.message?.content?.takeIf(String::isNotBlank)
            ?: error("OpenAI-compatible response did not contain a text message")
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
)

@Serializable
private data class OpenAiCompatibleMessage(
    val role: String,
    val content: String,
)

@Serializable
private data class OpenAiCompatibleChatResponse(
    val choices: List<OpenAiCompatibleChoice> = emptyList(),
)

@Serializable
private data class OpenAiCompatibleChoice(
    val message: OpenAiCompatibleResponseMessage? = null,
)

@Serializable
private data class OpenAiCompatibleResponseMessage(
    val content: String? = null,
    @SerialName("role") val role: String? = null,
)
