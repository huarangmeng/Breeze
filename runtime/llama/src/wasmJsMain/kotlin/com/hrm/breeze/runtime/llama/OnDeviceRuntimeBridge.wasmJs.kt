package com.hrm.breeze.runtime.llama

import com.hrm.breeze.domain.model.InferenceRuntimeState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

internal actual fun createOnDeviceRuntimeBridge(): OnDeviceRuntimeBridge =
    object : OnDeviceRuntimeBridge {
        override suspend fun ensureModelReady(request: OnDeviceRuntimeLaunchRequest): InferenceRuntimeState =
            InferenceRuntimeState.Failed

        override fun streamCompletion(request: OnDeviceRuntimeRequest): Flow<String> = flow {
            error("llama.cpp runtime is not implemented on Wasm yet")
        }
    }
