package com.hrm.breeze.data.storage.dao

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Upsert
import com.hrm.breeze.data.storage.entity.RagDocumentEntity

@Dao
interface RagDocumentDao {
    @Query("SELECT * FROM rag_documents WHERE id = :documentId LIMIT 1")
    suspend fun getDocument(documentId: String): RagDocumentEntity?

    @Upsert
    suspend fun upsertDocument(document: RagDocumentEntity)
}
