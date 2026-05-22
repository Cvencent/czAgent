package com.czagent.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.czagent.core.engine.ActionExecutor
import com.czagent.core.engine.ExecutionEngine
import com.czagent.core.engine.ScreenObserver
import com.czagent.core.model.ActionResult
import com.czagent.core.model.AgentAction
import com.czagent.core.model.AutomationTask
import com.czagent.core.model.RunStatus
import com.czagent.core.model.ScreenSnapshot
import com.czagent.core.model.StepLog
import com.czagent.core.model.StepLogStatus
import com.czagent.core.model.StepType
import com.czagent.core.model.SwipeDirection
import com.czagent.core.safety.SafetyGuard
import com.czagent.core.vision.RuleBasedVisionAnalyzer
import com.czagent.android.permissions.AndroidPermissionChecker
import com.czagent.android.scheduler.TaskScheduler
import com.czagent.data.RunDao
import com.czagent.data.SaveTaskOptions
import com.czagent.data.ShortcutEntity
import com.czagent.data.StepLogEntity
import com.czagent.data.TaskRepository
import com.czagent.runner.TaskRunPreflight
import com.czagent.runner.TaskRunner
import com.czagent.ui.task.DraftStep
import com.czagent.ui.task.TaskDraftBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AppState(
    private val taskRepository: TaskRepository? = null,
    private val runDao: RunDao? = null,
    private val screenObserver: ScreenObserver = EmptyScreenObserver,
    private val actionExecutor: ActionExecutor = NoOpActionExecutor,
    private val taskScheduler: TaskScheduler? = null,
    private val permissionChecker: AndroidPermissionChecker? = null,
) : ViewModel() {
    val tasks = mutableStateListOf<AutomationTask>()
    val shortcuts = mutableStateListOf<ShortcutCommand>()
    val logs = mutableStateListOf<StepLog>()
    val runs = mutableStateListOf<RunSummary>()

    var currentStatus by mutableStateOf<RunStatus?>(null)
        private set

    var taskName by mutableStateOf("")
    var taskDescription by mutableStateOf("")
    var targetPackage by mutableStateOf("")
    val draftSteps = mutableStateListOf<DraftStep>()
    var newStepType by mutableStateOf("CLICK_TEXT")
    var newStepText by mutableStateOf("")
    var newStepTargetText by mutableStateOf("")
    var newStepX by mutableStateOf("")
    var newStepY by mutableStateOf("")
    var newStepWaitMillis by mutableStateOf("1000")
    var newStepSwipeDirection by mutableStateOf("UP")
    var shortcutEnabled by mutableStateOf(false)
    var shortcutLabel by mutableStateOf("")
    var dailyScheduleEnabled by mutableStateOf(false)
    var dailyScheduleTime by mutableStateOf("08:30")

    fun load() {
        val repository = taskRepository ?: return
        viewModelScope.launch {
            val loadedTasks = withContext(Dispatchers.IO) { repository.listTasks() }
            tasks.clear()
            tasks.addAll(loadedTasks)
            val namesById = loadedTasks.associate { it.id to it.name }
            val loadedShortcuts = withContext(Dispatchers.IO) { repository.listShortcuts() }
            shortcuts.clear()
            shortcuts.addAll(loadedShortcuts.map { it.toCommand(loadedTasks) })
            val loadedRuns = withContext(Dispatchers.IO) { runDao?.recentRuns() ?: emptyList() }
            runs.clear()
            runs.addAll(
                loadedRuns.map {
                    RunSummary(
                        taskName = namesById[it.taskId] ?: "Task ${it.taskId}",
                        status = RunStatus.valueOf(it.status),
                        startedAt = it.startedAt,
                        failureReason = it.failureReason,
                    )
                },
            )
            val loadedLogs = withContext(Dispatchers.IO) { runDao?.listLogs() ?: emptyList() }
            logs.clear()
            logs.addAll(loadedLogs.map { it.toDomainLog() })
        }
    }

    fun saveDraftTask() {
        val now = System.currentTimeMillis()
        val task = TaskDraftBuilder.buildTask(
            id = now,
            name = taskName.ifBlank { "Untitled Task" },
            description = taskDescription,
            targetPackage = targetPackage,
            draftSteps = draftSteps.toList(),
        )
        val repository = taskRepository
        if (repository == null) {
            tasks += task
            if (shortcutEnabled) {
                shortcuts += ShortcutCommand(0, shortcutLabel.ifBlank { task.name }, task.id, task.name)
            }
        } else {
            viewModelScope.launch {
                withContext(Dispatchers.IO) {
                    repository.saveTask(
                        task,
                        SaveTaskOptions(
                            createShortcut = shortcutEnabled,
                            shortcutLabel = shortcutLabel.ifBlank { task.name },
                            dailyScheduleEnabled = dailyScheduleEnabled,
                            dailyScheduleLocalTime = dailyScheduleTime,
                        ),
                    )
                }
                taskScheduler?.rescheduleAll()
                load()
            }
        }
        taskName = ""
        taskDescription = ""
        targetPackage = ""
        draftSteps.clear()
        resetNewStepFields()
        shortcutEnabled = false
        shortcutLabel = ""
        dailyScheduleEnabled = false
        dailyScheduleTime = "08:30"
    }

    fun addDraftStep() {
        val step = when (newStepType) {
            "WAIT" -> DraftStep.Wait(newStepWaitMillis.toLongOrNull() ?: 1_000L)
            "CLICK_TEXT" -> DraftStep.ClickText(newStepText)
            "CLICK_COORDINATES" -> DraftStep.ClickCoordinates(
                x = newStepX.toIntOrNull() ?: 0,
                y = newStepY.toIntOrNull() ?: 0,
            )
            "INPUT_TEXT" -> DraftStep.InputText(
                targetText = newStepTargetText.ifBlank { null },
                inputText = newStepText,
            )
            "SWIPE" -> DraftStep.Swipe(SwipeDirection.valueOf(newStepSwipeDirection))
            "BACK" -> DraftStep.Back
            "SCREENSHOT" -> DraftStep.Screenshot
            else -> DraftStep.ClickText(newStepText)
        }
        draftSteps += step
        resetNewStepFields()
    }

    fun removeDraftStep(index: Int) {
        if (index in draftSteps.indices) {
            draftSteps.removeAt(index)
        }
    }

    fun clearDraftSteps() {
        draftSteps.clear()
    }

    private fun resetNewStepFields() {
        newStepText = ""
        newStepTargetText = ""
        newStepX = ""
        newStepY = ""
        newStepWaitMillis = "1000"
        newStepSwipeDirection = "UP"
    }

    fun markManualRun(task: AutomationTask) {
        runTask(task)
    }

    fun runShortcut(command: ShortcutCommand) {
        val task = tasks.firstOrNull { it.id == command.taskId } ?: return
        runTask(task)
    }

    fun runTask(task: AutomationTask) {
        val startedAt = System.currentTimeMillis()
        currentStatus = RunStatus.RUNNING
        val runSummary = RunSummary(task.name, RunStatus.RUNNING, startedAt, null)
        runs.add(0, runSummary)
        viewModelScope.launch {
            val status = if (taskRepository != null && runDao != null) {
                val runner = TaskRunner(
                    taskLookup = { id -> taskRepository.getTask(id) },
                    runDao = runDao,
                    screenObserver = screenObserver,
                    actionExecutor = actionExecutor,
                    preflightCheck = { preflightCheck() },
                )
                withContext(Dispatchers.IO) { runner.runTask(task) }
            } else {
                val logger = InMemoryRunLogger(logs) { status, reason ->
                    currentStatus = status
                    val index = runs.indexOf(runSummary)
                    if (index >= 0) runs[index] = runSummary.copy(status = status, failureReason = reason)
                }
                val engine = ExecutionEngine(
                    observer = screenObserver,
                    analyzer = RuleBasedVisionAnalyzer(),
                    executor = actionExecutor,
                    safetyGuard = SafetyGuard(),
                    logger = logger,
                )
                withContext(Dispatchers.IO) { engine.run(task) }
            }
            currentStatus = status
            val index = runs.indexOf(runSummary)
            if (index >= 0) runs[index] = runSummary.copy(status = status)
            load()
        }
    }

    private fun preflightCheck(): TaskRunPreflight {
        val checker = permissionChecker ?: return TaskRunPreflight.Passed
        return if (checker.isAccessibilityServiceEnabled()) {
            TaskRunPreflight.Passed
        } else {
            TaskRunPreflight.Failed("Accessibility service is not enabled")
        }
    }
}

data class RunSummary(
    val taskName: String,
    val status: RunStatus,
    val startedAt: Long,
    val failureReason: String?,
)

data class ShortcutCommand(
    val id: Long,
    val label: String,
    val taskId: Long,
    val taskName: String,
)

private object EmptyScreenObserver : ScreenObserver {
    override suspend fun observe(): ScreenSnapshot = ScreenSnapshot(null, null, emptyList())
}

private object NoOpActionExecutor : ActionExecutor {
    override suspend fun execute(action: AgentAction): ActionResult = ActionResult.Success
}

private class InMemoryRunLogger(
    private val logs: MutableList<StepLog>,
    private val onFinish: (RunStatus, String?) -> Unit,
) : com.czagent.core.engine.RunLogger {
    override suspend fun log(entry: StepLog) {
        logs += entry
    }

    override suspend fun finish(status: RunStatus, reason: String?) {
        onFinish(status, reason)
    }
}

private fun StepLogEntity.toDomainLog(): StepLog = StepLog(
    stepId = stepId,
    status = StepLogStatus.valueOf(status),
    message = message,
    timestampMillis = timestamp,
)

private fun ShortcutEntity.toCommand(tasks: List<AutomationTask>): ShortcutCommand {
    val task = tasks.firstOrNull { it.id == taskId }
    return ShortcutCommand(
        id = id,
        label = label,
        taskId = taskId,
        taskName = task?.name ?: "Task $taskId",
    )
}
