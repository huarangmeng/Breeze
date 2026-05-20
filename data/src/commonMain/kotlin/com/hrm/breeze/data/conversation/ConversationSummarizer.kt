package com.hrm.breeze.data.conversation

import com.hrm.breeze.core.logging.Log
import com.hrm.breeze.data.llm.LlmCompletionRequest
import com.hrm.breeze.data.llm.LlmMessage
import com.hrm.breeze.data.llm.LlmProvider
import com.hrm.breeze.data.storage.dao.ConversationSummaryDao
import com.hrm.breeze.data.storage.entity.ConversationSummaryEntity
import com.hrm.breeze.data.storage.entity.MessageEntity
import com.hrm.breeze.domain.model.ModelProfile
import kotlinx.coroutines.flow.collect
import kotlin.time.Clock

private const val CONVERSATION_SUMMARIZER_LOG_TAG = "ConversationSummarizer"

class ConversationSummarizer(
    private val summaryDao: ConversationSummaryDao,
    private val policy: ConversationSummaryPolicy = ConversationSummaryPolicy(),
    private val tokenEstimator: ConversationTokenEstimator = ConversationTokenEstimator(),
    private val clock: Clock = Clock.System,
) {
    suspend fun refreshIfNeeded(
        conversationId: String,
        messages: List<MessageEntity>,
        existingSummary: ConversationSummaryEntity?,
        provider: LlmProvider,
        model: ModelProfile,
        temperature: Float,
        topP: Float,
        maxTokens: Int,
        contextWindow: Int,
    ) {
        if (!policy.shouldSummarize(messages, existingSummary, contextWindow, maxTokens)) {
            return
        }

        val messagesToSummarize = policy.messagesToSummarize(messages, existingSummary)
        if (messagesToSummarize.isEmpty()) return

        var rollingSummary = existingSummary?.summary.orEmpty()
        val summaryModel = model.copy(reasoningEnabled = false)
        val chunkBudget = (contextWindow - SUMMARY_MAX_TOKENS - SUMMARY_SAFETY_MARGIN_TOKENS)
            .coerceAtLeast(MINIMUM_SUMMARY_INPUT_BUDGET)

        for (chunk in messagesToSummarize.chunkByEstimatedTokens(chunkBudget)) {
            val generatedSummary = generateSummary(
                conversationId = conversationId,
                currentSummary = rollingSummary,
                messages = chunk,
                provider = provider,
                model = summaryModel,
                temperature = temperature,
                topP = topP,
                contextWindow = contextWindow,
            )
            if (generatedSummary.isBlank()) {
                Log.w(CONVERSATION_SUMMARIZER_LOG_TAG) {
                    "Skipping blank summary update conversationId=$conversationId"
                }
                return
            }
            rollingSummary = generatedSummary.trim()
        }

        val coveredUntil = messagesToSummarize.last()
        summaryDao.upsertSummary(
            ConversationSummaryEntity(
                conversationId = conversationId,
                summary = rollingSummary,
                coveredUntilMessageCreatedAtEpochMillis = coveredUntil.createdAtEpochMillis,
                coveredUntilMessageId = coveredUntil.id,
                updatedAtEpochMillis = clock.now().toEpochMilliseconds(),
            )
        )
    }

    private suspend fun generateSummary(
        conversationId: String,
        currentSummary: String,
        messages: List<MessageEntity>,
        provider: LlmProvider,
        model: ModelProfile,
        temperature: Float,
        topP: Float,
        contextWindow: Int,
    ): String {
        var summaryText = ""
        provider.stream(
            LlmCompletionRequest(
                conversationId = "$conversationId-summary",
                messages = buildSummaryPrompt(currentSummary, messages),
                model = model,
                temperature = temperature,
                topP = topP,
                maxTokens = SUMMARY_MAX_TOKENS,
                contextWindow = contextWindow,
            )
        ).collect { delta ->
            summaryText += delta.contentDelta
        }
        return summaryText
    }

    private fun buildSummaryPrompt(
        currentSummary: String,
        messages: List<MessageEntity>,
    ): List<LlmMessage> =
        listOf(
            LlmMessage(
                role = LlmMessage.Role.System,
                content =
                    "You maintain a compact conversation summary for future context. " +
                        "Preserve confirmed facts, user preferences, current goals, constraints, " +
                        "open questions, and important code or file references. Do not invent details. " +
                        "Return only the updated summary.",
            ),
            LlmMessage(
                role = LlmMessage.Role.User,
                content = buildString {
                    append("Existing summary:\n")
                    append(currentSummary.ifBlank { "(none)" })
                    append("\n\nNew transcript to fold into the summary:\n")
                    messages.forEach { message ->
                        append(message.role)
                        append(": ")
                        append(message.content)
                        append('\n')
                    }
                },
            ),
        )

    private fun List<MessageEntity>.chunkByEstimatedTokens(budget: Int): List<List<MessageEntity>> {
        val chunks = mutableListOf<List<MessageEntity>>()
        var current = mutableListOf<MessageEntity>()

        for (message in this) {
            val candidate = current + message
            val candidateTokens = tokenEstimator.estimate(candidate.map(MessageEntity::toLlmMessage))
            if (current.isNotEmpty() && candidateTokens > budget) {
                chunks += current
                current = mutableListOf(message)
            } else {
                current.add(message)
            }
        }

        if (current.isNotEmpty()) {
            chunks += current
        }
        return chunks
    }

    private companion object {
        const val SUMMARY_MAX_TOKENS = 512
        const val SUMMARY_SAFETY_MARGIN_TOKENS = 256
        const val MINIMUM_SUMMARY_INPUT_BUDGET = 512
    }
}
