package com.czagent.core.skill

import com.czagent.core.model.AutomationTask
import com.czagent.core.model.TaskStep
import kotlin.math.abs

class SkillResolver {
    fun resolve(
        skill: Skill,
        params: Map<String, String>,
    ): AutomationTask {
        // Validate required params
        validateParams(skill, params)

        // Resolve placeholders in steps
        val resolvedSteps = skill.steps.map { step ->
            resolveStep(step, params)
        }

        // Create task with proper ID, adding OPEN_APP and COMPLETE like TaskDraftBuilder
        val taskId = abs(skill.hashCode().toLong())
        val allSteps = buildList {
            // Find target package from skill (or first OPEN_APP step?)
            // For now, use a placeholder; we can improve later
            add(
                TaskStep(
                    id = taskId + 1,
                    orderIndex = 0,
                    type = com.czagent.core.model.StepType.OPEN_APP,
                    label = "Open app",
                )
            )
            addAll(
                resolvedSteps.mapIndexed { i, step ->
                    step.copy(id = taskId + 2 + i, orderIndex = i + 1)
                }
            )
            add(
                TaskStep(
                    id = taskId + 2 + resolvedSteps.size,
                    orderIndex = resolvedSteps.size + 1,
                    type = com.czagent.core.model.StepType.COMPLETE,
                    label = "Complete",
                )
            )
        }

        return AutomationTask(
            id = taskId,
            name = skill.name,
            description = skill.description,
            targetPackage = null, // Can infer later
            steps = allSteps,
        )
    }

    private fun validateParams(skill: Skill, params: Map<String, String>) {
        skill.parameters.forEach { param ->
            if (param.required && !params.containsKey(param.name)) {
                throw IllegalArgumentException("Missing required parameter: ${param.displayName}")
            }
        }
    }

    private fun resolveStep(step: SkillStep, params: Map<String, String>): TaskStep {
        fun resolve(param: ParamRef?): String? {
            if (param == null) return null
            var result = param
            params.forEach { (key, value) ->
                result = result?.replace("{$key}", value)
            }
            return result
        }

        return TaskStep(
            id = 0L, // Will be set later
            orderIndex = step.orderIndex,
            type = step.type,
            label = resolve(step.label) ?: step.label,
            selectorText = resolve(step.selectorText),
            x = step.x,
            y = step.y,
            inputText = resolve(step.inputText),
            swipeDirection = step.swipeDirection,
            waitMillis = step.waitMillis,
            requiresConfirmation = step.requiresConfirmation,
        )
    }
}
