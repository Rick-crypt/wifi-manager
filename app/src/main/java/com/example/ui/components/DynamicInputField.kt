package com.example.ui.components

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.data.entity.FieldDefinitionEntity
import com.example.data.model.FieldType

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DynamicInputField(
    field: FieldDefinitionEntity,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    errorMessage: String? = null,
    enabled: Boolean = true
) {
    var isPasswordVisible by remember { mutableStateOf(false) }
    var dropdownExpanded by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 4.dp)
        ) {
            Text(
                text = field.label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (field.isRequired) {
                Text(
                    text = " *",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
            }
            if (field.isAuthKey) {
                Spacer(modifier = Modifier.size(6.dp))
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Clé d'authentification",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        when (field.fieldType) {
            FieldType.TEXT -> {
                OutlinedTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("field_${field.key}"),
                    placeholder = {
                        Text(field.placeholder.ifEmpty { "Entrez ${field.label.lowercase()}" })
                    },
                    singleLine = true,
                    enabled = enabled,
                    isError = isError,
                    shape = RoundedCornerShape(12.dp)
                )
            }

            FieldType.NUMBER -> {
                OutlinedTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("field_${field.key}"),
                    placeholder = {
                        Text(field.placeholder.ifEmpty { "Ex: 2026" })
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    enabled = enabled,
                    isError = isError,
                    shape = RoundedCornerShape(12.dp)
                )
            }

            FieldType.PHONE -> {
                OutlinedTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("field_${field.key}"),
                    placeholder = {
                        Text(field.placeholder.ifEmpty { "+241 07 ..." })
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    enabled = enabled,
                    isError = isError,
                    shape = RoundedCornerShape(12.dp)
                )
            }

            FieldType.DATE -> {
                OutlinedTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("field_${field.key}"),
                    placeholder = {
                        Text(field.placeholder.ifEmpty { "JJ/MM/AAAA" })
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    singleLine = true,
                    enabled = enabled,
                    isError = isError,
                    shape = RoundedCornerShape(12.dp)
                )
            }

            FieldType.PASSWORD -> {
                OutlinedTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("field_${field.key}"),
                    placeholder = {
                        Text(field.placeholder.ifEmpty { "••••••••" })
                    },
                    singleLine = true,
                    enabled = enabled,
                    isError = isError,
                    visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        IconButton(
                            onClick = { isPasswordVisible = !isPasswordVisible },
                            modifier = Modifier.testTag("toggle_pwd_${field.key}")
                        ) {
                            Icon(
                                imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (isPasswordVisible) "Masquer" else "Afficher"
                            )
                        }
                    },
                    shape = RoundedCornerShape(12.dp)
                )
            }

            FieldType.SELECT -> {
                val options = field.getOptionsList().ifEmpty { listOf("Option 1", "Option 2") }
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = value,
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("field_${field.key}")
                            .clickable(enabled = enabled) { dropdownExpanded = true },
                        placeholder = {
                            Text(field.placeholder.ifEmpty { "Sélectionner une option" })
                        },
                        trailingIcon = {
                            IconButton(onClick = { dropdownExpanded = true }) {
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = "Ouvrir options"
                                )
                            }
                        },
                        enabled = enabled,
                        isError = isError,
                        shape = RoundedCornerShape(12.dp)
                    )

                    DropdownMenu(
                        expanded = dropdownExpanded,
                        onDismissRequest = { dropdownExpanded = false },
                        modifier = Modifier.fillMaxWidth(0.9f)
                    ) {
                        options.forEach { option ->
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(option)
                                        if (value == option) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                },
                                onClick = {
                                    onValueChange(option)
                                    dropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            FieldType.MULTI_SELECT -> {
                val options = field.getOptionsList().ifEmpty { listOf("Choix A", "Choix B", "Choix C") }
                val currentSelected = value.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet()

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    options.forEach { opt ->
                        val isSelected = opt in currentSelected
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                val next = if (isSelected) currentSelected - opt else currentSelected + opt
                                onValueChange(next.joinToString(", "))
                            },
                            label = { Text(opt) },
                            leadingIcon = if (isSelected) {
                                {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            } else null,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }
            }

            FieldType.BOOLEAN -> {
                val isChecked = value.equals("true", ignoreCase = true) || value.equals("oui", ignoreCase = true)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (isChecked) "Oui (Activé)" else "Non (Désactivé)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Switch(
                        checked = isChecked,
                        onCheckedChange = { onValueChange(if (it) "true" else "false") },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }
        }

        if (field.helperText.isNotBlank()) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = field.helperText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )
        }

        if (isError && !errorMessage.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}
