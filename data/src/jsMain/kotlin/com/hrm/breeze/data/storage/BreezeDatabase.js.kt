package com.hrm.breeze.data.storage

import androidx.room3.Room
import androidx.room3.RoomDatabase

actual fun createPlatformDatabaseBuilder(name: String): RoomDatabase.Builder<BreezeDatabase> =
    Room.databaseBuilder<BreezeDatabase>(
        name = name,
        factory = BreezeDatabaseConstructor::initialize,
    )
