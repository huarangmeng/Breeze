package com.hrm.breeze.data.settings

import okio.Path
import okio.Path.Companion.toPath

actual fun createPlatformSettingsPath(namespace: String): Path =
    "${System.getProperty("user.home")}/.breeze/datastore/$namespace.preferences_pb".toPath()
