package com.hrm.breeze.runtime.llama

import com.hrm.breeze.domain.model.InferenceRuntimeState
import kotlinx.coroutines.flow.Flow

class OnDeviceRuntimeManager {
    private val bridge = createOnDeviceRuntimeBridge()

    suspend fun ensureModelReady(
        modelId: String,
        localPath: String?,
        contextWindow: Int,
    ): InferenceRuntimeState =
        bridge.ensureModelReady(
            OnDeviceRuntimeLaunchRequest(
                modelId = modelId,
                localPath = localPath,
                contextWindow = contextWindow,
            )
        )

    fun streamCompletion(
        modelId: String,
        localPath: String?,
        messages: List<LlamaMessage>,
        temperature: Float,
        topP: Float,
        maxTokens: Int,
        contextWindow: Int,
    ): Flow<String> =
        bridge.streamCompletion(
            OnDeviceRuntimeRequest(
                modelId = modelId,
                localPath = localPath,
                messages = messages,
                temperature = temperature,
                topP = topP,
                maxTokens = maxTokens,
                contextWindow = contextWindow,
            )
        )
}

internal data class OnDeviceRuntimeLaunchRequest(
    val modelId: String,
    val localPath: String?,
    val contextWindow: Int,
)

internal data class OnDeviceRuntimeRequest(
    val modelId: String,
    val localPath: String?,
    val messages: List<LlamaMessage>,
    val temperature: Float,
    val topP: Float,
    val maxTokens: Int,
    val contextWindow: Int,
)

data class LlamaMessage(
    val role: Role,
    val content: String,
) {
    enum class Role { System, User, Assistant }
}

internal interface OnDeviceRuntimeBridge {
    suspend fun ensureModelReady(request: OnDeviceRuntimeLaunchRequest): InferenceRuntimeState

    fun streamCompletion(request: OnDeviceRuntimeRequest): Flow<String>
}

internal expect fun createOnDeviceRuntimeBridge(): OnDeviceRuntimeBridge
