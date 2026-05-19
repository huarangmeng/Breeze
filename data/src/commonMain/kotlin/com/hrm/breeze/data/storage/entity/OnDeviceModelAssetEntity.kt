package com.hrm.breeze.data.storage.entity

import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "on_device_model_assets")
data class OnDeviceModelAssetEntity(
    @PrimaryKey val presetId: String,
    val downloadStatus: String,
    val runtimeState: String,
    val localPath: String?,
    val downloadedBytes: Long,
    val totalBytes: Long?,
    val lastError: String?,
    val lastUsedAtEpochMillis: Long?,
)
