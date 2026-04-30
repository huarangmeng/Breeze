package com.hrm.breeze.data.platform

import java.io.File

internal fun resolveBreezeJvmAppSupportFile(relativePath: String): File {
    val userHome = System.getProperty("user.home")
    val osName = System.getProperty("os.name").lowercase()
    val baseDirectory =
        when {
            osName.contains("mac") -> File(userHome, "Library/Application Support/Breeze")
            osName.contains("win") -> {
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
