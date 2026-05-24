package com.czagent.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.czagent.ui.AppState

@Composable
fun HistoryScreen(appState: AppState, modifier: Modifier = Modifier) {
    val scrollState = rememberScrollState()
    Column(
        modifier.fillMaxSize().verticalScroll(scrollState).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("History", style = MaterialTheme.typography.headlineMedium)
        Text("Review the latest task runs and why they finished the way they did.")

        if (appState.runs.isEmpty()) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.History, null)
                    Text("No history yet", style = MaterialTheme.typography.titleSmall)
                    Text("Run a task and its result will appear here.")
                }
            }
        } else {
            appState.runs.forEach { run ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(formatRunSummaryLine(run.taskName, run.status, run.failureReason), style = MaterialTheme.typography.titleSmall)
                        Text("Started at ${run.startedAt}")
                        run.failureReason?.let {
                            Text(it)
                        }
                    }
                }
            }
        }
    }
}
