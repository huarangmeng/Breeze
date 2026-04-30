package com.hrm.breeze.data.storage

import androidx.room3.Room
import androidx.room3.RoomDatabase
import com.hrm.breeze.data.platform.resolveBreezeIosAppSupportPath

actual fun createPlatformDatabaseBuilder(name: String): RoomDatabase.Builder<BreezeDatabase> {
    return Room.databaseBuilder<BreezeDatabase>(
        name = resolveBreezeIosAppSupportPath(name),
        factory = BreezeDatabaseConstructor::initialize,
    )
}
