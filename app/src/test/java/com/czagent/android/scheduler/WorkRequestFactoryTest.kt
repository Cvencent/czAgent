package com.czagent.android.scheduler

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Duration

class WorkRequestFactoryTest {
    @Test
    fun `daily work name includes task id`() {
        assertEquals("task-42-daily", WorkRequestFactory.dailyWorkName(42))
    }

    @Test
    fun `daily input data contains task id`() {
        val request = WorkRequestFactory.dailyTaskRequest(taskId = 42, initialDelay = Duration.ofMinutes(15))

        assertEquals(42, request.workSpec.input.getLong("taskId", -1))
        assertEquals("DAILY", request.workSpec.input.getString("scheduleType"))
        assertEquals(15 * 60 * 1000L, request.workSpec.initialDelay)
    }
}
