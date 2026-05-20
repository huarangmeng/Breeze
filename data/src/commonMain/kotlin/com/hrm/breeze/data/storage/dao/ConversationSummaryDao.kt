package com.hrm.breeze.data.storage.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import com.hrm.breeze.data.storage.entity.ConversationSummaryEntity

@Dao
interface ConversationSummaryDao {
    @Query("SELECT * FROM conversation_summaries WHERE conversationId = :conversationId")
    suspend fun getSummary(conversationId: String): ConversationSummaryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSummary(summary: ConversationSummaryEntity)
}
