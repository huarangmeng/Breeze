package com.hrm.breeze.data.conversation

import com.hrm.breeze.data.llm.LlmMessage

class ConversationTokenEstimator(
    private val messageOverheadTokens: Int = 6,
) {
    fun estimate(messages: List<LlmMessage>): Int =
        messages.sumOf(::estimate)

    fun estimate(message: LlmMessage): Int =
        estimateText(message.content) + messageOverheadTokens

    fun estimateText(text: String): Int {
        if (text.isBlank()) return 1

        val nonAsciiChars = text.count { char -> char.code > 127 }
        val asciiChars = text.length - nonAsciiChars
        val asciiTokens = (asciiChars + ASCII_CHARS_PER_TOKEN - 1) / ASCII_CHARS_PER_TOKEN
        return (asciiTokens + nonAsciiChars).coerceAtLeast(1)
    }

    private companion object {
        const val ASCII_CHARS_PER_TOKEN = 4
    }
}
