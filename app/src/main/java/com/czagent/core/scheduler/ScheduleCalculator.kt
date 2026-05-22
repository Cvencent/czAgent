package com.czagent.core.scheduler

import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime

object ScheduleCalculator {
    fun delayUntilNextDailyRun(now: LocalDateTime, localTime: LocalTime): Duration {
        var next = now.toLocalDate().atTime(localTime)
        if (!next.isAfter(now)) {
            next = next.plusDays(1)
        }
        return Duration.between(now, next)
    }
}
