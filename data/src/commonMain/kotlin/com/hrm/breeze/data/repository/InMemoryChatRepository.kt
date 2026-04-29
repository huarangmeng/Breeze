package com.hrm.breeze.data.repository

import com.hrm.breeze.domain.model.Conversation
import com.hrm.breeze.domain.model.Message
import com.hrm.breeze.domain.repository.ChatRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * 占位实现：M3 里替换为真正基于 Ktor/SQLDelight 的实现。
 */
class InMemoryChatRepository : ChatRepository {
    override fun observeConversations(): Flow<List<Conversation>> = flowOf(emptyList())
    override fun observeMessages(conversationId: String): Flow<List<Message>> = flowOf(emptyList())
    override fun sendMessage(conversationId: String, text: String): Flow<Message> = flowOf()
}
