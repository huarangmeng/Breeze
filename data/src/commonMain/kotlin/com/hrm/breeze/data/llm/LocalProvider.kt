package com.hrm.breeze.data.llm

import com.hrm.breeze.data.llm.ondevice.OnDeviceModelRepository
import com.hrm.breeze.data.llm.ondevice.OnDeviceRuntimeManager
import com.hrm.breeze.domain.model.LlmProviderId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

class LocalProvider(
    private val onDeviceModelRepository: OnDeviceModelRepository,
    private val runtimeManager: OnDeviceRuntimeManager,
) : LlmProvider {
    override val id: LlmProviderId = LlmProviderId.Local

    override fun stream(request: LlmCompletionRequest): Flow<LlmStreamDelta> = flow {
        val readyModel = onDeviceModelRepository.ensureCurrentModelReady(request.contextWindow)
        emitAll(
            runtimeManager.streamCompletion(
                modelId = readyModel.preset.id,
                localPath = readyModel.localPath,
                messages = request.messages,
                temperature = request.temperature,
                topP = request.topP,
                maxTokens = request.maxTokens,
                contextWindow = request.contextWindow,
            ).map { chunk -> LlmStreamDelta(contentDelta = chunk) }
        )
    }
}
