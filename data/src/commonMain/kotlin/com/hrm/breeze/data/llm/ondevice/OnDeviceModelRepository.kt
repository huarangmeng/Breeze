package com.hrm.breeze.data.llm.ondevice

import com.hrm.breeze.data.platform.BreezeModelPaths
import com.hrm.breeze.data.platform.createBreezeModelPaths
import com.hrm.breeze.data.storage.dao.OnDeviceModelAssetDao
import com.hrm.breeze.data.storage.entity.OnDeviceModelAssetEntity
import com.hrm.breeze.domain.model.InferenceRuntimeState
import com.hrm.breeze.domain.model.LlmProviderId
import com.hrm.breeze.domain.model.OnDeviceDownloadStatus
import com.hrm.breeze.domain.model.OnDeviceModelPreset
import com.hrm.breeze.domain.model.OnDeviceModelState
import com.hrm.breeze.domain.repository.ModelConfigRepository
import com.hrm.breeze.runtime.api.OnDeviceRuntime
import io.ktor.client.HttpClient
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.HttpStatement
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import okio.Path
import okio.Path.Companion.toPath

class OnDeviceModelRepository(
    private val assetDao: OnDeviceModelAssetDao,
    private val modelConfigRepository: ModelConfigRepository,
    private val httpClient: HttpClient,
    private val runtimeManager: OnDeviceRuntime,
    private val modelPaths: BreezeModelPaths = createBreezeModelPaths(),
) {
    val presets: List<OnDeviceModelPreset> = OnDeviceModelCatalog.presets

    fun observeModels(): Flow<List<OnDeviceModelState>> =
        combine(
            assetDao.observeAssets(),
            modelConfigRepository.observeActiveModelConfig(),
        ) { assets, activeModelConfig ->
            val assetsById = assets.associateBy(OnDeviceModelAssetEntity::presetId)
            presets.map { preset ->
                assetsById[preset.id].toDomain(
                    preset = preset,
                    isCurrent = activeModelConfig?.providerId == LlmProviderId.Local && activeModelConfig.modelId == preset.id,
                )
            }
        }

    fun observeCurrentModel(): Flow<OnDeviceModelState?> =
        observeModels().map { models -> models.firstOrNull(OnDeviceModelState::isCurrent) }

    suspend fun selectModel(presetId: String) {
        OnDeviceModelCatalog.requirePreset(presetId)
        modelConfigRepository.createAndActivateConfig(
            providerId = LlmProviderId.Local,
            endpoint = LOCAL_RUNTIME_ENDPOINT,
            apiToken = null,
            modelId = presetId,
        )
    }

    suspend fun ensureCurrentModelReady(
        contextWindow: Int? = null,
    ): OnDeviceModelState {
        val current = observeCurrentModel().first() ?: error("No on-device model selected")
        return ensureModelReady(current, contextWindow)
    }

    private suspend fun ensureModelReady(
        current: OnDeviceModelState,
        contextWindow: Int?,
    ): OnDeviceModelState {
        if (!current.isReadyForChat) {
            error("Selected on-device model is not ready")
        }
        val runtimeState =
            runCatching {
                runtimeManager.ensureModelReady(
                    modelId = current.preset.id,
                    localPath = current.localPath,
                    contextWindow = contextWindow ?: current.preset.recommendedContextWindow,
                )
            }.getOrElse { throwable ->
                assetDao.upsertAsset(
                    current.toEntity(
                        runtimeState = InferenceRuntimeState.Failed,
                        lastError = throwable.message ?: "Runtime is not ready",
                    )
                )
                throw throwable
            }
        if (runtimeState != InferenceRuntimeState.Ready) {
            assetDao.upsertAsset(
                current.toEntity(
                    runtimeState = runtimeState,
                    lastError = "Runtime is not ready",
                )
            )
            error("On-device runtime is not ready")
        }
        assetDao.upsertAsset(
            current.toEntity(
                runtimeState = runtimeState,
                lastError = null,
            )
        )
        return current.copy(runtimeState = runtimeState, lastError = null)
    }

    suspend fun downloadModel(presetId: String) {
        val preset = OnDeviceModelCatalog.requirePreset(presetId)
        ensureDirectories()
        val tempPath = child(modelPaths.temp, "${preset.id}.partial")
        val finalPath = child(modelPaths.files, preset.fileName)
        assetDao.upsertAsset(
            OnDeviceModelAssetEntity(
                presetId = preset.id,
                downloadStatus = OnDeviceDownloadStatus.Downloading.name,
                runtimeState = InferenceRuntimeState.Idle.name,
                localPath = finalPath.toString(),
                downloadedBytes = 0,
                totalBytes = preset.fileSizeBytes,
                lastError = null,
                lastUsedAtEpochMillis = null,
            )
        )

        runCatching {
            httpClient.prepareGet(preset.downloadUrl).executeAndPersist(tempPath, finalPath) { downloadedBytes, totalBytes ->
                assetDao.upsertAsset(
                    OnDeviceModelAssetEntity(
                        presetId = preset.id,
                        downloadStatus = OnDeviceDownloadStatus.Downloading.name,
                        runtimeState = InferenceRuntimeState.Idle.name,
                        localPath = finalPath.toString(),
                        downloadedBytes = downloadedBytes,
                        totalBytes = totalBytes ?: preset.fileSizeBytes,
                        lastError = null,
                        lastUsedAtEpochMillis = null,
                    )
                )
            }
        }.onSuccess { totalBytes ->
            assetDao.upsertAsset(
                OnDeviceModelAssetEntity(
                    presetId = preset.id,
                    downloadStatus = OnDeviceDownloadStatus.Downloaded.name,
                    runtimeState = InferenceRuntimeState.Idle.name,
                    localPath = finalPath.toString(),
                    downloadedBytes = totalBytes,
                    totalBytes = totalBytes,
                    lastError = null,
                    lastUsedAtEpochMillis = null,
                )
            )
        }.onFailure { throwable ->
            deleteModelFile(tempPath)
            assetDao.upsertAsset(
                OnDeviceModelAssetEntity(
                    presetId = preset.id,
                    downloadStatus = OnDeviceDownloadStatus.Failed.name,
                    runtimeState = InferenceRuntimeState.Failed.name,
                    localPath = finalPath.toString(),
                    downloadedBytes = 0,
                    totalBytes = preset.fileSizeBytes,
                    lastError = throwable.message ?: "Download failed",
                    lastUsedAtEpochMillis = null,
                )
            )
            throw throwable
        }
    }

    suspend fun deleteModel(presetId: String) {
        val preset = OnDeviceModelCatalog.requirePreset(presetId)
        val activeConfig = modelConfigRepository.getActiveModelConfig()
        if (activeConfig?.providerId == LlmProviderId.Local && activeConfig.modelId == presetId) {
            error("Deleting the active on-device model requires selecting another model first")
        }

        val finalPath = child(modelPaths.files, preset.fileName)
        deleteModelFile(finalPath)
        deleteModelFile(child(modelPaths.temp, "${preset.id}.partial"))
        assetDao.deleteAsset(presetId)
    }

    private fun ensureDirectories() {
        ensureModelDirectories(modelPaths)
    }
}

private suspend fun HttpStatement.executeAndPersist(
    tempPath: Path,
    finalPath: Path,
    onProgress: suspend (downloadedBytes: Long, totalBytes: Long?) -> Unit,
): Long = persistStatementToFile(this, tempPath, finalPath, onProgress)

private fun child(
    parent: Path,
    child: String,
): Path = "${parent}/${child}".toPath()

private const val LOCAL_RUNTIME_ENDPOINT = "local://runtime"

private fun OnDeviceModelAssetEntity?.toDomain(
    preset: OnDeviceModelPreset,
    isCurrent: Boolean,
): OnDeviceModelState =
    OnDeviceModelState(
        preset = preset,
        downloadStatus = this?.downloadStatus?.let(OnDeviceDownloadStatus::valueOf) ?: OnDeviceDownloadStatus.NotDownloaded,
        runtimeState = this?.runtimeState?.let(InferenceRuntimeState::valueOf) ?: InferenceRuntimeState.Idle,
        isCurrent = isCurrent,
        localPath = this?.localPath,
        downloadedBytes = this?.downloadedBytes ?: 0,
        totalBytes = this?.totalBytes ?: preset.fileSizeBytes,
        lastError = this?.lastError,
    )

private fun OnDeviceModelState.toEntity(
    runtimeState: InferenceRuntimeState = this.runtimeState,
    lastError: String? = this.lastError,
): OnDeviceModelAssetEntity =
    OnDeviceModelAssetEntity(
        presetId = preset.id,
        downloadStatus = downloadStatus.name,
        runtimeState = runtimeState.name,
        localPath = localPath,
        downloadedBytes = downloadedBytes,
        totalBytes = totalBytes,
        lastError = lastError,
        lastUsedAtEpochMillis = null,
    )
