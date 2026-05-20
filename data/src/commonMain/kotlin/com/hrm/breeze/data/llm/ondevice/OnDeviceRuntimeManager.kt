package com.hrm.breeze.data.llm.ondevice

import com.hrm.breeze.data.platform.BreezeModelPaths
import com.hrm.breeze.data.platform.createBreezeModelPaths
import com.hrm.breeze.domain.model.InferenceRuntimeState
import io.ktor.client.HttpClient

class OnDeviceRuntimeManager(
    httpClient: HttpClient,
    modelPaths: BreezeModelPaths = createBreezeModelPaths(),
) {
    private val bridge = createOnDeviceRuntimeBridge(httpClient, modelPaths)

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

    suspend fun requireEndpoint(
        modelId: String,
        localPath: String?,
        contextWindow: Int,
    ): String =
        bridge.requireEndpoint(
            OnDeviceRuntimeLaunchRequest(
                modelId = modelId,
                localPath = localPath,
                contextWindow = contextWindow,
            )
        )
}

internal data class OnDeviceRuntimeLaunchRequest(
    val modelId: String,
    val localPath: String?,
    val contextWindow: Int,
)

internal interface OnDeviceRuntimeBridge {
    suspend fun ensureModelReady(request: OnDeviceRuntimeLaunchRequest): InferenceRuntimeState

    suspend fun requireEndpoint(request: OnDeviceRuntimeLaunchRequest): String
}

internal expect fun createOnDeviceRuntimeBridge(
    httpClient: HttpClient,
    modelPaths: BreezeModelPaths,
): OnDeviceRuntimeBridge
