package com.hrm.breeze.runtime.llama

import com.hrm.breeze.domain.model.InferenceRuntimeState
import com.hrm.breeze.runtime.api.EmbeddingRuntimeLaunchRequest
import com.hrm.breeze.runtime.api.EmbeddingRuntimeRequest
import com.hrm.breeze.runtime.api.EmbeddingVector
import com.hrm.breeze.runtime.api.InferenceMessage
import com.hrm.breeze.runtime.api.OnDeviceEmbeddingRuntime
import com.hrm.breeze.runtime.api.OnDeviceRuntimeBackend
import com.hrm.breeze.runtime.api.OnDeviceRuntimeCapability
import com.hrm.breeze.runtime.api.OnDeviceRuntimeCompletionRequest
import com.hrm.breeze.runtime.api.OnDeviceRuntimeFamily
import com.hrm.breeze.runtime.api.OnDeviceRuntimeLaunchRequest
import com.hrm.breeze.runtime.api.OnDeviceRuntime
import com.hrm.breeze.runtime.api.OnDeviceRuntimeTargetPlatform
import kotlinx.coroutines.flow.Flow

class LlamaOnDeviceRuntime : OnDeviceRuntime, OnDeviceEmbeddingRuntime {
    private val bridge = createOnDeviceRuntimeBridge()

    override val capability: OnDeviceRuntimeCapability
        get() = bridge.capability

    override suspend fun ensureModelReady(request: OnDeviceRuntimeLaunchRequest): InferenceRuntimeState =
        bridge.ensureModelReady(request.normalized())

    override fun streamCompletion(request: OnDeviceRuntimeCompletionRequest): Flow<String> =
        bridge.streamCompletion(request.normalized())

    override suspend fun ensureEmbeddingModelReady(request: EmbeddingRuntimeLaunchRequest): InferenceRuntimeState =
        InferenceRuntimeState.Failed

    override suspend fun embed(request: EmbeddingRuntimeRequest): List<EmbeddingVector> =
        error("llama.cpp embedding runtime is not implemented yet")
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

internal fun List<InferenceMessage>.toChatMlPrompt(): String =
    buildString {
        val hasSystemMessage = this@toChatMlPrompt.any { message -> message.role == InferenceMessage.Role.System }
        if (!hasSystemMessage) {
            append("<|im_start|>system\n")
            append("You are Breeze, a helpful on-device assistant.\n")
            append("<|im_end|>\n")
        }
        for (message in this@toChatMlPrompt) {
            val role =
                when (message.role) {
                    InferenceMessage.Role.System -> "system"
                    InferenceMessage.Role.User -> "user"
                    InferenceMessage.Role.Assistant -> "assistant"
                }
            append("<|im_start|>")
            append(role)
            append('\n')
            append(message.content)
            append('\n')
            append("<|im_end|>\n")
        }
        append("<|im_start|>assistant\n")
    }

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
