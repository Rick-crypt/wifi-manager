package com.example.data.wifi

data class WifiConnectionInfo(
    val isConnected: Boolean = false,
    val ssid: String = "",
    val bssid: String = "",
    val ipAddress: String = "",
    val linkSpeedMbps: Int = 0,
    val rssiDbm: Int = 0,
    val signalStrengthLevel: Int = 0, // 0 to 4
    val frequencyMhz: Int = 0,
    val frequencyBand: String = "", // 2.4 GHz, 5 GHz, 6 GHz
    val gatewayIp: String = "",
    val netmask: String = "",
    val dns1: String = "",
    val macAddress: String = "",
    val hasInternetAccess: Boolean = false
) {
    val signalPercentage: Int
        get() = (signalStrengthLevel * 25).coerceIn(0, 100)
}
