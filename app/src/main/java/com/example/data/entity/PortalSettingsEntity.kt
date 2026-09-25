package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "portal_settings")
data class PortalSettingsEntity(
    @PrimaryKey
    val id: Int = 1,
    val isPortalActive: Boolean = true, // Portail activé ou désactivé
    val requireAdminApproval: Boolean = false, // false = confirmation automatique, true = approbation manuelle de l'admin
    val approvalPolicy: String = "PRE_REGISTERED_AUTO", // PRE_REGISTERED_AUTO, ALL_AUTO, ALL_PENDING, GUESTS_PENDING
    val requireGuestApproval: Boolean = false, // Si true, le mode invité requiert une validation manuelle
    val directBoxPassword: String = "WiFiPass2026!", // Mot de passe standard de la box quand portail désactivé
    val networkSsid: String = "Campus-WiFi-Secure",
    val portalTitle: String = "WIFI MANAGER",
    val portalSubtitle: String = "Portail Captif d'Authentification",
    val welcomeMessage: String = "Connectez-vous avec vos identifiants autorisés pour activer l'accès Internet.",
    val guestModeEnabled: Boolean = true,
    val guestDurationHours: Int = 2,
    val sessionDurationHours: Int = 24,
    val autoAuthorizeMac: Boolean = true
)
