package com.czagent.android.scheduler

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.room.Room
import com.czagent.android.automation.AndroidActionExecutor
import com.czagent.android.observation.AndroidScreenObserver
import com.czagent.data.AppDatabase
import com.czagent.data.TaskRepository
import com.czagent.runner.TaskRunner

class TaskRunWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val taskId = inputData.getLong("taskId", -1L)
        val scheduleType = inputData.getString("scheduleType")
        if (taskId <= 0) return Result.failure()
        val database = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "czagent.db",
        ).build()
        val repository = TaskRepository(database.taskDao())
        val runner = TaskRunner(
            taskLookup = { id -> repository.getTask(id) },
            runDao = database.runDao(),
            screenObserver = AndroidScreenObserver(),
            actionExecutor = AndroidActionExecutor(applicationContext),
        )
        val scheduler = TaskScheduler(applicationContext, repository)
        val coordinator = ScheduledTaskCoordinator(
            runTask = { id -> runner.runTaskById(id) },
            rescheduleDaily = { id -> scheduler.rescheduleDaily(id) },
        )
        return when (coordinator.runScheduledTask(taskId, scheduleType)) {
            ScheduledTaskResult.SUCCESS -> Result.success()
            ScheduledTaskResult.FAILURE -> Result.failure()
        }
    }
}
