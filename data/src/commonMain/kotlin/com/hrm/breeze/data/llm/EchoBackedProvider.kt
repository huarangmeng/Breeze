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
    override suspend fun complete(request: LlmCompletionRequest): String =
        chatApi.echoMessage(
            conversationId = request.conversationId,
            text = request.text,
            modelId = request.model.id,
        )
}
