package com.hrm.breeze.runtime.api

import com.hrm.breeze.domain.model.InferenceRuntimeState
import kotlinx.coroutines.flow.Flow

interface OnDeviceRuntime {
    val capability: OnDeviceRuntimeCapability
        get() =
            OnDeviceRuntimeCapability.unsupported(
                reason = "On-device runtime is not available on this platform",
            )

    suspend fun ensureModelReady(request: OnDeviceRuntimeLaunchRequest): InferenceRuntimeState

    fun streamCompletion(request: OnDeviceRuntimeCompletionRequest): Flow<String>

    suspend fun ensureModelReady(
        modelId: String,
        localPath: String?,
        contextWindow: Int,
    ): InferenceRuntimeState =
        ensureModelReady(
            OnDeviceRuntimeLaunchRequest(
                modelId = modelId,
                localPath = localPath,
                contextWindow = contextWindow,
            )
        )

    fun streamCompletion(
        modelId: String,
        localPath: String?,
        messages: List<InferenceMessage>,
        temperature: Float,
        topP: Float,
        maxTokens: Int,
        contextWindow: Int,
    ): Flow<String> =
        streamCompletion(
            OnDeviceRuntimeCompletionRequest(
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

interface OnDeviceEmbeddingRuntime {
    val capability: OnDeviceRuntimeCapability
        get() =
            OnDeviceRuntimeCapability.unsupported(
                reason = "On-device embedding runtime is not available on this platform",
            )

    suspend fun ensureEmbeddingModelReady(request: EmbeddingRuntimeLaunchRequest): InferenceRuntimeState

    suspend fun embed(request: EmbeddingRuntimeRequest): List<EmbeddingVector>
}

data class OnDeviceRuntimeLaunchRequest(
    val modelId: String,
    val localPath: String?,
    val contextWindow: Int,
)

data class OnDeviceRuntimeCompletionRequest(
    val modelId: String,
    val localPath: String?,
    val messages: List<InferenceMessage>,
    val temperature: Float,
    val topP: Float,
    val maxTokens: Int,
    val contextWindow: Int,
)

data class EmbeddingRuntimeLaunchRequest(
    val modelId: String,
    val localPath: String?,
    val contextWindow: Int,
)

data class EmbeddingRuntimeRequest(
    val modelId: String,
    val localPath: String,
    val inputs: List<String>,
    val normalize: Boolean = true,
    val contextWindow: Int = 8192,
)

data class EmbeddingVector(
    val textIndex: Int,
    val values: FloatArray,
)

data class OnDeviceRuntimeCapability(
    val isAvailable: Boolean,
    val family: OnDeviceRuntimeFamily,
    val targetPlatforms: Set<OnDeviceRuntimeTargetPlatform> = emptySet(),
    val defaultBackend: OnDeviceRuntimeBackend = OnDeviceRuntimeBackend.Cpu,
    val supportedBackends: Set<OnDeviceRuntimeBackend> = setOf(OnDeviceRuntimeBackend.Cpu),
    val supportsModelPersistence: Boolean = false,
    val unavailableReason: String? = null,
) {
    companion object {
        fun desktopJvm(
            defaultBackend: OnDeviceRuntimeBackend,
            targetPlatforms: Set<OnDeviceRuntimeTargetPlatform> =
                setOf(
                    OnDeviceRuntimeTargetPlatform.MacOs,
                    OnDeviceRuntimeTargetPlatform.Windows,
                ),
            supportedBackends: Set<OnDeviceRuntimeBackend> = setOf(OnDeviceRuntimeBackend.Cpu, defaultBackend),
            supportsModelPersistence: Boolean = true,
        ): OnDeviceRuntimeCapability =
            OnDeviceRuntimeCapability(
                isAvailable = true,
                family = OnDeviceRuntimeFamily.DesktopJvm,
                targetPlatforms = targetPlatforms,
                defaultBackend = defaultBackend,
                supportedBackends = supportedBackends,
                supportsModelPersistence = supportsModelPersistence,
            )

        fun unsupported(
            reason: String,
            family: OnDeviceRuntimeFamily = OnDeviceRuntimeFamily.Unsupported,
            targetPlatforms: Set<OnDeviceRuntimeTargetPlatform> = emptySet(),
            supportsModelPersistence: Boolean = false,
        ): OnDeviceRuntimeCapability =
            OnDeviceRuntimeCapability(
                isAvailable = false,
                family = family,
                targetPlatforms = targetPlatforms,
                defaultBackend = OnDeviceRuntimeBackend.Cpu,
                supportedBackends = setOf(OnDeviceRuntimeBackend.Cpu),
                supportsModelPersistence = supportsModelPersistence,
                unavailableReason = reason,
            )
    }
}

enum class OnDeviceRuntimeFamily {
    DesktopJvm,
    AndroidJni,
    AppleNative,
    WebAssembly,
    Unsupported,
}

enum class OnDeviceRuntimeTargetPlatform {
    MacOs,
    Windows,
    Linux,
    Android,
    Ios,
    Web,
}

enum class OnDeviceRuntimeBackend {
    Cpu,
    Metal,
    Vulkan,
    Cuda,
    Hip,
    Sycl,
    OpenCl,
    WebGpu,
}

data class InferenceMessage(
    val role: Role,
    val content: String,
) {
    enum class Role { System, User, Assistant }
}
