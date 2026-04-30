package com.hrm.breeze.data.storage

import androidx.room3.Room
import androidx.room3.RoomDatabase
import java.io.File

actual fun createPlatformDatabaseBuilder(name: String): RoomDatabase.Builder<BreezeDatabase> {
    val dbFile = resolveJvmDatabaseFile(name)
    dbFile.parentFile?.mkdirs()
    return Room.databaseBuilder<BreezeDatabase>(
        name = dbFile.absolutePath,
        factory = BreezeDatabaseConstructor::initialize,
    )
}

private fun resolveJvmDatabaseFile(name: String): File {
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
    return File(baseDirectory, name)
}
