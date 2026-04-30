package com.hrm.breeze.data.storage

import androidx.room3.Room
import androidx.room3.RoomDatabase
import com.hrm.breeze.data.platform.resolveBreezeJvmAppSupportFile

actual fun createPlatformDatabaseBuilder(name: String): RoomDatabase.Builder<BreezeDatabase> {
    val dbFile = resolveBreezeJvmAppSupportFile(name)
    return Room.databaseBuilder<BreezeDatabase>(
        name = dbFile.absolutePath,
        factory = BreezeDatabaseConstructor::initialize,
    )
}
