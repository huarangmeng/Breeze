package com.hrm.breeze.data.llm

import com.hrm.breeze.data.network.OpenAiCompatibleChatApi
import com.hrm.breeze.domain.model.LlmProviderId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow

class OpenAiCompatibleProvider(
    override val id: LlmProviderId,
    private val chatApi: OpenAiCompatibleChatApi,
) : LlmProvider {
    override fun stream(request: LlmCompletionRequest): Flow<LlmStreamDelta> = flow {
        emitAll(
            chatApi.streamChat(
                endpoint = request.model.endpoint.orEmpty(),
                apiToken = request.model.apiToken,
                modelId = request.model.id,
                messages = request.messages,
                reasoningEnabled = request.model.reasoningEnabled,
                temperature = request.temperature,
                topP = request.topP,
                maxTokens = request.maxTokens,
            )
        )
    }
}
