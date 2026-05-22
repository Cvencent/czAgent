package com.czagent.ui.task

import com.czagent.core.model.StepType
import com.czagent.core.model.SwipeDirection
import org.junit.Assert.assertEquals
import org.junit.Test

class TaskDraftBuilderTest {
    @Test
    fun `build task includes open app user steps and complete in order`() {
        val task = TaskDraftBuilder.buildTask(
            id = 100,
            name = "Routine",
            description = "Do things",
            targetPackage = "com.example",
            draftSteps = listOf(
                DraftStep.ClickText("Rewards"),
                DraftStep.Wait(1500),
                DraftStep.InputText(targetText = "Search", inputText = "hello"),
                DraftStep.Swipe(SwipeDirection.UP),
                DraftStep.Back,
            ),
        )

        assertEquals(
            listOf(
                StepType.OPEN_APP,
                StepType.CLICK_TEXT,
                StepType.WAIT,
                StepType.INPUT_TEXT,
                StepType.SWIPE,
                StepType.BACK,
                StepType.COMPLETE,
            ),
            task.steps.map { it.type },
        )
        assertEquals(listOf(0, 1, 2, 3, 4, 5, 6), task.steps.map { it.orderIndex })
        assertEquals("Rewards", task.steps[1].selectorText)
        assertEquals(1500L, task.steps[2].waitMillis)
        assertEquals("hello", task.steps[3].inputText)
        assertEquals(SwipeDirection.UP, task.steps[4].swipeDirection)
    }

    @Test
    fun `blank target package becomes null`() {
        val task = TaskDraftBuilder.buildTask(
            id = 100,
            name = "Routine",
            description = "",
            targetPackage = " ",
            draftSteps = emptyList(),
        )

        assertEquals(null, task.targetPackage)
    }
}
