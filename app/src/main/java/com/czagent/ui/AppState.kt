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
import com.czagent.core.model.TaskStep
import com.czagent.core.safety.SafetyGuard
import com.czagent.core.vision.RuleBasedVisionAnalyzer
import com.czagent.android.scheduler.TaskScheduler
import com.czagent.data.RoomRunLogger
import com.czagent.data.RunDao
import com.czagent.data.SaveTaskOptions
import com.czagent.data.ShortcutEntity
import com.czagent.data.StepLogEntity
import com.czagent.data.TaskRepository
import com.czagent.data.TaskRunEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AppState(
    private val taskRepository: TaskRepository? = null,
    private val runDao: RunDao? = null,
    private val screenObserver: ScreenObserver = EmptyScreenObserver,
    private val actionExecutor: ActionExecutor = NoOpActionExecutor,
    private val taskScheduler: TaskScheduler? = null,
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
    var firstClickText by mutableStateOf("")
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
        val steps = buildList {
            add(TaskStep(id = now, orderIndex = 0, type = StepType.OPEN_APP))
            if (firstClickText.isNotBlank()) {
                add(TaskStep(id = now + 1, orderIndex = 1, type = StepType.CLICK_TEXT, selectorText = firstClickText))
            }
            add(TaskStep(id = now + 2, orderIndex = size, type = StepType.COMPLETE))
        }
        val task = AutomationTask(
            id = now,
            name = taskName.ifBlank { "Untitled Task" },
            description = taskDescription,
            targetPackage = targetPackage.ifBlank { null },
            steps = steps,
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
        firstClickText = ""
        shortcutEnabled = false
        shortcutLabel = ""
        dailyScheduleEnabled = false
        dailyScheduleTime = "08:30"
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
        val dao = runDao
        viewModelScope.launch {
            val logger = if (dao != null) {
                val runId = withContext(Dispatchers.IO) {
                    dao.insertRun(TaskRunEntity(taskId = task.id, status = RunStatus.RUNNING.name, startedAt = startedAt, endedAt = null, failureReason = null))
                }
                RoomRunLogger(runId, dao)
            } else {
                InMemoryRunLogger(logs) { status, reason ->
                    currentStatus = status
                    val index = runs.indexOf(runSummary)
                    if (index >= 0) runs[index] = runSummary.copy(status = status, failureReason = reason)
                }
            }
            val engine = ExecutionEngine(
                observer = screenObserver,
                analyzer = RuleBasedVisionAnalyzer(),
                executor = actionExecutor,
                safetyGuard = SafetyGuard(),
                logger = logger,
            )
            val status = withContext(Dispatchers.IO) { engine.run(task) }
            currentStatus = status
            val index = runs.indexOf(runSummary)
            if (index >= 0) runs[index] = runSummary.copy(status = status, failureReason = (logger as? RoomRunLogger)?.finishedReason)
            load()
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
