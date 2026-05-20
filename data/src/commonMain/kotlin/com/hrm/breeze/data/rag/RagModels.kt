package com.hrm.breeze.data.rag

data class RetrievedChunk(
    val chunkId: String,
    val content: String,
    val score: Float,
    val sourceType: String,
    val sourceId: String?,
)

data class RetrievedContext(
    val chunks: List<RetrievedChunk>,
) {
    val isEmpty: Boolean
        get() = chunks.isEmpty()

    companion object {
        val Empty = RetrievedContext(emptyList())
    }
}

data class RetrievalScope(
    val sourceTypes: Set<String> = setOf(RagSourceTypeConversation),
    val topK: Int = 5,
    val minScore: Float = 0.05f,
)

const val RagSourceTypeConversation = "conversation"
