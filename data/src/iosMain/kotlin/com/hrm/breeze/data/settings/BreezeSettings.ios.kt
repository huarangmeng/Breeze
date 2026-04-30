package com.hrm.breeze.data.settings

import com.hrm.breeze.data.platform.resolveBreezeIosAppSupportPath
import okio.Path
import okio.Path.Companion.toPath

actual fun createPlatformSettingsPath(namespace: String): Path =
    resolveBreezeIosAppSupportPath("$namespace.preferences_pb").toPath()
