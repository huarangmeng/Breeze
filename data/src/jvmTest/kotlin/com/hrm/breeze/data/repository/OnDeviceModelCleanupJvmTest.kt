package com.hrm.breeze.data.repository

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.hrm.breeze.core.coroutines.AppDispatchers
import com.hrm.breeze.data.llm.ondevice.OnDeviceModelCatalog
import com.hrm.breeze.data.llm.ondevice.OnDeviceModelRepository
import com.hrm.breeze.data.platform.BreezeModelPaths
import com.hrm.breeze.data.settings.BreezeSettings
import com.hrm.breeze.data.storage.BreezeDatabase
import com.hrm.breeze.data.storage.createPlatformDatabaseBuilder
import com.hrm.breeze.data.storage.entity.OnDeviceModelAssetEntity
import com.hrm.breeze.domain.model.InferenceRuntimeState
import com.hrm.breeze.domain.model.LlmProviderId
import com.hrm.breeze.domain.model.OnDeviceDownloadStatus
import com.hrm.breeze.runtime.api.InferenceMessage
import com.hrm.breeze.runtime.api.OnDeviceRuntimeCompletionRequest
import com.hrm.breeze.runtime.api.OnDeviceRuntimeLaunchRequest
import com.hrm.breeze.runtime.api.OnDeviceRuntime
import io.ktor.client.HttpClient
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import okio.buffer
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock

@OptIn(ExperimentalCoroutinesApi::class)
class OnDeviceModelCleanupJvmTest {
    @Test
    fun observeModelsRemovesAssetWhenModelFileIsMissing() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val dispatchers = TestAppDispatchers(dispatcher)
        val tempDirectory = Files.createTempDirectory("breeze-on-device-cleanup-asset-test")
        val modelPaths = createTestModelPaths(tempDirectory.toString())
        val database = createDatabase(tempDirectory.toString(), dispatchers)
        val settings = createSettings(tempDirectory.toString())
        val modelConfigRepository = ModelConfigRepositoryImpl(database, settings, clock = Clock.System, modelPaths = modelPaths)
        val httpClient = HttpClient()
        val repository =
            OnDeviceModelRepository(
                assetDao = database.onDeviceModelAssetDao(),
                modelConfigRepository = modelConfigRepository,
                httpClient = httpClient,
                runtimeManager = FakeOnDeviceRuntime(),
                modelPaths = modelPaths,
            )
        val preset = OnDeviceModelCatalog.requirePreset("smollm2_360m_q8_0")
        val finalPath = "${modelPaths.files}/${preset.fileName}".toPath()

        try {
            writeTestFile(finalPath)
            database.onDeviceModelAssetDao().upsertAsset(
                OnDeviceModelAssetEntity(
                    presetId = preset.id,
                    downloadStatus = OnDeviceDownloadStatus.Downloaded.name,
                    runtimeState = InferenceRuntimeState.Idle.name,
                    localPath = finalPath.toString(),
                    downloadedBytes = preset.fileSizeBytes,
                    totalBytes = preset.fileSizeBytes,
                    lastError = null,
                    lastUsedAtEpochMillis = null,
                )
            )
            FileSystem.SYSTEM.delete(finalPath)

            val models = repository.observeModels().first()
            advanceUntilIdle()

            val cleanedState = models.first { it.preset.id == preset.id }
            assertEquals(OnDeviceDownloadStatus.NotDownloaded, cleanedState.downloadStatus)
            assertNull(database.onDeviceModelAssetDao().getAsset(preset.id))
        } finally {
            httpClient.close()
            database.close()
        }
    }

    @Test
    fun observeModelConfigsRemovesLocalConfigWhenModelFileIsMissing() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val dispatchers = TestAppDispatchers(dispatcher)
        val tempDirectory = Files.createTempDirectory("breeze-on-device-cleanup-config-test")
        val modelPaths = createTestModelPaths(tempDirectory.toString())
        val database = createDatabase(tempDirectory.toString(), dispatchers)
        val settings = createSettings(tempDirectory.toString())
        val repository = ModelConfigRepositoryImpl(database, settings, clock = Clock.System, modelPaths = modelPaths)
        val preset = OnDeviceModelCatalog.requirePreset("smollm2_360m_q8_0")
        val finalPath = "${modelPaths.files}/${preset.fileName}".toPath()

        try {
            writeTestFile(finalPath)
            val config =
                repository.createAndActivateConfig(
                    providerId = LlmProviderId.Local,
                    endpoint = "local://runtime",
                    apiToken = null,
                    modelId = preset.id,
                )
            FileSystem.SYSTEM.delete(finalPath)

            val configs = repository.observeModelConfigs().first()
            advanceUntilIdle()

            assertTrue(configs.none { it.id == config.id })
            assertNull(repository.getActiveModelConfig())
            assertNull(database.modelConfigDao().getModelConfig(config.id))
            assertNull(settings.getActiveModelConfigId())
        } finally {
            database.close()
        }
    }
}

private fun createDatabase(
    tempDirectory: String,
    dispatchers: AppDispatchers,
): BreezeDatabase =
    BreezeDatabase.build(
        builder = createPlatformDatabaseBuilder("$tempDirectory/breeze-test.db"),
        driver = BundledSQLiteDriver(),
        dispatchers = dispatchers,
    )

private fun createSettings(
    tempDirectory: String,
): BreezeSettings = BreezeSettings(
    dataStore =
        PreferenceDataStoreFactory.createWithPath(
            produceFile = { "$tempDirectory/breeze-settings.preferences_pb".toPath() },
        )
)

private fun createTestModelPaths(tempDirectory: String): BreezeModelPaths =
    BreezeModelPaths(
        root = "$tempDirectory/models".toPath(),
        files = "$tempDirectory/models/files".toPath(),
        temp = "$tempDirectory/models/tmp".toPath(),
        logs = "$tempDirectory/models/logs".toPath(),
    )

private fun writeTestFile(path: Path) {
    FileSystem.SYSTEM.createDirectories(path.parent!!)
    FileSystem.SYSTEM.sink(path).buffer().use { sink ->
        sink.writeUtf8("mock gguf")
    }
}

private class FakeOnDeviceRuntime : OnDeviceRuntime {
    override suspend fun ensureModelReady(
        request: OnDeviceRuntimeLaunchRequest,
    ): InferenceRuntimeState = InferenceRuntimeState.Ready

    override fun streamCompletion(
        request: OnDeviceRuntimeCompletionRequest,
    ): Flow<String> = emptyFlow()
}

private class TestAppDispatchers(
    dispatcher: CoroutineDispatcher,
) : AppDispatchers {
    override val main: CoroutineDispatcher = dispatcher
    override val default: CoroutineDispatcher = dispatcher
    override val io: CoroutineDispatcher = dispatcher
}
