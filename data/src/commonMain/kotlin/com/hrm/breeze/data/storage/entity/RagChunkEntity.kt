package com.hrm.breeze.data.storage.entity

import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey

@Entity(
    tableName = "rag_chunks",
    foreignKeys = [
        ForeignKey(
            entity = RagDocumentEntity::class,
            parentColumns = ["id"],
            childColumns = ["documentId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index(value = ["documentId"])],
)
data class RagChunkEntity(
    @PrimaryKey val id: String,
    val documentId: String,
    val content: String,
    val ordinal: Int,
    val tokenEstimate: Int,
    val metadataJson: String?,
    val createdAtEpochMillis: Long,
)
