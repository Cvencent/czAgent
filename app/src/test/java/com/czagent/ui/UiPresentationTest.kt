package com.czagent.ui

import com.czagent.core.model.RunStatus
import com.czagent.core.model.StepLog
import com.czagent.core.model.StepLogStatus
import com.czagent.ui.screens.formatAccessibilityState
import com.czagent.ui.screens.formatMonitorHeader
import com.czagent.ui.screens.formatRunSummaryLine
import com.czagent.ui.screens.formatTaskStatsLine
import com.czagent.ui.screens.toDisplayLine
import org.junit.Assert.assertEquals
import org.junit.Test

class UiPresentationTest {
    @Test
    fun `task stats line includes task and shortcut counts`() {
        val line = formatTaskStatsLine(taskCount = 4, shortcutCount = 2, runCount = 7)
        assertEquals("4 tasks  2 shortcuts  7 runs", line)
    }

    @Test
    fun `run summary line includes failure reason when present`() {
        val line = formatRunSummaryLine(
            taskName = "Daily check-in",
            status = RunStatus.FAILED,
            failureReason = "Accessibility service is not enabled",
        )
        assertEquals("Daily check-in - FAILED - Accessibility service is not enabled", line)
    }

    @Test
    fun `monitor header prefers current step and status`() {
        val line = formatMonitorHeader(
            status = RunStatus.RUNNING,
            currentTask = "Rewards loop",
            currentStep = "Click text",
        )
        assertEquals("RUNNING • Rewards loop • Click text", line)
    }

    @Test
    fun `accessibility state is formatted for disabled permission`() {
        assertEquals("disabled", formatAccessibilityState(false))
        assertEquals("enabled", formatAccessibilityState(true))
    }

    @Test
    fun `step log line keeps message intact`() {
        val log = StepLog(12, StepLogStatus.FAILED, "Target not found", 1000)
        assertEquals("FAILED: Target not found", log.toDisplayLine())
    }
}
