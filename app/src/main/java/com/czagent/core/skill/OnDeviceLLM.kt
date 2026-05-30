package com.czagent.core.skill

import com.czagent.core.model.AgentAction
import com.czagent.core.model.ScreenSnapshot
import com.czagent.core.model.TaskContext
import com.czagent.core.vision.AgentDecision

interface OnDeviceLLM {
    suspend fun understandIntent(
        userInput: String,
        availableSkills: List<Skill>,
    ): SkillMatch?

    suspend fun decideAction(
        screen: ScreenSnapshot,
        taskContext: TaskContext,
    ): AgentDecision
}

class RuleBasedOnDeviceLLM(
    private val skillRepository: com.czagent.data.SkillRepository,
) : OnDeviceLLM {
    override suspend fun understandIntent(
        userInput: String,
        availableSkills: List<Skill>,
    ): SkillMatch? {
        // Simple keyword matching for now
        val inputLower = userInput.lowercase()
        val bestMatch = availableSkills
            .filter { skill ->
                val keywords = listOf(skill.name, skill.description) + skill.tags
                keywords.any { keyword ->
                    keyword.lowercase() in inputLower
                }
            }
            .maxByOrNull { it.tags.size + 2 } // Prefer tags

        return bestMatch?.let {
            SkillMatch(
                skill = it,
                confidence = 0.5f,
                extractedParams = emptyMap(), // Ask user for params
            )
        }
    }

    override suspend fun decideAction(
        screen: ScreenSnapshot,
        taskContext: TaskContext,
    ): AgentDecision {
        // Delegate to existing RuleBasedVisionAnalyzer
        // This is a placeholder that will be replaced by real LLM later
        return com.czagent.core.vision.RuleBasedVisionAnalyzer()
            .analyze(screen, taskContext)
    }
}
