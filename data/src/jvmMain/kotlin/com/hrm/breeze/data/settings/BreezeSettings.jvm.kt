package com.hrm.breeze.data.settings

import androidx.datastore.core.DataMigration
import androidx.datastore.preferences.core.Preferences
import com.russhwolf.settings.PreferencesSettings
import okio.Path
import okio.Path.Companion.toPath

actual fun createLegacySettingsMigration(namespace: String): DataMigration<Preferences> =
    createLegacySettingsMigration(
        legacySettingsStore =
            object : LegacySettingsStore {
                private val delegate = PreferencesSettings.Factory().create(namespace)

                override fun hasKey(key: String): Boolean = delegate.hasKey(key)

                override fun getStringOrNull(key: String): String? = delegate.getStringOrNull(key)

                override fun remove(key: String) {
                    delegate.remove(key)
                }
            }
    )

actual fun createPlatformSettingsPath(namespace: String): Path =
    "${System.getProperty("user.home")}/.breeze/datastore/$namespace.preferences_pb".toPath()
