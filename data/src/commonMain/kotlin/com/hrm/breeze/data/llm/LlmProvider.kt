package com.hrm.breeze.data.llm

import com.hrm.breeze.domain.model.LlmProviderId
import com.hrm.breeze.domain.model.ModelProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

data class LlmCompletionRequest(
    val conversationId: String,
    val messages: List<LlmMessage>,
    val model: ModelProfile,
)

data class LlmMessage(
    val role: Role,
    val content: String,
) {
    enum class Role { System, User, Assistant }
}

interface LlmProvider {
    val id: LlmProviderId

    suspend fun complete(request: LlmCompletionRequest): String

    fun stream(request: LlmCompletionRequest): Flow<String> =
        flow {
            emit(complete(request))
        }
}
