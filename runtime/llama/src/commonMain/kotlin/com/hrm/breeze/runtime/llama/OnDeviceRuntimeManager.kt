package com.hrm.breeze.runtime.llama

import com.hrm.breeze.domain.model.InferenceRuntimeState
import com.hrm.breeze.runtime.api.InferenceMessage
import com.hrm.breeze.runtime.api.OnDeviceRuntimeBackend
import com.hrm.breeze.runtime.api.OnDeviceRuntimeCapability
import com.hrm.breeze.runtime.api.OnDeviceRuntimeCompletionRequest
import com.hrm.breeze.runtime.api.OnDeviceRuntimeFamily
import com.hrm.breeze.runtime.api.OnDeviceRuntimeLaunchRequest
import com.hrm.breeze.runtime.api.OnDeviceRuntime
import com.hrm.breeze.runtime.api.OnDeviceRuntimeTargetPlatform
import kotlinx.coroutines.flow.Flow

class LlamaOnDeviceRuntime : OnDeviceRuntime {
    private val bridge = createOnDeviceRuntimeBridge()

    override val capability: OnDeviceRuntimeCapability
        get() = bridge.capability

    override suspend fun ensureModelReady(request: OnDeviceRuntimeLaunchRequest): InferenceRuntimeState =
        bridge.ensureModelReady(request.normalized())

    override fun streamCompletion(request: OnDeviceRuntimeCompletionRequest): Flow<String> =
        bridge.streamCompletion(request.normalized())
}

internal interface OnDeviceRuntimeBridge {
    val capability: OnDeviceRuntimeCapability

    suspend fun ensureModelReady(request: OnDeviceRuntimeLaunchRequest): InferenceRuntimeState

    fun streamCompletion(request: OnDeviceRuntimeCompletionRequest): Flow<String>
}

internal expect fun createOnDeviceRuntimeBridge(): OnDeviceRuntimeBridge

internal fun OnDeviceRuntimeLaunchRequest.normalized(): OnDeviceRuntimeLaunchRequest {
    require(contextWindow > 0) { "Context window must be positive" }
    require(modelId.isNotBlank()) { "Model id must not be blank" }
    requireLocalPath()
    return copy(localPath = localPath)
}

internal fun OnDeviceRuntimeCompletionRequest.normalized(): OnDeviceRuntimeCompletionRequest {
    require(contextWindow > 0) { "Context window must be positive" }
    require(maxTokens > 0) { "Max tokens must be positive" }
    require(modelId.isNotBlank()) { "Model id must not be blank" }
    require(messages.isNotEmpty()) { "Messages must not be empty" }
    requireLocalPath()
    return copy(localPath = localPath)
}

internal fun OnDeviceRuntimeLaunchRequest.requireLocalPath(): String =
    checkNotNull(localPath?.takeIf(String::isNotBlank)) { "Missing local model file path" }

internal fun OnDeviceRuntimeCompletionRequest.requireLocalPath(): String =
    checkNotNull(localPath?.takeIf(String::isNotBlank)) { "Missing local model file path" }

internal fun OnDeviceRuntimeCompletionRequest.toLaunchRequest(): OnDeviceRuntimeLaunchRequest =
    OnDeviceRuntimeLaunchRequest(
        modelId = modelId,
        localPath = requireLocalPath(),
        contextWindow = contextWindow,
    )

internal fun desktopJvmLlamaCapability(
    defaultBackend: OnDeviceRuntimeBackend,
    targetPlatforms: Set<OnDeviceRuntimeTargetPlatform> =
        setOf(
            OnDeviceRuntimeTargetPlatform.MacOs,
            OnDeviceRuntimeTargetPlatform.Windows,
        ),
    supportedBackends: Set<OnDeviceRuntimeBackend> = setOf(OnDeviceRuntimeBackend.Cpu, defaultBackend),
): OnDeviceRuntimeCapability =
    OnDeviceRuntimeCapability.desktopJvm(
        defaultBackend = defaultBackend,
        targetPlatforms = targetPlatforms,
        supportedBackends = supportedBackends,
        supportsModelPersistence = true,
    )

internal fun unsupportedLlamaCapability(
    reason: String,
    family: OnDeviceRuntimeFamily = OnDeviceRuntimeFamily.Unsupported,
    targetPlatforms: Set<OnDeviceRuntimeTargetPlatform> = emptySet(),
    supportsModelPersistence: Boolean = false,
): OnDeviceRuntimeCapability =
    OnDeviceRuntimeCapability.unsupported(
        reason = reason,
        family = family,
        targetPlatforms = targetPlatforms,
        supportsModelPersistence = supportsModelPersistence,
    )
