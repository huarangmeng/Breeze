package com.hrm.breeze.data.llm.ondevice

import com.hrm.breeze.data.platform.BreezeModelPaths
import com.hrm.breeze.data.platform.createBreezeModelPaths
import com.hrm.breeze.data.llm.LlmMessage
import com.hrm.breeze.domain.model.InferenceRuntimeState
import kotlinx.coroutines.flow.Flow

class OnDeviceRuntimeManager(
    modelPaths: BreezeModelPaths = createBreezeModelPaths(),
) {
    private val bridge = createOnDeviceRuntimeBridge(modelPaths)

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
        messages: List<LlmMessage>,
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
    val messages: List<LlmMessage>,
    val temperature: Float,
    val topP: Float,
    val maxTokens: Int,
    val contextWindow: Int,
)

internal interface OnDeviceRuntimeBridge {
    suspend fun ensureModelReady(request: OnDeviceRuntimeLaunchRequest): InferenceRuntimeState

    fun streamCompletion(request: OnDeviceRuntimeRequest): Flow<String>
}

internal expect fun createOnDeviceRuntimeBridge(
    modelPaths: BreezeModelPaths,
): OnDeviceRuntimeBridge
