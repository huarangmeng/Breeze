package com.hrm.breeze.runtime.llama

import com.hrm.breeze.domain.model.InferenceRuntimeState
import com.hrm.breeze.runtime.api.OnDeviceRuntimeCompletionRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

internal actual fun createOnDeviceRuntimeBridge(): OnDeviceRuntimeBridge =
    object : OnDeviceRuntimeBridge {
        override val capability =
            unsupportedLlamaCapability(
                reason = "llama.cpp runtime is not implemented on iOS yet",
                family = com.hrm.breeze.runtime.api.OnDeviceRuntimeFamily.AppleNative,
                targetPlatforms = setOf(com.hrm.breeze.runtime.api.OnDeviceRuntimeTargetPlatform.Ios),
                supportsModelPersistence = true,
            )

        override suspend fun ensureModelReady(request: OnDeviceRuntimeLaunchRequest): InferenceRuntimeState =
            InferenceRuntimeState.Failed

        override fun streamCompletion(request: OnDeviceRuntimeCompletionRequest): Flow<String> = flow {
            error("llama.cpp runtime is not implemented on iOS yet")
        }
    }
