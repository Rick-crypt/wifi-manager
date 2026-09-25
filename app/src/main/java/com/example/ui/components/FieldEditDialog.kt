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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.entity.FieldDefinitionEntity
import com.example.data.model.FieldType

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FieldEditDialog(
    fieldToEdit: FieldDefinitionEntity?,
    onDismiss: () -> Unit,
    onSave: (FieldDefinitionEntity) -> Unit
) {
    val isNew = fieldToEdit == null
    var label by remember { mutableStateOf(fieldToEdit?.label.orEmpty()) }
    var key by remember { mutableStateOf(fieldToEdit?.key.orEmpty()) }
    var selectedType by remember { mutableStateOf(fieldToEdit?.fieldType ?: FieldType.TEXT) }
    var isRequired by remember { mutableStateOf(fieldToEdit?.isRequired ?: false) }
    var isAuthKey by remember { mutableStateOf(fieldToEdit?.isAuthKey ?: false) }
    var isVisibleInPortal by remember { mutableStateOf(fieldToEdit?.isVisibleInPortal ?: true) }
    var isEnabled by remember { mutableStateOf(fieldToEdit?.isEnabled ?: true) }
    var placeholder by remember { mutableStateOf(fieldToEdit?.placeholder.orEmpty()) }
    var helperText by remember { mutableStateOf(fieldToEdit?.helperText.orEmpty()) }

    // Options list for SELECT & MULTI_SELECT
    var optionsList by remember {
        mutableStateOf(fieldToEdit?.getOptionsList() ?: emptyList())
    }
    var newOptionInput by remember { mutableStateOf("") }
    var typeDropdownOpen by remember { mutableStateOf(false) }

    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isNew) "Ajouter un champ d'identification" else "Modifier le champ",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Label
                OutlinedTextField(
                    value = label,
                    onValueChange = {
                        label = it
                        if (isNew && key.isBlank()) {
                            key = it.lowercase()
                                .replace(" ", "_")
                                .replace("é", "e")
                                .replace("è", "e")
                                .replace("ê", "e")
                                .replace("à", "a")
                                .replace("'", "")
                                .filter { ch -> ch.isLetterOrDigit() || ch == '_' }
                        }
                    },
                    label = { Text("Nom du champ (Label)") },
                    placeholder = { Text("Ex: Numéro étudiant, Nationalité...") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_field_label")
                )

                // Key (Identifiant technique)
                OutlinedTextField(
                    value = key,
                    onValueChange = { key = it.filter { ch -> ch.isLetterOrDigit() || ch == '_' } },
                    label = { Text("Identifiant unique (clé)") },
                    placeholder = { Text("Ex: student_id, nationality") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_field_key")
                )

                // Type selector
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = "${selectedType.name} - ${selectedType.label}",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Type de données") },
                        trailingIcon = {
                            IconButton(onClick = { typeDropdownOpen = true }) {
                                Icon(Icons.Default.ArrowDropDown, contentDescription = "Choisir le type")
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { typeDropdownOpen = true }
                            .testTag("dropdown_field_type")
                    )

                    DropdownMenu(
                        expanded = typeDropdownOpen,
                        onDismissRequest = { typeDropdownOpen = false },
                        modifier = Modifier.fillMaxWidth(0.85f)
                    ) {
                        FieldType.entries.forEach { type ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(text = "${type.name} (${type.label})", fontWeight = FontWeight.Bold)
                                        Text(text = type.description, style = MaterialTheme.typography.bodySmall)
                                    }
                                },
                                onClick = {
                                    selectedType = type
                                    typeDropdownOpen = false
                                }
                            )
                        }
                    }
                }

                // If SELECT or MULTI_SELECT, provide options manager
                if (selectedType == FieldType.SELECT || selectedType == FieldType.MULTI_SELECT) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                    ) {
                        Text(
                            text = "Options de la liste :",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = newOptionInput,
                                onValueChange = { newOptionInput = it },
                                placeholder = { Text("Ajouter une option (ex: Gabonaise)") },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("input_new_option")
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Button(
                                onClick = {
                                    val trimmed = newOptionInput.trim()
                                    if (trimmed.isNotEmpty() && trimmed !in optionsList) {
                                        optionsList = optionsList + trimmed
                                        newOptionInput = ""
                                    }
                                },
                                modifier = Modifier.testTag("btn_add_option")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Ajouter option")
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            optionsList.forEach { option ->
                                InputChip(
                                    selected = false,
                                    onClick = {},
                                    label = { Text(option) },
                                    trailingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Supprimer option",
                                            modifier = Modifier
                                                .size(16.dp)
                                                .clickable {
                                                    optionsList = optionsList - option
                                                }
                                        )
                                    }
                                )
                            }
                        }
                    }
                }

                // Placeholder & Helper
                OutlinedTextField(
                    value = placeholder,
                    onValueChange = { placeholder = it },
                    label = { Text("Texte indicatif (Placeholder)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = helperText,
                    onValueChange = { helperText = it },
                    label = { Text("Texte d'aide") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Toggles
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Utilisé pour l'authentification", fontWeight = FontWeight.SemiBold)
                        Text(
                            "Servira de clé/facteur pour autoriser l'accès Wi-Fi",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = isAuthKey,
                        onCheckedChange = {
                            isAuthKey = it
                            if (it) isVisibleInPortal = true
                        },
                        modifier = Modifier.testTag("switch_dialog_auth")
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Visible sur le portail captif", fontWeight = FontWeight.SemiBold)
                        Text(
                            "Affiché sur la page de connexion des utilisateurs",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = isVisibleInPortal,
                        onCheckedChange = { isVisibleInPortal = it },
                        modifier = Modifier.testTag("switch_dialog_portal")
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Champ obligatoire", fontWeight = FontWeight.SemiBold)
                        Text(
                            "La saisie sera obligatoire pour valider",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = isRequired,
                        onCheckedChange = { isRequired = it },
                        modifier = Modifier.testTag("switch_dialog_required")
                    )
                }

                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (label.isBlank()) {
                        errorMessage = "Veuillez spécifier le nom du champ."
                        return@Button
                    }
                    if (key.isBlank()) {
                        errorMessage = "Veuillez spécifier un identifiant unique."
                        return@Button
                    }
                    if ((selectedType == FieldType.SELECT || selectedType == FieldType.MULTI_SELECT) && optionsList.isEmpty()) {
                        errorMessage = "Veuillez ajouter au moins une option dans la liste."
                        return@Button
                    }

                    val optionsJson = optionsList.joinToString(
                        separator = "\", \"",
                        prefix = "[\"",
                        postfix = "\"]"
                    )

                    val updatedField = (fieldToEdit ?: FieldDefinitionEntity(
                        key = key,
                        label = label,
                        type = selectedType.name
                    )).copy(
                        key = key,
                        label = label,
                        type = selectedType.name,
                        isRequired = isRequired,
                        isAuthKey = isAuthKey,
                        isVisibleInPortal = isVisibleInPortal,
                        isEnabled = isEnabled,
                        optionsJson = if (selectedType == FieldType.SELECT || selectedType == FieldType.MULTI_SELECT) optionsJson else "[]",
                        placeholder = placeholder,
                        helperText = helperText
                    )

                    onSave(updatedField)
                },
                modifier = Modifier.testTag("btn_save_field")
            ) {
                Text(if (isNew) "Ajouter" else "Enregistrer")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )
}
