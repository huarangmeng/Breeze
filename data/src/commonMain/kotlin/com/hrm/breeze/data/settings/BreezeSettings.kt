package com.hrm.breeze.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import okio.Path

private const val DEFAULT_NAMESPACE = "breeze.preferences"
private const val DEFAULT_APP_LANGUAGE_TAG = "system"
private const val DEFAULT_REASONING_ENABLED = false
private const val DEFAULT_TEMPERATURE = 0.7f
private const val DEFAULT_TOP_P = 0.9f
private const val DEFAULT_MAX_TOKENS = 2048
private const val DEFAULT_CONTEXT_WINDOW = 2048

internal const val KEY_ACTIVE_MODEL_CONFIG_ID = "model.active_config_id"
internal const val KEY_APP_LANGUAGE_TAG = "app.language_tag"
internal const val KEY_REASONING_ENABLED = "model.reasoning_enabled"
internal const val KEY_TEMPERATURE = "model.temperature"
internal const val KEY_TOP_P = "model.top_p"
internal const val KEY_MAX_TOKENS = "model.max_tokens"
internal const val KEY_CONTEXT_WINDOW = "model.context_window"

private val activeModelConfigIdKey = stringPreferencesKey(KEY_ACTIVE_MODEL_CONFIG_ID)
private val appLanguageTagKey = stringPreferencesKey(KEY_APP_LANGUAGE_TAG)
private val reasoningEnabledKey = booleanPreferencesKey(KEY_REASONING_ENABLED)
private val temperatureKey = floatPreferencesKey(KEY_TEMPERATURE)
private val topPKey = floatPreferencesKey(KEY_TOP_P)
private val maxTokensKey = intPreferencesKey(KEY_MAX_TOKENS)
private val contextWindowKey = intPreferencesKey(KEY_CONTEXT_WINDOW)

data class BreezeSettingsSnapshot(
    val activeModelConfigId: String? = null,
    val appLanguageTag: String = DEFAULT_APP_LANGUAGE_TAG,
    val reasoningEnabled: Boolean = DEFAULT_REASONING_ENABLED,
    val temperature: Float = DEFAULT_TEMPERATURE,
    val topP: Float = DEFAULT_TOP_P,
    val maxTokens: Int = DEFAULT_MAX_TOKENS,
    val contextWindow: Int = DEFAULT_CONTEXT_WINDOW,
)

class BreezeSettings(
    private val dataStore: DataStore<Preferences>,
) {
    val snapshot: Flow<BreezeSettingsSnapshot> =
        dataStore.data.map(Preferences::toBreezeSettingsSnapshot)

    suspend fun getActiveModelConfigId(): String? = snapshot.first().activeModelConfigId

    suspend fun updateActiveModelConfigId(value: String?) {
        dataStore.edit { preferences ->
            if (value.isNullOrBlank()) {
                preferences.remove(activeModelConfigIdKey)
            } else {
                preferences[activeModelConfigIdKey] = value
            }
        }
    }

    suspend fun updateAppLanguageTag(value: String) {
        dataStore.edit { preferences ->
            preferences[appLanguageTagKey] = value.ifBlank { DEFAULT_APP_LANGUAGE_TAG }
        }
    }

    suspend fun getReasoningEnabled(): Boolean = snapshot.first().reasoningEnabled

    suspend fun updateReasoningEnabled(value: Boolean) {
        dataStore.edit { preferences ->
            preferences[reasoningEnabledKey] = value
        }
    }

    suspend fun updateTemperature(value: Float) {
        dataStore.edit { preferences ->
            preferences[temperatureKey] = value
        }
    }

    suspend fun updateTopP(value: Float) {
        dataStore.edit { preferences ->
            preferences[topPKey] = value
        }
    }

    suspend fun updateMaxTokens(value: Int) {
        dataStore.edit { preferences ->
            preferences[maxTokensKey] = value
        }
    }

    suspend fun updateContextWindow(value: Int) {
        dataStore.edit { preferences ->
            preferences[contextWindowKey] = value
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
        activeModelConfigId = this[activeModelConfigIdKey],
        appLanguageTag = this[appLanguageTagKey] ?: DEFAULT_APP_LANGUAGE_TAG,
        reasoningEnabled = this[reasoningEnabledKey] ?: DEFAULT_REASONING_ENABLED,
        temperature = this[temperatureKey] ?: DEFAULT_TEMPERATURE,
        topP = this[topPKey] ?: DEFAULT_TOP_P,
        maxTokens = this[maxTokensKey] ?: DEFAULT_MAX_TOKENS,
        contextWindow = this[contextWindowKey] ?: DEFAULT_CONTEXT_WINDOW,
    )

expect fun createPlatformSettingsPath(namespace: String): Path
