package com.czagent.android.scheduler

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class TaskRunWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val taskId = inputData.getLong("taskId", -1L)
        return if (taskId > 0) {
            Result.failure()
        } else {
            Result.failure()
        }
    }
}
