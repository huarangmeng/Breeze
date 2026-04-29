package com.hrm.breeze.data.storage.dao

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Upsert
import com.hrm.breeze.data.storage.entity.ConversationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationDao {
    @Query("SELECT * FROM conversations ORDER BY updatedAtEpochMillis DESC")
    fun observeConversations(): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM conversations WHERE id = :conversationId LIMIT 1")
    suspend fun getConversation(conversationId: String): ConversationEntity?

    @Upsert
    suspend fun upsertConversation(conversation: ConversationEntity)
}
