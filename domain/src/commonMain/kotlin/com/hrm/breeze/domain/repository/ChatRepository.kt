package com.hrm.breeze.domain.repository

import com.hrm.breeze.domain.model.Conversation
import com.hrm.breeze.domain.model.Message
import kotlinx.coroutines.flow.Flow

/**
 * 聊天仓库接口。接口放在 :domain，实现放在 :data。
 */
interface ChatRepository {
    fun observeConversations(): Flow<List<Conversation>>
    fun observeMessages(conversationId: String): Flow<List<Message>>

    /**
     * 发送用户消息，返回助手端的流式响应。
     */
    fun sendMessage(conversationId: String, text: String): Flow<Message>
}
