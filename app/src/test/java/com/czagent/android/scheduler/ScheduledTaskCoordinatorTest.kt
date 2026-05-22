package com.czagent.android.scheduler

import com.czagent.core.model.RunStatus
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ScheduledTaskCoordinatorTest {
    @Test
    fun `successful daily run reschedules task`() = runTest {
        val rescheduled = mutableListOf<Long>()
        val coordinator = ScheduledTaskCoordinator(
            runTask = { RunStatus.SUCCEEDED },
            rescheduleDaily = { rescheduled += it },
        )

        val result = coordinator.runScheduledTask(taskId = 42, scheduleType = "DAILY")

        assertEquals(ScheduledTaskResult.SUCCESS, result)
        assertEquals(listOf(42L), rescheduled)
    }

    @Test
    fun `failed daily run still reschedules next occurrence`() = runTest {
        val rescheduled = mutableListOf<Long>()
        val coordinator = ScheduledTaskCoordinator(
            runTask = { RunStatus.FAILED },
            rescheduleDaily = { rescheduled += it },
        )

        val result = coordinator.runScheduledTask(taskId = 42, scheduleType = "DAILY")

        assertEquals(ScheduledTaskResult.FAILURE, result)
        assertEquals(listOf(42L), rescheduled)
    }

    @Test
    fun `invalid task id fails without rescheduling`() = runTest {
        val rescheduled = mutableListOf<Long>()
        val coordinator = ScheduledTaskCoordinator(
            runTask = { RunStatus.SUCCEEDED },
            rescheduleDaily = { rescheduled += it },
        )

        val result = coordinator.runScheduledTask(taskId = -1, scheduleType = "DAILY")

        assertEquals(ScheduledTaskResult.FAILURE, result)
        assertEquals(emptyList<Long>(), rescheduled)
    }
}
