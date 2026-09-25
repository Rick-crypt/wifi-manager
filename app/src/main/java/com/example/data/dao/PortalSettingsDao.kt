package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.entity.PortalSettingsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PortalSettingsDao {
    @Query("SELECT * FROM portal_settings WHERE id = 1 LIMIT 1")
    fun getSettings(): Flow<PortalSettingsEntity?>

    @Query("SELECT * FROM portal_settings WHERE id = 1 LIMIT 1")
    suspend fun getSettingsDirect(): PortalSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(settings: PortalSettingsEntity)

    @Update
    suspend fun update(settings: PortalSettingsEntity)
}
