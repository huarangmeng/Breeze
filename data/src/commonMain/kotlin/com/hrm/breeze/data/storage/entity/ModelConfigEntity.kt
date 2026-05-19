package com.hrm.breeze.data.storage.entity

import androidx.room3.Entity
import androidx.room3.PrimaryKey
import com.hrm.breeze.domain.model.LlmProviderId
import com.hrm.breeze.domain.model.ModelConfig
import kotlin.time.Instant

@Entity(tableName = "model_configs")
data class ModelConfigEntity(
    @PrimaryKey val id: String,
    val providerId: String,
    val endpoint: String,
    val apiToken: String?,
    val modelId: String,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
)

fun ModelConfigEntity.toDomain(): ModelConfig =
    ModelConfig(
        id = id,
        providerId = LlmProviderId.fromStorageValue(providerId),
        endpoint = endpoint,
        apiToken = apiToken,
        modelId = modelId,
        createdAt = Instant.fromEpochMilliseconds(createdAtEpochMillis),
        updatedAt = Instant.fromEpochMilliseconds(updatedAtEpochMillis),
    )
