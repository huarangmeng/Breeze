package com.hrm.breeze.data.repository

import com.hrm.breeze.core.coroutines.AppDispatchers
import com.hrm.breeze.core.coroutines.defaultAppDispatchers
import com.hrm.breeze.core.logging.Log
import com.hrm.breeze.data.conversation.ConversationContextManager
import com.hrm.breeze.data.llm.LlmCompletionRequest
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
import com.hrm.breeze.domain.repository.ModelConfigRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlin.time.Clock

private const val CHAT_REPOSITORY_LOG_TAG = "ChatRepository"

class ChatRepositoryImpl(
    private val database: BreezeDatabase,
    private val llmProviderRegistry: LlmProviderRegistry,
    private val modelConfigRepository: ModelConfigRepository,
    private val settings: BreezeSettings,
    private val conversationContextManager: ConversationContextManager,
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

    override fun sendMessage(
        conversationId: String,
        text: String,
        reasoningEnabled: Boolean,
    ): Flow<Message> = flow {
        val now = clock.now()
        val title = text.trim().ifBlank { "新对话" }.take(32)
        val activeConfig = modelConfigRepository.getActiveModelConfig()
            ?: error("No active model config selected")
        val messageDao = database.messageDao()
        val conversationDao = database.conversationDao()
        val settingsSnapshot = settings.snapshot.first()
        val modelProfile =
            ModelProfile(
                id = activeConfig.modelId,
                providerId = activeConfig.providerId,
                displayName = activeConfig.modelId,
                endpoint = activeConfig.endpoint,
                apiToken = activeConfig.apiToken,
                reasoningEnabled = reasoningEnabled,
            )

        conversationDao.upsertConversation(
            ConversationEntity(
                id = conversationId,
                title = title,
                modelId = activeConfig.modelId,
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
        val persistedHistoryMessages = messageDao.getMessages(conversationId)
        val conversationContext =
            conversationContextManager.prepareBeforeSend(
                conversationId = conversationId,
                historyMessages = persistedHistoryMessages,
                currentUserMessage = userMessage,
                contextWindow = settingsSnapshot.contextWindow,
                maxTokens = settingsSnapshot.maxTokens,
            )
        messageDao.insertMessage(userMessage)

        val assistantTime = clock.now()
        val assistantMessageId = "$conversationId-assistant-${assistantTime.toEpochMilliseconds()}"
        val request =
            LlmCompletionRequest(
                conversationId = conversationId,
                messages = conversationContext.requestMessages,
                model = modelProfile,
                temperature = settingsSnapshot.temperature,
                topP = settingsSnapshot.topP,
                maxTokens = settingsSnapshot.maxTokens,
                contextWindow = settingsSnapshot.contextWindow,
            )
        val provider = llmProviderRegistry.require(modelProfile.providerId)
        var assistantText = ""
        var reasoningText = ""

        Log.i(CHAT_REPOSITORY_LOG_TAG) {
            "Sending message conversationId=$conversationId provider=${modelProfile.providerId} model=${modelProfile.id} endpoint=${modelProfile.endpoint.orEmpty()}"
        }
        var finalAssistantMessage: MessageEntity? = null
        try {
            provider.stream(request).collect { delta ->
                if (delta.isEmpty) {
                    return@collect
                }

                assistantText += delta.contentDelta
                reasoningText += delta.reasoningDelta
                val assistantMessage =
                    MessageEntity(
                        id = assistantMessageId,
                        conversationId = conversationId,
                        role = "assistant",
                        content = assistantText,
                        reasoningContent = reasoningText.ifBlank { null },
                        createdAtEpochMillis = assistantTime.toEpochMilliseconds(),
                    )
                finalAssistantMessage = assistantMessage
                messageDao.insertMessage(assistantMessage)
                conversationDao.upsertConversation(
                    ConversationEntity(
                        id = conversationId,
                        title = title,
                        modelId = activeConfig.modelId,
                        updatedAtEpochMillis = assistantTime.toEpochMilliseconds(),
                    )
                )
                emit(assistantMessage.toDomain())
            }
        } catch (throwable: Throwable) {
            Log.e(CHAT_REPOSITORY_LOG_TAG, throwable) {
                "Failed to complete message send conversationId=$conversationId provider=${modelProfile.providerId} model=${modelProfile.id}"
            }
            throw throwable
        }

        if (assistantText.isEmpty() && reasoningText.isEmpty()) {
            error("LLM stream finished without assistant content")
        }

        conversationDao.upsertConversation(
            ConversationEntity(
                id = conversationId,
                title = title,
                modelId = activeConfig.modelId,
                updatedAtEpochMillis = assistantTime.toEpochMilliseconds(),
            )
        )

        conversationContextManager.refreshAfterAssistantFinished(
            conversationId = conversationId,
            context = conversationContext,
            userMessage = userMessage,
            assistantMessage = finalAssistantMessage,
            provider = provider,
            model = modelProfile,
            temperature = settingsSnapshot.temperature,
            topP = settingsSnapshot.topP,
            maxTokens = settingsSnapshot.maxTokens,
            contextWindow = settingsSnapshot.contextWindow,
        )
    }.flowOn(dispatchers.io)
}
