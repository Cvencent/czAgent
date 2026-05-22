package com.czagent.data

import com.czagent.core.model.AutomationTask

class TaskRepository(
    private val taskDao: TaskDao,
    private val clock: () -> Long = { System.currentTimeMillis() },
) {
    suspend fun listTasks(): List<AutomationTask> = taskDao.listTasksWithSteps().map { it.toDomain() }

    suspend fun saveTask(task: AutomationTask): Long {
        val entities = task.toEntities(clock())
        val taskId = taskDao.upsertTask(entities.task)
        taskDao.deleteStepsForTask(taskId)
        taskDao.insertSteps(entities.steps.map { it.copy(taskId = taskId) })
        return taskId
    }
}
