package com.hrm.breeze.data.llm

import com.hrm.breeze.data.network.OpenAiCompatibleChatApi
import com.hrm.breeze.data.settings.BreezeSettings
import com.hrm.breeze.domain.model.LlmProviderId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList

class OpenAiCompatibleProvider(
    override val id: LlmProviderId,
    private val settings: BreezeSettings,
    private val chatApi: OpenAiCompatibleChatApi,
) : LlmProvider {
    override suspend fun complete(request: LlmCompletionRequest): String =
        stream(request).toList().joinToString(separator = "")

    override fun stream(request: LlmCompletionRequest): Flow<String> = flow {
        val endpoint = settings.getEchoEndpoint()
        val apiToken = settings.getApiToken()
        emitAll(
            chatApi.streamChat(
                endpoint = endpoint,
                apiToken = apiToken,
                modelId = request.model.id,
                messages = request.messages,
                reasoningEnabled = settings.getReasoningEnabled(),
            )
        )
    }
}
