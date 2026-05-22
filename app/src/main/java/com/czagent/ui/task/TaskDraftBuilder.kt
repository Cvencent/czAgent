package com.czagent.ui.task

import com.czagent.core.model.AutomationTask
import com.czagent.core.model.StepType
import com.czagent.core.model.SwipeDirection
import com.czagent.core.model.TaskStep

sealed class DraftStep {
    data class Wait(val millis: Long) : DraftStep()
    data class ClickText(val text: String) : DraftStep()
    data class ClickCoordinates(val x: Int, val y: Int) : DraftStep()
    data class InputText(val targetText: String?, val inputText: String) : DraftStep()
    data class Swipe(val direction: SwipeDirection) : DraftStep()
    data object Back : DraftStep()
    data object Screenshot : DraftStep()
}

object TaskDraftBuilder {
    fun buildTask(
        id: Long,
        name: String,
        description: String,
        targetPackage: String,
        draftSteps: List<DraftStep>,
    ): AutomationTask {
        val steps = mutableListOf<TaskStep>()
        steps += TaskStep(
            id = id,
            orderIndex = 0,
            type = StepType.OPEN_APP,
            label = "Open app",
        )
        draftSteps.forEachIndexed { index, step ->
            steps += step.toTaskStep(id = id + index + 1, orderIndex = index + 1)
        }
        steps += TaskStep(
            id = id + draftSteps.size + 1,
            orderIndex = steps.size,
            type = StepType.COMPLETE,
            label = "Complete",
        )
        return AutomationTask(
            id = id,
            name = name.ifBlank { "Untitled Task" },
            description = description,
            targetPackage = targetPackage.ifBlank { null },
            steps = steps,
        )
    }

    private fun DraftStep.toTaskStep(id: Long, orderIndex: Int): TaskStep = when (this) {
        is DraftStep.Wait -> TaskStep(
            id = id,
            orderIndex = orderIndex,
            type = StepType.WAIT,
            label = "Wait ${millis}ms",
            waitMillis = millis,
        )
        is DraftStep.ClickText -> TaskStep(
            id = id,
            orderIndex = orderIndex,
            type = StepType.CLICK_TEXT,
            label = "Click text",
            selectorText = text,
        )
        is DraftStep.ClickCoordinates -> TaskStep(
            id = id,
            orderIndex = orderIndex,
            type = StepType.CLICK_COORDINATES,
            label = "Click coordinates",
            x = x,
            y = y,
        )
        is DraftStep.InputText -> TaskStep(
            id = id,
            orderIndex = orderIndex,
            type = StepType.INPUT_TEXT,
            label = "Input text",
            selectorText = targetText,
            inputText = inputText,
        )
        is DraftStep.Swipe -> TaskStep(
            id = id,
            orderIndex = orderIndex,
            type = StepType.SWIPE,
            label = "Swipe $direction",
            swipeDirection = direction,
        )
        DraftStep.Back -> TaskStep(
            id = id,
            orderIndex = orderIndex,
            type = StepType.BACK,
            label = "Back",
        )
        DraftStep.Screenshot -> TaskStep(
            id = id,
            orderIndex = orderIndex,
            type = StepType.SCREENSHOT,
            label = "Screenshot",
        )
    }
}
