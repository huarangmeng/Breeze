package com.hrm.breeze.data.conversation

import com.hrm.breeze.data.llm.LlmMessage
import com.hrm.breeze.data.storage.entity.ConversationSummaryEntity
import com.hrm.breeze.data.storage.entity.MessageEntity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ConversationContextAssemblerJvmTest {
    @Test
    fun shortConversationKeepsFullHistoryAndCurrentMessage() {
        val assembler = ConversationContextAssembler()
        val history =
            listOf(
                message(id = "m1", role = "user", content = "hello", createdAt = 1),
                message(id = "m2", role = "assistant", content = "hi", createdAt = 2),
            )
        val current = message(id = "m3", role = "user", content = "what next?", createdAt = 3)

        val result =
            assembler.assemble(
                historyMessages = history,
                currentUserMessage = current,
                summary = null,
                contextWindow = 4096,
                maxTokens = 512,
            )

        assertEquals(listOf("hello", "hi", "what next?"), result.map { it.content })
    }

    @Test
    fun longConversationKeepsCurrentMessageAndTrimsOldestMessages() {
        val assembler = ConversationContextAssembler()
        val history =
            (1..30).map { index ->
                message(
                    id = "m$index",
                    role = if (index % 2 == 0) "assistant" else "user",
                    content = "message-$index " + "x".repeat(120),
                    createdAt = index.toLong(),
                )
            }
        val current = message(id = "current", role = "user", content = "final question", createdAt = 31)

        val result =
            assembler.assemble(
                historyMessages = history,
                currentUserMessage = current,
                summary = null,
                contextWindow = 768,
                maxTokens = 128,
            )

        assertEquals("final question", result.last().content)
        assertFalse(result.any { it.content.startsWith("message-1 ") })
        assertTrue(result.any { it.content.startsWith("message-30 ") })
    }

    @Test
    fun summaryReplacesCoveredMessagesBeforeRecentRawMessages() {
        val assembler = ConversationContextAssembler()
        val history =
            (1..12).map { index ->
                message(
                    id = "m$index",
                    role = if (index % 2 == 0) "assistant" else "user",
                    content = "message-$index",
                    createdAt = index.toLong(),
                )
            }
        val summary =
            ConversationSummaryEntity(
                conversationId = "c1",
                summary = "User wants a KMP chat app.",
                coveredUntilMessageCreatedAtEpochMillis = 8,
                coveredUntilMessageId = "m8",
                updatedAtEpochMillis = 20,
            )
        val current = message(id = "current", role = "user", content = "continue", createdAt = 13)

        val result =
            assembler.assemble(
                historyMessages = history,
                currentUserMessage = current,
                summary = summary,
                contextWindow = 4096,
                maxTokens = 512,
            )

        assertEquals(LlmMessage.Role.System, result.first().role)
        assertTrue(result.first().content.contains("User wants a KMP chat app."))
        assertFalse(result.any { it.content == "message-8" })
        assertTrue(result.any { it.content == "message-9" })
        assertEquals("continue", result.last().content)
    }

    private fun message(
        id: String,
        role: String,
        content: String,
        createdAt: Long,
    ): MessageEntity =
        MessageEntity(
            id = id,
            conversationId = "c1",
            role = role,
            content = content,
            reasoningContent = "hidden reasoning",
            createdAtEpochMillis = createdAt,
        )
}
