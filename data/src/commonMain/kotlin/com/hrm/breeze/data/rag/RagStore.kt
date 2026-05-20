package com.hrm.breeze.data.rag

import com.hrm.breeze.data.storage.dao.RagChunkDao
import com.hrm.breeze.data.storage.dao.RagDocumentDao
import com.hrm.breeze.data.storage.entity.RagChunkEntity
import com.hrm.breeze.data.storage.entity.RagDocumentEntity
import com.hrm.breeze.data.storage.entity.RagEmbeddingEntity
import com.hrm.breeze.data.storage.entity.RagLexicalIndexEntity

class RagStore(
    private val documentDao: RagDocumentDao,
    private val chunkDao: RagChunkDao,
) {
    suspend fun upsertDocument(document: RagDocumentEntity) {
        documentDao.upsertDocument(document)
    }

    suspend fun upsertChunks(
        chunks: List<RagChunkEntity>,
        lexicalIndices: List<RagLexicalIndexEntity>,
    ) {
        if (chunks.isEmpty()) return
        chunkDao.upsertChunks(chunks)
        chunkDao.upsertLexicalIndices(lexicalIndices)
    }

    suspend fun upsertEmbeddings(embeddings: List<RagEmbeddingEntity>) {
        if (embeddings.isNotEmpty()) {
            chunkDao.upsertEmbeddings(embeddings)
        }
    }

    suspend fun getAllChunks(): List<RagChunkEntity> = chunkDao.getAllChunks()

    suspend fun getChunks(chunkIds: List<String>): List<RagChunkEntity> =
        if (chunkIds.isEmpty()) emptyList() else chunkDao.getChunks(chunkIds)

    suspend fun getLexicalIndices(): List<RagLexicalIndexEntity> = chunkDao.getLexicalIndices()

    suspend fun getEmbeddings(embeddingModelId: String): List<RagEmbeddingEntity> =
        chunkDao.getEmbeddings(embeddingModelId)

    suspend fun getEmbeddedChunkIds(embeddingModelId: String): Set<String> =
        chunkDao.getEmbeddedChunkIds(embeddingModelId).toSet()
}
