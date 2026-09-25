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
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.entity.FieldDefinitionEntity
import com.example.ui.theme.InfoCyan
import com.example.ui.theme.PurpleAccent
import com.example.ui.theme.SuccessGreen

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FieldConfigCard(
    field: FieldDefinitionEntity,
    isFirst: Boolean,
    isLast: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleEnabled: () -> Unit,
    onToggleAuthKey: () -> Unit,
    onTogglePortalVisible: () -> Unit,
    onToggleRequired: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("field_config_${field.key}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (field.isEnabled) MaterialTheme.colorScheme.surface
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (field.isEnabled) 2.dp else 0.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header: Handle / Order + Label + Type + Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // Reorder controls
                    Column {
                        IconButton(
                            onClick = onMoveUp,
                            enabled = !isFirst,
                            modifier = Modifier
                                .size(28.dp)
                                .testTag("move_up_${field.key}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowUpward,
                                contentDescription = "Monter le champ",
                                tint = if (!isFirst) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        IconButton(
                            onClick = onMoveDown,
                            enabled = !isLast,
                            modifier = Modifier
                                .size(28.dp)
                                .testTag("move_down_${field.key}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowDownward,
                                contentDescription = "Descendre le champ",
                                tint = if (!isLast) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = field.label,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (field.isEnabled) MaterialTheme.colorScheme.onSurface
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                            if (field.isRequired) {
                                Text(
                                    text = " *",
                                    color = MaterialTheme.colorScheme.error,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                            ) {
                                Text(
                                    text = field.fieldType.name,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "clé: ${field.key}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                }

                // Edit & Delete icons
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.testTag("edit_field_${field.key}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Modifier",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.testTag("delete_field_${field.key}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Supprimer",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Configuration toggles row
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Auth Key toggle
                FilterChip(
                    selected = field.isAuthKey,
                    onClick = onToggleAuthKey,
                    label = {
                        Text(
                            text = if (field.isAuthKey) "Facteur d'Authentification" else "Pas d'authentification",
                            style = MaterialTheme.typography.labelSmall
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = if (field.isAuthKey) Icons.Default.Key else Icons.Default.Lock,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = PurpleAccent.copy(alpha = 0.18f),
                        selectedLabelColor = PurpleAccent,
                        selectedLeadingIconColor = PurpleAccent
                    ),
                    modifier = Modifier.testTag("toggle_auth_${field.key}")
                )

                // Portal visibility toggle
                FilterChip(
                    selected = field.isVisibleInPortal,
                    onClick = onTogglePortalVisible,
                    label = {
                        Text(
                            text = if (field.isVisibleInPortal) "Visible Portail" else "Caché du portail",
                            style = MaterialTheme.typography.labelSmall
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = if (field.isVisibleInPortal) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = InfoCyan.copy(alpha = 0.18f),
                        selectedLabelColor = InfoCyan,
                        selectedLeadingIconColor = InfoCyan
                    ),
                    modifier = Modifier.testTag("toggle_portal_${field.key}")
                )

                // Required toggle
                FilterChip(
                    selected = field.isRequired,
                    onClick = onToggleRequired,
                    label = {
                        Text(
                            text = if (field.isRequired) "Obligatoire" else "Facultatif",
                            style = MaterialTheme.typography.labelSmall
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = if (field.isRequired) Icons.Default.Check else Icons.Default.Close,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    modifier = Modifier.testTag("toggle_required_${field.key}")
                )
            }

            // Options preview if SELECT
            val options = field.getOptionsList()
            if (options.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Options : ${options.joinToString(", ")}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Enabled / Disabled row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (field.isEnabled) "Champ actif" else "Champ temporairement désactivé",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = if (field.isEnabled) SuccessGreen else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Switch(
                    checked = field.isEnabled,
                    onCheckedChange = { onToggleEnabled() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.testTag("switch_enabled_${field.key}")
                )
            }
        }
    }
}
