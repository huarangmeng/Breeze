package com.hrm.breeze.data.conversation

import com.hrm.breeze.data.llm.LlmMessage
import com.hrm.breeze.data.rag.RetrievedContext
import com.hrm.breeze.data.storage.entity.ConversationSummaryEntity
import com.hrm.breeze.data.storage.entity.MessageEntity

class ConversationContextAssembler(
    private val tokenEstimator: ConversationTokenEstimator = ConversationTokenEstimator(),
) {
    fun assemble(
        historyMessages: List<MessageEntity>,
        currentUserMessage: MessageEntity,
        summary: ConversationSummaryEntity?,
        retrievedContext: RetrievedContext = RetrievedContext.Empty,
        contextWindow: Int,
        maxTokens: Int,
    ): List<LlmMessage> {
        val inputBudget = inputBudget(contextWindow = contextWindow, maxTokens = maxTokens)
        val summaryMessage = summary?.toSummaryMessage()
        val retrievedContextMessage = retrievedContext.toContextMessage()
        val historyContextMessages =
            historyMessages
                .filter { message ->
                    summary == null ||
                        message.createdAtEpochMillis > summary.coveredUntilMessageCreatedAtEpochMillis
                }
                .map(MessageEntity::toLlmMessage)
        val currentMessage = currentUserMessage.toLlmMessage()
        val selectedTurns = historyContextMessages.toConversationTurns().toMutableList()

        while (
            selectedTurns.isNotEmpty() &&
            tokenEstimator.estimate(
                summaryMessage.asList() +
                    retrievedContextMessage.asList() +
                    selectedTurns.flattenMessages() +
                    currentMessage
            ) > inputBudget
        ) {
            selectedTurns.removeAt(0)
        }

        return summaryMessage.asList() +
            retrievedContextMessage.asList() +
            selectedTurns.flattenMessages() +
            currentMessage
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

    private fun RetrievedContext.toContextMessage(): LlmMessage? {
        if (isEmpty) return null
        return LlmMessage(
            role = LlmMessage.Role.System,
            content = buildString {
                append("Relevant memory snippets:\n")
                chunks.forEachIndexed { index, chunk ->
                    append('[')
                    append(index + 1)
                    append("] source=")
                    append(chunk.sourceType)
                    append(" score=")
                    append(chunk.score)
                    append('\n')
                    append(chunk.content)
                    append("\n\n")
                }
                append("Use these snippets only when relevant. Do not invent facts not present in them.")
            },
        )
    }

    private fun LlmMessage?.asList(): List<LlmMessage> =
        if (this == null) emptyList() else listOf(this)

    private fun List<ConversationTurn>.flattenMessages(): List<LlmMessage> =
        flatMap { turn -> turn.messages }

    private fun List<LlmMessage>.toConversationTurns(): List<ConversationTurn> {
        val turns = mutableListOf<ConversationTurn>()
        var current = mutableListOf<LlmMessage>()

        for (message in this) {
            if (message.role == LlmMessage.Role.User && current.isNotEmpty()) {
                turns += ConversationTurn(current)
                current = mutableListOf()
            }
            current.add(message)
        }

        if (current.isNotEmpty()) {
            turns += ConversationTurn(current)
        }
        return turns
    }

    private companion object {
        const val SAFETY_MARGIN_TOKENS = 256
        const val MINIMUM_RECENT_BUDGET = 512
    }
}

private data class ConversationTurn(
    val messages: List<LlmMessage>,
)

fun MessageEntity.toLlmMessage(): LlmMessage =
    LlmMessage(
        role = when (role) {
            "assistant" -> LlmMessage.Role.Assistant
            "system" -> LlmMessage.Role.System
            else -> LlmMessage.Role.User
        },
        content = content,
    )
