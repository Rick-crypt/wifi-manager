package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.entity.FieldDefinitionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FieldDefinitionDao {
    @Query("SELECT * FROM field_definitions ORDER BY displayOrder ASC, id ASC")
    fun getAllFields(): Flow<List<FieldDefinitionEntity>>

    @Query("SELECT * FROM field_definitions WHERE isEnabled = 1 ORDER BY displayOrder ASC, id ASC")
    fun getEnabledFields(): Flow<List<FieldDefinitionEntity>>

    @Query("SELECT * FROM field_definitions WHERE isEnabled = 1 AND isVisibleInPortal = 1 ORDER BY displayOrder ASC, id ASC")
    fun getPortalFields(): Flow<List<FieldDefinitionEntity>>

    @Query("SELECT * FROM field_definitions WHERE isEnabled = 1 AND isVisibleInPortal = 1 ORDER BY displayOrder ASC, id ASC")
    suspend fun getPortalFieldsDirect(): List<FieldDefinitionEntity>

    @Query("SELECT * FROM field_definitions WHERE isEnabled = 1 AND isAuthKey = 1 ORDER BY displayOrder ASC, id ASC")
    fun getAuthFields(): Flow<List<FieldDefinitionEntity>>

    @Query("SELECT * FROM field_definitions WHERE id = :id LIMIT 1")
    suspend fun getFieldById(id: Long): FieldDefinitionEntity?

    @Query("SELECT COUNT(*) FROM field_definitions")
    suspend fun getCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(field: FieldDefinitionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(fields: List<FieldDefinitionEntity>)

    @Update
    suspend fun update(field: FieldDefinitionEntity)

    @Delete
    suspend fun delete(field: FieldDefinitionEntity)

    @Query("DELETE FROM field_definitions WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM field_definitions")
    suspend fun deleteAll()
}
