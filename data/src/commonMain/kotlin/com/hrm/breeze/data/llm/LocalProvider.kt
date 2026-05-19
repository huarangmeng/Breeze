package com.hrm.breeze.data.llm

import com.hrm.breeze.domain.model.LlmProviderId

class LocalProvider : LlmProvider {
    override val id: LlmProviderId = LlmProviderId.Local

    override suspend fun complete(request: LlmCompletionRequest): String {
        val latestUserMessage =
            request.messages.lastOrNull { it.role == LlmMessage.Role.User }?.content.orEmpty()

        return "Breeze local(${request.model.id}): $latestUserMessage"
    }
}
