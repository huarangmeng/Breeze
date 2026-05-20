package com.hrm.breeze.data.llm

import com.hrm.breeze.data.llm.ondevice.OnDeviceModelRepository
import com.hrm.breeze.data.llm.ondevice.OnDeviceRuntimeManager
import com.hrm.breeze.data.network.OpenAiCompatibleChatApi
import com.hrm.breeze.domain.model.LlmProviderId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow

class LocalProvider(
    private val onDeviceModelRepository: OnDeviceModelRepository,
    private val runtimeManager: OnDeviceRuntimeManager,
    private val chatApi: OpenAiCompatibleChatApi,
) : LlmProvider {
    override val id: LlmProviderId = LlmProviderId.Local

    override fun stream(request: LlmCompletionRequest): Flow<LlmStreamDelta> = flow {
        val readyModel = onDeviceModelRepository.ensureCurrentModelReady(request.contextWindow)
        val endpoint =
            runtimeManager.requireEndpoint(
                modelId = readyModel.preset.id,
                localPath = readyModel.localPath,
                contextWindow = request.contextWindow,
            )
        emitAll(
            chatApi.streamChat(
                endpoint = endpoint,
                apiToken = null,
                modelId = readyModel.preset.id,
                messages = request.messages,
                reasoningEnabled = request.model.reasoningEnabled,
                temperature = request.temperature,
                topP = request.topP,
                maxTokens = request.maxTokens,
            )
        )
    }
}
