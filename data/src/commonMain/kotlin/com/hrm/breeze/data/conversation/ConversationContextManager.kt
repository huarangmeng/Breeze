package com.hrm.breeze.data.conversation

import com.hrm.breeze.core.logging.Log
import com.hrm.breeze.data.llm.LlmMessage
import com.hrm.breeze.data.llm.LlmProvider
import com.hrm.breeze.data.rag.RagContextProvider
import com.hrm.breeze.data.rag.RagIndexer
import com.hrm.breeze.data.storage.dao.ConversationSummaryDao
import com.hrm.breeze.data.storage.entity.ConversationSummaryEntity
import com.hrm.breeze.data.storage.entity.MessageEntity
import com.hrm.breeze.domain.model.ModelProfile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

private const val CONVERSATION_CONTEXT_MANAGER_LOG_TAG = "ConversationContextManager"

class ConversationContextManager(
    private val summaryDao: ConversationSummaryDao,
    private val contextAssembler: ConversationContextAssembler,
    private val conversationSummarizer: ConversationSummarizer,
    private val ragContextProvider: RagContextProvider,
    private val ragIndexer: RagIndexer,
    private val backgroundScope: CoroutineScope,
) {
    suspend fun prepareBeforeSend(
        conversationId: String,
        historyMessages: List<MessageEntity>,
        currentUserMessage: MessageEntity,
        contextWindow: Int,
        maxTokens: Int,
    ): ConversationContext {
        val existingSummary = summaryDao.getSummary(conversationId)
        val retrievedContext =
            runCatching { ragContextProvider.retrieveContext(currentUserMessage.content) }
                .getOrElse { throwable ->
                    Log.w(CONVERSATION_CONTEXT_MANAGER_LOG_TAG, throwable) {
                        "Failed to retrieve RAG context conversationId=$conversationId"
                    }
                    com.hrm.breeze.data.rag.RetrievedContext.Empty
                }
        val requestMessages =
            contextAssembler.assemble(
                historyMessages = historyMessages,
                currentUserMessage = currentUserMessage,
                summary = existingSummary,
                retrievedContext = retrievedContext,
                contextWindow = contextWindow,
                maxTokens = maxTokens,
            )
        return ConversationContext(
            requestMessages = requestMessages,
            persistedHistoryMessages = historyMessages,
            existingSummary = existingSummary,
        )
    }

    fun refreshAfterAssistantFinished(
        conversationId: String,
        context: ConversationContext,
        userMessage: MessageEntity,
        assistantMessage: MessageEntity?,
        provider: LlmProvider,
        model: ModelProfile,
        temperature: Float,
        topP: Float,
        maxTokens: Int,
        contextWindow: Int,
    ) {
        val summarizableMessages =
            context.persistedHistoryMessages + userMessage + listOfNotNull(assistantMessage)
        backgroundScope.launch {
            runCatching {
                conversationSummarizer.refreshIfNeeded(
                    conversationId = conversationId,
                    messages = summarizableMessages,
                    existingSummary = context.existingSummary,
                    provider = provider,
                    model = model,
                    temperature = temperature,
                    topP = topP,
                    maxTokens = maxTokens,
                    contextWindow = contextWindow,
                )
            }.onFailure { throwable ->
                Log.w(CONVERSATION_CONTEXT_MANAGER_LOG_TAG, throwable) {
                    "Failed to refresh conversation summary conversationId=$conversationId"
                }
            }
            runCatching {
                ragIndexer.indexConversationTurn(
                    conversationId = conversationId,
                    userMessage = userMessage,
                    assistantMessage = assistantMessage,
                )
                ragIndexer.backfillEmbeddingsIfReady()
            }.onFailure { throwable ->
                Log.w(CONVERSATION_CONTEXT_MANAGER_LOG_TAG, throwable) {
                    "Failed to refresh RAG index conversationId=$conversationId"
                }
            }
        }
    }
}

data class ConversationContext(
    val requestMessages: List<LlmMessage>,
    val persistedHistoryMessages: List<MessageEntity>,
    val existingSummary: ConversationSummaryEntity?,
)
