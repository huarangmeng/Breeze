package com.hrm.breeze.data.conversation

import com.hrm.breeze.data.storage.entity.ConversationSummaryEntity
import com.hrm.breeze.data.storage.entity.MessageEntity

class ConversationSummaryPolicy(
    private val contextAssembler: ConversationContextAssembler = ConversationContextAssembler(),
    private val tokenEstimator: ConversationTokenEstimator = ConversationTokenEstimator(),
) {
    fun shouldSummarize(
        messages: List<MessageEntity>,
        existingSummary: ConversationSummaryEntity?,
        contextWindow: Int,
        maxTokens: Int,
    ): Boolean {
        val uncoveredMessages = messages.uncoveredBy(existingSummary)
        val summarizableMessages = uncoveredMessages.dropLast(RECENT_RAW_MESSAGES_TO_KEEP)
        if (summarizableMessages.isEmpty()) return false

        val uncoveredTokens = tokenEstimator.estimate(uncoveredMessages.map(MessageEntity::toLlmMessage))
        val fullHistoryTokens = tokenEstimator.estimate(messages.map(MessageEntity::toLlmMessage))
        val inputBudget = contextAssembler.inputBudget(contextWindow = contextWindow, maxTokens = maxTokens)

        return uncoveredTokens > (contextWindow * UNCOVERED_CONTEXT_RATIO).toInt() ||
            fullHistoryTokens > inputBudget ||
            uncoveredMessages.size > MAX_UNCOVERED_MESSAGES
    }

    fun messagesToSummarize(
        messages: List<MessageEntity>,
        existingSummary: ConversationSummaryEntity?,
    ): List<MessageEntity> =
        messages.uncoveredBy(existingSummary).dropLast(RECENT_RAW_MESSAGES_TO_KEEP)

    private fun List<MessageEntity>.uncoveredBy(
        existingSummary: ConversationSummaryEntity?,
    ): List<MessageEntity> {
        val coveredUntil = existingSummary?.coveredUntilMessageCreatedAtEpochMillis ?: Long.MIN_VALUE
        return filter { message -> message.createdAtEpochMillis > coveredUntil }
    }

    companion object {
        const val RECENT_RAW_MESSAGES_TO_KEEP = 8
        private const val UNCOVERED_CONTEXT_RATIO = 0.45f
        private const val MAX_UNCOVERED_MESSAGES = 24
    }
}
