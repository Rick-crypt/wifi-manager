package com.example.data.model

/**
 * Status of the Captive Portal on the Gateway Data Plane.
 * Conforms to Section 15: DISABLED, DEPLOYING, ACTIVE, DEGRADED, ERROR, SYNC_REQUIRED
 */
enum class PortalStatus {
    DISABLED,       // Portail explicitement désactivé sur le gateway
    DEPLOYING,      // Déploiement en cours vers le routeur/gateway
    ACTIVE,         // Opérationnel et appliqué par le firewall du gateway
    DEGRADED,       // Fonctionnel mais composant secondaire en anomalie
    ERROR,          // Erreur d'application sur le gateway
    SYNC_REQUIRED   // Changement d'admin non encore déployé sur le routeur
}

/**
 * Explicit multi-node system health state as requested in Section 6 & 7:
 * NETWORK_ACTIVE, PORTAL_ACTIVE, ADMIN_ONLINE, BACKEND_ONLINE, ROUTER_ONLINE
 */
data class SystemHealthState(
    val networkActive: Boolean = true,     // Réseau Wi-Fi/LAN connecté
    val portalActive: Boolean = false,     // Portail actif et filtrant sur le gateway
    val adminOnline: Boolean = true,       // Application mobile admin en cours d'exécution
    val backendOnline: Boolean = true,     // Backend cloud / API centrale joignable
    val routerOnline: Boolean = true       // Passerelle / Routeur joignable sur le réseau local
) {
    /**
     * Le portail continue à fonctionner de manière autonome sur le gateway même si
     * l'application admin ou le backend distant sont hors ligne !
     */
    val isAutonomousOperationActive: Boolean
        get() = portalActive && routerOnline && (!adminOnline || !backendOnline)
}

/**
 * 7-step deployment lifecycle as specified in Section 2:
 * 1. Router reachability
 * 2. Hardware captive portal support check
 * 3. Configuration generation
 * 4. Deployment to gateway
 * 5. Firewall verification
 * 6. DNS/DHCP verification
 * 7. Mark as DEPLOYED / ACTIVE
 */
enum class DeploymentStep(val order: Int, val description: String) {
    CHECK_ROUTER(1, "Vérification de la disponibilité du routeur / passerelle"),
    CHECK_CAPABILITIES(2, "Vérification du support matériel (firewall, captive engine)"),
    GENERATE_CONFIG(3, "Génération du payload de configuration persistante"),
    DEPLOY_TO_GATEWAY(4, "Déploiement de la configuration sur le gateway"),
    VERIFY_FIREWALL(5, "Contrôle des règles de filtrage firewall (iptables/nftables)"),
    VERIFY_DNS_DHCP(6, "Contrôle des redirections DNS/DHCP (dnsmasq)"),
    COMMIT_ACTIVE(7, "Activation finale & enregistrement DEPLOYED/ACTIVE")
}

data class DeploymentProgress(
    val currentStep: DeploymentStep? = null,
    val isDeploying: Boolean = false,
    val completedSteps: List<DeploymentStep> = emptyList(),
    val errorMessage: String? = null,
    val isSuccess: Boolean = false
)

/**
 * Payload sent to the router/gateway to configure its persistent local data plane.
 */
data class GatewayConfigPayload(
    val version: Int,
    val portalActive: Boolean,
    val networkSsid: String,
    val portalTitle: String,
    val portalSubtitle: String,
    val welcomeMessage: String,
    val guestModeEnabled: Boolean,
    val guestDurationHours: Int,
    val sessionDurationHours: Int,
    val requireAdminApproval: Boolean,
    val directBoxPassword: String,
    val requiredFields: List<FieldConfigDto>,
    val authorizedUsers: List<UserCredentialDto>,
    val timestamp: Long = System.currentTimeMillis()
)

data class FieldConfigDto(
    val id: Long,
    val key: String,
    val label: String,
    val type: String,
    val isRequired: Boolean,
    val isAuthKey: Boolean,
    val isVisible: Boolean,
    val optionsJson: String
)

data class UserCredentialDto(
    val id: Long,
    val status: String,
    val credentials: Map<String, String> // key -> value
)

data class GatewayStatusResponse(
    val routerOnline: Boolean = true,
    val portalActive: Boolean = false,
    val deployedVersion: Int = 0,
    val firewallActive: Boolean = true,
    val activeSessionsCount: Int = 0,
    val hardwareModel: String = "OpenWrt / Linux Embedded Gateway",
    val uptimeSeconds: Long = 86400L,
    val message: String = "Gateway opérationnel"
)
