package com.hrm.breeze.data.storage

import androidx.room3.Room
import androidx.room3.RoomDatabase
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

@OptIn(ExperimentalForeignApi::class)
actual fun createPlatformDatabaseBuilder(name: String): RoomDatabase.Builder<BreezeDatabase> {
    val supportDirectory = NSFileManager.defaultManager.URLForDirectory(
        directory = NSApplicationSupportDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = true,
        error = null,
    )?.path ?: error("无法定位 iOS Application Support 目录来创建 Breeze 数据库。")
    return Room.databaseBuilder<BreezeDatabase>(
        name = "$supportDirectory/$name",
        factory = BreezeDatabaseConstructor::initialize,
    )
}
