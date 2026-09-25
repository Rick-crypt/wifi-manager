package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "gateway_audit_logs")
data class GatewayAuditLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val eventType: String, // ACTIVATION, DEPLOYMENT, SYNC, ROUTER_REBOOT, PORTAL_STOP, FIREWALL_UPDATE, BACKEND_OFFLINE, BACKEND_ONLINE
    val description: String,
    val configVersion: Int = 1,
    val success: Boolean = true
)
