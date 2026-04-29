package com.hrm.breeze.domain.usecase

import com.hrm.breeze.domain.model.Message
import com.hrm.breeze.domain.repository.ChatRepository
import kotlinx.coroutines.flow.Flow

class SendMessageUseCase(
    private val repository: ChatRepository,
) {
    operator fun invoke(conversationId: String, text: String): Flow<Message> =
        repository.sendMessage(conversationId, text)
}
