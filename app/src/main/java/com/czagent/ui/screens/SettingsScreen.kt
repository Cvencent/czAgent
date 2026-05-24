package com.czagent.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SettingsAccessibility
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.czagent.android.permissions.AndroidPermissionChecker

@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val accessibilityEnabled = remember { mutableStateOf(false) }

    fun refreshAccessibilityState() {
        accessibilityEnabled.value = AndroidPermissionChecker(context).isAccessibilityServiceEnabled()
    }

    LaunchedEffect(Unit) {
        refreshAccessibilityState()
    }

    Column(
        modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineMedium)
        Text("Permission and analyzer status for the local agent.")

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Permissions", style = MaterialTheme.typography.titleMedium)
                PermissionRow("Accessibility service", formatAccessibilityState(accessibilityEnabled.value))
                PermissionRow("Screenshot permission", "not connected")
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Analyzer", style = MaterialTheme.typography.titleMedium)
                Text("Rule-based")
                Text("Sensitive actions still require confirmation.")
            }
        }

        Button(onClick = { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }) {
            Icon(Icons.Default.SettingsAccessibility, null)
            Text("Open Accessibility Settings")
        }
        Button(onClick = { refreshAccessibilityState() }) {
            Icon(Icons.Default.Refresh, null)
            Text("Refresh permission state")
        }
    }
}

@Composable
private fun PermissionRow(label: String, state: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label)
        Text(state, style = MaterialTheme.typography.titleSmall)
    }
}
