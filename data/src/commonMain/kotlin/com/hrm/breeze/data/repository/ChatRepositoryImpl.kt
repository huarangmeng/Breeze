package com.hrm.breeze.data.repository

import com.hrm.breeze.core.coroutines.AppDispatchers
import com.hrm.breeze.core.coroutines.defaultAppDispatchers
import com.hrm.breeze.data.llm.LlmCompletionRequest
import com.hrm.breeze.data.llm.LlmMessage
import com.hrm.breeze.data.llm.LlmProviderRegistry
import com.hrm.breeze.data.settings.BreezeSettings
import com.hrm.breeze.data.storage.BreezeDatabase
import com.hrm.breeze.data.storage.entity.ConversationEntity
import com.hrm.breeze.data.storage.entity.MessageEntity
import com.hrm.breeze.data.storage.entity.toDomain
import com.hrm.breeze.domain.model.Conversation
import com.hrm.breeze.domain.model.Message
import com.hrm.breeze.domain.model.ModelProfile
import com.hrm.breeze.domain.repository.ChatRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlin.time.Clock

class ChatRepositoryImpl(
    private val database: BreezeDatabase,
    private val llmProviderRegistry: LlmProviderRegistry,
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
        val providerId = settings.getCurrentProviderId()
        val modelId = settings.getCurrentModelId()
        val messageDao = database.messageDao()
        val modelProfile =
            ModelProfile(
                id = modelId,
                providerId = providerId,
                displayName = modelId,
            )

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
        val historyMessages =
            messageDao.getMessages(conversationId).map(MessageEntity::toLlmMessage) +
                userMessage.toLlmMessage()
        messageDao.insertMessage(userMessage)

        val assistantText =
            llmProviderRegistry.require(modelProfile.providerId).complete(
                LlmCompletionRequest(
                    conversationId = conversationId,
                    messages = historyMessages,
                    model = modelProfile,
                )
            )
        val assistantTime = clock.now()
        val assistantMessage = MessageEntity(
            id = "$conversationId-assistant-${assistantTime.toEpochMilliseconds()}",
            conversationId = conversationId,
            role = "assistant",
            content = assistantText,
            createdAtEpochMillis = assistantTime.toEpochMilliseconds(),
        )
        messageDao.insertMessage(assistantMessage)
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

private fun MessageEntity.toLlmMessage(): LlmMessage =
    LlmMessage(
        role = when (role) {
            "assistant" -> LlmMessage.Role.Assistant
            "system" -> LlmMessage.Role.System
            else -> LlmMessage.Role.User
        },
        content = content,
    )
