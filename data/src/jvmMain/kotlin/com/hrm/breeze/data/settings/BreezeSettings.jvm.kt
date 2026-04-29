package com.hrm.breeze.data.settings

import com.russhwolf.settings.PreferencesSettings
import com.russhwolf.settings.Settings

actual fun createPlatformSettings(namespace: String): Settings =
    PreferencesSettings.Factory().create(namespace)
