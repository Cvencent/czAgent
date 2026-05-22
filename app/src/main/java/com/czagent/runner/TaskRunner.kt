package com.czagent.runner

import com.czagent.core.engine.ActionExecutor
import com.czagent.core.engine.ExecutionEngine
import com.czagent.core.engine.ScreenObserver
import com.czagent.core.model.AutomationTask
import com.czagent.core.model.RunStatus
import com.czagent.core.safety.SafetyGuard
import com.czagent.core.vision.RuleBasedVisionAnalyzer
import com.czagent.data.RoomRunLogger
import com.czagent.data.RunDao
import com.czagent.data.TaskRunEntity

sealed class TaskRunPreflight {
    data object Passed : TaskRunPreflight()
    data class Failed(val reason: String) : TaskRunPreflight()
}

class TaskRunner(
    private val taskLookup: suspend (Long) -> AutomationTask?,
    private val runDao: RunDao,
    private val screenObserver: ScreenObserver,
    private val actionExecutor: ActionExecutor,
    private val preflightCheck: (AutomationTask) -> TaskRunPreflight = { TaskRunPreflight.Passed },
    private val clock: () -> Long = { System.currentTimeMillis() },
) {
    suspend fun runTaskById(taskId: Long): RunStatus {
        val task = taskLookup(taskId) ?: return RunStatus.FAILED
        return runTask(task)
    }

    suspend fun runTask(task: AutomationTask): RunStatus {
        val startedAt = clock()
        val runId = runDao.insertRun(
            TaskRunEntity(
                taskId = task.id,
                status = RunStatus.RUNNING.name,
                startedAt = startedAt,
                endedAt = null,
                failureReason = null,
            ),
        )
        val logger = RoomRunLogger(runId, runDao, clock)
        when (val preflight = preflightCheck(task)) {
            TaskRunPreflight.Passed -> Unit
            is TaskRunPreflight.Failed -> {
                logger.finish(RunStatus.FAILED, preflight.reason)
                return RunStatus.FAILED
            }
        }
        val engine = ExecutionEngine(
            observer = screenObserver,
            analyzer = RuleBasedVisionAnalyzer(),
            executor = actionExecutor,
            safetyGuard = SafetyGuard(),
            logger = logger,
            clock = clock,
        )
        return engine.run(task)
    }
}
