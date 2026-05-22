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
        val status = runner.runTaskById(taskId)
        return if (status == com.czagent.core.model.RunStatus.SUCCEEDED) Result.success() else Result.failure()
    }
}
