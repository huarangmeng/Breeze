package com.hrm.breeze.data.storage.entity

import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index

@Entity(
    tableName = "rag_embeddings",
    primaryKeys = ["chunkId", "embeddingModelId"],
    foreignKeys = [
        ForeignKey(
            entity = RagChunkEntity::class,
            parentColumns = ["id"],
            childColumns = ["chunkId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index(value = ["chunkId"])],
)
data class RagEmbeddingEntity(
    val chunkId: String,
    val embeddingModelId: String,
    val dimension: Int,
    val vector: ByteArray,
    val normalized: Boolean,
    val createdAtEpochMillis: Long,
)
