package com.hrm.breeze.data.storage.entity

import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey

@Entity(
    tableName = "conversation_summaries",
    foreignKeys = [
        ForeignKey(
            entity = ConversationEntity::class,
            parentColumns = ["id"],
            childColumns = ["conversationId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index(value = ["conversationId"])],
)
data class ConversationSummaryEntity(
    @PrimaryKey val conversationId: String,
    val summary: String,
    val coveredUntilMessageCreatedAtEpochMillis: Long,
    val coveredUntilMessageId: String,
    val updatedAtEpochMillis: Long,
)
