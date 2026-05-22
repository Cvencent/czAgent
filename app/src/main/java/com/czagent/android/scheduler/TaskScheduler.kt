package com.czagent.android.scheduler

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.WorkManager
import com.czagent.core.scheduler.ScheduleCalculator
import com.czagent.data.TaskRepository
import java.time.LocalDateTime
import java.time.LocalTime

class TaskScheduler(
    context: Context,
    private val taskRepository: TaskRepository,
    private val now: () -> LocalDateTime = { LocalDateTime.now() },
) {
    private val workManager = WorkManager.getInstance(context)

    suspend fun rescheduleAll() {
        taskRepository.listSchedules().forEach { schedule ->
            if (schedule.type == "DAILY") {
                val delay = ScheduleCalculator.delayUntilNextDailyRun(
                    now = now(),
                    localTime = LocalTime.parse(schedule.localTime),
                )
                workManager.enqueueUniqueWork(
                    WorkRequestFactory.dailyWorkName(schedule.taskId),
                    ExistingWorkPolicy.REPLACE,
                    WorkRequestFactory.dailyTaskRequest(schedule.taskId, delay),
                )
            }
        }
    }
}
