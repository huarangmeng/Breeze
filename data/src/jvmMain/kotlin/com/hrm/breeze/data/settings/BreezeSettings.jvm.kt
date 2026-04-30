package com.hrm.breeze.data.settings

import com.hrm.breeze.data.platform.resolveBreezeJvmAppSupportFile
import okio.Path
import okio.Path.Companion.toPath

actual fun createPlatformSettingsPath(namespace: String): Path =
    resolveBreezeJvmAppSupportFile("$namespace.preferences_pb").absolutePath.toPath()
