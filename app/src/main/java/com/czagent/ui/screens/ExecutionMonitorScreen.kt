package com.czagent.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.czagent.ui.AppState

@Composable
fun ExecutionMonitorScreen(appState: AppState, modifier: Modifier = Modifier) {
    val scrollState = rememberScrollState()
    Column(
        modifier.fillMaxSize().verticalScroll(scrollState).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Execution Monitor", style = MaterialTheme.typography.headlineMedium)
        Text("Track the current run, the latest logs, and screenshot state.")

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(formatMonitorHeader(appState.currentStatus, appState.runs.firstOrNull()?.taskName, appState.logs.lastOrNull()?.message))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Timeline, null)
                    Text("Current status: ${appState.currentStatus ?: "Idle"}")
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Image, null)
                    Text("Latest screenshot: not connected")
                }
            }
        }

        MonitorSection("Recent logs", appState.logs.isNotEmpty()) {
            if (appState.logs.isEmpty()) {
                Text("No logs yet.")
            } else {
                appState.logs.takeLast(12).forEach { log ->
                    Text(log.toDisplayLine())
                    Text("Step ${log.stepId} at ${log.timestampMillis}", style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        MonitorSection("Run context", true) {
            val currentRun = appState.runs.firstOrNull()
            if (currentRun == null) {
                Text("No active or recent run.")
            } else {
                Text(formatRunSummaryLine(currentRun.taskName, currentRun.status, currentRun.failureReason))
                currentRun.failureReason?.let {
                    Text(it)
                } ?: Text("No failure reason recorded.")
            }
        }
    }
}

@Composable
private fun MonitorSection(title: String, hasContent: Boolean, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            if (!hasContent) {
                Icon(Icons.Default.Info, null)
            }
        }
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp), content = content)
        }
    }
}
