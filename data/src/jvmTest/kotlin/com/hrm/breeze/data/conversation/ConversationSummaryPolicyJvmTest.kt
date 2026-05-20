package com.hrm.breeze.data.conversation

import com.hrm.breeze.data.storage.entity.ConversationSummaryEntity
import com.hrm.breeze.data.storage.entity.MessageEntity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ConversationSummaryPolicyJvmTest {
    @Test
    fun shortConversationDoesNotNeedSummary() {
        val policy = ConversationSummaryPolicy()
        val messages = (1..4).map { index -> message(index) }

        assertFalse(
            policy.shouldSummarize(
                messages = messages,
                existingSummary = null,
                contextWindow = 4096,
                maxTokens = 512,
            )
        )
    }

    @Test
    fun manyUncoveredMessagesNeedSummary() {
        val policy = ConversationSummaryPolicy()
        val messages = (1..36).map { index -> message(index) }

        assertTrue(
            policy.shouldSummarize(
                messages = messages,
                existingSummary = null,
                contextWindow = 4096,
                maxTokens = 512,
            )
        )
    }

    @Test
    fun messagesToSummarizeKeepsRecentRawTail() {
        val policy = ConversationSummaryPolicy()
        val messages = (1..20).map { index -> message(index) }
        val summary =
            ConversationSummaryEntity(
                conversationId = "c1",
                summary = "already covered",
                coveredUntilMessageCreatedAtEpochMillis = 4,
                coveredUntilMessageId = "m4",
                updatedAtEpochMillis = 100,
            )

        val result = policy.messagesToSummarize(messages, summary)

        assertEquals("m5", result.first().id)
        assertEquals("m12", result.last().id)
    }

    private fun message(index: Int): MessageEntity =
        MessageEntity(
            id = "m$index",
            conversationId = "c1",
            role = if (index % 2 == 0) "assistant" else "user",
            content = "message-$index " + "x".repeat(80),
            createdAtEpochMillis = index.toLong(),
        )
}
