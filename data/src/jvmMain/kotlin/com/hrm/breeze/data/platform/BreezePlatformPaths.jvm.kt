package com.hrm.breeze.data.platform

import com.hrm.breeze.platform.PlatformKind
import com.hrm.breeze.platform.platformInfo
import java.io.File

internal fun resolveBreezeJvmAppSupportFile(relativePath: String): File {
    val userHome = System.getProperty("user.home")
    val baseDirectory =
        when (platformInfo.kind) {
            PlatformKind.MacOS -> File(userHome, "Library/Application Support/Breeze")
            PlatformKind.Windows -> {
                val appData = System.getenv("APPDATA")
                if (appData.isNullOrBlank()) {
                    File(userHome, "AppData/Roaming/Breeze")
                } else {
                    File(appData, "Breeze")
                }
            }
            else -> {
                val xdgDataHome = System.getenv("XDG_DATA_HOME")
                if (xdgDataHome.isNullOrBlank()) {
                    File(userHome, ".local/share/Breeze")
                } else {
                    File(xdgDataHome, "Breeze")
                }
            }
        }
    return File(baseDirectory, relativePath).also { file ->
        file.parentFile?.mkdirs()
    }
}
