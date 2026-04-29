package com.hrm.breeze.data.settings

import com.russhwolf.settings.Settings
import com.russhwolf.settings.StorageSettings

actual fun createPlatformSettings(namespace: String): Settings = StorageSettings()
