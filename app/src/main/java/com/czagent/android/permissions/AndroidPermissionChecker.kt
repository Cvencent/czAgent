package com.czagent.android.permissions

import android.content.Context
import android.provider.Settings
import com.czagent.android.automation.MobileAgentAccessibilityService

class AndroidPermissionChecker(
    private val context: Context,
) {
    fun isAccessibilityServiceEnabled(): Boolean {
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        )
        return AccessibilityPermissionParser.isServiceEnabled(
            enabledServices = enabledServices,
            packageName = context.packageName,
            serviceClassName = MobileAgentAccessibilityService::class.java.name,
        )
    }
}
