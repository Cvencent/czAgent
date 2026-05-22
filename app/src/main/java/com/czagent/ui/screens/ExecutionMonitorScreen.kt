package com.czagent.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.czagent.ui.AppState

@Composable
fun ExecutionMonitorScreen(appState: AppState, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Execution Monitor", style = androidx.compose.material3.MaterialTheme.typography.headlineMedium)
        Text("Status: ${appState.currentStatus ?: "Idle"}")
        Text("Latest screenshot: not connected")
        appState.logs.takeLast(12).forEach { log ->
            Text("${log.status}: ${log.message}")
        }
    }
}
