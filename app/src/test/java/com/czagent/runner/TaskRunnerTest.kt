package com.czagent.runner

import com.czagent.core.engine.ActionExecutor
import com.czagent.core.engine.ScreenObserver
import com.czagent.core.model.ActionResult
import com.czagent.core.model.AgentAction
import com.czagent.core.model.AutomationTask
import com.czagent.core.model.RunStatus
import com.czagent.core.model.ScreenSnapshot
import com.czagent.core.model.StepType
import com.czagent.core.model.TaskStep
import com.czagent.data.RunDao
import com.czagent.data.StepLogEntity
import com.czagent.data.TaskRunEntity
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class TaskRunnerTest {
    @Test
    fun `runs stored task by id and writes successful run`() = runTest {
        val runDao = FakeRunDao()
        val runner = TaskRunner(
            taskLookup = { id ->
                if (id == 42L) {
                    AutomationTask(42, "Task", "desc", "com.example", listOf(TaskStep(1, 0, StepType.COMPLETE)))
                } else {
                    null
                }
            },
            runDao = runDao,
            screenObserver = object : ScreenObserver {
                override suspend fun observe() = ScreenSnapshot(null, null, emptyList())
            },
            actionExecutor = object : ActionExecutor {
                override suspend fun execute(action: AgentAction) = ActionResult.Success
            },
            clock = { 1000 },
        )

        val result = runner.runTaskById(42)

        assertEquals(RunStatus.SUCCEEDED, result)
        assertEquals(RunStatus.SUCCEEDED.name, runDao.run.status)
        assertEquals(42, runDao.run.taskId)
    }

    @Test
    fun `missing task id returns failed without creating run`() = runTest {
        val runDao = FakeRunDao()
        val runner = TaskRunner(
            taskLookup = { null },
            runDao = runDao,
            screenObserver = object : ScreenObserver {
                override suspend fun observe() = ScreenSnapshot(null, null, emptyList())
            },
            actionExecutor = object : ActionExecutor {
                override suspend fun execute(action: AgentAction) = ActionResult.Success
            },
        )

        val result = runner.runTaskById(99)

        assertEquals(RunStatus.FAILED, result)
        assertEquals(null, runDao.createdRunId)
    }

    private class FakeRunDao : RunDao {
        var createdRunId: Long? = null
        var run = TaskRunEntity(1, taskId = -1, status = RunStatus.RUNNING.name, startedAt = 1, endedAt = null, failureReason = null)

        override suspend fun insertRun(run: TaskRunEntity): Long {
            this.run = run.copy(id = 1)
            createdRunId = 1
            return 1
        }

        override suspend fun insertLog(log: StepLogEntity) = Unit

        override suspend fun recentRuns(limit: Int): List<TaskRunEntity> = emptyList()

        override suspend fun listLogs(): List<StepLogEntity> = emptyList()

        override suspend fun updateRun(run: TaskRunEntity) {
            this.run = run
        }

        override suspend fun getRun(runId: Long): TaskRunEntity? = run.takeIf { it.id == runId }
    }
}
