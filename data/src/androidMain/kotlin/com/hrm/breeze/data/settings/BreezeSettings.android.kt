package com.hrm.breeze.data.settings

import com.hrm.breeze.data.platform.requireBreezeAndroidContext
import okio.Path
import okio.Path.Companion.toPath

actual fun createPlatformSettingsPath(namespace: String): Path =
    requireBreezeAndroidContext()
        .filesDir
        .resolve("datastore/$namespace.preferences_pb")
        .absolutePath
        .toPath()
