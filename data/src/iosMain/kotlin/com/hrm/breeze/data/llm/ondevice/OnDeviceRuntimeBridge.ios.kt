package com.hrm.breeze.data.llm.ondevice

import com.hrm.breeze.data.platform.BreezeModelPaths
import com.hrm.breeze.domain.model.InferenceRuntimeState
import io.ktor.client.HttpClient

internal actual fun createOnDeviceRuntimeBridge(
    httpClient: HttpClient,
    modelPaths: BreezeModelPaths,
): OnDeviceRuntimeBridge =
    object : OnDeviceRuntimeBridge {
        override suspend fun ensureModelReady(request: OnDeviceRuntimeLaunchRequest): InferenceRuntimeState =
            InferenceRuntimeState.Failed

        override suspend fun requireEndpoint(request: OnDeviceRuntimeLaunchRequest): String =
            error("llama.cpp runtime is not implemented on iOS yet")
    }
