package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.HowToReg
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.PendingActions
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.entity.FieldDefinitionEntity
import com.example.data.entity.PortalSettingsEntity
import com.example.ui.components.FieldConfigCard
import com.example.ui.components.FieldEditDialog
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.WarningAmber
import com.example.ui.viewmodel.WifiManagerViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ConfigurationScreen(
    viewModel: WifiManagerViewModel,
    modifier: Modifier = Modifier
) {
    val fields by viewModel.allFields.collectAsState()
    val portalSettings by viewModel.portalSettings.collectAsState()
    val wifiInfo by viewModel.wifiConnectionInfo.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    var showAddFieldDialog by remember { mutableStateOf(false) }
    var fieldToEdit by remember { mutableStateOf<FieldDefinitionEntity?>(null) }
    var showTemplatesDialog by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    Column(modifier = modifier.fillMaxSize()) {
        // Tab row between Fields config and Network/Portal config
        TabRow(
            selectedTabIndex = selectedTab,
            modifier = Modifier.fillMaxWidth()
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Critères d'Accès") },
                icon = { Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Paramètres Portail & Box") },
                icon = { Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (selectedTab == 0) {
                // TAB 0: CHAMPS D'IDENTIFICATION & AUTHENTIFICATION

                // Educational Banner on Dynamic Configuration
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .size(22.dp)
                                .padding(top = 2.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Identification dynamique & Clés d'authentification",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "L'administrateur choisit les champs demandés sans rien coder en dur. Activez « Facteur d'Authentification » sur les champs qui servent de clé de connexion Wi-Fi.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                            )
                        }
                    }
                }

                // Action buttons: Add Field & Templates
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { showAddFieldDialog = true },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_add_field")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Ajouter un champ")
                    }

                    OutlinedButton(
                        onClick = { showTemplatesDialog = true },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_open_templates")
                    ) {
                        Icon(Icons.Default.AutoFixHigh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Modèles types")
                    }
                }

                // Ordered fields list
                Text(
                    text = "FORMULAIRE DE CONNEXION (${fields.size} champs)",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                fields.forEachIndexed { index, field ->
                    FieldConfigCard(
                        field = field,
                        isFirst = index == 0,
                        isLast = index == fields.lastIndex,
                        onMoveUp = { viewModel.moveField(field.id, directionUp = true) },
                        onMoveDown = { viewModel.moveField(field.id, directionUp = false) },
                        onEdit = { fieldToEdit = field },
                        onDelete = { viewModel.deleteField(field.id) },
                        onToggleEnabled = { viewModel.toggleFieldEnabled(field.id) },
                        onToggleAuthKey = { viewModel.toggleFieldAuthKey(field.id) },
                        onTogglePortalVisible = { viewModel.toggleFieldPortalVisible(field.id) },
                        onToggleRequired = { viewModel.toggleFieldRequired(field.id) }
                    )
                }
            } else {
                // TAB 1: PARAMÈTRES DU PORTAIL, BOX & APPROBATION
                var isPortalActive by remember(portalSettings) { mutableStateOf(portalSettings.isPortalActive) }
                var requireAdminApproval by remember(portalSettings) { mutableStateOf(portalSettings.requireAdminApproval) }
                var approvalPolicy by remember(portalSettings) { mutableStateOf(portalSettings.approvalPolicy) }
                var requireGuestApproval by remember(portalSettings) { mutableStateOf(portalSettings.requireGuestApproval) }
                var directBoxPassword by remember(portalSettings) { mutableStateOf(portalSettings.directBoxPassword) }
                var networkSsid by remember(portalSettings) { mutableStateOf(portalSettings.networkSsid) }
                var portalTitle by remember(portalSettings) { mutableStateOf(portalSettings.portalTitle) }
                var portalSubtitle by remember(portalSettings) { mutableStateOf(portalSettings.portalSubtitle) }
                var welcomeMessage by remember(portalSettings) { mutableStateOf(portalSettings.welcomeMessage) }
                var guestModeEnabled by remember(portalSettings) { mutableStateOf(portalSettings.guestModeEnabled) }
                var guestDurationHours by remember(portalSettings) { mutableStateOf(portalSettings.guestDurationHours.toString()) }
                var sessionDurationHours by remember(portalSettings) { mutableStateOf(portalSettings.sessionDurationHours.toString()) }
                var savedNotification by remember { mutableStateOf(false) }

                // Quick auto-fill SSID from detected WiFi
                if (wifiInfo.isConnected && wifiInfo.ssid.isNotBlank() && wifiInfo.ssid != "Wi-Fi Connecté" && networkSsid != wifiInfo.ssid) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Wi-Fi détecté : ${wifiInfo.ssid}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                                Text("Voulez-vous synchroniser le SSID du portail avec ce réseau ?", style = MaterialTheme.typography.labelSmall)
                            }
                            FilledTonalButton(
                                onClick = { networkSsid = wifiInfo.ssid },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Copier", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }

                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "Contrôle du Portail & Mode de Connexion",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        // Master Portal Switch
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (isPortalActive) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (isPortalActive) "Portail Captif Activé" else "Portail Captif Désactivé",
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = if (isPortalActive) "Identification par critères requise" else "Accès direct par mot de passe box",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = isPortalActive,
                                    onCheckedChange = { isPortalActive = it }
                                )
                            }
                        }

                        // Direct Box Password
                        OutlinedTextField(
                            value = directBoxPassword,
                            onValueChange = { directBoxPassword = it },
                            label = { Text("Mot de passe direct de la Box") },
                            leadingIcon = { Icon(Icons.Default.Key, contentDescription = null) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Granular Access Approval Policy Section
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(
                                    text = "Règles d'Approbation des Utilisateurs",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Définissez qui entre automatiquement et qui doit attendre votre validation manuelle :",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                // Policy 1: Pre-registered Auto, New Pending
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (approvalPolicy == "PRE_REGISTERED_AUTO") MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f) else MaterialTheme.colorScheme.surface,
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (approvalPolicy == "PRE_REGISTERED_AUTO") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                    ),
                                    onClick = { approvalPolicy = "PRE_REGISTERED_AUTO"; requireAdminApproval = false },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.HowToReg, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Pré-enregistrés Auto • Nouveaux en Attente", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                                            Text("Les membres enregistrés passent tout de suite. Les nouveaux inconnus attendent votre accord.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }

                                // Policy 2: All Auto
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (approvalPolicy == "ALL_AUTO") MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f) else MaterialTheme.colorScheme.surface,
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (approvalPolicy == "ALL_AUTO") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                    ),
                                    onClick = { approvalPolicy = "ALL_AUTO"; requireAdminApproval = false },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = SuccessGreen)
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Validation Automatique pour Tous", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                                            Text("Connexion immédiate si les critères sont remplis. Aucune attente.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }

                                // Policy 3: All Pending
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (approvalPolicy == "ALL_PENDING") MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f) else MaterialTheme.colorScheme.surface,
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (approvalPolicy == "ALL_PENDING") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                    ),
                                    onClick = { approvalPolicy = "ALL_PENDING"; requireAdminApproval = true },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.PendingActions, contentDescription = null, tint = WarningAmber)
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Approbation Manuelle Oblligatoire pour Tous", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                                            Text("100% des personnes doivent être validées manuellement par l'admin.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }

                                // Policy 4: Guests Pending
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (approvalPolicy == "GUESTS_PENDING") MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f) else MaterialTheme.colorScheme.surface,
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (approvalPolicy == "GUESTS_PENDING") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                    ),
                                    onClick = { approvalPolicy = "GUESTS_PENDING"; requireGuestApproval = true },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Membres Auto • Invités en Attente", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                                            Text("Les membres reconnus entrent directement. Les invités (Mode Invité) attendent votre accord.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Valider aussi les Invités à la main", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                        Text("Force les demandes invités à passer en attente d'approbation", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Switch(
                                        checked = requireGuestApproval,
                                        onCheckedChange = { requireGuestApproval = it },
                                        colors = SwitchDefaults.colors(checkedThumbColor = WarningAmber)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "Diffusion Réseau & Identité",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        OutlinedTextField(
                            value = networkSsid,
                            onValueChange = { networkSsid = it },
                            label = { Text("Nom du réseau Wi-Fi (SSID)") },
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Default.Wifi, contentDescription = null) },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_network_ssid")
                        )

                        OutlinedTextField(
                            value = portalTitle,
                            onValueChange = { portalTitle = it },
                            label = { Text("Titre du portail captif") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = portalSubtitle,
                            onValueChange = { portalSubtitle = it },
                            label = { Text("Sous-titre") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = welcomeMessage,
                            onValueChange = { welcomeMessage = it },
                            label = { Text("Message de bienvenue / Consigne d'accès") },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = sessionDurationHours,
                            onValueChange = { sessionDurationHours = it.filter { ch -> ch.isDigit() } },
                            label = { Text("Durée de session standard (en heures)") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Guest Mode section
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Autoriser le Mode Invité", fontWeight = FontWeight.Bold)
                                        Text(
                                            "Permet une connexion rapide sur demande de Nom + Téléphone",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Switch(
                                        checked = guestModeEnabled,
                                        onCheckedChange = { guestModeEnabled = it },
                                        modifier = Modifier.testTag("switch_guest_mode")
                                    )
                                }

                                if (guestModeEnabled) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    OutlinedTextField(
                                        value = guestDurationHours,
                                        onValueChange = { guestDurationHours = it.filter { ch -> ch.isDigit() } },
                                        label = { Text("Durée d'accès invité (heures)") },
                                        singleLine = true,
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }

                        if (savedNotification) {
                            Text(
                                text = "✓ Paramètres réseau et portail enregistrés avec succès !",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = SuccessGreen
                            )
                        }

                        Button(
                            onClick = {
                                val updated = portalSettings.copy(
                                    isPortalActive = isPortalActive,
                                    requireAdminApproval = requireAdminApproval,
                                    approvalPolicy = approvalPolicy,
                                    requireGuestApproval = requireGuestApproval,
                                    directBoxPassword = directBoxPassword.trim().ifEmpty { "WiFiPass2026!" },
                                    networkSsid = networkSsid.trim().ifEmpty { "Campus-WiFi-Secure" },
                                    portalTitle = portalTitle.trim().ifEmpty { "WIFI MANAGER" },
                                    portalSubtitle = portalSubtitle.trim(),
                                    welcomeMessage = welcomeMessage.trim(),
                                    guestModeEnabled = guestModeEnabled,
                                    guestDurationHours = guestDurationHours.toIntOrNull() ?: 2,
                                    sessionDurationHours = sessionDurationHours.toIntOrNull() ?: 24
                                )
                                viewModel.updatePortalSettings(updated)
                                savedNotification = true
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("btn_save_network_settings")
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Enregistrer les paramètres")
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Gateway Data Plane & Resilience Tests (Section 5, 11, 13)
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Router, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Passerelle Réseau & Survie Hors Ligne", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                }

                                Text(
                                    text = "Le portail captif est exécuté sur la passerelle réseau. Testez ici la résilience en cas de coupure du backend ou de redémarrage du routeur :",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    val isBackendOnline = viewModel.systemHealth.collectAsState().value.backendOnline
                                    OutlinedButton(
                                        onClick = { viewModel.setBackendOnline(!isBackendOnline) },
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            text = if (isBackendOnline) "Couper Backend" else "Rétablir Backend",
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }

                                    OutlinedButton(
                                        onClick = { viewModel.simulateRouterReboot() },
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Reboot Routeur", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Dialog: Add / Edit Field
    if (showAddFieldDialog || fieldToEdit != null) {
        FieldEditDialog(
            fieldToEdit = fieldToEdit,
            onDismiss = {
                showAddFieldDialog = false
                fieldToEdit = null
            },
            onSave = { field ->
                viewModel.saveField(field)
                showAddFieldDialog = false
                fieldToEdit = null
            }
        )
    }

    // Dialog: Templates selector (Section 13)
    if (showTemplatesDialog) {
        AlertDialog(
            onDismissRequest = { showTemplatesDialog = false },
            title = {
                Text(
                    text = "Modèles de configuration pré-définis",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Appliquez en 1 clic une combinaison d'identification adaptée à votre cas d'usage :",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Template 1: Campus / Cité Universitaire
                    FilledTonalButton(
                        onClick = {
                            viewModel.applyPresetTemplate("CAMPUS_RESIDENCE")
                            showTemplatesDialog = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text("🎓 Campus & Résidence Étudiante", fontWeight = FontWeight.Bold)
                            Text(
                                "Passeport/CNI + Nationalité + Promotion",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                    // Template 2: Université / Étudiant
                    FilledTonalButton(
                        onClick = {
                            viewModel.applyPresetTemplate("UNIVERSITY_STUDENT")
                            showTemplatesDialog = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text("🏛️ Université & Grandes Écoles", fontWeight = FontWeight.Bold)
                            Text(
                                "Numéro étudiant + Date de naissance + Filière",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                    // Template 3: Hôtel / Résidence
                    FilledTonalButton(
                        onClick = {
                            viewModel.applyPresetTemplate("HOTEL_ROOM")
                            showTemplatesDialog = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text("🏨 Hôtel & Hébergement", fontWeight = FontWeight.Bold)
                            Text(
                                "Numéro de chambre + Code personnel (PIN)",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                    // Template 4: Entreprise / Événement
                    FilledTonalButton(
                        onClick = {
                            viewModel.applyPresetTemplate("EVENT_BADGE")
                            showTemplatesDialog = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text("💼 Conférence & Événement", fontWeight = FontWeight.Bold)
                            Text(
                                "Code badge + Téléphone + Entreprise",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showTemplatesDialog = false }) {
                    Text("Fermer")
                }
            }
        )
    }
}
