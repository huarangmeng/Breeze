package com.hrm.breeze.data.storage

import androidx.room3.ConstructedBy
import androidx.room3.Database
import androidx.room3.RoomDatabase
import androidx.room3.RoomDatabaseConstructor
import androidx.sqlite.SQLiteDriver
import com.hrm.breeze.core.coroutines.AppDispatchers
import com.hrm.breeze.core.coroutines.defaultAppDispatchers
import com.hrm.breeze.data.storage.driver.createPlatformSQLiteDriver
import com.hrm.breeze.data.storage.dao.ConversationDao
import com.hrm.breeze.data.storage.dao.MessageDao
import com.hrm.breeze.data.storage.entity.ConversationEntity
import com.hrm.breeze.data.storage.entity.MessageEntity

@Database(
    entities = [ConversationEntity::class, MessageEntity::class],
    version = 1,
    exportSchema = true,
)
@ConstructedBy(BreezeDatabaseConstructor::class)
abstract class BreezeDatabase : RoomDatabase() {
    abstract fun conversationDao(): ConversationDao
    abstract fun messageDao(): MessageDao

    companion object {
        const val DefaultName: String = "breeze.db"

        fun build(
            builder: RoomDatabase.Builder<BreezeDatabase>,
            driver: SQLiteDriver,
            dispatchers: AppDispatchers = defaultAppDispatchers(),
        ): BreezeDatabase = builder
            .setDriver(driver)
            .setQueryCoroutineContext(dispatchers.io)
            .fallbackToDestructiveMigration()
            .build()

        fun create(
            dispatchers: AppDispatchers = defaultAppDispatchers(),
            name: String = DefaultName,
        ): BreezeDatabase = build(
            builder = createPlatformDatabaseBuilder(name),
            driver = createPlatformSQLiteDriver(),
            dispatchers = dispatchers,
        )
    }
}

expect fun createPlatformDatabaseBuilder(name: String): RoomDatabase.Builder<BreezeDatabase>

@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object BreezeDatabaseConstructor : RoomDatabaseConstructor<BreezeDatabase> {
    override fun initialize(): BreezeDatabase
}
