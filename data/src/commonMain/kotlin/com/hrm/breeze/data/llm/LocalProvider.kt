package com.hrm.breeze.data.llm

import com.hrm.breeze.data.network.BreezeChatApi
import com.hrm.breeze.domain.model.LlmProviderId

class LocalProvider(
    private val chatApi: BreezeChatApi,
) : LlmProvider {
    override val id: LlmProviderId = LlmProviderId.Local

    override suspend fun complete(request: LlmCompletionRequest): String =
        chatApi.echoMessage(
            conversationId = request.conversationId,
            text = request.text,
            modelId = request.model.id,
        )
}
