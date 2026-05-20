package com.hrm.breeze.data.storage.entity

import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey

@Entity(
    tableName = "rag_documents",
    indices = [Index(value = ["sourceType", "sourceId"])],
)
data class RagDocumentEntity(
    @PrimaryKey val id: String,
    val sourceType: String,
    val sourceId: String?,
    val title: String,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
)
