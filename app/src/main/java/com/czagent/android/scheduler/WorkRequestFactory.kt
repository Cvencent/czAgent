package com.czagent.android.scheduler

import androidx.work.Data
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import java.time.Duration
import java.util.concurrent.TimeUnit

object WorkRequestFactory {
    fun dailyWorkName(taskId: Long): String = "task-$taskId-daily"

    fun dailyTaskRequest(taskId: Long, initialDelay: Duration): OneTimeWorkRequest =
        OneTimeWorkRequestBuilder<TaskRunWorker>()
            .setInputData(
                Data.Builder()
                    .putLong("taskId", taskId)
                    .putString("scheduleType", ScheduledTaskCoordinator.SCHEDULE_TYPE_DAILY)
                    .build(),
            )
            .setInitialDelay(initialDelay.toMillis(), TimeUnit.MILLISECONDS)
            .build()
}
