package com.hrm.breeze.data.settings

import androidx.datastore.core.DataMigration
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.hrm.breeze.data.network.BREEZE_MOCK_ECHO_ENDPOINT
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import okio.Path

private const val DEFAULT_NAMESPACE = "breeze.preferences"
private const val DEFAULT_ECHO_ENDPOINT = BREEZE_MOCK_ECHO_ENDPOINT
private const val DEFAULT_MODEL_ID = "breeze-echo"

internal const val KEY_ECHO_ENDPOINT = "network.echo_endpoint"
internal const val KEY_MODEL_ID = "model.current_id"
internal const val KEY_API_TOKEN = "network.api_token"
private const val KEY_LEGACY_MIGRATED = "settings.legacy_migrated"

private val echoEndpointKey = stringPreferencesKey(KEY_ECHO_ENDPOINT)
private val modelIdKey = stringPreferencesKey(KEY_MODEL_ID)
private val apiTokenKey = stringPreferencesKey(KEY_API_TOKEN)
private val legacyMigratedKey = booleanPreferencesKey(KEY_LEGACY_MIGRATED)

data class BreezeSettingsSnapshot(
    val echoEndpoint: String = DEFAULT_ECHO_ENDPOINT,
    val currentModelId: String = DEFAULT_MODEL_ID,
    val apiToken: String? = null,
)

class BreezeSettings(
    private val dataStore: DataStore<Preferences>,
) {
    val snapshot: Flow<BreezeSettingsSnapshot> =
        dataStore.data.map(Preferences::toBreezeSettingsSnapshot)

    suspend fun getEchoEndpoint(): String = snapshot.first().echoEndpoint

    suspend fun updateEchoEndpoint(value: String) {
        dataStore.edit { preferences ->
            preferences[echoEndpointKey] = value
        }
    }

    suspend fun getCurrentModelId(): String = snapshot.first().currentModelId

    suspend fun updateCurrentModelId(value: String) {
        dataStore.edit { preferences ->
            preferences[modelIdKey] = value
        }
    }

    suspend fun getApiToken(): String? = snapshot.first().apiToken

    suspend fun updateApiToken(value: String?) {
        dataStore.edit { preferences ->
            if (value.isNullOrBlank()) {
                preferences.remove(apiTokenKey)
            } else {
                preferences[apiTokenKey] = value
            }
        }
    }
}

fun createBreezeSettings(
    namespace: String = DEFAULT_NAMESPACE,
): BreezeSettings = BreezeSettings(createPlatformSettingsDataStore(namespace))

internal fun createPlatformSettingsDataStore(namespace: String): DataStore<Preferences> =
    PreferenceDataStoreFactory.createWithPath(
        migrations = listOf(createLegacySettingsMigration(namespace)),
        produceFile = { createPlatformSettingsPath(namespace) },
    )

internal fun Preferences.toBreezeSettingsSnapshot(): BreezeSettingsSnapshot =
    BreezeSettingsSnapshot(
        echoEndpoint = this[echoEndpointKey] ?: DEFAULT_ECHO_ENDPOINT,
        currentModelId = this[modelIdKey] ?: DEFAULT_MODEL_ID,
        apiToken = this[apiTokenKey],
    )

internal interface LegacySettingsStore {
    fun hasKey(key: String): Boolean
    fun getStringOrNull(key: String): String?
    fun remove(key: String)
}

internal fun createLegacySettingsMigration(
    legacySettingsStore: LegacySettingsStore,
): DataMigration<Preferences> =
    object : DataMigration<Preferences> {
        override suspend fun shouldMigrate(currentData: Preferences): Boolean =
            currentData[legacyMigratedKey] != true

        override suspend fun migrate(currentData: Preferences): Preferences =
            currentData.toMutablePreferences().apply {
                if (echoEndpointKey !in this) {
                    legacySettingsStore.getStringOrNull(KEY_ECHO_ENDPOINT)?.let { value ->
                        this[echoEndpointKey] = value
                    }
                }
                if (modelIdKey !in this) {
                    legacySettingsStore.getStringOrNull(KEY_MODEL_ID)?.let { value ->
                        this[modelIdKey] = value
                    }
                }
                if (apiTokenKey !in this) {
                    legacySettingsStore.getStringOrNull(KEY_API_TOKEN)?.let { value ->
                        this[apiTokenKey] = value
                    }
                }
                this[legacyMigratedKey] = true
            }.toPreferences()

        override suspend fun cleanUp() {
            if (legacySettingsStore.hasKey(KEY_ECHO_ENDPOINT)) {
                legacySettingsStore.remove(KEY_ECHO_ENDPOINT)
            }
            if (legacySettingsStore.hasKey(KEY_MODEL_ID)) {
                legacySettingsStore.remove(KEY_MODEL_ID)
            }
            if (legacySettingsStore.hasKey(KEY_API_TOKEN)) {
                legacySettingsStore.remove(KEY_API_TOKEN)
            }
        }
    }

expect fun createLegacySettingsMigration(namespace: String): DataMigration<Preferences>

expect fun createPlatformSettingsPath(namespace: String): Path
