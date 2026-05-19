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
        val latestUserMessage =
            request.messages.lastOrNull { it.role == LlmMessage.Role.User }?.content.orEmpty()
        val readyModel = onDeviceModelRepository.ensureCurrentModelReady()
        emitAll(
            runtimeManager.streamCompletion(
                modelId = readyModel.preset.id,
                prompt = latestUserMessage,
            ).map { chunk -> LlmStreamDelta(contentDelta = chunk) }
        )
    }
}
