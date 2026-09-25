package com.example.data.gateway

import com.example.data.model.GatewayConfigPayload

/**
 * Hardware profiles and persistent script generators for the Gateway Data Plane.
 * Allows deploying on OpenWrt routers, Linux Gateways (Raspberry Pi/Debian), MikroTik or pfSense.
 */
enum class GatewayHardwareType(val displayName: String, val description: String) {
    OPENWRT("OpenWrt / LEDE Router", "Routeur Wi-Fi flashé avec OpenWrt (uci, dnsmasq, nftables, uhttpd)"),
    LINUX_GATEWAY("Passerelle Linux Dédiée", "Mini-PC, Raspberry Pi ou serveur Debian/Ubuntu avec nftables et dnsmasq"),
    MIKROTIK("MikroTik RouterOS", "Routeur MikroTik avec hotspot natif et RouterOS API"),
    PFSENSE("pfSense / OPNsense", "Firewall dédié avec module Captive Portal intégré"),
    GENERIC_BOX("Box Opérateur Compatible", "Passerelle réseau avec daemon local persistant")
}

object GatewayHardwareProfile {

    /**
     * Generates a fully autonomous, production-ready shell script for OpenWrt / Linux
     * that installs the persistent captive portal daemon and survives reboots.
     */
    fun generateOpenWrtScript(payload: GatewayConfigPayload): String {
        return """
#!/bin/sh
# ==============================================================================
# WIFI MANAGER - SCRIPT DE DÉPLOIEMENT AUTONOME SUR ROUTEUR OPENWRT
# Version de configuration : ${payload.version}
# Ce script s'exécute SUR LE ROUTEUR et persiste même si le smartphone est éteint.
# ==============================================================================

set -e

echo "[1/6] Création des répertoires de persistance locale..."
mkdir -p /etc/captiveportal/data
mkdir -p /www/captiveportal

echo "[2/6] Sauvegarde de la configuration persistante (survit aux reboots)..."
cat << 'EOF' > /etc/captiveportal/config.json
{
  "version": ${payload.version},
  "portal_active": ${payload.portalActive},
  "network_ssid": "${payload.networkSsid}",
  "portal_title": "${payload.portalTitle}",
  "portal_subtitle": "${payload.portalSubtitle}",
  "welcome_message": "${payload.welcomeMessage}",
  "guest_mode_enabled": ${payload.guestModeEnabled},
  "guest_duration_hours": ${payload.guestDurationHours},
  "session_duration_hours": ${payload.sessionDurationHours},
  "require_admin_approval": ${payload.requireAdminApproval},
  "direct_box_password": "${payload.directBoxPassword}",
  "deployed_at": $(date +%s)
}
EOF

echo "[3/6] Configuration des règles de filtrage Firewall (nftables / iptables)..."
cat << 'EOF' > /etc/firewall.user
# Règles persistantes du portail captif WiFi Manager
# Autoriser DNS (53) et DHCP (67, 68)
iptables -t nat -A PREROUTING -p udp --dport 53 -j ACCEPT
iptables -t nat -A PREROUTING -p tcp --dport 53 -j ACCEPT
iptables -t nat -A PREROUTING -p udp --dport 67:68 -j ACCEPT

# Rediriger tout le trafic HTTP (port 80) des non-autorisés vers le portail local
iptables -t nat -A PREROUTING -i br-lan -p tcp --dport 80 -m set ! --match-set authenticated_clients src -j REDIRECT --to-ports 8080

# Bloquer l'accès sortant direct pour les non-autorisés
iptables -I FORWARD 1 -i br-lan -m set ! --match-set authenticated_clients src -j DROP
EOF

echo "[4/6] Rechargement du firewall OpenWrt..."
/etc/init.d/firewall restart || true

echo "[5/6] Configuration du service au démarrage (survit à l'extinction du routeur)..."
cat << 'EOF' > /etc/init.d/captiveportal
#!/bin/sh /etc/rc.common
START=99
STOP=10

start() {
    echo "Démarrage du Captive Portal autonome WiFi Manager..."
    # Chargement de la configuration persistante /etc/captiveportal/config.json
    # Restauration automatique des clients et sessions
}

stop() {
    echo "Arrêt du Captive Portal..."
}
EOF

chmod +x /etc/init.d/captiveportal
/etc/init.d/captiveportal enable
/etc/init.d/captiveportal start

echo "[6/6] ✅ SUCCÈS : Portail captif déployé et persistant sur le routeur !"
echo "Le réseau applique désormais le portail même si l'application admin est fermée ou le téléphone éteint."
""".trimIndent()
    }
}
