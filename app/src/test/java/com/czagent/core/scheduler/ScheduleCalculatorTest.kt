package com.czagent.core.scheduler

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime

class ScheduleCalculatorTest {
    @Test
    fun `same day future time returns remaining delay`() {
        val delay = ScheduleCalculator.delayUntilNextDailyRun(
            now = LocalDateTime.of(2026, 5, 22, 9, 0),
            localTime = LocalTime.of(10, 30),
        )

        assertEquals(Duration.ofMinutes(90), delay)
    }

    @Test
    fun `past time rolls over to next day`() {
        val delay = ScheduleCalculator.delayUntilNextDailyRun(
            now = LocalDateTime.of(2026, 5, 22, 18, 0),
            localTime = LocalTime.of(8, 0),
        )

        assertEquals(Duration.ofHours(14), delay)
    }
}
