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
fun HistoryScreen(appState: AppState, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("History", style = androidx.compose.material3.MaterialTheme.typography.headlineMedium)
        appState.runs.forEach { run ->
            Text("${run.taskName}: ${run.status}")
        }
    }
}
