package com.hrm.breeze.data.conversation

import com.hrm.breeze.data.llm.LlmMessage
import com.hrm.breeze.data.storage.entity.ConversationSummaryEntity
import com.hrm.breeze.data.storage.entity.MessageEntity

class ConversationContextAssembler(
    private val tokenEstimator: ConversationTokenEstimator = ConversationTokenEstimator(),
) {
    fun assemble(
        historyMessages: List<MessageEntity>,
        currentUserMessage: MessageEntity,
        summary: ConversationSummaryEntity?,
        contextWindow: Int,
        maxTokens: Int,
    ): List<LlmMessage> {
        val inputBudget = inputBudget(contextWindow = contextWindow, maxTokens = maxTokens)
        val summaryMessage = summary?.toSummaryMessage()
        val rawMessages =
            historyMessages
                .filter { message ->
                    summary == null ||
                        message.createdAtEpochMillis > summary.coveredUntilMessageCreatedAtEpochMillis
                }
                .map(MessageEntity::toLlmMessage) + currentUserMessage.toLlmMessage()

        val selected = rawMessages.toMutableList()
        while (selected.size > 1 && tokenEstimator.estimate(summaryMessage.asList() + selected) > inputBudget) {
            selected.removeAt(0)
        }

        return summaryMessage.asList() + selected
    }

    fun inputBudget(contextWindow: Int, maxTokens: Int): Int {
        val usableContext = contextWindow.coerceAtLeast(MINIMUM_RECENT_BUDGET)
        val reservedOutput = maxTokens.coerceAtLeast(0)
        return (usableContext - reservedOutput - SAFETY_MARGIN_TOKENS)
            .coerceAtLeast(MINIMUM_RECENT_BUDGET)
    }

    private fun ConversationSummaryEntity.toSummaryMessage(): LlmMessage? {
        val normalized = summary.trim()
        if (normalized.isBlank()) return null
        return LlmMessage(
            role = LlmMessage.Role.System,
            content = "Conversation summary so far:\n$normalized",
        )
    }

    private fun LlmMessage?.asList(): List<LlmMessage> =
        if (this == null) emptyList() else listOf(this)

    private companion object {
        const val SAFETY_MARGIN_TOKENS = 256
        const val MINIMUM_RECENT_BUDGET = 512
    }
}

fun MessageEntity.toLlmMessage(): LlmMessage =
    LlmMessage(
        role = when (role) {
            "assistant" -> LlmMessage.Role.Assistant
            "system" -> LlmMessage.Role.System
            else -> LlmMessage.Role.User
        },
        content = content,
    )
