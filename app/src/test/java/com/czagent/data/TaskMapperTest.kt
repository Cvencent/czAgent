package com.czagent.data

import com.czagent.core.model.AutomationTask
import com.czagent.core.model.StepType
import com.czagent.core.model.SwipeDirection
import com.czagent.core.model.TaskStep
import org.junit.Assert.assertEquals
import org.junit.Test

class TaskMapperTest {
    @Test
    fun `domain task maps to entities and back without losing step fields`() {
        val task = AutomationTask(
            id = 42,
            name = "Morning routine",
            description = "Open app and click reward",
            targetPackage = "com.example",
            steps = listOf(
                TaskStep(
                    id = 7,
                    orderIndex = 1,
                    type = StepType.SWIPE,
                    label = "scroll",
                    selectorText = "Rewards",
                    x = 10,
                    y = 20,
                    inputText = "secret",
                    swipeDirection = SwipeDirection.UP,
                    waitMillis = 500,
                    requiresConfirmation = true,
                ),
            ),
        )

        val entities = task.toEntities(now = 1000)
        val roundTrip = entities.toDomain()

        assertEquals(task, roundTrip)
        assertEquals("SWIPE", entities.steps.single().type)
        assertEquals("UP", entities.steps.single().swipeDirection)
    }

    @Test
    fun `steps are sorted by order when mapping back to domain`() {
        val entities = TaskWithSteps(
            task = TaskEntity(
                id = 1,
                name = "Task",
                description = "Desc",
                targetPackage = null,
                createdAt = 1,
                updatedAt = 1,
            ),
            steps = listOf(
                TaskStepEntity(taskId = 1, orderIndex = 2, type = "COMPLETE", label = "", selectorText = null, x = null, y = null, inputText = null, swipeDirection = null, waitMillis = null, requiresConfirmation = false),
                TaskStepEntity(taskId = 1, orderIndex = 0, type = "WAIT", label = "", selectorText = null, x = null, y = null, inputText = null, swipeDirection = null, waitMillis = 100, requiresConfirmation = false),
            ),
        )

        val task = entities.toDomain()

        assertEquals(listOf(StepType.WAIT, StepType.COMPLETE), task.steps.map { it.type })
    }
}
