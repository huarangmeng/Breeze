package com.hrm.breeze.data.settings

import okio.Path
import okio.Path.Companion.toPath
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask

actual fun createPlatformSettingsPath(namespace: String): Path {
    val basePath = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, true)
        .firstOrNull() as? String ?: error(
        "无法定位 iOS 文档目录来创建 Breeze settings DataStore。"
    )
    return "$basePath/$namespace.preferences_pb".toPath()
}
