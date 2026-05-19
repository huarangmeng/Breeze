package com.hrm.breeze.data.storage.dao

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Upsert
import com.hrm.breeze.data.storage.entity.OnDeviceModelAssetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface OnDeviceModelAssetDao {
    @Query("SELECT * FROM on_device_model_assets ORDER BY presetId ASC")
    fun observeAssets(): Flow<List<OnDeviceModelAssetEntity>>

    @Query("SELECT * FROM on_device_model_assets WHERE presetId = :presetId LIMIT 1")
    suspend fun getAsset(presetId: String): OnDeviceModelAssetEntity?

    @Upsert
    suspend fun upsertAsset(asset: OnDeviceModelAssetEntity)

    @Query("DELETE FROM on_device_model_assets WHERE presetId = :presetId")
    suspend fun deleteAsset(presetId: String)
}
