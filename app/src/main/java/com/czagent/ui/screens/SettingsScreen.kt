package com.czagent.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
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
    Column(modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Settings", style = androidx.compose.material3.MaterialTheme.typography.headlineMedium)
        Text("Analyzer: Rule-based")
        Text("Accessibility service: ${if (accessibilityEnabled.value) "enabled" else "disabled"}")
        Text("Screenshot permission: not connected")
        Button(onClick = { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }) {
            Text("Open Accessibility Settings")
        }
        Button(onClick = { refreshAccessibilityState() }) {
            Text("Refresh permission state")
        }
    }
}
