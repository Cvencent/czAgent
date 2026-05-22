package com.czagent.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.czagent.ui.AppState
import com.czagent.ui.task.DraftStep

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskEditorScreen(appState: AppState, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Task Editor", style = androidx.compose.material3.MaterialTheme.typography.headlineMedium)
        OutlinedTextField(appState.taskName, { appState.taskName = it }, Modifier.fillMaxWidth(), label = { Text("Task name") })
        OutlinedTextField(appState.taskDescription, { appState.taskDescription = it }, Modifier.fillMaxWidth(), label = { Text("Natural language description") })
        OutlinedTextField(appState.targetPackage, { appState.targetPackage = it }, Modifier.fillMaxWidth(), label = { Text("Target package") })
        Text("Steps", style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
        appState.draftSteps.forEachIndexed { index, step ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${index + 1}. ${step.describe()}")
                Button(onClick = { appState.removeDraftStep(index) }) {
                    Text("Remove")
                }
            }
        }
        StepInput(appState)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Shortcut command")
            Switch(appState.shortcutEnabled, { appState.shortcutEnabled = it })
        }
        if (appState.shortcutEnabled) {
            OutlinedTextField(appState.shortcutLabel, { appState.shortcutLabel = it }, Modifier.fillMaxWidth(), label = { Text("Shortcut label") })
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Daily schedule")
            Switch(appState.dailyScheduleEnabled, { appState.dailyScheduleEnabled = it })
        }
        if (appState.dailyScheduleEnabled) {
            OutlinedTextField(appState.dailyScheduleTime, { appState.dailyScheduleTime = it }, Modifier.fillMaxWidth(), label = { Text("Daily time HH:mm") })
        }
        Button(onClick = { appState.saveDraftTask() }, enabled = appState.taskName.isNotBlank()) {
            Text("Save task")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StepInput(appState: AppState) {
    val stepTypes = listOf("CLICK_TEXT", "WAIT", "CLICK_COORDINATES", "INPUT_TEXT", "SWIPE", "BACK", "SCREENSHOT")
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = appState.newStepType,
            onValueChange = {},
            readOnly = true,
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true).fillMaxWidth(),
            label = { Text("Step type") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            stepTypes.forEach { type ->
                DropdownMenuItem(
                    text = { Text(type) },
                    onClick = {
                        appState.newStepType = type
                        expanded = false
                    },
                )
            }
        }
    }

    when (appState.newStepType) {
        "CLICK_TEXT" -> OutlinedTextField(appState.newStepText, { appState.newStepText = it }, Modifier.fillMaxWidth(), label = { Text("Text to click") })
        "WAIT" -> OutlinedTextField(appState.newStepWaitMillis, { appState.newStepWaitMillis = it }, Modifier.fillMaxWidth(), label = { Text("Wait milliseconds") })
        "CLICK_COORDINATES" -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(appState.newStepX, { appState.newStepX = it }, Modifier.weight(1f), label = { Text("X") })
            OutlinedTextField(appState.newStepY, { appState.newStepY = it }, Modifier.weight(1f), label = { Text("Y") })
        }
        "INPUT_TEXT" -> {
            OutlinedTextField(appState.newStepTargetText, { appState.newStepTargetText = it }, Modifier.fillMaxWidth(), label = { Text("Target field text") })
            OutlinedTextField(appState.newStepText, { appState.newStepText = it }, Modifier.fillMaxWidth(), label = { Text("Input text") })
        }
        "SWIPE" -> OutlinedTextField(appState.newStepSwipeDirection, { appState.newStepSwipeDirection = it.uppercase() }, Modifier.fillMaxWidth(), label = { Text("Direction UP/DOWN/LEFT/RIGHT") })
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = { appState.addDraftStep() }) {
            Text("Add step")
        }
        Button(onClick = { appState.clearDraftSteps() }, enabled = appState.draftSteps.isNotEmpty()) {
            Text("Clear")
        }
    }
}

private fun DraftStep.describe(): String = when (this) {
    DraftStep.Back -> "Back"
    is DraftStep.ClickCoordinates -> "Click ($x, $y)"
    is DraftStep.ClickText -> "Click text \"$text\""
    is DraftStep.InputText -> "Input \"$inputText\""
    DraftStep.Screenshot -> "Screenshot"
    is DraftStep.Swipe -> "Swipe $direction"
    is DraftStep.Wait -> "Wait ${millis}ms"
}
