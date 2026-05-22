package com.czagent.data

import com.czagent.core.model.AutomationTask

class TaskRepository(
    private val taskDao: TaskDao,
    private val clock: () -> Long = { System.currentTimeMillis() },
) {
    suspend fun listTasks(): List<AutomationTask> = taskDao.listTasksWithSteps().map { it.toDomain() }

    suspend fun getTask(taskId: Long): AutomationTask? = taskDao.getTaskWithSteps(taskId)?.toDomain()

    suspend fun listShortcuts(): List<ShortcutEntity> = taskDao.listShortcuts()

    suspend fun listSchedules(): List<TaskScheduleEntity> = taskDao.listSchedules()

    suspend fun saveTask(task: AutomationTask, options: SaveTaskOptions = SaveTaskOptions()): Long {
        val entities = task.toEntities(clock())
        val taskId = taskDao.upsertTask(entities.task)
        taskDao.deleteStepsForTask(taskId)
        taskDao.insertSteps(entities.steps.map { it.copy(taskId = taskId) })
        if (options.createShortcut) {
            taskDao.replaceShortcut(
                ShortcutEntity(
                    taskId = taskId,
                    label = options.shortcutLabel.ifBlank { task.name },
                    sortOrder = 0,
                ),
            )
        } else {
            taskDao.deleteShortcutForTask(taskId)
        }
        if (options.dailyScheduleEnabled && options.dailyScheduleLocalTime.isNotBlank()) {
            taskDao.replaceSchedule(
                TaskScheduleEntity(
                    taskId = taskId,
                    enabled = true,
                    type = "DAILY",
                    localTime = options.dailyScheduleLocalTime,
                ),
            )
        } else {
            taskDao.deleteScheduleForTask(taskId)
        }
        return taskId
    }
}

data class SaveTaskOptions(
    val createShortcut: Boolean = false,
    val shortcutLabel: String = "",
    val dailyScheduleEnabled: Boolean = false,
    val dailyScheduleLocalTime: String = "",
)
