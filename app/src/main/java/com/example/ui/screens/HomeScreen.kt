package com.example.ui.screens

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.HowToReg
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.PendingActions
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.SyncProblem
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.entity.FieldDefinitionEntity
import com.example.data.model.DeploymentStep
import com.example.data.model.PortalStatus
import com.example.data.model.UserStatus
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.InfoCyan
import com.example.ui.theme.PurpleAccent
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.WarningAmber
import com.example.ui.viewmodel.WifiManagerViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    viewModel: WifiManagerViewModel,
    onNavigateToPortal: () -> Unit,
    onNavigateToConfig: () -> Unit,
    onNavigateToUsers: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val wifiInfo by viewModel.wifiConnectionInfo.collectAsState()
    val portalSettings by viewModel.portalSettings.collectAsState()
    val gatewayConfig by viewModel.gatewayConfig.collectAsState()
    val systemHealth by viewModel.systemHealth.collectAsState()
    val deployProgress by viewModel.deployProgress.collectAsState()
    val isDeploying by viewModel.isDeploying.collectAsState()
    val allFields by viewModel.allFields.collectAsState()
    val pendingUsers by viewModel.pendingUsers.collectAsState()
    val activeSessions by viewModel.activeSessions.collectAsState()
    val serverStatus by viewModel.serverStatus.collectAsState()

    var showPasswordDialog by remember { mutableStateOf(false) }
    var showScriptDialog by remember { mutableStateOf(false) }
    var showSuperAdminDialog by remember { mutableStateOf(false) }
    var superAdminKeyInput by remember { mutableStateOf("") }
    var superAdminResultMsg by remember { mutableStateOf<String?>(null) }
    var passwordInput by remember(portalSettings.directBoxPassword) { mutableStateOf(portalSettings.directBoxPassword) }
    var passwordVisible by remember { mutableStateOf(false) }

    // Location Permission launcher for precise SSID reading on Android 10+
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.refreshWifiInfo()
        }
    }

    val hasLocationPermission = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // App Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "WiFi Manager",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Centre de contrôle réseau & portail captif",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(
                onClick = {
                    if (!hasLocationPermission) {
                        permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                    } else {
                        viewModel.refreshWifiInfo()
                    }
                },
                modifier = Modifier.testTag("btn_refresh_wifi")
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Actualiser le Wi-Fi",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        // PANEL 1: ARCHITECTURE D'INDÉPENDANCE RÉSEAU (SECTION 6 & 7)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("card_system_health_panel"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "État de l'Architecture Réseau",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (gatewayConfig.status == PortalStatus.ACTIVE) SuccessGreen.copy(alpha = 0.15f)
                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = if (gatewayConfig.status == PortalStatus.ACTIVE) "AUTONOME SUR GATEWAY" else "ARRÊTÉ",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            color = if (gatewayConfig.status == PortalStatus.ACTIVE) SuccessGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 5 Status indicators (Section 7)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // 1. Portail
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Portail Captif", style = MaterialTheme.typography.bodySmall)
                        }
                        val portalText = when (gatewayConfig.status) {
                            PortalStatus.ACTIVE -> "🟢 ACTIF"
                            PortalStatus.DEPLOYING -> "🟡 EN DÉPLOIEMENT"
                            PortalStatus.SYNC_REQUIRED -> "🟠 SYNC REQUISE"
                            PortalStatus.DEGRADED -> "🟠 DÉGRADÉ"
                            PortalStatus.ERROR -> "🔴 ERREUR"
                            PortalStatus.DISABLED -> "⚪ DÉSACTIVÉ"
                        }
                        Text(portalText, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    }

                    // 2. Configuration réseau
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(16.dp), tint = PurpleAccent)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Configuration Réseau", style = MaterialTheme.typography.bodySmall)
                        }
                        val configText = if (gatewayConfig.deployedConfigVersion > 0) {
                            "🟢 DÉPLOYÉE (v${gatewayConfig.deployedConfigVersion})"
                        } else {
                            "⚪ NON DÉPLOYÉE"
                        }
                        Text(configText, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    }

                    // 3. Routeur / Passerelle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Router, contentDescription = null, modifier = Modifier.size(16.dp), tint = InfoCyan)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Routeur / Passerelle", style = MaterialTheme.typography.bodySmall)
                        }
                        val routerText = if (systemHealth.routerOnline) "🟢 CONNECTÉ (${gatewayConfig.gatewayIp})" else "🔴 HORS LIGNE"
                        Text(routerText, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    }

                    // 4. Backend distant
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (systemHealth.backendOnline) Icons.Default.CloudDone else Icons.Default.CloudOff,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = if (systemHealth.backendOnline) SuccessGreen else WarningAmber
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Backend Distant", style = MaterialTheme.typography.bodySmall)
                        }
                        val backendText = if (systemHealth.backendOnline) "🟢 EN LIGNE" else "🟠 HORS LIGNE"
                        Text(backendText, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    }

                    // 5. Application mobile
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp), tint = SuccessGreen)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Application Mobile", style = MaterialTheme.typography.bodySmall)
                        }
                        Text("🟢 CONNECTÉE", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    }
                }

                // Autonomy notice (Section 5)
                if (!systemHealth.backendOnline || gatewayConfig.status == PortalStatus.ACTIVE) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = SuccessGreen,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Réseau opérationnel • Le portail captif fonctionne de manière 100% autonome sur la passerelle, même si le téléphone admin est éteint ou déconnecté.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        // PANEL 2: SYNCHRONISATION NÉCESSAIRE (SECTION 8 & 9)
        if (gatewayConfig.isSyncRequired && gatewayConfig.status == PortalStatus.SYNC_REQUIRED) {
            Card(
                colors = CardDefaults.cardColors(containerColor = WarningAmber.copy(alpha = 0.15f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, WarningAmber),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.SyncProblem, contentDescription = null, tint = WarningAmber)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Désynchronisation détectée", fontWeight = FontWeight.Bold, color = WarningAmber)
                            Text(
                                "Base admin v${gatewayConfig.configVersion} ≠ Déployé v${gatewayConfig.deployedConfigVersion}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                    Button(
                        onClick = { viewModel.syncWithGateway() },
                        colors = ButtonDefaults.buttonColors(containerColor = WarningAmber),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("SYNCHRONISER", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            }
        }

        // CARD 1: DÉTECTION AUTOMATIQUE DU WI-FI CONNECTÉ
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("card_wifi_detector"),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(
                                    if (wifiInfo.isConnected) SuccessGreen.copy(alpha = 0.15f)
                                    else MaterialTheme.colorScheme.errorContainer
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (wifiInfo.isConnected) Icons.Default.Wifi else Icons.Default.WifiOff,
                                contentDescription = null,
                                tint = if (wifiInfo.isConnected) SuccessGreen else MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Wi-Fi Détecté",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = if (wifiInfo.isConnected) wifiInfo.ssid else "Aucun Wi-Fi connecté",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (wifiInfo.isConnected) SuccessGreen.copy(alpha = 0.15f)
                        else MaterialTheme.colorScheme.errorContainer
                    ) {
                        Text(
                            text = if (wifiInfo.isConnected) "CONNECTÉ" else "DÉCONNECTÉ",
                            color = if (wifiInfo.isConnected) SuccessGreen else MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                if (wifiInfo.isConnected) {
                    Spacer(modifier = Modifier.height(16.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Adresse IP locale", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(wifiInfo.ipAddress, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Passerelle / Routeur", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(wifiInfo.gatewayIp, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Signal Wi-Fi", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${wifiInfo.signalPercentage}% (${wifiInfo.rssiDbm} dBm)", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Débit théorique", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${wifiInfo.linkSpeedMbps} Mbps", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    }

                    if (!hasLocationPermission) {
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(
                            onClick = { permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.NetworkCheck, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Autoriser la localisation pour afficher le SSID exact", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }

        // CARD 2: INTERRUPTEUR MAÎTRE DU PORTAIL CAPTIF (ACTIVER / DÉSACTIVER & DÉPLOIEMENT ROUTEUR)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("card_portal_master_toggle"),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (gatewayConfig.status == PortalStatus.ACTIVE)
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            ),
            border = if (gatewayConfig.status == PortalStatus.ACTIVE)
                androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
            else null
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(
                                    if (gatewayConfig.status == PortalStatus.ACTIVE) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (gatewayConfig.status == PortalStatus.ACTIVE) Icons.Default.Security else Icons.Default.Router,
                                contentDescription = null,
                                tint = if (gatewayConfig.status == PortalStatus.ACTIVE) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = if (gatewayConfig.status == PortalStatus.ACTIVE) "Portail Captif ACTIF" else "Portail Captif DÉSACTIVÉ",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                color = if (gatewayConfig.status == PortalStatus.ACTIVE) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (gatewayConfig.status == PortalStatus.ACTIVE)
                                    "Déployé sur le routeur • Fonctionne même téléphone éteint"
                                else "Connexion directe par le mot de passe de la box",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (isDeploying) {
                        CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 2.5.dp)
                    } else {
                        Switch(
                            checked = gatewayConfig.status == PortalStatus.ACTIVE,
                            onCheckedChange = { activate ->
                                viewModel.setPortalActive(activate)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.testTag("switch_portal_active")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (gatewayConfig.status == PortalStatus.ACTIVE) {
                    val authCount = allFields.count { it.isEnabled && it.isAuthKey }
                    val portalCount = allFields.count { it.isEnabled && it.isVisibleInPortal }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = PurpleAccent,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "$authCount critère(s) d'authentification",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    text = "$portalCount champ(s) affichés",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            val authFieldNames = allFields.filter { it.isEnabled && it.isAuthKey }.map { it.label }
                            if (authFieldNames.isNotEmpty()) {
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    authFieldNames.forEach { name ->
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = PurpleAccent.copy(alpha = 0.12f)
                                        ) {
                                            Text(
                                                text = name,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = PurpleAccent,
                                                fontWeight = FontWeight.SemiBold,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Clé Wi-Fi actuelle de la box :",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "••••••••••••",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            OutlinedButton(
                                onClick = { showPasswordDialog = true },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.testTag("btn_edit_box_password")
                            ) {
                                Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Modifier")
                            }
                        }
                    }
                }
            }
        }

        // CARD 3: CHOIX DU MODE D'APPROBATION DES UTILISATEURS
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("card_approval_mode"),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "Mode de Validation d'Accès",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Décidez si les personnes sont confirmées automatiquement ou soumises à votre approbation.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FilterChip(
                        selected = !portalSettings.requireAdminApproval,
                        onClick = { viewModel.setRequireAdminApproval(false) },
                        label = {
                            Text(
                                text = "Validation Automatique",
                                fontWeight = if (!portalSettings.requireAdminApproval) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        leadingIcon = {
                            if (!portalSettings.requireAdminApproval) {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("chip_auto_validation")
                    )

                    FilterChip(
                        selected = portalSettings.requireAdminApproval,
                        onClick = { viewModel.setRequireAdminApproval(true) },
                        label = {
                            Text(
                                text = "Approbation Manuelle",
                                fontWeight = if (portalSettings.requireAdminApproval) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        leadingIcon = {
                            if (portalSettings.requireAdminApproval) {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("chip_manual_approval")
                    )
                }
            }
        }

        // CARD 4: DEMANDES EN ATTENTE
        if (portalSettings.requireAdminApproval && pendingUsers.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("card_pending_approvals"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = WarningAmber.copy(alpha = 0.12f)
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, WarningAmber.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.PendingActions, contentDescription = null, tint = WarningAmber)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${pendingUsers.size} Demande(s) en attente d'approbation",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = WarningAmber
                            )
                        }
                        TextButton(onClick = onNavigateToUsers) {
                            Text("Voir tout")
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    pendingUsers.take(3).forEach { pending ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = pending.getDisplayName(allFields),
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = pending.getSummaryDisplay(allFields),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Row {
                                    IconButton(
                                        onClick = { viewModel.updateUserStatus(pending.user.id, UserStatus.SUSPENDED) }
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "Refuser", tint = MaterialTheme.colorScheme.error)
                                    }
                                    IconButton(
                                        onClick = { viewModel.approveUser(pending.user.id) }
                                    ) {
                                        Icon(Icons.Default.Check, contentDescription = "Approuver", tint = SuccessGreen)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // CARD 5: LIVE NATIVE HTTP SERVER CONCRETE CONTROL
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("card_live_http_server"),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(
                                    if (serverStatus.isRunning) SuccessGreen.copy(alpha = 0.15f)
                                    else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Devices,
                                contentDescription = null,
                                tint = if (serverStatus.isRunning) SuccessGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Serveur Web Portail Local",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (serverStatus.isRunning) "HTTP Actif sur port ${serverStatus.port}" else "Serveur HTTP en pause",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (serverStatus.isRunning) SuccessGreen.copy(alpha = 0.15f) else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
                    ) {
                        Text(
                            text = if (serverStatus.isRunning) "HTTP 🟢 EN LIGNE" else "HTTP 🔴 ARRÊTÉ",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            color = if (serverStatus.isRunning) SuccessGreen else MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                val liveUrl = "http://${wifiInfo.ipAddress.ifEmpty { "192.168.1.105" }}:8080/portal"
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("URL du portail captif :", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        IconButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("Portal URL", liveUrl))
                                Toast.makeText(context, "URL du portail copiée : $liveUrl", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copier", modifier = Modifier.size(16.dp))
                        }
                    }
                    Text(
                        text = liveUrl,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "Requêtes traitées : ${serverStatus.totalRequests} | Accessible depuis n'importe quel navigateur du réseau",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // CARD 6: ACTIONS RAPIDES ET MESURES SUPER-ADMIN
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            FilledTonalButton(
                onClick = onNavigateToPortal,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .testTag("btn_go_to_portal")
            ) {
                Icon(Icons.Default.Wifi, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Portail Client", fontWeight = FontWeight.Bold)
            }

            OutlinedButton(
                onClick = onNavigateToConfig,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .testTag("btn_go_to_config")
            ) {
                Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Critères & Champs", fontWeight = FontWeight.Bold)
            }
        }

        // BUTTON: SUPER-ADMIN EMERGENCY RECOVERY & BUGS EXTRÊMES (PROMPT 1)
        Button(
            onClick = { showSuperAdminDialog = true },
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer
            ),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("btn_super_admin_emergency")
        ) {
            Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("🚨 Mesures Super-Admin & Secours en cas de Bug Extrême", fontWeight = FontWeight.Bold)
        }

        // TOOL: EXPORT SCRIPT ROUTEUR (SECTION 17 & 18)
        OutlinedButton(
            onClick = { showScriptDialog = true },
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("btn_view_openwrt_script")
        ) {
            Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Script d'installation autonome Routeur (OpenWrt / Linux)")
        }
    }

    // DIALOG: SUPER-ADMIN EMERGENCY RECOVERY & DISASTER RESCUE (RESPONDS TO USER PROMPT 1)
    if (showSuperAdminDialog) {
        val liveIp = wifiInfo.ipAddress.ifEmpty { "192.168.1.105" }
        AlertDialog(
            onDismissRequest = {
                showSuperAdminDialog = false
                superAdminResultMsg = null
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Super-Admin & Secours d'Urgence", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = "En cas de bug extrême, blocage complet ou perte d'accès à l'application, voici les 4 moyens de secours physiques et virtuels mis en place :",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("1️⃣ URL HTTP d'Urgence à distance :", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                            Text(
                                text = "http://$liveIp:8080/emergency_reset?key=SUPERADMIN_2026_MASTER",
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text("Ouvrez cette URL dans n'importe quel navigateur pour débloquer immédiatement le réseau.", style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("2️⃣ Clé Maître Super-Admin :", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                            Text("Code Maître: SUPERADMIN_2026_MASTER", style = MaterialTheme.typography.labelSmall, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("3️⃣ Réinitialisation d'Urgence Locale :", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                            Text("Efface les blocages, coupe le portail captif et restaure l'accès Wi-Fi direct box avec le mot de passe '12345678'.", style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    OutlinedTextField(
                        value = superAdminKeyInput,
                        onValueChange = { superAdminKeyInput = it },
                        label = { Text("Saisir la clé Super-Admin") },
                        placeholder = { Text("SUPERADMIN_2026_MASTER") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    superAdminResultMsg?.let { msg ->
                        Text(
                            text = msg,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = if (msg.contains("Réussie")) SuccessGreen else MaterialTheme.colorScheme.error
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.executeEmergencyRecovery(superAdminKeyInput) { msg ->
                            superAdminResultMsg = msg
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Exécuter Réinitialisation d'Urgence")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showSuperAdminDialog = false
                    superAdminResultMsg = null
                }) {
                    Text("Fermer")
                }
            }
        )
    }

    // DIALOG: 7-STEP VERIFIED DEPLOYMENT MODAL (SECTION 2)
    if (deployProgress != null) {
        val progress = deployProgress!!
        AlertDialog(
            onDismissRequest = {
                if (!progress.isDeploying) viewModel.dismissDeployProgress()
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (progress.isDeploying) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.5.dp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Déploiement sur le Routeur...", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    } else if (progress.isSuccess) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SuccessGreen)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Déploiement Réussi !", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = SuccessGreen)
                    } else {
                        Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Échec du Déploiement", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Vérification en 7 étapes avant activation sur la passerelle :",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    DeploymentStep.entries.forEach { step ->
                        val isDone = progress.completedSteps.contains(step)
                        val isCurrent = progress.currentStep == step && progress.isDeploying

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (isCurrent) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(8.dp)
                        ) {
                            if (isDone) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(18.dp))
                            } else if (isCurrent) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "${step.order}. ${step.description}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isDone || isCurrent) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }

                    if (progress.errorMessage != null) {
                        Text(
                            text = progress.errorMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            },
            confirmButton = {
                if (!progress.isDeploying) {
                    Button(onClick = { viewModel.dismissDeployProgress() }) {
                        Text("Fermer")
                    }
                }
            }
        )
    }

    // DIALOG: SCRIPT ROUTEUR OPENWRT / LINUX (SECTION 17 & 18)
    if (showScriptDialog) {
        val script = viewModel.getOpenWrtScript()
        AlertDialog(
            onDismissRequest = { showScriptDialog = false },
            title = {
                Text("Script Autonome Routeur", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Exécutez ce script sur votre routeur (via SSH) pour installer le portail captif persistant qui continue à tourner même si votre téléphone est éteint :",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(8.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            Text(
                                text = script,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("OpenWrt Script", script)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "Script copié dans le presse-papier !", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Copier")
                }
            },
            dismissButton = {
                TextButton(onClick = { showScriptDialog = false }) {
                    Text("Fermer")
                }
            }
        )
    }

    // DIALOG: EDIT BOX PASSWORD
    if (showPasswordDialog) {
        AlertDialog(
            onDismissRequest = { showPasswordDialog = false },
            title = {
                Text(
                    text = "Mot de passe de la Box Wi-Fi",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Ce mot de passe permettra aux utilisateurs de se connecter directement lorsque le portail captif est désactivé.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = passwordInput,
                        onValueChange = { passwordInput = it },
                        label = { Text("Mot de passe Box") },
                        singleLine = true,
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = null
                                )
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_edit_box_pwd")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (passwordInput.isNotBlank()) {
                            viewModel.setDirectBoxPassword(passwordInput.trim())
                            showPasswordDialog = false
                        }
                    },
                    modifier = Modifier.testTag("btn_save_box_pwd")
                ) {
                    Text("Enregistrer")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPasswordDialog = false }) {
                    Text("Annuler")
                }
            }
        )
    }
}
