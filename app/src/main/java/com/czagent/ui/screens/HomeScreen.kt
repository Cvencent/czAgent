package com.czagent.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Shortcut
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.czagent.ui.AppState

@Composable
fun HomeScreen(appState: AppState, modifier: Modifier = Modifier) {
    val scrollState = rememberScrollState()
    Column(
        modifier.fillMaxSize().verticalScroll(scrollState).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Mobile Agent", style = MaterialTheme.typography.headlineMedium)
        Text("Rule-based local automation foundation")

        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    formatTaskStatsLine(appState.tasks.size, appState.shortcuts.size, appState.runs.size),
                    style = MaterialTheme.typography.titleMedium,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AssistChip(onClick = {}, label = { Text("Rule-based") }, leadingIcon = { Icon(Icons.Default.TaskAlt, null) })
                    AssistChip(onClick = {}, label = { Text("${appState.currentStatus ?: "Idle"}") }, leadingIcon = { Icon(Icons.Default.PlayArrow, null) })
                }
            }
        }

        SectionHeader("Shortcuts", appState.shortcuts.isNotEmpty())
        if (appState.shortcuts.isEmpty()) {
            EmptyState("No shortcuts yet", "Enable shortcut command when saving a task.")
        } else {
            appState.shortcuts.forEach { shortcut ->
                Card(Modifier.fillMaxWidth()) {
                    Row(
                        Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(shortcut.label, style = MaterialTheme.typography.titleMedium)
                            Text(shortcut.taskName)
                        }
                        OutlinedButton(onClick = { appState.runShortcut(shortcut) }) {
                            Icon(Icons.AutoMirrored.Filled.Shortcut, null)
                            Text("Run")
                        }
                    }
                }
            }
        }

        SectionHeader("Tasks", appState.tasks.isNotEmpty())
        if (appState.tasks.isEmpty()) {
            EmptyState("No tasks saved", "Create a task in the editor and save it here.")
        } else {
            appState.tasks.forEach { task ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(task.name, style = MaterialTheme.typography.titleMedium)
                                Text(task.description.ifBlank { "No description" })
                                Text(task.targetPackage ?: "No package set")
                            }
                            Button(onClick = { appState.markManualRun(task) }) {
                                Icon(Icons.Default.PlayArrow, null)
                                Text("Run")
                            }
                        }
                        Text("${task.steps.size} steps")
                    }
                }
            }
        }

        SectionHeader("Recent runs", appState.runs.isNotEmpty())
        if (appState.runs.isEmpty()) {
            EmptyState("No run history yet", "Run a task once and it will appear here.")
        } else {
            appState.runs.take(5).forEach { run ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(formatRunSummaryLine(run.taskName, run.status, run.failureReason), style = MaterialTheme.typography.titleSmall)
                        Text("Started at ${run.startedAt}")
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, hasContent: Boolean) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        if (!hasContent) {
            Text("Empty", style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun EmptyState(title: String, body: String) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(body)
        }
    }
}
