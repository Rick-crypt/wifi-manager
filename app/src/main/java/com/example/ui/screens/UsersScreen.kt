package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.PendingActions
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.model.UserStatus
import com.example.data.model.UserWithValues
import com.example.ui.components.UserCard
import com.example.ui.components.UserEditDialog
import com.example.ui.theme.WarningAmber
import com.example.ui.viewmodel.WifiManagerViewModel

@Composable
fun UsersScreen(
    viewModel: WifiManagerViewModel,
    modifier: Modifier = Modifier
) {
    val users by viewModel.filteredUsers.collectAsState()
    val pendingUsers by viewModel.pendingUsers.collectAsState()
    val allFields by viewModel.allFields.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val statusFilter by viewModel.statusFilter.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var userToEdit by remember { mutableStateOf<UserWithValues?>(null) }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header & Search
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Annuaire des Utilisateurs",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "${users.size} utilisateur(s) répertorié(s)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    ElevatedButton(
                        onClick = { showAddDialog = true },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("btn_open_add_user")
                    ) {
                        Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Ajouter")
                    }
                }

                // Banner if any users are pending admin approval
                if (pendingUsers.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = WarningAmber.copy(alpha = 0.14f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, WarningAmber.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PendingActions,
                                    contentDescription = null,
                                    tint = WarningAmber,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "${pendingUsers.size} accès en attente d'approbation",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = WarningAmber
                                    )
                                    Text(
                                        text = "Cliquez sur 'Filtrer' pour approuver leur accès",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Button(
                                onClick = { viewModel.statusFilter.value = UserStatus.PENDING },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Voir", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Search field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.searchQuery.value = it },
                    placeholder = { Text("Rechercher par nom, passeport, chambre...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.searchQuery.value = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Effacer")
                            }
                        }
                    },
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("user_search_input")
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Filter chips
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    item {
                        FilterChip(
                            selected = statusFilter == null,
                            onClick = { viewModel.statusFilter.value = null },
                            label = { Text("Tous") }
                        )
                    }
                    UserStatus.entries.forEach { status ->
                        item {
                            FilterChip(
                                selected = statusFilter == status,
                                onClick = { viewModel.statusFilter.value = status },
                                label = {
                                    Text(
                                        if (status == UserStatus.PENDING && pendingUsers.isNotEmpty())
                                            "${status.label} (${pendingUsers.size})"
                                        else status.label
                                    )
                                }
                            )
                        }
                    }
                }
            }

            // User list
            if (users.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        modifier = Modifier.fillMaxWidth(0.9f)
                    ) {
                        Column(
                            modifier = Modifier.padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.People,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Aucun utilisateur trouvé",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (searchQuery.isNotBlank()) "Aucun résultat pour \"$searchQuery\"."
                                else "Créez votre premier utilisateur pour autoriser sa connexion au Wi-Fi.",
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(users, key = { it.user.id }) { userWithValues ->
                        UserCard(
                            userWithValues = userWithValues,
                            fields = allFields,
                            onEditClick = { userToEdit = userWithValues },
                            onDeleteClick = { viewModel.deleteUser(userWithValues.user.id) },
                            onStatusChange = { newStatus ->
                                viewModel.updateUserStatus(userWithValues.user.id, newStatus)
                            }
                        )
                    }
                }
            }
        }

        // Floating Action Button
        FloatingActionButton(
            onClick = { showAddDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .testTag("fab_add_user"),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ) {
            Icon(Icons.Default.Add, contentDescription = "Ajouter un utilisateur")
        }
    }

    // Add or Edit Dialog with dynamic inputs
    if (showAddDialog || userToEdit != null) {
        UserEditDialog(
            userToEdit = userToEdit,
            fields = allFields,
            onDismiss = {
                showAddDialog = false
                userToEdit = null
            },
            onSave = { id, status, notes, values ->
                viewModel.saveUser(
                    userId = id,
                    status = status,
                    notes = notes,
                    fieldValues = values
                )
                showAddDialog = false
                userToEdit = null
            }
        )
    }
}
