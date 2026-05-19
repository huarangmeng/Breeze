package com.hrm.breeze.data.llm

import com.hrm.breeze.data.network.BreezeChatApi
import com.hrm.breeze.domain.model.LlmProviderId

/**
 * Temporary bridge provider used before provider-specific adapters land.
 */
class EchoBackedProvider(
    override val id: LlmProviderId,
    private val chatApi: BreezeChatApi,
) : LlmProvider {
    override suspend fun complete(request: LlmCompletionRequest): String {
        val latestUserMessage =
            request.messages.lastOrNull { it.role == LlmMessage.Role.User }?.content.orEmpty()

        return chatApi.echoMessage(
            conversationId = request.conversationId,
            text = latestUserMessage,
            modelId = request.model.id,
        )
    }
}
