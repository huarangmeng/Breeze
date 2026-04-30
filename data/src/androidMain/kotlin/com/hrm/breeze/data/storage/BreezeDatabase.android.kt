package com.hrm.breeze.data.storage

import androidx.room3.Room
import androidx.room3.RoomDatabase
import com.hrm.breeze.data.platform.requireBreezeAndroidContext

actual fun createPlatformDatabaseBuilder(name: String): RoomDatabase.Builder<BreezeDatabase> {
    val appContext = requireBreezeAndroidContext()
    val dbFile = appContext.getDatabasePath(name)
    return Room.databaseBuilder<BreezeDatabase>(
        context = appContext,
        name = dbFile.absolutePath,
        factory = BreezeDatabaseConstructor::initialize,
    )
}
