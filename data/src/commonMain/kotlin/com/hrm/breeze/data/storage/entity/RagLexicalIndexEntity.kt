package com.hrm.breeze.data.storage.entity

import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey

@Entity(
    tableName = "rag_lexical_indices",
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
data class RagLexicalIndexEntity(
    @PrimaryKey val chunkId: String,
    val terms: String,
    val createdAtEpochMillis: Long,
)
