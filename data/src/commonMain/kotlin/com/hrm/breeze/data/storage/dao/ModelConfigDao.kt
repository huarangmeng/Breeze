package com.hrm.breeze.data.storage.dao

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Upsert
import com.hrm.breeze.data.storage.entity.ModelConfigEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ModelConfigDao {
    @Query("SELECT * FROM model_configs ORDER BY updatedAtEpochMillis DESC")
    fun observeModelConfigs(): Flow<List<ModelConfigEntity>>

    @Query("SELECT * FROM model_configs WHERE id = :configId LIMIT 1")
    fun observeModelConfig(configId: String): Flow<ModelConfigEntity?>

    @Query("SELECT * FROM model_configs WHERE id = :configId LIMIT 1")
    suspend fun getModelConfig(configId: String): ModelConfigEntity?

    @Query(
        "SELECT * FROM model_configs WHERE providerId = :providerId AND endpoint = :endpoint " +
            "AND modelId = :modelId AND ((apiToken IS NULL AND :apiToken IS NULL) OR apiToken = :apiToken) " +
            "LIMIT 1"
    )
    suspend fun findModelConfig(
        providerId: String,
        endpoint: String,
        apiToken: String?,
        modelId: String,
    ): ModelConfigEntity?

    @Upsert
    suspend fun upsertModelConfig(config: ModelConfigEntity)

    @Query("DELETE FROM model_configs WHERE id = :configId")
    suspend fun deleteModelConfig(configId: String)
}
