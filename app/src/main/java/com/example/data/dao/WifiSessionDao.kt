package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.entity.WifiSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WifiSessionDao {
    @Query("SELECT * FROM wifi_sessions ORDER BY startedAt DESC")
    fun getAllSessions(): Flow<List<WifiSessionEntity>>

    @Query("SELECT * FROM wifi_sessions WHERE isActive = 1 ORDER BY startedAt DESC")
    fun getActiveSessions(): Flow<List<WifiSessionEntity>>

    @Query("SELECT * FROM wifi_sessions WHERE ipAddress = :ip AND isActive = 1 LIMIT 1")
    suspend fun getActiveSessionByIpDirect(ip: String): WifiSessionEntity?

    @Query("SELECT * FROM wifi_sessions WHERE id = :id LIMIT 1")
    suspend fun getSessionById(id: Long): WifiSessionEntity?

    @Query("SELECT COUNT(*) FROM wifi_sessions WHERE isActive = 1")
    fun getActiveSessionCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: WifiSessionEntity): Long

    @Update
    suspend fun updateSession(session: WifiSessionEntity)

    @Query("UPDATE wifi_sessions SET isActive = 0 WHERE id = :id")
    suspend fun terminateSession(id: Long)

    @Query("UPDATE wifi_sessions SET isActive = 0")
    suspend fun terminateAllSessions()

    @Delete
    suspend fun deleteSession(session: WifiSessionEntity)

    @Query("DELETE FROM wifi_sessions WHERE id = :id")
    suspend fun deleteSessionById(id: Long)
}
