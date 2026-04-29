package com.hrm.breeze.data.storage.entity

import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey
import com.hrm.breeze.domain.model.Message
import kotlin.time.Instant

@Entity(
    tableName = "messages",
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
data class MessageEntity(
    @PrimaryKey val id: String,
    val conversationId: String,
    val role: String,
    val content: String,
    val createdAtEpochMillis: Long,
)

fun MessageEntity.toDomain(): Message = Message(
    id = id,
    conversationId = conversationId,
    role = role.toDomainRole(),
    content = content,
    createdAt = Instant.fromEpochMilliseconds(createdAtEpochMillis),
)

private fun String.toDomainRole(): Message.Role = when (this) {
    "assistant" -> Message.Role.Assistant
    "system" -> Message.Role.System
    else -> Message.Role.User
}
