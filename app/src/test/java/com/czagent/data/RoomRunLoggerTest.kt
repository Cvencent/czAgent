package com.czagent.data

import com.czagent.core.model.RunStatus
import com.czagent.core.model.StepLog
import com.czagent.core.model.StepLogStatus
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class RoomRunLoggerTest {
    @Test
    fun `log writes step log entity using run id`() = runTest {
        val dao = FakeRunDao()
        val logger = RoomRunLogger(runId = 99, runDao = dao)
        val log = StepLog(7, StepLogStatus.STARTED, "start", 123)

        logger.log(log)

        assertEquals(99, dao.logs.single().runId)
        assertEquals(7, dao.logs.single().stepId)
        assertEquals("STARTED", dao.logs.single().status)
    }

    @Test
    fun `finish stores final status and reason`() = runTest {
        val dao = FakeRunDao()
        val logger = RoomRunLogger(runId = 99, runDao = dao)

        logger.finish(RunStatus.FAILED, "missing")

        assertEquals(RunStatus.FAILED, logger.finishedStatus)
        assertEquals("missing", logger.finishedReason)
    }

    private class FakeRunDao : RunDao {
        val logs = mutableListOf<StepLogEntity>()
        var run = TaskRunEntity(99, taskId = 1, status = RunStatus.RUNNING.name, startedAt = 1, endedAt = null, failureReason = null)

        override suspend fun insertRun(run: TaskRunEntity): Long = run.id

        override suspend fun insertLog(log: StepLogEntity) {
            logs += log
        }

        override suspend fun recentRuns(limit: Int): List<TaskRunEntity> = emptyList()

        override suspend fun listLogs(): List<StepLogEntity> = logs

        override suspend fun updateRun(run: TaskRunEntity) {
            this.run = run
        }

        override suspend fun getRun(runId: Long): TaskRunEntity? = run.takeIf { it.id == runId }
    }
}
