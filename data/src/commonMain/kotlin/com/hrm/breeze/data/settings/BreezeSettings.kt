package com.hrm.breeze.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.hrm.breeze.data.network.BREEZE_MOCK_ECHO_ENDPOINT
import com.hrm.breeze.domain.model.LlmProviderId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import okio.Path

private const val DEFAULT_NAMESPACE = "breeze.preferences"
private const val DEFAULT_ECHO_ENDPOINT = BREEZE_MOCK_ECHO_ENDPOINT
private const val DEFAULT_MODEL_ID = "breeze-echo"
private const val DEFAULT_PROVIDER_ID = "local"

internal const val KEY_ECHO_ENDPOINT = "network.echo_endpoint"
internal const val KEY_PROVIDER_ID = "network.provider_id"
internal const val KEY_MODEL_ID = "model.current_id"
internal const val KEY_API_TOKEN = "network.api_token"

private val echoEndpointKey = stringPreferencesKey(KEY_ECHO_ENDPOINT)
private val providerIdKey = stringPreferencesKey(KEY_PROVIDER_ID)
private val modelIdKey = stringPreferencesKey(KEY_MODEL_ID)
private val apiTokenKey = stringPreferencesKey(KEY_API_TOKEN)

data class BreezeSettingsSnapshot(
    val echoEndpoint: String = DEFAULT_ECHO_ENDPOINT,
    val currentProviderId: LlmProviderId = LlmProviderId.fromStorageValue(DEFAULT_PROVIDER_ID),
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

    suspend fun getCurrentProviderId(): LlmProviderId = snapshot.first().currentProviderId

    suspend fun updateCurrentProviderId(value: LlmProviderId) {
        dataStore.edit { preferences ->
            preferences[providerIdKey] = value.storageValue
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
        produceFile = { createPlatformSettingsPath(namespace) },
    )

internal fun Preferences.toBreezeSettingsSnapshot(): BreezeSettingsSnapshot =
    BreezeSettingsSnapshot(
        echoEndpoint = this[echoEndpointKey] ?: DEFAULT_ECHO_ENDPOINT,
        currentProviderId = LlmProviderId.fromStorageValue(this[providerIdKey] ?: DEFAULT_PROVIDER_ID),
        currentModelId = this[modelIdKey] ?: DEFAULT_MODEL_ID,
        apiToken = this[apiTokenKey],
    )

expect fun createPlatformSettingsPath(namespace: String): Path
