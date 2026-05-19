package com.hrm.breeze.domain.repository

import com.hrm.breeze.domain.model.LlmProviderId
import com.hrm.breeze.domain.model.ModelConfig
import kotlinx.coroutines.flow.Flow

interface ModelConfigRepository {
    fun observeModelConfigs(): Flow<List<ModelConfig>>

    fun observeActiveModelConfig(): Flow<ModelConfig?>

    suspend fun getActiveModelConfig(): ModelConfig?

    suspend fun createAndActivateConfig(
        providerId: LlmProviderId,
        endpoint: String,
        apiToken: String?,
        modelId: String,
    ): ModelConfig

    suspend fun setActiveConfig(configId: String)

    suspend fun updateActiveConfigModelId(modelId: String)
}
