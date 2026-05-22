package com.czagent.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY updatedAt DESC")
    suspend fun listTasks(): List<TaskEntity>

    @Transaction
    @Query("SELECT * FROM tasks ORDER BY updatedAt DESC")
    suspend fun listTasksWithSteps(): List<TaskWithSteps>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTask(task: TaskEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSteps(steps: List<TaskStepEntity>)

    @Query("DELETE FROM task_steps WHERE taskId = :taskId")
    suspend fun deleteStepsForTask(taskId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun replaceShortcut(shortcut: ShortcutEntity)

    @Query("DELETE FROM shortcuts WHERE taskId = :taskId")
    suspend fun deleteShortcutForTask(taskId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun replaceSchedule(schedule: TaskScheduleEntity)

    @Query("DELETE FROM task_schedules WHERE taskId = :taskId")
    suspend fun deleteScheduleForTask(taskId: Long)

    @Query("SELECT * FROM shortcuts ORDER BY sortOrder ASC")
    suspend fun listShortcuts(): List<ShortcutEntity>

    @Query("SELECT * FROM task_schedules WHERE enabled = 1")
    suspend fun listSchedules(): List<TaskScheduleEntity>
}

@Dao
interface RunDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRun(run: TaskRunEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: StepLogEntity)

    @Query("SELECT * FROM task_runs ORDER BY startedAt DESC LIMIT :limit")
    suspend fun recentRuns(limit: Int = 50): List<TaskRunEntity>

    @Query("SELECT * FROM step_logs ORDER BY timestamp ASC")
    suspend fun listLogs(): List<StepLogEntity>

    @Update
    suspend fun updateRun(run: TaskRunEntity)

    @Query("SELECT * FROM task_runs WHERE id = :runId LIMIT 1")
    suspend fun getRun(runId: Long): TaskRunEntity?
}
