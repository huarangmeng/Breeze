package com.hrm.breeze.data.llm

import com.hrm.breeze.domain.model.LlmProviderId
import com.hrm.breeze.domain.model.ModelProfile
import kotlinx.coroutines.flow.Flow

data class LlmCompletionRequest(
    val conversationId: String,
    val messages: List<LlmMessage>,
    val model: ModelProfile,
    val temperature: Float = 0.7f,
    val topP: Float = 0.9f,
    val maxTokens: Int = 2048,
    val contextWindow: Int = 2048,
)

data class LlmMessage(
    val role: Role,
    val content: String,
) {
    enum class Role { System, User, Assistant }
}

data class LlmStreamDelta(
    val contentDelta: String = "",
    val reasoningDelta: String = "",
) {
    val isEmpty: Boolean
        get() = contentDelta.isEmpty() && reasoningDelta.isEmpty()
}

interface LlmProvider {
    val id: LlmProviderId

    fun stream(request: LlmCompletionRequest): Flow<LlmStreamDelta>
}
