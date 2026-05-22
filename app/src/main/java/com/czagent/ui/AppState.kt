package com.czagent.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.czagent.core.model.AutomationTask
import com.czagent.core.model.RunStatus
import com.czagent.core.model.StepLog
import com.czagent.core.model.StepType
import com.czagent.core.model.TaskStep

class AppState : ViewModel() {
    val tasks = mutableStateListOf<AutomationTask>()
    val logs = mutableStateListOf<StepLog>()
    val runs = mutableStateListOf<RunSummary>()

    var currentStatus by mutableStateOf<RunStatus?>(null)
        private set

    var taskName by mutableStateOf("")
    var taskDescription by mutableStateOf("")
    var targetPackage by mutableStateOf("")
    var firstClickText by mutableStateOf("")
    var dailyScheduleEnabled by mutableStateOf(false)

    fun saveDraftTask() {
        val now = System.currentTimeMillis()
        val steps = buildList {
            add(TaskStep(id = now, orderIndex = 0, type = StepType.OPEN_APP))
            if (firstClickText.isNotBlank()) {
                add(TaskStep(id = now + 1, orderIndex = 1, type = StepType.CLICK_TEXT, selectorText = firstClickText))
            }
            add(TaskStep(id = now + 2, orderIndex = size, type = StepType.COMPLETE))
        }
        tasks += AutomationTask(
            id = now,
            name = taskName.ifBlank { "Untitled Task" },
            description = taskDescription,
            targetPackage = targetPackage.ifBlank { null },
            steps = steps,
        )
        taskName = ""
        taskDescription = ""
        targetPackage = ""
        firstClickText = ""
        dailyScheduleEnabled = false
    }

    fun markManualRun(task: AutomationTask) {
        currentStatus = RunStatus.RUNNING
        val startedAt = System.currentTimeMillis()
        runs.add(0, RunSummary(task.name, RunStatus.RUNNING, startedAt, null))
        logs.add(StepLog(task.id, com.czagent.core.model.StepLogStatus.STARTED, "Queued manual run for ${task.name}", startedAt))
    }
}

data class RunSummary(
    val taskName: String,
    val status: RunStatus,
    val startedAt: Long,
    val failureReason: String?,
)
