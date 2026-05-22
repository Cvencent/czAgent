package com.czagent.core.vision

import com.czagent.core.model.AgentAction
import com.czagent.core.model.AutomationTask
import com.czagent.core.model.RectBounds
import com.czagent.core.model.ScreenNode
import com.czagent.core.model.ScreenSnapshot
import com.czagent.core.model.StepType
import com.czagent.core.model.TaskContext
import com.czagent.core.model.TaskStep
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RuleBasedVisionAnalyzerTest {
    private val analyzer = RuleBasedVisionAnalyzer()

    @Test
    fun `click text step resolves matching accessibility node`() = runTest {
        val step = TaskStep(1, 0, StepType.CLICK_TEXT, selectorText = "Settings")
        val screen = ScreenSnapshot(
            packageName = "com.example",
            activityName = "Main",
            rootNodes = listOf(ScreenNode("Settings", null, "Button", RectBounds(1, 2, 30, 40), clickable = true)),
        )

        val decision = analyzer.analyze(screen, context(step))

        assertTrue(decision is AgentDecision.Action)
        assertEquals(AgentAction.ClickText("Settings", RectBounds(1, 2, 30, 40)), (decision as AgentDecision.Action).action)
    }

    @Test
    fun `coordinate step resolves coordinates without node`() = runTest {
        val step = TaskStep(1, 0, StepType.CLICK_COORDINATES, x = 10, y = 20)

        val decision = analyzer.analyze(ScreenSnapshot(null, null, emptyList()), context(step))

        assertEquals(AgentDecision.Action(AgentAction.ClickCoordinates(10, 20)), decision)
    }

    @Test
    fun `missing text returns target not found`() = runTest {
        val step = TaskStep(1, 0, StepType.CLICK_TEXT, selectorText = "Missing")

        val decision = analyzer.analyze(ScreenSnapshot(null, null, emptyList()), context(step))

        assertTrue(decision is AgentDecision.TargetNotFound)
    }

    private fun context(step: TaskStep) = TaskContext(
        task = AutomationTask(1, "Task", "desc", "com.example", listOf(step)),
        currentStep = step,
    )
}
