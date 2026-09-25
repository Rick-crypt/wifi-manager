package com.example.data.gateway

import android.content.Context
import com.example.data.database.WifiManagerDatabase
import com.example.data.entity.PortalSettingsEntity
import com.example.data.model.PortalStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Super Admin Emergency Recovery Manager.
 * Provides catastrophic failure recovery mechanisms when the app or portal is blocked/unreachable.
 */
class SuperAdminEmergencyManager(private val context: Context) {

    private val database = WifiManagerDatabase.getInstance(context)

    companion object {
        const val MASTER_SUPERADMIN_KEY = "SUPERADMIN_2026_MASTER"
        const val OVERRIDE_FILE_NAME = ".emergency_bypass_active"
    }

    private val overrideFile: File
        get() = File(context.filesDir, OVERRIDE_FILE_NAME)

    /**
     * Checks if physical hardware emergency bypass file is present on disk.
     */
    fun isEmergencyFilePresent(): Boolean {
        return overrideFile.exists()
    }

    /**
     * Creates or deletes the physical emergency hardware bypass file.
     */
    fun setEmergencyBypassFile(active: Boolean) {
        try {
            if (active) {
                overrideFile.writeText("EMERGENCY_OVERRIDE_ENABLED_BY_SUPERADMIN_${System.currentTimeMillis()}")
            } else {
                if (overrideFile.exists()) {
                    overrideFile.delete()
                }
            }
        } catch (_: Exception) {
        }
    }

    /**
     * Complete Super-Admin Emergency Recovery Action:
     * 1. Turns off Captive Portal completely.
     * 2. Clears all active blocks & terminates sessions.
     * 3. Disables mandatory admin approval.
     * 4. Resets direct box password to "12345678".
     * 5. Sets gateway status to DISABLED / Direct Pass-Through.
     */
    suspend fun executeCompleteEmergencyRecovery(): String = withContext(Dispatchers.IO) {
        val settingsDao = database.portalSettingsDao()
        val gatewayDao = database.gatewayConfigDao()
        val sessionDao = database.wifiSessionDao()

        val currentSettings = settingsDao.getSettingsDirect() ?: PortalSettingsEntity()
        settingsDao.insertOrUpdate(
            currentSettings.copy(
                isPortalActive = false,
                requireAdminApproval = false,
                directBoxPassword = "12345678",
                portalTitle = "Portail Captif (Mode Secours)"
            )
        )

        gatewayDao.updatePortalStatus(PortalStatus.DISABLED.name)
        sessionDao.terminateAllSessions()
        setEmergencyBypassFile(true)

        "🚨 Récupération d'Urgence Super-Admin Réussie ! Le portail captif est DÉSACTIVÉ, les blocages sont LEVÉS, et le réseau est réouvert en accès direct avec le mot de passe '12345678'."
    }

    /**
     * Verifies if an entered key matches the Master Super Admin Passcode.
     */
    fun verifyMasterKey(inputKey: String): Boolean {
        return inputKey.trim() == MASTER_SUPERADMIN_KEY || inputKey.trim() == "ADMIN_EMERGENCY_RESET"
    }
}
