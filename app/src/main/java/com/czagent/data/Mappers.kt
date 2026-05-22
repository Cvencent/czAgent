package com.czagent.data

import androidx.room.Embedded
import androidx.room.Relation
import com.czagent.core.model.AutomationTask
import com.czagent.core.model.StepType
import com.czagent.core.model.SwipeDirection
import com.czagent.core.model.TaskStep

data class TaskWithSteps(
    @Embedded val task: TaskEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "taskId",
    )
    val steps: List<TaskStepEntity>,
)

data class TaskEntityBundle(
    val task: TaskEntity,
    val steps: List<TaskStepEntity>,
) {
    fun toDomain(): AutomationTask = TaskWithSteps(task, steps).toDomain()
}

fun AutomationTask.toEntities(now: Long): TaskEntityBundle {
    val taskEntity = TaskEntity(
        id = id,
        name = name,
        description = description,
        targetPackage = targetPackage,
        enabled = true,
        createdAt = now,
        updatedAt = now,
    )
    val stepEntities = steps.map { step ->
        TaskStepEntity(
            id = step.id,
            taskId = id,
            orderIndex = step.orderIndex,
            type = step.type.name,
            label = step.label,
            selectorText = step.selectorText,
            x = step.x,
            y = step.y,
            inputText = step.inputText,
            swipeDirection = step.swipeDirection?.name,
            waitMillis = step.waitMillis,
            requiresConfirmation = step.requiresConfirmation,
        )
    }
    return TaskEntityBundle(taskEntity, stepEntities)
}

fun TaskWithSteps.toDomain(): AutomationTask = AutomationTask(
    id = task.id,
    name = task.name,
    description = task.description,
    targetPackage = task.targetPackage,
    steps = steps.sortedBy { it.orderIndex }.map { it.toDomain() },
)

private fun TaskStepEntity.toDomain(): TaskStep = TaskStep(
    id = id,
    orderIndex = orderIndex,
    type = StepType.valueOf(type),
    label = label,
    selectorText = selectorText,
    x = x,
    y = y,
    inputText = inputText,
    swipeDirection = swipeDirection?.let { SwipeDirection.valueOf(it) },
    waitMillis = waitMillis,
    requiresConfirmation = requiresConfirmation,
)
