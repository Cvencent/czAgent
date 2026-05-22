package com.czagent.core.vision

import com.czagent.core.model.AgentAction
import com.czagent.core.model.ScreenSnapshot
import com.czagent.core.model.StepType
import com.czagent.core.model.TaskContext

interface VisionAnalyzer {
    suspend fun analyze(screen: ScreenSnapshot, taskContext: TaskContext): AgentDecision
}

sealed class AgentDecision {
    data class Action(val action: AgentAction) : AgentDecision()
    data class TargetNotFound(val reason: String) : AgentDecision()
}

class RuleBasedVisionAnalyzer : VisionAnalyzer {
    override suspend fun analyze(screen: ScreenSnapshot, taskContext: TaskContext): AgentDecision {
        val step = taskContext.currentStep
        return when (step.type) {
            StepType.OPEN_APP -> {
                val packageName = taskContext.task.targetPackage
                if (packageName.isNullOrBlank()) {
                    AgentDecision.TargetNotFound("Target package is missing")
                } else {
                    AgentDecision.Action(AgentAction.OpenApp(packageName))
                }
            }
            StepType.WAIT -> AgentDecision.Action(AgentAction.Wait(step.waitMillis ?: 1_000L))
            StepType.CLICK_TEXT -> {
                val text = step.selectorText.orEmpty()
                val node = screen.allNodes().firstOrNull { node ->
                    node.visibleLabel().contains(text, ignoreCase = true)
                }
                if (text.isBlank() || node == null) {
                    AgentDecision.TargetNotFound("Text target not found: $text")
                } else {
                    AgentDecision.Action(AgentAction.ClickText(text, node.bounds))
                }
            }
            StepType.CLICK_COORDINATES -> {
                val x = step.x
                val y = step.y
                if (x == null || y == null) {
                    AgentDecision.TargetNotFound("Coordinates are missing")
                } else {
                    AgentDecision.Action(AgentAction.ClickCoordinates(x, y))
                }
            }
            StepType.SWIPE -> {
                val direction = step.swipeDirection
                if (direction == null) {
                    AgentDecision.TargetNotFound("Swipe direction is missing")
                } else {
                    AgentDecision.Action(AgentAction.Swipe(direction))
                }
            }
            StepType.INPUT_TEXT -> {
                val text = step.inputText
                if (text == null) {
                    AgentDecision.TargetNotFound("Input text is missing")
                } else {
                    AgentDecision.Action(AgentAction.InputText(text, step.selectorText))
                }
            }
            StepType.BACK -> AgentDecision.Action(AgentAction.Back)
            StepType.SCREENSHOT -> AgentDecision.Action(AgentAction.Screenshot)
            StepType.COMPLETE -> AgentDecision.Action(AgentAction.Complete)
        }
    }
}
