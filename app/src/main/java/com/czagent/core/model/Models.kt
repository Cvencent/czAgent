package com.czagent.core.model

data class AutomationTask(
    val id: Long,
    val name: String,
    val description: String,
    val targetPackage: String?,
    val steps: List<TaskStep>,
)

data class TaskStep(
    val id: Long,
    val orderIndex: Int,
    val type: StepType,
    val label: String = "",
    val selectorText: String? = null,
    val x: Int? = null,
    val y: Int? = null,
    val inputText: String? = null,
    val swipeDirection: SwipeDirection? = null,
    val waitMillis: Long? = null,
    val requiresConfirmation: Boolean = false,
)

enum class StepType {
    OPEN_APP,
    WAIT,
    CLICK_TEXT,
    CLICK_COORDINATES,
    SWIPE,
    INPUT_TEXT,
    BACK,
    SCREENSHOT,
    COMPLETE,
}

enum class SwipeDirection {
    UP,
    DOWN,
    LEFT,
    RIGHT,
}

data class RectBounds(
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int,
)

data class ScreenNode(
    val text: String?,
    val contentDescription: String?,
    val className: String?,
    val bounds: RectBounds?,
    val clickable: Boolean = false,
    val editable: Boolean = false,
    val children: List<ScreenNode> = emptyList(),
) {
    fun flatten(): List<ScreenNode> = listOf(this) + children.flatMap { it.flatten() }

    fun visibleLabel(): String = listOfNotNull(text, contentDescription)
        .joinToString(" ")
        .trim()
}

data class ScreenSnapshot(
    val packageName: String?,
    val activityName: String?,
    val rootNodes: List<ScreenNode>,
    val latestScreenshotUri: String? = null,
) {
    fun allNodes(): List<ScreenNode> = rootNodes.flatMap { it.flatten() }
    fun visibleText(): String = allNodes().joinToString(" ") { it.visibleLabel() }
}

data class TaskContext(
    val task: AutomationTask,
    val currentStep: TaskStep,
    val history: List<StepLog> = emptyList(),
)

sealed class AgentAction {
    data class OpenApp(val packageName: String) : AgentAction()
    data class ClickText(val text: String, val bounds: RectBounds? = null) : AgentAction()
    data class ClickCoordinates(val x: Int, val y: Int) : AgentAction()
    data class Swipe(val direction: SwipeDirection) : AgentAction()
    data class InputText(val text: String, val targetText: String? = null) : AgentAction()
    data object Back : AgentAction()
    data class Wait(val millis: Long) : AgentAction()
    data object Screenshot : AgentAction()
    data object Complete : AgentAction()
}

sealed class ActionResult {
    data object Success : ActionResult()
    data class Failure(val reason: String, val transient: Boolean = true) : ActionResult()
}

enum class RunStatus {
    RUNNING,
    SUCCEEDED,
    FAILED,
    WAITING_FOR_CONFIRMATION,
    CANCELLED,
    TIMEOUT,
}

enum class StepLogStatus {
    STARTED,
    SUCCEEDED,
    FAILED,
    RETRYING,
    BLOCKED,
}

data class StepLog(
    val stepId: Long,
    val status: StepLogStatus,
    val message: String,
    val timestampMillis: Long,
)
