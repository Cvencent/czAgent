package com.czagent.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.czagent.ui.AppState

@Composable
fun HomeScreen(appState: AppState, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Mobile Agent", style = androidx.compose.material3.MaterialTheme.typography.headlineMedium)
        Text("Rule-based local automation foundation")
        appState.tasks.forEach { task ->
            Card(Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text(task.name)
                        Text(task.targetPackage ?: "No package set")
                    }
                    Button(onClick = { appState.markManualRun(task) }) {
                        Text("Run")
                    }
                }
            }
        }
    }
}
