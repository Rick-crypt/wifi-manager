package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.ConfigurationScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.PortalScreen
import com.example.ui.screens.SessionsScreen
import com.example.ui.screens.UsersScreen
import com.example.ui.viewmodel.WifiManagerViewModel

data class NavItem(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

@Composable
fun MainScreen(
    viewModel: WifiManagerViewModel = viewModel()
) {
    var selectedIndex by rememberSaveable { mutableIntStateOf(0) }
    val activeSessions by viewModel.activeSessions.collectAsState()
    val pendingUsers by viewModel.pendingUsers.collectAsState()

    val navItems = listOf(
        NavItem(
            title = "Accueil",
            selectedIcon = Icons.Filled.Home,
            unselectedIcon = Icons.Outlined.Home,
            testTag = "nav_home"
        ),
        NavItem(
            title = "Portail",
            selectedIcon = Icons.Filled.Wifi,
            unselectedIcon = Icons.Outlined.Wifi,
            testTag = "nav_portal"
        ),
        NavItem(
            title = "Utilisateurs",
            selectedIcon = Icons.Filled.People,
            unselectedIcon = Icons.Outlined.People,
            testTag = "nav_users"
        ),
        NavItem(
            title = "Sessions",
            selectedIcon = Icons.Filled.Devices,
            unselectedIcon = Icons.Outlined.Devices,
            testTag = "nav_sessions"
        ),
        NavItem(
            title = "Configuration",
            selectedIcon = Icons.Filled.Tune,
            unselectedIcon = Icons.Outlined.Tune,
            testTag = "nav_config"
        )
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
        bottomBar = {
            NavigationBar(
                modifier = Modifier.testTag("main_bottom_nav"),
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                navItems.forEachIndexed { index, item ->
                    val isSelected = selectedIndex == index
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { selectedIndex = index },
                        icon = {
                            when (index) {
                                2 -> { // Utilisateurs (badge if pending approval requests)
                                    if (pendingUsers.isNotEmpty()) {
                                        BadgedBox(
                                            badge = {
                                                Badge {
                                                    Text("${pendingUsers.size}")
                                                }
                                            }
                                        ) {
                                            Icon(
                                                imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                                                contentDescription = item.title,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                    } else {
                                        Icon(
                                            imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                                            contentDescription = item.title,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                                3 -> { // Sessions (badge if active sessions)
                                    if (activeSessions.isNotEmpty()) {
                                        BadgedBox(
                                            badge = {
                                                Badge {
                                                    Text("${activeSessions.size}")
                                                }
                                            }
                                        ) {
                                            Icon(
                                                imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                                                contentDescription = item.title,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                    } else {
                                        Icon(
                                            imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                                            contentDescription = item.title,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                                else -> {
                                    Icon(
                                        imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = item.title,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        },
                        label = {
                            Text(
                                text = item.title,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        modifier = Modifier.testTag(item.testTag)
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when (selectedIndex) {
                0 -> HomeScreen(
                    viewModel = viewModel,
                    onNavigateToPortal = { selectedIndex = 1 },
                    onNavigateToUsers = { selectedIndex = 2 },
                    onNavigateToConfig = { selectedIndex = 4 }
                )
                1 -> PortalScreen(viewModel = viewModel)
                2 -> UsersScreen(viewModel = viewModel)
                3 -> SessionsScreen(viewModel = viewModel)
                4 -> ConfigurationScreen(viewModel = viewModel)
            }
        }
    }
}
