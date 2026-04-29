package com.hrm.breeze.data.network

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

interface BreezeChatApi {
    suspend fun echoMessage(
        conversationId: String,
        text: String,
        modelId: String,
    ): String
}

class KtorBreezeChatApi(
    private val httpClient: HttpClient,
    private val endpointProvider: () -> String,
) : BreezeChatApi {
    override suspend fun echoMessage(
        conversationId: String,
        text: String,
        modelId: String,
    ): String {
        val response = httpClient.post(endpointProvider()) {
            setBody(
                ChatEchoRequest(
                    conversationId = conversationId,
                    text = text,
                    modelId = modelId,
                )
            )
        }.body<ChatEchoResponse>()
        return response.message
    }
}

@Serializable
data class ChatEchoRequest(
    @SerialName("conversation_id") val conversationId: String,
    val text: String,
    @SerialName("model_id") val modelId: String,
)

@Serializable
data class ChatEchoResponse(
    val message: String,
)
