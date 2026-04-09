package com.alittleapp.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.alittleapp.feature_clipboard.ui.ClipboardScreen
import com.alittleapp.feature_nas.ui.NasScreen
import com.alittleapp.feature_notes.ui.NotesScreen
import com.alittleapp.feature_tasks.ui.TasksScreen
import com.alittleapp.feature_timer.ui.TimerScreen
import com.alittleapp.feature_transfer.ui.TransferScreen
import com.alittleapp.feature_utils.ui.UtilsScreen
import com.alittleapp.feature_vault.ui.VaultScreen
import com.alittleapp.feature_webui.ui.WebUiScreen
import com.alittleapp.ui.dashboard.DashboardScreen

sealed class Screen(val route: String, val label: String) {
    object Dashboard : Screen("dashboard", "Home")
    object Notes : Screen("notes", "Notes")
    object Tasks : Screen("tasks", "Tasks")
    object Utils : Screen("utils", "Tools")
    object Transfer : Screen("transfer", "Transfer")
    // Not in bottom nav — accessed from Dashboard quick actions or menu
    object Nas : Screen("nas", "NAS")
    object Vault : Screen("vault", "Vault")
    object Timer : Screen("timer", "Timers")
    object Clipboard : Screen("clipboard", "Clipboard")
    object WebUi : Screen("webui", "Go Live")
}

@Composable
fun AppNavGraph() {
    val navController = rememberNavController()

    val bottomItems = listOf(Screen.Dashboard, Screen.Notes, Screen.Tasks, Screen.Utils, Screen.Transfer)
    val icons = listOf(
        Icons.Filled.Home,
        Icons.Filled.Description,
        Icons.Filled.CheckCircle,
        Icons.Filled.Tune,
        Icons.Filled.Send
    )

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                bottomItems.forEachIndexed { index, screen ->
                    NavigationBarItem(
                        icon = { Icon(icons[index], contentDescription = screen.label) },
                        label = { Text(screen.label) },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Dashboard.route) {
                DashboardScreen(
                    onNavigateToNotes = { navController.navigate(Screen.Notes.route) },
                    onNavigateToTasks = { navController.navigate(Screen.Tasks.route) },
                    onNavigateToUtils = { navController.navigate(Screen.Utils.route) },
                    onNavigateToTransfer = { navController.navigate(Screen.Transfer.route) },
                    onNavigateToNas = { navController.navigate(Screen.Nas.route) },
                    onNavigateToVault = { navController.navigate(Screen.Vault.route) },
                    onNavigateToTimer = { navController.navigate(Screen.Timer.route) },
                    onNavigateToClipboard = { navController.navigate(Screen.Clipboard.route) },
                    onNavigateToWebUi = { navController.navigate(Screen.WebUi.route) }
                )
            }
            composable(Screen.Notes.route) { NotesScreen() }
            composable(Screen.Tasks.route) { TasksScreen() }
            composable(Screen.Utils.route) { UtilsScreen() }
            composable(Screen.Transfer.route) { TransferScreen() }
            composable(Screen.Nas.route) { NasScreen() }
            composable(Screen.Vault.route) { VaultScreen() }
            composable(Screen.Timer.route) { TimerScreen() }
            composable(Screen.Clipboard.route) { ClipboardScreen() }
            composable(Screen.WebUi.route) { WebUiScreen() }
        }
    }
}
