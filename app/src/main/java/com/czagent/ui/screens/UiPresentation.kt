package com.czagent.ui.screens

import com.czagent.core.model.RunStatus
import com.czagent.core.model.StepLog

fun formatTaskStatsLine(taskCount: Int, shortcutCount: Int, runCount: Int): String =
    "$taskCount tasks  $shortcutCount shortcuts  $runCount runs"

fun formatRunSummaryLine(taskName: String, status: RunStatus, failureReason: String?): String =
    listOfNotNull(taskName, status.name, failureReason?.takeIf { it.isNotBlank() }).joinToString(" - ")

fun formatMonitorHeader(status: RunStatus?, currentTask: String?, currentStep: String?): String {
    val parts = buildList {
        add(status?.name ?: "IDLE")
        currentTask?.takeIf { it.isNotBlank() }?.let(::add)
        currentStep?.takeIf { it.isNotBlank() }?.let(::add)
    }
    return parts.joinToString(" • ")
}

fun formatAccessibilityState(enabled: Boolean): String = if (enabled) "enabled" else "disabled"

fun StepLog.toDisplayLine(): String = "${status.name}: $message"
