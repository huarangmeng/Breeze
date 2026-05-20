package com.hrm.breeze.data.storage

import androidx.room3.AutoMigration
import androidx.room3.ConstructedBy
import androidx.room3.Database
import androidx.room3.RoomDatabase
import androidx.room3.RoomDatabaseConstructor
import androidx.sqlite.SQLiteDriver
import com.hrm.breeze.core.coroutines.AppDispatchers
import com.hrm.breeze.core.coroutines.defaultAppDispatchers
import com.hrm.breeze.data.storage.dao.ConversationDao
import com.hrm.breeze.data.storage.dao.ConversationSummaryDao
import com.hrm.breeze.data.storage.dao.MessageDao
import com.hrm.breeze.data.storage.dao.ModelConfigDao
import com.hrm.breeze.data.storage.dao.OnDeviceModelAssetDao
import com.hrm.breeze.data.storage.dao.RagChunkDao
import com.hrm.breeze.data.storage.dao.RagDocumentDao
import com.hrm.breeze.data.storage.driver.createPlatformSQLiteDriver
import com.hrm.breeze.data.storage.entity.ConversationEntity
import com.hrm.breeze.data.storage.entity.ConversationSummaryEntity
import com.hrm.breeze.data.storage.entity.MessageEntity
import com.hrm.breeze.data.storage.entity.ModelConfigEntity
import com.hrm.breeze.data.storage.entity.OnDeviceModelAssetEntity
import com.hrm.breeze.data.storage.entity.RagChunkEntity
import com.hrm.breeze.data.storage.entity.RagDocumentEntity
import com.hrm.breeze.data.storage.entity.RagEmbeddingEntity
import com.hrm.breeze.data.storage.entity.RagLexicalIndexEntity

@Database(
    entities = [
        ConversationEntity::class,
        ConversationSummaryEntity::class,
        MessageEntity::class,
        ModelConfigEntity::class,
        OnDeviceModelAssetEntity::class,
        RagDocumentEntity::class,
        RagChunkEntity::class,
        RagEmbeddingEntity::class,
        RagLexicalIndexEntity::class,
    ],
    version = 3,
    exportSchema = true,
    autoMigrations = [AutoMigration(from = 1, to = 2), AutoMigration(from = 2, to = 3)],
)
@ConstructedBy(BreezeDatabaseConstructor::class)
abstract class BreezeDatabase : RoomDatabase() {
    abstract fun conversationDao(): ConversationDao
    abstract fun conversationSummaryDao(): ConversationSummaryDao
    abstract fun messageDao(): MessageDao
    abstract fun modelConfigDao(): ModelConfigDao
    abstract fun onDeviceModelAssetDao(): OnDeviceModelAssetDao
    abstract fun ragDocumentDao(): RagDocumentDao
    abstract fun ragChunkDao(): RagChunkDao

    companion object {
        const val DefaultName: String = "breeze.db"

        fun build(
            builder: RoomDatabase.Builder<BreezeDatabase>,
            driver: SQLiteDriver,
            dispatchers: AppDispatchers = defaultAppDispatchers(),
        ): BreezeDatabase = builder
            .setDriver(driver)
            .setQueryCoroutineContext(dispatchers.io)
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
