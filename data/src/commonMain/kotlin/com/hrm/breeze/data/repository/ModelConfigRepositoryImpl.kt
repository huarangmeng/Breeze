package com.hrm.breeze.data.repository

import com.hrm.breeze.data.llm.ondevice.OnDeviceModelCatalog
import com.hrm.breeze.data.llm.ondevice.modelFileExists
import com.hrm.breeze.data.platform.BreezeModelPaths
import com.hrm.breeze.data.platform.createBreezeModelPaths
import com.hrm.breeze.data.settings.BreezeSettings
import com.hrm.breeze.data.storage.BreezeDatabase
import com.hrm.breeze.data.storage.entity.ModelConfigEntity
import com.hrm.breeze.data.storage.entity.toDomain
import com.hrm.breeze.domain.model.LlmProviderId
import com.hrm.breeze.domain.model.ModelConfig
import com.hrm.breeze.domain.repository.ModelConfigRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import okio.Path.Companion.toPath
import kotlin.time.Clock

@OptIn(ExperimentalCoroutinesApi::class)
class ModelConfigRepositoryImpl(
    private val database: BreezeDatabase,
    private val settings: BreezeSettings,
    private val clock: Clock = Clock.System,
    private val modelPaths: BreezeModelPaths = createBreezeModelPaths(),
) : ModelConfigRepository {
    override fun observeModelConfigs(): Flow<List<ModelConfig>> =
        database.modelConfigDao()
            .observeModelConfigs()
            .map { items -> sanitizeModelConfigs(items).map(ModelConfigEntity::toDomain) }

    override fun observeActiveModelConfig(): Flow<ModelConfig?> =
        settings.snapshot
            .map { it.activeModelConfigId }
            .flatMapLatest { configId ->
                if (configId.isNullOrBlank()) {
                    flowOf(null)
                } else {
                    database.modelConfigDao()
                        .observeModelConfig(configId)
                        .map { config -> config?.let { sanitizeModelConfig(it) }?.toDomain() }
                }
            }

    override suspend fun getActiveModelConfig(): ModelConfig? {
        val activeConfigId = settings.getActiveModelConfigId() ?: return null
        return database.modelConfigDao().getModelConfig(activeConfigId)?.let { sanitizeModelConfig(it) }?.toDomain()
    }

    override suspend fun createAndActivateConfig(
        providerId: LlmProviderId,
        endpoint: String,
        apiToken: String?,
        modelId: String,
    ): ModelConfig {
        val now = clock.now().toEpochMilliseconds()
        val dao = database.modelConfigDao()
        val existingConfig = dao.findModelConfig(
            providerId = providerId.storageValue,
            endpoint = endpoint,
            apiToken = apiToken,
            modelId = modelId,
        )
        val config =
            existingConfig?.copy(
                updatedAtEpochMillis = now,
            ) ?: ModelConfigEntity(
                id = createModelConfigId(now),
                providerId = providerId.storageValue,
                endpoint = endpoint,
                apiToken = apiToken,
                modelId = modelId,
                createdAtEpochMillis = now,
                updatedAtEpochMillis = now,
            )
        dao.upsertModelConfig(config)
        settings.updateActiveModelConfigId(config.id)
        return config.toDomain()
    }

    override suspend fun setActiveConfig(configId: String) {
        checkNotNull(database.modelConfigDao().getModelConfig(configId)?.let { sanitizeModelConfig(it) }) {
            "Model config not found: $configId"
        }
        settings.updateActiveModelConfigId(configId)
    }

    override suspend fun updateActiveConfigModelId(modelId: String) {
        val activeConfigId = settings.getActiveModelConfigId() ?: error("No active model config")
        val currentConfig =
            database.modelConfigDao().getModelConfig(activeConfigId)
                ?: error("Active model config not found: $activeConfigId")
        database.modelConfigDao().upsertModelConfig(
            currentConfig.copy(
                modelId = modelId,
                updatedAtEpochMillis = clock.now().toEpochMilliseconds(),
            )
        )
    }

    override suspend fun removeConfig(configId: String) {
        val dao = database.modelConfigDao()
        dao.deleteModelConfig(configId)
        if (settings.getActiveModelConfigId() == configId) {
            settings.updateActiveModelConfigId(null)
        }
    }

    private suspend fun sanitizeModelConfigs(items: List<ModelConfigEntity>): List<ModelConfigEntity> {
        val staleConfigs = items.filter(::isStaleLocalConfig)
        staleConfigs.forEach { config -> removeConfig(config.id) }
        return items - staleConfigs.toSet()
    }

    private suspend fun sanitizeModelConfig(config: ModelConfigEntity): ModelConfigEntity? {
        if (!isStaleLocalConfig(config)) {
            return config
        }
        removeConfig(config.id)
        return null
    }

    private fun isStaleLocalConfig(config: ModelConfigEntity): Boolean {
        if (LlmProviderId.fromStorageValue(config.providerId) != LlmProviderId.Local) {
            return false
        }
        val preset = OnDeviceModelCatalog.findPreset(config.modelId) ?: return true
        return !modelFileExists("${modelPaths.files}/${preset.fileName}".toPath())
    }
}

private fun createModelConfigId(timestampMillis: Long): String = "model-config-$timestampMillis"
