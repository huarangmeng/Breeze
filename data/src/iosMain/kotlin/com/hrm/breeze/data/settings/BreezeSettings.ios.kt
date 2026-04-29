package com.hrm.breeze.data.settings

import com.russhwolf.settings.NSUserDefaultsSettings
import com.russhwolf.settings.Settings

actual fun createPlatformSettings(namespace: String): Settings =
    NSUserDefaultsSettings.Factory().create(namespace)
