package com.czagent.android.permissions

object AccessibilityPermissionParser {
    fun isServiceEnabled(
        enabledServices: String?,
        packageName: String,
        serviceClassName: String,
    ): Boolean {
        if (enabledServices.isNullOrBlank()) return false
        val fullComponent = "$packageName/$serviceClassName"
        val relativeClassName = serviceClassName.removePrefix(packageName)
        val relativeComponent = "$packageName/$relativeClassName"
        return enabledServices
            .split(':')
            .map { it.trim() }
            .any { it.equals(fullComponent, ignoreCase = true) || it.equals(relativeComponent, ignoreCase = true) }
    }
}
