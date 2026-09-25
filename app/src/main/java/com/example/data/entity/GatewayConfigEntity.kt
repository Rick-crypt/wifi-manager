package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.PortalStatus

@Entity(tableName = "gateway_config")
data class GatewayConfigEntity(
    @PrimaryKey
    val id: Int = 1,
    val gatewayIp: String = "192.168.1.1",
    val gatewayApiUrl: String = "http://192.168.1.1:8080",
    val configVersion: Int = 1,
    val deployedConfigVersion: Int = 0,
    val portalStatus: String = PortalStatus.DISABLED.name,
    val lastDeployedAt: Long = 0L,
    val hardwareType: String = "OPENWRT", // OPENWRT, LINUX_GATEWAY, MIKROTIK, PFSENSE, GENERIC_BOX
    val isBackendOnline: Boolean = true,
    val isRouterOnline: Boolean = true
) {
    val status: PortalStatus
        get() = try {
            PortalStatus.valueOf(portalStatus)
        } catch (_: Exception) {
            PortalStatus.DISABLED
        }

    val isSynced: Boolean
        get() = configVersion == deployedConfigVersion && deployedConfigVersion > 0

    val isSyncRequired: Boolean
        get() = configVersion != deployedConfigVersion
}
