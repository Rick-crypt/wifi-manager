package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.entity.FieldDefinitionEntity
import com.example.data.model.UserStatus
import com.example.data.model.UserWithValues
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.WarningAmber

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun UserCard(
    userWithValues: UserWithValues,
    fields: List<FieldDefinitionEntity>,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onStatusChange: (UserStatus) -> Unit,
    modifier: Modifier = Modifier
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val user = userWithValues.user
    val displayName = userWithValues.getDisplayName(fields)

    val (statusColor, statusBg, statusIcon) = when (user.userStatus) {
        UserStatus.AUTHORIZED -> Triple(SuccessGreen, SuccessGreen.copy(alpha = 0.12f), Icons.Default.CheckCircle)
        UserStatus.SUSPENDED -> Triple(ErrorRed, ErrorRed.copy(alpha = 0.12f), Icons.Default.Block)
        UserStatus.PENDING -> Triple(WarningAmber, WarningAmber.copy(alpha = 0.12f), Icons.Default.Schedule)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("user_card_${user.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
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
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = displayName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        // Status Badge
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = statusBg
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = statusIcon,
                                    contentDescription = null,
                                    tint = statusColor,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = user.userStatus.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = statusColor
                                )
                            }
                        }
                    }
                }

                Box {
                    IconButton(
                        onClick = { menuExpanded = true },
                        modifier = Modifier.testTag("user_menu_${user.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Options utilisateur"
                        )
                    }

                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Modifier l'utilisateur") },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onEditClick()
                            }
                        )

                        if (user.userStatus != UserStatus.AUTHORIZED) {
                            DropdownMenuItem(
                                text = { Text("Définir comme Autorisé") },
                                leadingIcon = { Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SuccessGreen) },
                                onClick = {
                                    menuExpanded = false
                                    onStatusChange(UserStatus.AUTHORIZED)
                                }
                            )
                        }

                        if (user.userStatus != UserStatus.SUSPENDED) {
                            DropdownMenuItem(
                                text = { Text("Suspendre l'accès Wi-Fi") },
                                leadingIcon = { Icon(Icons.Default.Block, contentDescription = null, tint = ErrorRed) },
                                onClick = {
                                    menuExpanded = false
                                    onStatusChange(UserStatus.SUSPENDED)
                                }
                            )
                        }

                        DropdownMenuItem(
                            text = { Text("Supprimer", color = MaterialTheme.colorScheme.error) },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                            onClick = {
                                menuExpanded = false
                                onDeleteClick()
                            }
                        )
                    }
                }
            }

            // Dynamic Attribute chips
            val relevantValues = fields.mapNotNull { field ->
                val valStr = userWithValues.values[field.id]
                if (!valStr.isNullOrBlank()) field to valStr else null
            }

            if (relevantValues.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    relevantValues.forEach { (field, valStr) ->
                        SuggestionChip(
                            onClick = {},
                            label = {
                                Text(
                                    text = "${field.label}: $valStr",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = if (field.isAuthKey)
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                else MaterialTheme.colorScheme.surfaceVariant
                            ),
                            border = null
                        )
                    }
                }
            }

            if (user.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Note : ${user.notes}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }
        }
    }
}
