package com.czagent.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.czagent.ui.AppState

@Composable
fun TaskEditorScreen(appState: AppState, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Task Editor", style = androidx.compose.material3.MaterialTheme.typography.headlineMedium)
        OutlinedTextField(appState.taskName, { appState.taskName = it }, Modifier.fillMaxWidth(), label = { Text("Task name") })
        OutlinedTextField(appState.taskDescription, { appState.taskDescription = it }, Modifier.fillMaxWidth(), label = { Text("Natural language description") })
        OutlinedTextField(appState.targetPackage, { appState.targetPackage = it }, Modifier.fillMaxWidth(), label = { Text("Target package") })
        OutlinedTextField(appState.firstClickText, { appState.firstClickText = it }, Modifier.fillMaxWidth(), label = { Text("First click text") })
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Daily schedule")
            Switch(appState.dailyScheduleEnabled, { appState.dailyScheduleEnabled = it })
        }
        Button(onClick = { appState.saveDraftTask() }, enabled = appState.taskName.isNotBlank()) {
            Text("Save task")
        }
    }
}
