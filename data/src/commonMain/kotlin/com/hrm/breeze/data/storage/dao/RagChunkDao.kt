package com.hrm.breeze.data.storage.dao

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Upsert
import com.hrm.breeze.data.storage.entity.RagChunkEntity
import com.hrm.breeze.data.storage.entity.RagEmbeddingEntity
import com.hrm.breeze.data.storage.entity.RagLexicalIndexEntity

@Dao
interface RagChunkDao {
    @Query("SELECT * FROM rag_chunks ORDER BY createdAtEpochMillis DESC")
    suspend fun getAllChunks(): List<RagChunkEntity>

    @Query("SELECT * FROM rag_chunks WHERE id IN (:chunkIds)")
    suspend fun getChunks(chunkIds: List<String>): List<RagChunkEntity>

    @Query("SELECT * FROM rag_lexical_indices")
    suspend fun getLexicalIndices(): List<RagLexicalIndexEntity>

    @Query("SELECT * FROM rag_embeddings WHERE embeddingModelId = :embeddingModelId")
    suspend fun getEmbeddings(embeddingModelId: String): List<RagEmbeddingEntity>

    @Query("SELECT chunkId FROM rag_embeddings WHERE embeddingModelId = :embeddingModelId")
    suspend fun getEmbeddedChunkIds(embeddingModelId: String): List<String>

    @Upsert
    suspend fun upsertChunks(chunks: List<RagChunkEntity>)

    @Upsert
    suspend fun upsertLexicalIndices(indices: List<RagLexicalIndexEntity>)

    @Upsert
    suspend fun upsertEmbeddings(embeddings: List<RagEmbeddingEntity>)
}
