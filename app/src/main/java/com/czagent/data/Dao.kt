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
