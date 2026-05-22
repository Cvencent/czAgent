package com.czagent.core.engine

import com.czagent.core.model.ActionResult
import com.czagent.core.model.AgentAction
import com.czagent.core.model.AutomationTask
import com.czagent.core.model.RunStatus
import com.czagent.core.model.ScreenSnapshot
import com.czagent.core.model.StepLog
import com.czagent.core.model.StepLogStatus
import com.czagent.core.model.TaskContext
import com.czagent.core.safety.SafetyDecision
import com.czagent.core.safety.SafetyGuard
import com.czagent.core.vision.AgentDecision
import com.czagent.core.vision.VisionAnalyzer

interface ScreenObserver {
    suspend fun observe(): ScreenSnapshot
}

interface ActionExecutor {
    suspend fun execute(action: AgentAction): ActionResult
}

interface RunLogger {
    suspend fun log(entry: StepLog)
    suspend fun finish(status: RunStatus, reason: String?)
}

fun interface CancellationSignal {
    fun isCancellationRequested(): Boolean
}

class ExecutionEngine(
    private val observer: ScreenObserver,
    private val analyzer: VisionAnalyzer,
    private val executor: ActionExecutor,
    private val safetyGuard: SafetyGuard,
    private val logger: RunLogger,
    private val clock: () -> Long = { System.currentTimeMillis() },
    private val maxSteps: Int = 50,
    private val maxRetries: Int = 3,
) {
    suspend fun run(task: AutomationTask, cancellationSignal: CancellationSignal = CancellationSignal { false }): RunStatus {
        val history = mutableListOf<StepLog>()
        if (task.steps.size > maxSteps) {
            logger.finish(RunStatus.FAILED, "Task exceeds maximum step count")
            return RunStatus.FAILED
        }

        for (step in task.steps.sortedBy { it.orderIndex }) {
            if (cancellationSignal.isCancellationRequested()) {
                logger.finish(RunStatus.CANCELLED, "Cancelled before step ${step.id}")
                return RunStatus.CANCELLED
            }

            val screen = observer.observe()
            when (val safety = safetyGuard.evaluate(step, screen)) {
                SafetyDecision.Allowed -> Unit
                is SafetyDecision.RequiresConfirmation -> {
                    val log = StepLog(step.id, StepLogStatus.BLOCKED, safety.reason, clock())
                    logger.log(log)
                    history += log
                    logger.finish(RunStatus.WAITING_FOR_CONFIRMATION, safety.reason)
                    return RunStatus.WAITING_FOR_CONFIRMATION
                }
            }

            val started = StepLog(step.id, StepLogStatus.STARTED, "Starting step ${step.type}", clock())
            logger.log(started)
            history += started

            var attempts = 0
            while (attempts <= maxRetries) {
                attempts += 1
                val latestScreen = observer.observe()
                val context = TaskContext(task, step, history)
                when (val decision = analyzer.analyze(latestScreen, context)) {
                    is AgentDecision.TargetNotFound -> {
                        if (attempts > maxRetries) {
                            val failed = StepLog(step.id, StepLogStatus.FAILED, decision.reason, clock())
                            logger.log(failed)
                            logger.finish(RunStatus.FAILED, decision.reason)
                            return RunStatus.FAILED
                        }
                        logger.log(StepLog(step.id, StepLogStatus.RETRYING, decision.reason, clock()))
                    }
                    is AgentDecision.Action -> {
                        when (val result = executor.execute(decision.action)) {
                            ActionResult.Success -> {
                                val success = StepLog(step.id, StepLogStatus.SUCCEEDED, "Step succeeded", clock())
                                logger.log(success)
                                history += success
                                attempts = maxRetries + 1
                            }
                            is ActionResult.Failure -> {
                                if (!result.transient || attempts > maxRetries) {
                                    val failed = StepLog(step.id, StepLogStatus.FAILED, result.reason, clock())
                                    logger.log(failed)
                                    logger.finish(RunStatus.FAILED, result.reason)
                                    return RunStatus.FAILED
                                }
                                logger.log(StepLog(step.id, StepLogStatus.RETRYING, result.reason, clock()))
                            }
                        }
                    }
                }
            }
        }

        logger.finish(RunStatus.SUCCEEDED, null)
        return RunStatus.SUCCEEDED
    }
}
