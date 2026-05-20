package com.hrm.breeze.data.rag

import com.hrm.breeze.core.logging.Log
import com.hrm.breeze.data.conversation.ConversationTokenEstimator
import com.hrm.breeze.data.embedding.EmbeddingProvider
import com.hrm.breeze.data.storage.entity.MessageEntity
import com.hrm.breeze.data.storage.entity.RagChunkEntity
import com.hrm.breeze.data.storage.entity.RagDocumentEntity
import com.hrm.breeze.data.storage.entity.RagEmbeddingEntity
import com.hrm.breeze.data.storage.entity.RagLexicalIndexEntity
import kotlin.time.Clock

private const val RAG_INDEXER_LOG_TAG = "RagIndexer"

class RagIndexer(
    private val ragStore: RagStore,
    private val embeddingProvider: EmbeddingProvider,
    private val lexicalIndex: LexicalIndex = LexicalIndex(),
    private val tokenEstimator: ConversationTokenEstimator = ConversationTokenEstimator(),
    private val clock: Clock = Clock.System,
) {
    suspend fun indexConversationTurn(
        conversationId: String,
        userMessage: MessageEntity,
        assistantMessage: MessageEntity?,
    ) {
        val content = buildTurnContent(userMessage, assistantMessage)
        if (content.isBlank()) return

        val now = clock.now().toEpochMilliseconds()
        val documentId = "conversation-$conversationId"
        val chunkId = "conversation-$conversationId-${userMessage.id}"
        val document =
            RagDocumentEntity(
                id = documentId,
                sourceType = RagSourceTypeConversation,
                sourceId = conversationId,
                title = "Conversation $conversationId",
                createdAtEpochMillis = userMessage.createdAtEpochMillis,
                updatedAtEpochMillis = now,
            )
        val chunk =
            RagChunkEntity(
                id = chunkId,
                documentId = documentId,
                content = content,
                ordinal = userMessage.createdAtEpochMillis.toInt(),
                tokenEstimate = tokenEstimator.estimateText(content),
                metadataJson = null,
                createdAtEpochMillis = now,
            )
        ragStore.upsertDocument(document)
        ragStore.upsertChunks(
            chunks = listOf(chunk),
            lexicalIndices =
                listOf(
                    RagLexicalIndexEntity(
                        chunkId = chunkId,
                        terms = lexicalIndex.encodeTerms(content),
                        createdAtEpochMillis = now,
                    )
                ),
        )
        indexEmbeddingsIfReady(listOf(chunk), now)
    }

    suspend fun backfillEmbeddingsIfReady() {
        if (!embeddingProvider.isReady()) return
        val embeddedChunkIds = ragStore.getEmbeddedChunkIds(embeddingProvider.modelId)
        val chunks = ragStore.getAllChunks().filterNot { chunk -> chunk.id in embeddedChunkIds }
        indexEmbeddingsIfReady(chunks, clock.now().toEpochMilliseconds())
    }

    private suspend fun indexEmbeddingsIfReady(chunks: List<RagChunkEntity>, now: Long) {
        if (chunks.isEmpty() || !embeddingProvider.isReady()) return
        runCatching {
            val vectors = embeddingProvider.embed(chunks.map(RagChunkEntity::content))
            val embeddings =
                chunks.zip(vectors).map { (chunk, vector) ->
                    RagEmbeddingEntity(
                        chunkId = chunk.id,
                        embeddingModelId = embeddingProvider.modelId,
                        dimension = vector.size,
                        vector = RagVectorCodec.encode(vector),
                        normalized = true,
                        createdAtEpochMillis = now,
                    )
                }
            ragStore.upsertEmbeddings(embeddings)
        }.onFailure { throwable ->
            Log.w(RAG_INDEXER_LOG_TAG, throwable) {
                "Embedding index failed; lexical index remains available"
            }
        }
    }

    private fun buildTurnContent(
        userMessage: MessageEntity,
        assistantMessage: MessageEntity?,
    ): String =
        buildString {
            append("user: ")
            append(userMessage.content)
            if (assistantMessage != null && assistantMessage.content.isNotBlank()) {
                append('\n')
                append("assistant: ")
                append(assistantMessage.content)
            }
        }
}
