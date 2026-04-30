package com.hrm.breeze.data.llm

import com.hrm.breeze.domain.model.LlmProviderId
import com.hrm.breeze.domain.model.ModelProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

data class LlmCompletionRequest(
    val conversationId: String,
    val text: String,
    val model: ModelProfile,
)

interface LlmProvider {
    val id: LlmProviderId

    suspend fun complete(request: LlmCompletionRequest): String

    fun stream(request: LlmCompletionRequest): Flow<String> =
        flow {
            emit(complete(request))
        }
}
