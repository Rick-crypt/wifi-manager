package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.entity.FieldDefinitionEntity
import com.example.data.model.UserStatus
import com.example.data.model.UserWithValues

@Composable
fun UserEditDialog(
    userToEdit: UserWithValues?,
    fields: List<FieldDefinitionEntity>,
    onDismiss: () -> Unit,
    onSave: (userId: Long, status: UserStatus, notes: String, values: Map<Long, String>) -> Unit
) {
    val isNew = userToEdit == null
    val valuesState = remember {
        mutableStateMapOf<Long, String>().apply {
            if (userToEdit != null) {
                putAll(userToEdit.values)
            }
        }
    }
    var statusState by remember {
        mutableStateOf(userToEdit?.user?.userStatus ?: UserStatus.AUTHORIZED)
    }
    var notesState by remember {
        mutableStateOf(userToEdit?.user?.notes.orEmpty())
    }
    var statusDropdownOpen by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }

    val activeFields = fields.filter { it.isEnabled }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isNew) "Ajouter un utilisateur" else "Modifier l'utilisateur",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Les informations demandées s'adaptent automatiquement à votre configuration d'identification.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Dynamically generated inputs from configured fields
                activeFields.forEach { field ->
                    val currentValue = valuesState[field.id].orEmpty()
                    DynamicInputField(
                        field = field,
                        value = currentValue,
                        onValueChange = { valuesState[field.id] = it }
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Statut de l'utilisateur
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = statusState.label,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Statut d'accès") },
                        trailingIcon = {
                            IconButton(onClick = { statusDropdownOpen = true }) {
                                Icon(Icons.Default.ArrowDropDown, contentDescription = "Choisir statut")
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dropdown_user_status")
                    )

                    DropdownMenu(
                        expanded = statusDropdownOpen,
                        onDismissRequest = { statusDropdownOpen = false }
                    ) {
                        UserStatus.entries.forEach { status ->
                            DropdownMenuItem(
                                text = { Text(status.label) },
                                onClick = {
                                    statusState = status
                                    statusDropdownOpen = false
                                }
                            )
                        }
                    }
                }

                // Notes / Commentaires
                OutlinedTextField(
                    value = notesState,
                    onValueChange = { notesState = it },
                    label = { Text("Notes & Remarques administratives") },
                    placeholder = { Text("Ex: Étudiant Master 2, Chambre B204...") },
                    modifier = Modifier.fillMaxWidth()
                )

                if (validationError != null) {
                    Text(
                        text = validationError!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    // Check required fields
                    val missingRequired = activeFields.filter { it.isRequired && valuesState[it.id].isNullOrBlank() }
                    if (missingRequired.isNotEmpty()) {
                        validationError = "Champs obligatoires manquants : ${missingRequired.joinToString(", ") { it.label }}"
                        return@Button
                    }

                    onSave(
                        userToEdit?.user?.id ?: 0L,
                        statusState,
                        notesState,
                        valuesState.toMap()
                    )
                },
                modifier = Modifier.testTag("btn_save_user")
            ) {
                Text(if (isNew) "Créer l'utilisateur" else "Enregistrer")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )
}
