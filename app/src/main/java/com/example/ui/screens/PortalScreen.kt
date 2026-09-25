package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PendingActions
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AuthResult
import com.example.ui.components.DynamicInputField
import com.example.ui.theme.InfoCyan
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.WarningAmber
import com.example.ui.viewmodel.WifiManagerViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PortalScreen(
    viewModel: WifiManagerViewModel,
    modifier: Modifier = Modifier
) {
    val wifiInfo by viewModel.wifiConnectionInfo.collectAsState()
    val portalFields by viewModel.portalFields.collectAsState()
    val authFields by viewModel.authFields.collectAsState()
    val portalSettings by viewModel.portalSettings.collectAsState()
    val portalValues by viewModel.portalValues.collectAsState()
    val directBoxPasswordInput by viewModel.directBoxPasswordInput.collectAsState()
    val authResult by viewModel.authResult.collectAsState()
    val isConnecting by viewModel.isConnecting.collectAsState()
    val currentClientSession by viewModel.currentClientSession.collectAsState()
    val isGuestModeActive by viewModel.isGuestModeActive.collectAsState()
    val guestName by viewModel.guestName.collectAsState()
    val guestPhone by viewModel.guestPhone.collectAsState()

    var directPasswordVisible by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    val effectiveSsid = if (wifiInfo.isConnected && wifiInfo.ssid.isNotBlank()) {
        wifiInfo.ssid
    } else {
        portalSettings.networkSsid
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Network Header Badge
        Surface(
            shape = RoundedCornerShape(30.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Wifi,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Réseau : $effectiveSsid • Passerelle Autonome",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Portal Title & Subtitle
        Text(
            text = if (portalSettings.isPortalActive) portalSettings.portalTitle else "Connexion Directe Box",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.sp,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = if (portalSettings.isPortalActive) portalSettings.portalSubtitle
            else "Le portail captif est désactivé. Saisissez le mot de passe Wi-Fi de la box.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(20.dp))

        // CASE 1: CLIENT IS CONNECTED
        if (currentClientSession != null && currentClientSession!!.isActive) {
            val session = currentClientSession!!
            val timeFormat = SimpleDateFormat("HH:mm", Locale.FRENCH)
            val expiresStr = timeFormat.format(Date(session.expiresAt))

            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("portal_connected_card"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .clip(CircleShape)
                            .background(SuccessGreen.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = SuccessGreen,
                            modifier = Modifier.size(40.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Accès Wi-Fi Autorisé !",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Votre appareil est connecté et navigue sur Internet en toute sécurité.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Connection metrics box
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Identifiant validé", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(session.identifierSummary, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Adresse IP attribuée", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(session.ipAddress, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Adresse MAC", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(session.deviceMac, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Session active jusqu'à", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(expiresStr, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = { viewModel.disconnectClientSession() },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("portal_disconnect_button")
                    ) {
                        Icon(Icons.Default.LinkOff, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Déconnecter cet appareil")
                    }
                }
            }
        }
        // CASE 2: PORTAL IS DISABLED -> DIRECT BOX PASSWORD FORM
        else if (!portalSettings.isPortalActive) {
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("card_direct_box_login"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(modifier = Modifier.padding(22.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Router,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Connexion Wi-Fi Standard",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Portail désactivé par l'administrateur",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Veuillez entrer la clé de sécurité Wi-Fi de la box pour vous connecter directement :",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = directBoxPasswordInput,
                        onValueChange = { viewModel.directBoxPasswordInput.value = it },
                        label = { Text("Mot de passe de la Box Wi-Fi") },
                        placeholder = { Text("Entrez la clé de sécurité") },
                        singleLine = true,
                        visualTransformation = if (directPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        leadingIcon = { Icon(Icons.Default.Key, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                        trailingIcon = {
                            IconButton(onClick = { directPasswordVisible = !directPasswordVisible }) {
                                Icon(
                                    imageVector = if (directPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = null
                                )
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_direct_box_password")
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Auth Results Banners
                    AnimatedVisibility(
                        visible = authResult != null,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        authResult?.let { result ->
                            val (bgColor, tintColor, message) = when (result) {
                                is AuthResult.DirectBoxSuccess -> Triple(
                                    SuccessGreen.copy(alpha = 0.15f),
                                    SuccessGreen,
                                    result.message
                                )
                                is AuthResult.Error -> Triple(
                                    MaterialTheme.colorScheme.errorContainer,
                                    MaterialTheme.colorScheme.error,
                                    result.message
                                )
                                else -> Triple(
                                    MaterialTheme.colorScheme.errorContainer,
                                    MaterialTheme.colorScheme.error,
                                    "Erreur de connexion"
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = bgColor,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (result is AuthResult.DirectBoxSuccess) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                                        contentDescription = null,
                                        tint = tintColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = message,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = tintColor
                                    )
                                }
                            }
                        }
                    }

                    Button(
                        onClick = { viewModel.authenticateDirectBox() },
                        enabled = !isConnecting && directBoxPasswordInput.isNotBlank(),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("btn_submit_direct_box_login")
                    ) {
                        if (isConnecting) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(22.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Connexion à la box...")
                        } else {
                            Icon(Icons.Default.Wifi, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("SE CONNECTER AU RÉSEAU", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
        // CASE 3: PORTAL IS ACTIVE -> DYNAMIC CAPTIVE PORTAL LOGIN FORM
        else {
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("portal_login_card"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    // Tab Row: Registered credentials vs Guest mode
                    if (portalSettings.guestModeEnabled) {
                        TabRow(
                            selectedTabIndex = if (isGuestModeActive) 1 else 0,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Tab(
                                selected = !isGuestModeActive,
                                onClick = { viewModel.toggleGuestMode(false) },
                                text = { Text("Identification") },
                                icon = { Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(18.dp)) }
                            )
                            Tab(
                                selected = isGuestModeActive,
                                onClick = { viewModel.toggleGuestMode(true) },
                                text = { Text("Mode Invité") },
                                icon = { Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(18.dp)) }
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Welcome instruction
                    Text(
                        text = if (!isGuestModeActive) portalSettings.welcomeMessage
                        else "Accès temporaire de ${portalSettings.guestDurationHours}h pour les invités.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    if (!isGuestModeActive) {
                        // Dynamically generated portal fields
                        if (portalFields.isEmpty()) {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Aucun champ configuré comme visible sur le portail. Rendez-vous dans l'onglet Configuration pour en activer.",
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(14.dp),
                                    textAlign = TextAlign.Center
                                )
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                portalFields.forEach { field ->
                                    val currentValue = portalValues[field.id].orEmpty()
                                    DynamicInputField(
                                        field = field,
                                        value = currentValue,
                                        onValueChange = { viewModel.updatePortalValue(field.id, it) }
                                    )
                                }
                            }
                        }
                    } else {
                        // Guest mode form (Nom + Téléphone)
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            OutlinedTextField(
                                value = guestName,
                                onValueChange = { viewModel.guestName.value = it },
                                label = { Text("Nom et Prénom *") },
                                placeholder = { Text("Ex: Andy Mbourou") },
                                singleLine = true,
                                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_guest_name")
                            )

                            OutlinedTextField(
                                value = guestPhone,
                                onValueChange = { viewModel.guestPhone.value = it },
                                label = { Text("Numéro de téléphone *") },
                                placeholder = { Text("+241 07 00 00 00") },
                                singleLine = true,
                                leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_guest_phone")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Auth Results Banners
                    AnimatedVisibility(
                        visible = authResult != null,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        authResult?.let { result ->
                            val (bgColor, tintColor, message) = when (result) {
                                is AuthResult.Success -> Triple(
                                    SuccessGreen.copy(alpha = 0.15f),
                                    SuccessGreen,
                                    result.message
                                )
                                is AuthResult.PendingApproval -> Triple(
                                    WarningAmber.copy(alpha = 0.15f),
                                    WarningAmber,
                                    result.message
                                )
                                is AuthResult.DirectBoxSuccess -> Triple(
                                    SuccessGreen.copy(alpha = 0.15f),
                                    SuccessGreen,
                                    result.message
                                )
                                is AuthResult.Suspended -> Triple(
                                    MaterialTheme.colorScheme.errorContainer,
                                    MaterialTheme.colorScheme.error,
                                    result.reason
                                )
                                is AuthResult.NotFound -> Triple(
                                    MaterialTheme.colorScheme.errorContainer,
                                    MaterialTheme.colorScheme.error,
                                    result.message
                                )
                                is AuthResult.ValidationError -> Triple(
                                    MaterialTheme.colorScheme.errorContainer,
                                    MaterialTheme.colorScheme.error,
                                    result.message + if (result.missingFields.isNotEmpty()) " (${result.missingFields.joinToString(", ")})" else ""
                                )
                                is AuthResult.Error -> Triple(
                                    MaterialTheme.colorScheme.errorContainer,
                                    MaterialTheme.colorScheme.error,
                                    result.message
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = bgColor,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp)
                                    .testTag("auth_result_banner")
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = when (result) {
                                            is AuthResult.Success -> Icons.Default.CheckCircle
                                            is AuthResult.PendingApproval -> Icons.Default.PendingActions
                                            else -> Icons.Default.ErrorOutline
                                        },
                                        contentDescription = null,
                                        tint = tintColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = message,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = tintColor
                                    )
                                }
                            }
                        }
                    }

                    // Connect action button
                    Button(
                        onClick = { viewModel.authenticatePortal() },
                        enabled = !isConnecting,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("btn_submit_portal_login")
                    ) {
                        if (isConnecting) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(22.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Vérification des accès...")
                        } else {
                            Icon(Icons.Default.Wifi, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (!isGuestModeActive) "SE CONNECTER AU WI-FI" else "ACTIVER L'ACCÈS INVITÉ",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Notice on dynamic security
                    if (authFields.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Facteurs vérifiés : ${authFields.joinToString(" + ") { it.label }}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (portalSettings.requireAdminApproval) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.PendingActions,
                                contentDescription = null,
                                tint = WarningAmber,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Mode approbation manuelle actif : l'administrateur valide votre accès",
                                style = MaterialTheme.typography.labelSmall,
                                color = WarningAmber
                            )
                        }
                    }
                }
            }
        }
    }
}
