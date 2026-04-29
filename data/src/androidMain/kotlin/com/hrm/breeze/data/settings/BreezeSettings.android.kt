package com.hrm.breeze.data.settings

import com.russhwolf.settings.Settings
import com.russhwolf.settings.SharedPreferencesSettings
import com.hrm.breeze.data.platform.requireBreezeAndroidContext

actual fun createPlatformSettings(namespace: String): Settings =
    SharedPreferencesSettings.Factory(requireBreezeAndroidContext()).create(namespace)
