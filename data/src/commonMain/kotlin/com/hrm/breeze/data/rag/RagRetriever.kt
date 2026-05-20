package com.hrm.breeze.data.rag

import com.hrm.breeze.core.logging.Log
import com.hrm.breeze.data.embedding.EmbeddingProvider

private const val RAG_RETRIEVER_LOG_TAG = "RagRetriever"

interface RagRetriever {
    suspend fun retrieve(query: String, scope: RetrievalScope = RetrievalScope()): RetrievedContext
}

class HybridRagRetriever(
    private val embeddingProvider: EmbeddingProvider,
    private val ragStore: RagStore,
    private val lexicalIndex: LexicalIndex = LexicalIndex(),
) : RagRetriever {
    override suspend fun retrieve(query: String, scope: RetrievalScope): RetrievedContext {
        if (query.isBlank()) return RetrievedContext.Empty
        return if (embeddingProvider.isReady()) {
            runCatching { retrieveByEmbedding(query, scope) }
                .getOrElse { throwable ->
                    Log.w(RAG_RETRIEVER_LOG_TAG, throwable) {
                        "Embedding retrieval failed; falling back to lexical retrieval"
                    }
                    retrieveByLexical(query, scope)
                }
        } else {
            retrieveByLexical(query, scope)
        }
    }

    private suspend fun retrieveByEmbedding(query: String, scope: RetrievalScope): RetrievedContext {
        val queryVector = embeddingProvider.embed(listOf(query)).singleOrNull() ?: return RetrievedContext.Empty
        val embeddings = ragStore.getEmbeddings(embeddingProvider.modelId)
        if (embeddings.isEmpty()) return retrieveByLexical(query, scope)

        val chunkScores =
            embeddings
                .map { embedding ->
                    embedding.chunkId to RagVectorCodec.cosine(queryVector, RagVectorCodec.decode(embedding.vector))
                }
                .filter { (_, score) -> score >= scope.minScore }
                .sortedByDescending { (_, score) -> score }
                .take(scope.topK)
        val chunksById = ragStore.getChunks(chunkScores.map { (chunkId, _) -> chunkId }).associateBy { it.id }
        return RetrievedContext(
            chunks =
                chunkScores.mapNotNull { (chunkId, score) ->
                    chunksById[chunkId]?.let { chunk ->
                        RetrievedChunk(
                            chunkId = chunk.id,
                            content = chunk.content,
                            score = score,
                            sourceType = RagSourceTypeConversation,
                            sourceId = chunk.documentId,
                        )
                    }
                }
        )
    }

    private suspend fun retrieveByLexical(query: String, scope: RetrievalScope): RetrievedContext {
        val chunksById = ragStore.getAllChunks().associateBy { chunk -> chunk.id }
        val scored =
            ragStore.getLexicalIndices()
                .mapNotNull { index ->
                    val chunk = chunksById[index.chunkId] ?: return@mapNotNull null
                    val score = lexicalIndex.score(query, index.terms, chunk.content)
                    if (score >= scope.minScore) {
                        RetrievedChunk(
                            chunkId = chunk.id,
                            content = chunk.content,
                            score = score,
                            sourceType = RagSourceTypeConversation,
                            sourceId = chunk.documentId,
                        )
                    } else {
                        null
                    }
                }
                .sortedByDescending { chunk -> chunk.score }
                .take(scope.topK)
        return RetrievedContext(scored)
    }
}
