package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "wifi_sessions")
data class WifiSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: Long? = null,
    val identifierSummary: String, // ex: "P123456 • Gabonaise • 2026" ou "Invité: Andy"
    val deviceName: String, // ex: "iPhone 15 Pro", "MacBook Pro"
    val deviceMac: String, // ex: "3C:22:FB:4A:91:02"
    val ipAddress: String, // ex: "192.168.1.142"
    val startedAt: Long = System.currentTimeMillis(),
    val expiresAt: Long = System.currentTimeMillis() + (24 * 60 * 60 * 1000L),
    val isActive: Boolean = true,
    val isGuest: Boolean = false,
    val bytesUsedMb: Double = 145.2
) {
    val isExpired: Boolean
        get() = System.currentTimeMillis() > expiresAt
}
