package com.hrm.breeze.data.repository

import com.hrm.breeze.core.coroutines.AppDispatchers
import com.hrm.breeze.core.coroutines.defaultAppDispatchers
import com.hrm.breeze.data.network.BreezeChatApi
import com.hrm.breeze.data.settings.BreezeSettings
import com.hrm.breeze.data.storage.BreezeDatabase
import com.hrm.breeze.data.storage.entity.ConversationEntity
import com.hrm.breeze.data.storage.entity.MessageEntity
import com.hrm.breeze.data.storage.entity.toDomain
import com.hrm.breeze.domain.model.Conversation
import com.hrm.breeze.domain.model.Message
import com.hrm.breeze.domain.repository.ChatRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlin.time.Clock

class ChatRepositoryImpl(
    private val database: BreezeDatabase,
    private val chatApi: BreezeChatApi,
    private val settings: BreezeSettings,
    private val dispatchers: AppDispatchers = defaultAppDispatchers(),
    private val clock: Clock = Clock.System,
) : ChatRepository {
    override fun observeConversations(): Flow<List<Conversation>> =
        database.conversationDao()
            .observeConversations()
            .map { items -> items.map(ConversationEntity::toDomain) }

    override fun observeMessages(conversationId: String): Flow<List<Message>> =
        database.messageDao()
            .observeMessages(conversationId)
            .map { items -> items.map(MessageEntity::toDomain) }

    override fun sendMessage(conversationId: String, text: String): Flow<Message> = flow {
        val now = clock.now()
        val title = text.trim().ifBlank { "新对话" }.take(32)
        val modelId = settings.getCurrentModelId()

        database.conversationDao().upsertConversation(
            ConversationEntity(
                id = conversationId,
                title = title,
                modelId = modelId,
                updatedAtEpochMillis = now.toEpochMilliseconds(),
            )
        )

        val userMessage = MessageEntity(
            id = "$conversationId-user-${now.toEpochMilliseconds()}",
            conversationId = conversationId,
            role = "user",
            content = text,
            createdAtEpochMillis = now.toEpochMilliseconds(),
        )
        database.messageDao().insertMessage(userMessage)

        val assistantText = chatApi.echoMessage(
            conversationId = conversationId,
            text = text,
            modelId = modelId,
        )
        val assistantTime = clock.now()
        val assistantMessage = MessageEntity(
            id = "$conversationId-assistant-${assistantTime.toEpochMilliseconds()}",
            conversationId = conversationId,
            role = "assistant",
            content = assistantText,
            createdAtEpochMillis = assistantTime.toEpochMilliseconds(),
        )
        database.messageDao().insertMessage(assistantMessage)
        database.conversationDao().upsertConversation(
            ConversationEntity(
                id = conversationId,
                title = title,
                modelId = modelId,
                updatedAtEpochMillis = assistantTime.toEpochMilliseconds(),
            )
        )

        emit(assistantMessage.toDomain())
    }.flowOn(dispatchers.io)
}
