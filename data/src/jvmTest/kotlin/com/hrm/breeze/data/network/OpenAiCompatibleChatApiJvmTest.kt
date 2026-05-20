package com.hrm.breeze.data.network

import com.hrm.breeze.data.llm.LlmStreamDelta
import kotlin.test.Test
import kotlin.test.assertEquals

class OpenAiCompatibleChatApiJvmTest {
    @Test
    fun splitterMovesThinkBlockIntoReasoningWithinSingleChunk() {
        val splitter = ThinkTagStreamSplitter()

        val delta = splitter.consume(LlmStreamDelta(contentDelta = "Answer<think>plan</think> done"))

        assertEquals("Answer done", delta.contentDelta)
        assertEquals("plan", delta.reasoningDelta)
        assertEquals(LlmStreamDelta(), splitter.finish())
    }

    @Test
    fun splitterMovesThinkBlockIntoReasoningAcrossChunks() {
        val splitter = ThinkTagStreamSplitter()

        assertEquals(LlmStreamDelta(), splitter.consume(LlmStreamDelta(contentDelta = "<th")))
        assertEquals(
            LlmStreamDelta(reasoningDelta = "先分析"),
            splitter.consume(LlmStreamDelta(contentDelta = "ink>先分析")),
        )
        assertEquals(LlmStreamDelta(), splitter.consume(LlmStreamDelta(contentDelta = "</th")))
        assertEquals(
            LlmStreamDelta(contentDelta = "最终答案"),
            splitter.consume(LlmStreamDelta(contentDelta = "ink>最终答案")),
        )
        assertEquals(LlmStreamDelta(), splitter.finish())
    }

    @Test
    fun splitterPreservesExplicitReasoningField() {
        val splitter = ThinkTagStreamSplitter()

        val delta = splitter.consume(
            LlmStreamDelta(
                contentDelta = "正文",
                reasoningDelta = "独立推理",
            )
        )

        assertEquals("正文", delta.contentDelta)
        assertEquals("独立推理", delta.reasoningDelta)
    }

    @Test
    fun splitterFlushesIncompleteTagFragmentsOnFinish() {
        val splitter = ThinkTagStreamSplitter()

        val delta = splitter.consume(LlmStreamDelta(contentDelta = "Hello <thi"))

        assertEquals("Hello ", delta.contentDelta)
        assertEquals("", delta.reasoningDelta)
        assertEquals(LlmStreamDelta(contentDelta = "<thi"), splitter.finish())
    }
}
