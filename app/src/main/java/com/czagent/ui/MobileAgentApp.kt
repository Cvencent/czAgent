package com.czagent.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Monitor
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.czagent.ui.screens.ExecutionMonitorScreen
import com.czagent.ui.screens.HistoryScreen
import com.czagent.ui.screens.HomeScreen
import com.czagent.ui.screens.SettingsScreen
import com.czagent.ui.screens.TaskEditorScreen

private enum class Tab(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    HOME("Home", Icons.Default.Dashboard),
    TASKS("Tasks", Icons.AutoMirrored.Filled.ListAlt),
    MONITOR("Monitor", Icons.Default.Monitor),
    HISTORY("History", Icons.Default.History),
    SETTINGS("Settings", Icons.Default.Settings),
}

@Composable
fun MobileAgentApp(factory: ViewModelProvider.Factory? = null) {
    val appState: AppState = if (factory == null) viewModel() else viewModel(factory = factory)
    var selected by remember { mutableStateOf(Tab.HOME) }
    LaunchedEffect(Unit) {
        appState.load()
    }
    MaterialTheme {
        Scaffold(
            bottomBar = {
                NavigationBar {
                    Tab.entries.forEach { tab ->
                        NavigationBarItem(
                            selected = selected == tab,
                            onClick = { selected = tab },
                            label = { Text(tab.label) },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                        )
                    }
                }
            },
        ) { padding ->
            val modifier = Modifier.padding(padding)
            when (selected) {
                Tab.HOME -> HomeScreen(appState, modifier)
                Tab.TASKS -> TaskEditorScreen(appState, modifier)
                Tab.MONITOR -> ExecutionMonitorScreen(appState, modifier)
                Tab.HISTORY -> HistoryScreen(appState, modifier)
                Tab.SETTINGS -> SettingsScreen(modifier)
            }
        }
    }
}
