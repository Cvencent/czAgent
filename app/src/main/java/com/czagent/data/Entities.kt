package com.czagent.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String,
    val targetPackage: String?,
    val enabled: Boolean = true,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(tableName = "task_steps")
data class TaskStepEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val taskId: Long,
    val orderIndex: Int,
    val type: String,
    val selectorText: String?,
    val x: Int?,
    val y: Int?,
    val inputText: String?,
    val swipeDirection: String?,
    val waitMillis: Long?,
    val requiresConfirmation: Boolean,
)

@Entity(tableName = "task_schedules")
data class TaskScheduleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val taskId: Long,
    val enabled: Boolean,
    val type: String,
    val localTime: String,
)

@Entity(tableName = "shortcuts")
data class ShortcutEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val taskId: Long,
    val label: String,
    val sortOrder: Int,
)

@Entity(tableName = "task_runs")
data class TaskRunEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val taskId: Long,
    val status: String,
    val startedAt: Long,
    val endedAt: Long?,
    val failureReason: String?,
)

@Entity(tableName = "step_logs")
data class StepLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val runId: Long,
    val stepId: Long,
    val status: String,
    val message: String,
    val timestamp: Long,
)
