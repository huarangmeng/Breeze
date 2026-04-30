package com.hrm.breeze.data.settings

import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.preferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BreezeSettingsMigrationTest {
    @Test
    fun migratesLegacyValuesAndCleansUpLegacyStore() = runTest {
        val legacyStore =
            FakeLegacySettingsStore(
                mutableMapOf(
                    KEY_ECHO_ENDPOINT to "https://legacy.example/v1/chat/echo",
                    KEY_MODEL_ID to "legacy-model",
                    KEY_API_TOKEN to "legacy-token",
                )
            )
        val migration = createLegacySettingsMigration(legacyStore)

        assertTrue(migration.shouldMigrate(emptyPreferences()))

        val migrated = migration.migrate(emptyPreferences())
        val snapshot = migrated.toBreezeSettingsSnapshot()

        assertEquals("https://legacy.example/v1/chat/echo", snapshot.echoEndpoint)
        assertEquals("legacy-model", snapshot.currentModelId)
        assertEquals("legacy-token", snapshot.apiToken)
        assertFalse(migration.shouldMigrate(migrated))

        migration.cleanUp()

        assertFalse(legacyStore.hasKey(KEY_ECHO_ENDPOINT))
        assertFalse(legacyStore.hasKey(KEY_MODEL_ID))
        assertFalse(legacyStore.hasKey(KEY_API_TOKEN))
    }

    @Test
    fun keepsCurrentDatastoreValuesWhenLegacyStoreStillHasData() = runTest {
        val legacyStore =
            FakeLegacySettingsStore(
                mutableMapOf(
                    KEY_ECHO_ENDPOINT to "https://legacy.example/v1/chat/echo",
                    KEY_MODEL_ID to "legacy-model",
                    KEY_API_TOKEN to "legacy-token",
                )
            )
        val migration = createLegacySettingsMigration(legacyStore)
        val currentData =
            preferencesOf(
                stringPreferencesKey(KEY_ECHO_ENDPOINT) to "https://current.example/v1/chat/echo",
                stringPreferencesKey(KEY_MODEL_ID) to "current-model",
                stringPreferencesKey(KEY_API_TOKEN) to "current-token",
            )

        val migrated = migration.migrate(currentData)
        val snapshot = migrated.toBreezeSettingsSnapshot()

        assertEquals("https://current.example/v1/chat/echo", snapshot.echoEndpoint)
        assertEquals("current-model", snapshot.currentModelId)
        assertEquals("current-token", snapshot.apiToken)
    }
}

private class FakeLegacySettingsStore(
    private val values: MutableMap<String, String>,
) : LegacySettingsStore {
    override fun hasKey(key: String): Boolean = values.containsKey(key)

    override fun getStringOrNull(key: String): String? = values[key]

    override fun remove(key: String) {
        values.remove(key)
    }
}
