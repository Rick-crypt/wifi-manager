package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.entity.GatewayAuditLogEntity
import com.example.data.entity.GatewayConfigEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GatewayConfigDao {
    @Query("SELECT * FROM gateway_config WHERE id = 1 LIMIT 1")
    fun getConfig(): Flow<GatewayConfigEntity?>

    @Query("SELECT * FROM gateway_config WHERE id = 1 LIMIT 1")
    suspend fun getConfigDirect(): GatewayConfigEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(config: GatewayConfigEntity)

    @Query("UPDATE gateway_config SET configVersion = configVersion + 1 WHERE id = 1")
    suspend fun incrementConfigVersion()

    @Query("UPDATE gateway_config SET deployedConfigVersion = :version, portalStatus = :status, lastDeployedAt = :timestamp WHERE id = 1")
    suspend fun markDeployed(version: Int, status: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE gateway_config SET portalStatus = :status WHERE id = 1")
    suspend fun updatePortalStatus(status: String)

    @Query("UPDATE gateway_config SET isBackendOnline = :online WHERE id = 1")
    suspend fun updateBackendOnline(online: Boolean)

    @Query("UPDATE gateway_config SET isRouterOnline = :online WHERE id = 1")
    suspend fun updateRouterOnline(online: Boolean)
}

@Dao
interface GatewayAuditLogDao {
    @Query("SELECT * FROM gateway_audit_logs ORDER BY timestamp DESC LIMIT 50")
    fun getRecentLogs(): Flow<List<GatewayAuditLogEntity>>

    @Insert
    suspend fun insertLog(log: GatewayAuditLogEntity): Long

    @Query("DELETE FROM gateway_audit_logs WHERE id NOT IN (SELECT id FROM gateway_audit_logs ORDER BY timestamp DESC LIMIT 200)")
    suspend fun pruneOldLogs()
}
