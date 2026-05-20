package com.hrm.breeze.runtime.api

import com.hrm.breeze.domain.model.InferenceRuntimeState
import kotlinx.coroutines.flow.Flow

interface OnDeviceRuntime {
    suspend fun ensureModelReady(
        modelId: String,
        localPath: String?,
        contextWindow: Int,
    ): InferenceRuntimeState

    fun streamCompletion(
        modelId: String,
        localPath: String?,
        messages: List<InferenceMessage>,
        temperature: Float,
        topP: Float,
        maxTokens: Int,
        contextWindow: Int,
    ): Flow<String>
}

data class InferenceMessage(
    val role: Role,
    val content: String,
) {
    enum class Role { System, User, Assistant }
}
