package com.hrm.breeze.data.settings

import androidx.datastore.core.DataMigration
import androidx.datastore.preferences.core.Preferences
import com.russhwolf.settings.NSUserDefaultsSettings
import okio.Path
import okio.Path.Companion.toPath
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask

actual fun createLegacySettingsMigration(namespace: String): DataMigration<Preferences> =
    createLegacySettingsMigration(
        legacySettingsStore =
            object : LegacySettingsStore {
                private val delegate = NSUserDefaultsSettings.Factory().create(namespace)

                override fun hasKey(key: String): Boolean = delegate.hasKey(key)

                override fun getStringOrNull(key: String): String? = delegate.getStringOrNull(key)

                override fun remove(key: String) {
                    delegate.remove(key)
                }
            }
    )

actual fun createPlatformSettingsPath(namespace: String): Path {
    val basePath = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, true)
        .firstOrNull() as? String ?: error(
        "无法定位 iOS 文档目录来创建 Breeze settings DataStore。"
    )
    return "$basePath/$namespace.preferences_pb".toPath()
}
