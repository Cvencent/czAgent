package com.czagent.android.scheduler

import com.czagent.core.model.RunStatus

enum class ScheduledTaskResult {
    SUCCESS,
    FAILURE,
}

class ScheduledTaskCoordinator(
    private val runTask: suspend (Long) -> RunStatus,
    private val rescheduleDaily: suspend (Long) -> Unit,
) {
    suspend fun runScheduledTask(taskId: Long, scheduleType: String?): ScheduledTaskResult {
        if (taskId <= 0) return ScheduledTaskResult.FAILURE

        val status = runTask(taskId)
        if (scheduleType == SCHEDULE_TYPE_DAILY) {
            rescheduleDaily(taskId)
        }

        return if (status == RunStatus.SUCCEEDED) {
            ScheduledTaskResult.SUCCESS
        } else {
            ScheduledTaskResult.FAILURE
        }
    }

    companion object {
        const val SCHEDULE_TYPE_DAILY = "DAILY"
    }
}
