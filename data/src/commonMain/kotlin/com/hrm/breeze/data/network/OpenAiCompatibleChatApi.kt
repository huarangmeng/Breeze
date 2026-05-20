package com.hrm.breeze.data.network

import com.hrm.breeze.core.logging.Log
import com.hrm.breeze.data.llm.LlmStreamDelta
import com.hrm.breeze.data.llm.LlmMessage
import io.ktor.client.HttpClient
import io.ktor.client.plugins.sse.sse
import io.ktor.client.plugins.sse.SSEClientException
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.header
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.isSuccess
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.takeWhile
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

private const val STREAM_LOG_TAG = "OpenAiChatStream"

interface OpenAiCompatibleChatApi {
    fun streamChat(
        endpoint: String,
        apiToken: String?,
        modelId: String,
        messages: List<LlmMessage>,
        reasoningEnabled: Boolean = false,
        temperature: Float = 0.7f,
        topP: Float = 0.9f,
        maxTokens: Int = 2048,
    ): Flow<LlmStreamDelta>
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
        temperature: Float,
        topP: Float,
        maxTokens: Int,
    ): Flow<LlmStreamDelta> = flow {
        val thinkTagSplitter = ThinkTagStreamSplitter()
        val chatUrl = endpoint.toChatCompletionsUrl()
        Log.i(STREAM_LOG_TAG) {
            "Starting SSE chat request url=$chatUrl model=$modelId messages=${messages.size} reasoning=$reasoningEnabled"
        }
        try {
            httpClient.sse(chatUrl, request = {
                applyChatRequest(
                    apiToken = apiToken,
                    modelId = modelId,
                    messages = messages,
                    reasoningEnabled = reasoningEnabled,
                    temperature = temperature,
                    topP = topP,
                    maxTokens = maxTokens,
                )
            }) {
                if (!call.response.status.isSuccess()) {
                    val responseText = call.response.bodyAsText()
                    Log.w(STREAM_LOG_TAG) {
                        "SSE chat request failed status=${call.response.status.value} description=${call.response.status.description} body=$responseText"
                    }
                    throw OpenAiCompatibleApiException(
                        statusCode = call.response.status.value,
                        statusDescription = call.response.status.description,
                        serviceMessage = responseText.extractServiceMessage(),
                    )
                }

                incoming
                    .takeWhile { event ->
                        val data = event.data?.trim().orEmpty()
                        if (data.isEmpty()) {
                            true
                        } else {
                            Log.d(STREAM_LOG_TAG) { "SSE data: $data" }
                            data != "[DONE]"
                        }
                    }.collect { event ->
                        val data = event.data?.trim().orEmpty()
                        if (data.isEmpty()) return@collect
                        data.extractStreamDeltaOrNull()
                            ?.let(thinkTagSplitter::consume)
                            ?.also(::logStreamDelta)
                            ?.takeUnless(LlmStreamDelta::isEmpty)
                            ?.let { emit(it) }
                    }
            }
        } catch (throwable: Throwable) {
            val mappedThrowable = throwable.toOpenAiCompatibleThrowable()
            if (mappedThrowable is OpenAiCompatibleApiException) {
                Log.w(STREAM_LOG_TAG) {
                    "SSE chat request failed status=${mappedThrowable.statusCode} description=${mappedThrowable.statusDescription} message=${mappedThrowable.serviceMessage.orEmpty()}"
                }
            }
            Log.e(STREAM_LOG_TAG, throwable) {
                "SSE chat request aborted url=$chatUrl model=$modelId"
            }
            throw mappedThrowable
        }
        thinkTagSplitter.finish()
            .also(::logStreamDelta)
            .takeUnless(LlmStreamDelta::isEmpty)
            ?.let { emit(it) }
    }
}

private fun HttpRequestBuilder.applyChatRequest(
    apiToken: String?,
    modelId: String,
    messages: List<LlmMessage>,
    reasoningEnabled: Boolean,
    temperature: Float,
    topP: Float,
    maxTokens: Int,
) {
    method = HttpMethod.Post
    if (!apiToken.isNullOrBlank()) {
        header(HttpHeaders.Authorization, "Bearer $apiToken")
    }
    header(HttpHeaders.Accept, ContentType.Text.EventStream.toString())
    setBody(
        OpenAiCompatibleChatRequest(
            model = modelId,
            messages = messages.map(LlmMessage::toNetwork),
            stream = true,
            temperature = temperature,
            topP = topP,
            maxTokens = maxTokens,
            reasoning = if (reasoningEnabled) OpenAiCompatibleReasoningRequest(enabled = true) else null,
        )
    )
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

private suspend fun Throwable.toOpenAiCompatibleThrowable(): Throwable {
    val sseException = this as? SSEClientException ?: return this
    val response = sseException.response ?: return this
    val responseText = runCatching { response.bodyAsText() }.getOrNull()
    val serviceMessage = responseText?.extractServiceMessage()
    return OpenAiCompatibleApiException(
        statusCode = response.status.value,
        statusDescription = response.status.description,
        serviceMessage = serviceMessage,
    )
}

private fun JsonElement?.extractNestedMessage(): String? {
    val obj = this?.jsonObject ?: return null
    return obj["message"]?.jsonPrimitive?.contentOrNull
        ?: obj["metadata"]?.jsonObject?.get("raw")?.jsonPrimitive?.contentOrNull
}

private fun logStreamDelta(delta: LlmStreamDelta) {
    Log.d(STREAM_LOG_TAG) {
        "SSE delta: content='${delta.contentDelta}' reasoning='${delta.reasoningDelta}'"
    }
}

internal class ThinkTagStreamSplitter {
    private var mode: SegmentMode = SegmentMode.Content
    private var pending: String = ""

    fun consume(delta: LlmStreamDelta): LlmStreamDelta {
        if (delta.contentDelta.isEmpty()) {
            return delta
        }
        val split = consumeContent(delta.contentDelta)
        return LlmStreamDelta(
            contentDelta = split.contentDelta,
            reasoningDelta = delta.reasoningDelta + split.reasoningDelta,
        )
    }

    fun finish(): LlmStreamDelta {
        if (pending.isEmpty()) {
            return LlmStreamDelta()
        }
        val tail = pending
        pending = ""
        return when (mode) {
            SegmentMode.Content -> LlmStreamDelta(contentDelta = tail)
            SegmentMode.Reasoning -> LlmStreamDelta(reasoningDelta = tail)
        }
    }

    private fun consumeContent(chunk: String): LlmStreamDelta {
        val input = pending + chunk
        pending = ""
        var index = 0
        val content = StringBuilder()
        val reasoning = StringBuilder()
        while (index < input.length) {
            val marker = mode.marker
            val markerIndex = input.indexOf(marker, startIndex = index)
            if (markerIndex >= 0) {
                appendSegment(
                    source = input,
                    startIndex = index,
                    endIndex = markerIndex,
                    content = content,
                    reasoning = reasoning,
                )
                mode = mode.next()
                index = markerIndex + marker.length
                continue
            }

            val remainder = input.substring(index)
            val carryLength = remainder.longestTagPrefixSuffix(marker)
            val safeEnd = input.length - carryLength
            appendSegment(
                source = input,
                startIndex = index,
                endIndex = safeEnd,
                content = content,
                reasoning = reasoning,
            )
            pending = input.substring(safeEnd)
            break
        }
        return LlmStreamDelta(
            contentDelta = content.toString(),
            reasoningDelta = reasoning.toString(),
        )
    }

    private fun appendSegment(
        source: String,
        startIndex: Int,
        endIndex: Int,
        content: StringBuilder,
        reasoning: StringBuilder,
    ) {
        if (startIndex >= endIndex) return
        when (mode) {
            SegmentMode.Content -> content.append(source, startIndex, endIndex)
            SegmentMode.Reasoning -> reasoning.append(source, startIndex, endIndex)
        }
    }
}

private enum class SegmentMode(
    val marker: String,
) {
    Content("<think>"),
    Reasoning("</think>"),
    ;

    fun next(): SegmentMode = when (this) {
        Content -> Reasoning
        Reasoning -> Content
    }
}

private fun String.longestTagPrefixSuffix(tag: String): Int {
    val maxLength = minOf(length, tag.length - 1)
    for (size in maxLength downTo 1) {
        if (endsWith(tag.take(size))) {
            return size
        }
    }
    return 0
}

private fun String.extractStreamDeltaOrNull(): LlmStreamDelta? =
    runCatching {
        val response = BreezeJson.decodeFromString<OpenAiCompatibleChatStreamResponse>(this)
        response.error?.let { throw OpenAiCompatibleStreamException(it.message) }
        response.choices.fold(
            initial = LlmStreamDelta(),
        ) { acc, choice ->
            val delta = choice.delta
            LlmStreamDelta(
                contentDelta = acc.contentDelta + delta?.content.orEmpty(),
                reasoningDelta = acc.reasoningDelta + delta?.reasoning.orEmpty(),
            )
        }.takeUnless(LlmStreamDelta::isEmpty)
    }.onFailure { throwable ->
        Log.w(STREAM_LOG_TAG, throwable) { "Failed to parse SSE payload: $this" }
    }.getOrNull()

class OpenAiCompatibleApiException(
    val statusCode: Int,
    val statusDescription: String,
    val serviceMessage: String?,
) : IllegalStateException(buildMessage(statusCode, statusDescription, serviceMessage))

class OpenAiCompatibleStreamException(
    message: String,
) : IllegalStateException(message)

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
    val stream: Boolean = true,
    @SerialName("temperature")
    val temperature: Float? = null,
    @SerialName("top_p")
    val topP: Float? = null,
    @SerialName("max_tokens")
    val maxTokens: Int? = null,
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
    val error: OpenAiCompatibleStreamError? = null,
)

@Serializable
private data class OpenAiCompatibleStreamChoice(
    val delta: OpenAiCompatibleStreamDelta? = null,
)

@Serializable
private data class OpenAiCompatibleStreamDelta(
    val content: String? = null,
    val reasoning: String? = null,
)

@Serializable
private data class OpenAiCompatibleStreamError(
    val message: String,
)
