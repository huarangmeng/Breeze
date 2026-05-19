package com.hrm.breeze.data.llm

import com.hrm.breeze.data.network.OpenAiCompatibleChatApi
import com.hrm.breeze.data.settings.BreezeSettings
import com.hrm.breeze.domain.model.LlmProviderId

class OpenAiCompatibleProvider(
    override val id: LlmProviderId,
    private val settings: BreezeSettings,
    private val chatApi: OpenAiCompatibleChatApi,
) : LlmProvider {
    override suspend fun complete(request: LlmCompletionRequest): String =
        chatApi.completeChat(
            endpoint = settings.getEchoEndpoint(),
            apiToken = settings.getApiToken(),
            modelId = request.model.id,
            messages = request.messages,
        )
}
