package com.czagent.core.skill

import com.czagent.core.model.StepType
import com.czagent.core.model.SwipeDirection

data class Skill(
    val id: String,
    val name: String,
    val description: String,
    val tags: List<String>,
    val version: Int,
    val parameters: List<SkillParameter>,
    val steps: List<SkillStep>,
    val triggers: List<Trigger>,
    val enabled: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
) {
    companion object
}

data class SkillParameter(
    val name: String,
    val displayName: String,
    val type: ParamType,
    val defaultValue: String?,
    val required: Boolean,
)

enum class ParamType {
    TEXT, NUMBER, BOOLEAN
}

data class SkillStep(
    val orderIndex: Int,
    val type: StepType,
    val label: String,
    val selectorText: ParamRef? = null,
    val inputText: ParamRef? = null,
    val x: Int? = null,
    val y: Int? = null,
    val swipeDirection: SwipeDirection? = null,
    val waitMillis: Long? = null,
    val requiresConfirmation: Boolean = false,
)

typealias ParamRef = String

sealed class Trigger {
    data object Manual : Trigger()
    data class Notification(val packageName: String, val keywordFilter: String?) : Trigger()
    data class AppSwitch(val packageName: String, val onEntry: Boolean) : Trigger()
    data class DailySchedule(val localTime: String) : Trigger()
    data class NetworkChange(val ssid: String?) : Trigger()
}

data class SkillMatch(
    val skill: Skill,
    val confidence: Float,
    val extractedParams: Map<String, String>,
) {
    companion object
}
