package com.monnhuitne.app.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.monnhuitne.app.data.N8nApi
import com.monnhuitne.app.data.N8nCredentials
import com.monnhuitne.app.data.SettingsStore
import com.monnhuitne.app.ui.screens.HealthScreen
import com.monnhuitne.app.ui.screens.SettingsScreen
import com.monnhuitne.app.ui.screens.UnlockScreen
import com.monnhuitne.app.ui.screens.WorkflowDetailScreen
import com.monnhuitne.app.ui.screens.WorkflowsScreen

private const val ROUTE_WORKFLOWS = "workflows"
private const val ROUTE_HEALTH = "health"
private const val ROUTE_SETTINGS = "settings"

@Composable
fun MonNhuitNeApp() {
    val context = LocalContext.current
    val store = remember { SettingsStore(context) }
    var hasCredentials by remember { mutableStateOf(store.hasCredentials()) }
    var unlockedCreds by remember { mutableStateOf<N8nCredentials?>(null) }

    when {
        !hasCredentials -> {
            SettingsScreen(
                store = store,
                existing = null,
                onSaved = { creds ->
                    hasCredentials = true
                    unlockedCreds = creds
                }
            )
        }
        unlockedCreds == null -> {
            UnlockScreen(
                store = store,
                onUnlocked = { unlockedCreds = store.loadCredentials() },
                onReset = {
                    store.clear()
                    hasCredentials = false
                }
            )
        }
        else -> {
            MainScaffold(
                credentials = unlockedCreds!!,
                store = store,
                onLock = { unlockedCreds = null },
                onFullReset = {
                    store.clear()
                    hasCredentials = false
                    unlockedCreds = null
                }
            )
        }
    }
}

@Composable
private fun MainScaffold(
    credentials: N8nCredentials,
    store: SettingsStore,
    onLock: () -> Unit,
    onFullReset: () -> Unit
) {
    val navController = rememberNavController()
    val api = remember(credentials) { N8nApi(credentials.baseUrl, credentials.apiKey) }

    Scaffold(
        bottomBar = {
            val backStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = backStackEntry?.destination?.route
            NavigationBar {
                NavigationBarItem(
                    selected = currentRoute == ROUTE_WORKFLOWS,
                    onClick = { navController.navigate(ROUTE_WORKFLOWS) { launchSingleTop = true } },
                    icon = { Icon(Icons.Filled.Home, contentDescription = null) },
                    label = { Text("Workflows") }
                )
                NavigationBarItem(
                    selected = currentRoute == ROUTE_HEALTH,
                    onClick = { navController.navigate(ROUTE_HEALTH) { launchSingleTop = true } },
                    icon = { Icon(Icons.Filled.Favorite, contentDescription = null) },
                    label = { Text("Santé") }
                )
                NavigationBarItem(
                    selected = currentRoute == ROUTE_SETTINGS,
                    onClick = { navController.navigate(ROUTE_SETTINGS) { launchSingleTop = true } },
                    icon = { Icon(Icons.Filled.Settings, contentDescription = null) },
                    label = { Text("Réglages") }
                )
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = ROUTE_WORKFLOWS,
            modifier = Modifier.padding(padding)
        ) {
            composable(ROUTE_WORKFLOWS) {
                WorkflowsScreen(api = api, onOpenWorkflow = { id -> navController.navigate("workflow/$id") })
            }
            composable("workflow/{id}") { backStackEntry ->
                val id = backStackEntry.arguments?.getString("id")
                if (id != null) {
                    WorkflowDetailScreen(api = api, workflowId = id, onBack = { navController.popBackStack() })
                }
            }
            composable(ROUTE_HEALTH) {
                HealthScreen(api = api)
            }
            composable(ROUTE_SETTINGS) {
                SettingsScreen(
                    store = store,
                    existing = credentials,
                    onSaved = {},
                    onLoggedOut = onLock,
                    onReset = onFullReset
                )
            }
        }
    }
}
