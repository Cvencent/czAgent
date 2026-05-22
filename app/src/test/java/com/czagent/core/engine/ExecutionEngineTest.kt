package com.czagent.core.engine

import com.czagent.core.model.ActionResult
import com.czagent.core.model.AgentAction
import com.czagent.core.model.AutomationTask
import com.czagent.core.model.RunStatus
import com.czagent.core.model.ScreenSnapshot
import com.czagent.core.model.StepLog
import com.czagent.core.model.StepType
import com.czagent.core.model.TaskStep
import com.czagent.core.safety.SafetyGuard
import com.czagent.core.vision.RuleBasedVisionAnalyzer
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExecutionEngineTest {
    @Test
    fun `runs steps to completion and records logs`() = runTest {
        val logger = FakeLogger()
        val engine = engine(logger = logger)
        val task = task(listOf(TaskStep(1, 0, StepType.WAIT, waitMillis = 10)))

        val result = engine.run(task)

        assertEquals(RunStatus.SUCCEEDED, result)
        assertEquals(RunStatus.SUCCEEDED, logger.finishedStatus)
        assertTrue(logger.logs.any { it.message == "Step succeeded" })
    }

    @Test
    fun `retries transient target not found three times`() = runTest {
        val logger = FakeLogger()
        val engine = engine(logger = logger)
        val task = task(listOf(TaskStep(1, 0, StepType.CLICK_TEXT, selectorText = "Missing")))

        val result = engine.run(task)

        assertEquals(RunStatus.FAILED, result)
        assertEquals(3, logger.logs.count { it.message.contains("Text target not found") && it.status.name == "RETRYING" })
    }

    @Test
    fun `stops when safety confirmation is required`() = runTest {
        val logger = FakeLogger()
        val engine = engine(logger = logger)
        val task = task(listOf(TaskStep(1, 0, StepType.CLICK_TEXT, label = "支付", selectorText = "支付")))

        val result = engine.run(task)

        assertEquals(RunStatus.WAITING_FOR_CONFIRMATION, result)
        assertEquals(RunStatus.WAITING_FOR_CONFIRMATION, logger.finishedStatus)
    }

    @Test
    fun `stops when max steps is exceeded`() = runTest {
        val logger = FakeLogger()
        val engine = engine(logger = logger, maxSteps = 1)
        val task = task(
            listOf(
                TaskStep(1, 0, StepType.WAIT, waitMillis = 10),
                TaskStep(2, 1, StepType.WAIT, waitMillis = 10),
            ),
        )

        val result = engine.run(task)

        assertEquals(RunStatus.FAILED, result)
        assertEquals("Task exceeds maximum step count", logger.finishedReason)
    }

    @Test
    fun `honors cancellation before next step`() = runTest {
        val logger = FakeLogger()
        val engine = engine(logger = logger)
        val task = task(
            listOf(
                TaskStep(1, 0, StepType.WAIT, waitMillis = 10),
                TaskStep(2, 1, StepType.WAIT, waitMillis = 10),
            ),
        )
        var calls = 0

        val result = engine.run(task) {
            calls += 1
            calls > 1
        }

        assertEquals(RunStatus.CANCELLED, result)
    }

    private fun engine(logger: FakeLogger, maxSteps: Int = 50) = ExecutionEngine(
        observer = object : ScreenObserver {
            override suspend fun observe() = ScreenSnapshot(null, null, emptyList())
        },
        analyzer = RuleBasedVisionAnalyzer(),
        executor = object : ActionExecutor {
            override suspend fun execute(action: AgentAction) = ActionResult.Success
        },
        safetyGuard = SafetyGuard(),
        logger = logger,
        clock = { 1L },
        maxSteps = maxSteps,
    )

    private fun task(steps: List<TaskStep>) = AutomationTask(1, "Task", "desc", "com.example", steps)

    private class FakeLogger : RunLogger {
        val logs = mutableListOf<StepLog>()
        var finishedStatus: RunStatus? = null
        var finishedReason: String? = null

        override suspend fun log(entry: StepLog) {
            logs += entry
        }

        override suspend fun finish(status: RunStatus, reason: String?) {
            finishedStatus = status
            finishedReason = reason
        }
    }
}
