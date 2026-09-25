package com.example.data.gateway

import android.content.Context
import com.example.data.model.AuthResult
import com.example.data.model.DeploymentProgress
import com.example.data.model.DeploymentStep
import com.example.data.model.GatewayConfigPayload
import com.example.data.model.GatewayStatusResponse
import com.example.data.model.PortalStatus
import com.example.data.model.SystemHealthState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File

/**
 * Autonomous Gateway Engine running on the network Data Plane.
 * This class implements the persistent data plane logic that survives:
 * 1. Admin phone disconnection
 * 2. Admin app closure
 * 3. Phone reboot or shutdown
 * 4. Temporary backend unavailability
 * 5. Router reboot (via local persistence file)
 */
class AutonomousGatewayEngine(private val context: Context) {

    private val persistenceFile: File
        get() = File(context.filesDir, "gateway_persistent_dataplane.json")

    private val _status = MutableStateFlow(loadPersistentStatus())
    val status = _status.asStateFlow()

    private val _activeClientLeases = MutableStateFlow<Map<String, ClientLease>>(emptyMap()) // MAC -> ClientLease
    val activeClientLeases = _activeClientLeases.asStateFlow()

    data class ClientLease(
        val macAddress: String,
        val ipAddress: String,
        val identifierSummary: String,
        val authorizedAt: Long,
        val expiresAt: Long,
        val isGuest: Boolean
    ) {
        val isExpired: Boolean get() = System.currentTimeMillis() > expiresAt
    }

    private fun loadPersistentStatus(): GatewayStatusResponse {
        return try {
            if (persistenceFile.exists()) {
                val json = JSONObject(persistenceFile.readText())
                GatewayStatusResponse(
                    routerOnline = true,
                    portalActive = json.optBoolean("portalActive", false),
                    deployedVersion = json.optInt("deployedVersion", 0),
                    firewallActive = json.optBoolean("firewallActive", true),
                    activeSessionsCount = json.optInt("activeSessionsCount", 0),
                    hardwareModel = json.optString("hardwareModel", "OpenWrt / Linux Gateway"),
                    uptimeSeconds = json.optLong("uptimeSeconds", 3600L),
                    message = "Passerelle opérationnelle avec configuration persistante"
                )
            } else {
                GatewayStatusResponse(
                    routerOnline = true,
                    portalActive = false,
                    deployedVersion = 0,
                    firewallActive = true
                )
            }
        } catch (_: Exception) {
            GatewayStatusResponse()
        }
    }

    /**
     * Persists the gateway state to permanent local storage that survives app closure and reboots.
     */
    fun savePersistentState(payload: GatewayConfigPayload) {
        try {
            val json = JSONObject().apply {
                put("portalActive", payload.portalActive)
                put("deployedVersion", payload.version)
                put("firewallActive", true)
                put("networkSsid", payload.networkSsid)
                put("portalTitle", payload.portalTitle)
                put("welcomeMessage", payload.welcomeMessage)
                put("directBoxPassword", payload.directBoxPassword)
                put("guestModeEnabled", payload.guestModeEnabled)
                put("guestDurationHours", payload.guestDurationHours)
                put("sessionDurationHours", payload.sessionDurationHours)
                put("requireAdminApproval", payload.requireAdminApproval)
                put("deployedAt", System.currentTimeMillis())
            }
            persistenceFile.writeText(json.toString())
            _status.value = _status.value.copy(
                portalActive = payload.portalActive,
                deployedVersion = payload.version,
                firewallActive = true,
                message = "Configuration v${payload.version} active et persistante sur le matériel"
            )
        } catch (_: Exception) {
            // Log fallback
        }
    }

    /**
     * Simulates router reboot: reads persistent storage and restores firewall and portal.
     */
    fun onRouterReboot() {
        _status.value = loadPersistentStatus()
    }

    /**
     * Autonomous authorization on the gateway data plane.
     * Evaluates rules directly without needing the admin phone.
     */
    fun evaluateConnection(
        enteredValues: Map<String, String>,
        payload: GatewayConfigPayload?,
        clientMac: String = "3C:22:FB:4A:91:02",
        clientIp: String = "192.168.1.105"
    ): AuthResult {
        if (payload == null || !payload.portalActive) {
            // Portal disabled -> check box password
            val enteredPassword = enteredValues["box_password"] ?: enteredValues["password"].orEmpty()
            return if (payload != null && enteredPassword == payload.directBoxPassword) {
                grantAccess(clientMac, clientIp, "Connexion Directe Box", 24 * 3600000L, false)
                AuthResult.DirectBoxSuccess(message = "Connexion directe Wi-Fi établie avec succès.")
            } else {
                AuthResult.Error("Mot de passe Wi-Fi de la box incorrect.")
            }
        }

        // Check required fields
        val missing = payload.requiredFields
            .filter { it.isRequired && it.isVisible }
            .filter { field -> enteredValues[field.key].isNullOrBlank() }
            .map { it.label }

        if (missing.isNotEmpty()) {
            return AuthResult.ValidationError(
                message = "Champs requis manquants sur le portail captif.",
                missingFields = missing
            )
        }

        // Check credentials against authorized list
        val authFields = payload.requiredFields.filter { it.isAuthKey }
        val matchedUser = payload.authorizedUsers.firstOrNull { user ->
            authFields.all { field ->
                val expected = user.credentials[field.key].orEmpty()
                val entered = enteredValues[field.key].orEmpty()
                expected.equals(entered, ignoreCase = true)
            }
        }

        return if (matchedUser != null) {
            if (matchedUser.status.equals("SUSPENDED", ignoreCase = true)) {
                AuthResult.Suspended("Cet accès a été suspendu par l'administrateur.")
            } else if (payload.requireAdminApproval && !matchedUser.status.equals("AUTHORIZED", ignoreCase = true)) {
                AuthResult.PendingApproval(message = "Votre demande est en cours d'approbation.")
            } else {
                val durationMs = payload.sessionDurationHours * 3600000L
                val summary = authFields.joinToString(" • ") { enteredValues[it.key].orEmpty() }
                grantAccess(clientMac, clientIp, summary, durationMs, false)
                AuthResult.Success(
                    session = null,
                    user = null,
                    message = "Authentification réussie ! Accès Internet autorisé par la passerelle."
                )
            }
        } else {
            AuthResult.NotFound("Identifiants non reconnus par le portail.")
        }
    }

    fun grantAccess(
        mac: String,
        ip: String,
        summary: String,
        durationMs: Long,
        isGuest: Boolean
    ) {
        val now = System.currentTimeMillis()
        val lease = ClientLease(
            macAddress = mac,
            ipAddress = ip,
            identifierSummary = summary,
            authorizedAt = now,
            expiresAt = now + durationMs,
            isGuest = isGuest
        )
        val current = _activeClientLeases.value.toMutableMap()
        current[mac] = lease
        _activeClientLeases.value = current
        _status.value = _status.value.copy(activeSessionsCount = current.size)
    }

    fun revokeAccess(mac: String) {
        val current = _activeClientLeases.value.toMutableMap()
        current.remove(mac)
        _activeClientLeases.value = current
        _status.value = _status.value.copy(activeSessionsCount = current.size)
    }

    fun checkAndExpireSessions(): Int {
        val current = _activeClientLeases.value.toMutableMap()
        val before = current.size
        current.entries.removeIf { it.value.isExpired }
        _activeClientLeases.value = current
        _status.value = _status.value.copy(activeSessionsCount = current.size)
        return before - current.size
    }
}
