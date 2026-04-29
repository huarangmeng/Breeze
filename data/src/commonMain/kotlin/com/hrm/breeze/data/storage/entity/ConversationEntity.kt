package com.hrm.breeze.data.storage.entity

import androidx.room3.Entity
import androidx.room3.PrimaryKey
import com.hrm.breeze.domain.model.Conversation
import kotlin.time.Instant

@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey val id: String,
    val title: String,
    val modelId: String,
    val updatedAtEpochMillis: Long,
)

fun ConversationEntity.toDomain(): Conversation = Conversation(
    id = id,
    title = title,
    modelId = modelId,
    updatedAt = Instant.fromEpochMilliseconds(updatedAtEpochMillis),
)
