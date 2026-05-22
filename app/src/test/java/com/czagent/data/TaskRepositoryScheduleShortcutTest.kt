package com.czagent.data

import com.czagent.core.model.AutomationTask
import com.czagent.core.model.StepType
import com.czagent.core.model.TaskStep
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class TaskRepositoryScheduleShortcutTest {
    @Test
    fun `save task options persists shortcut and daily schedule`() = runTest {
        val dao = FakeTaskDao()
        val repository = TaskRepository(dao, clock = { 1000 })
        val task = AutomationTask(
            id = 42,
            name = "Reward",
            description = "Collect reward",
            targetPackage = "com.example",
            steps = listOf(TaskStep(1, 0, StepType.COMPLETE)),
        )

        repository.saveTask(
            task,
            SaveTaskOptions(
                createShortcut = true,
                shortcutLabel = "Daily Reward",
                dailyScheduleEnabled = true,
                dailyScheduleLocalTime = "08:30",
            ),
        )

        assertEquals("Daily Reward", dao.shortcuts.single().label)
        assertEquals(42, dao.shortcuts.single().taskId)
        assertEquals("DAILY", dao.schedules.single().type)
        assertEquals("08:30", dao.schedules.single().localTime)
    }

    private class FakeTaskDao : TaskDao {
        val shortcuts = mutableListOf<ShortcutEntity>()
        val schedules = mutableListOf<TaskScheduleEntity>()
        private val tasks = mutableListOf<TaskEntity>()
        private val steps = mutableListOf<TaskStepEntity>()

        override suspend fun listTasks(): List<TaskEntity> = tasks

        override suspend fun listTasksWithSteps(): List<TaskWithSteps> = tasks.map { task ->
            TaskWithSteps(task, steps.filter { it.taskId == task.id })
        }

        override suspend fun getTaskWithSteps(taskId: Long): TaskWithSteps? {
            val task = tasks.firstOrNull { it.id == taskId } ?: return null
            return TaskWithSteps(task, steps.filter { it.taskId == taskId })
        }

        override suspend fun upsertTask(task: TaskEntity): Long {
            tasks.removeAll { it.id == task.id }
            tasks += task
            return task.id
        }

        override suspend fun insertSteps(steps: List<TaskStepEntity>) {
            this.steps += steps
        }

        override suspend fun deleteStepsForTask(taskId: Long) {
            steps.removeAll { it.taskId == taskId }
        }

        override suspend fun replaceShortcut(shortcut: ShortcutEntity) {
            shortcuts.removeAll { it.taskId == shortcut.taskId }
            shortcuts += shortcut
        }

        override suspend fun deleteShortcutForTask(taskId: Long) {
            shortcuts.removeAll { it.taskId == taskId }
        }

        override suspend fun replaceSchedule(schedule: TaskScheduleEntity) {
            schedules.removeAll { it.taskId == schedule.taskId }
            schedules += schedule
        }

        override suspend fun deleteScheduleForTask(taskId: Long) {
            schedules.removeAll { it.taskId == taskId }
        }

        override suspend fun listShortcuts(): List<ShortcutEntity> = shortcuts

        override suspend fun listSchedules(): List<TaskScheduleEntity> = schedules
    }
}
