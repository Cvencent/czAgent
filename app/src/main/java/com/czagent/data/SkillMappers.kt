package com.czagent.data

import com.czagent.core.model.StepType
import com.czagent.core.model.SwipeDirection
import com.czagent.core.skill.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val json = Json {
    ignoreUnknownKeys = true
    prettyPrint = false
}

// Serialization DTOs
@Serializable
data class SerializableSkillStep(
    val orderIndex: Int,
    val type: String,
    val label: String,
    val selectorText: String? = null,
    val inputText: String? = null,
    val x: Int? = null,
    val y: Int? = null,
    val swipeDirection: String? = null,
    val waitMillis: Long? = null,
    val requiresConfirmation: Boolean = false,
)

@Serializable
sealed class SerializableTrigger {
    @Serializable
    object Manual : SerializableTrigger()

    @Serializable
    data class Notification(
        val packageName: String,
        val keywordFilter: String? = null,
    ) : SerializableTrigger()

    @Serializable
    data class AppSwitch(
        val packageName: String,
        val onEntry: Boolean,
    ) : SerializableTrigger()

    @Serializable
    data class DailySchedule(
        val localTime: String,
    ) : SerializableTrigger()

    @Serializable
    data class NetworkChange(
        val ssid: String? = null,
    ) : SerializableTrigger()
}

// Entity → Domain
fun SkillEntity.toDomain(parameters: List<SkillParameterEntity>): Skill {
    val steps = try {
        json.decodeFromString<List<SerializableSkillStep>>(stepsJson).map { it.toDomain() }
    } catch (e: Exception) {
        emptyList()
    }

    val triggers = try {
        json.decodeFromString<List<SerializableTrigger>>(triggersJson).map { it.toDomain() }
    } catch (e: Exception) {
        listOf(Trigger.Manual)
    }

    return Skill(
        id = id,
        name = name,
        description = description,
        tags = try {
            json.decodeFromString<List<String>>(tags)
        } catch (e: Exception) {
            emptyList()
        },
        version = version,
        parameters = parameters.map { it.toDomain() },
        steps = steps,
        triggers = triggers,
        enabled = enabled,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
}

fun SkillParameterEntity.toDomain(): SkillParameter = SkillParameter(
    name = name,
    displayName = displayName,
    type = ParamType.valueOf(type),
    defaultValue = defaultValue,
    required = required,
)

fun SerializableSkillStep.toDomain(): SkillStep = SkillStep(
    orderIndex = orderIndex,
    type = StepType.valueOf(type),
    label = label,
    selectorText = selectorText,
    inputText = inputText,
    x = x,
    y = y,
    swipeDirection = swipeDirection?.let { SwipeDirection.valueOf(it) },
    waitMillis = waitMillis,
    requiresConfirmation = requiresConfirmation,
)

fun SerializableTrigger.toDomain(): Trigger = when (this) {
    is SerializableTrigger.Manual -> Trigger.Manual
    is SerializableTrigger.Notification -> Trigger.Notification(packageName, keywordFilter)
    is SerializableTrigger.AppSwitch -> Trigger.AppSwitch(packageName, onEntry)
    is SerializableTrigger.DailySchedule -> Trigger.DailySchedule(localTime)
    is SerializableTrigger.NetworkChange -> Trigger.NetworkChange(ssid)
}

// Domain → Entity
fun Skill.toEntity(clock: () -> Long = System::currentTimeMillis): SkillEntity {
    val stepsJson = json.encodeToString(steps.map { it.toSerializable() })
    val triggersJson = json.encodeToString(triggers.map { it.toSerializable() })
    val tagsJson = json.encodeToString(tags)

    return SkillEntity(
        id = id,
        name = name,
        description = description,
        tags = tagsJson,
        version = version,
        stepsJson = stepsJson,
        triggersJson = triggersJson,
        enabled = enabled,
        createdAt = createdAt,
        updatedAt = clock(),
    )
}

fun SkillParameter.toEntity(skillId: String): SkillParameterEntity = SkillParameterEntity(
    skillId = skillId,
    name = name,
    displayName = displayName,
    type = type.name,
    defaultValue = defaultValue,
    required = required,
)

fun SkillStep.toSerializable(): SerializableSkillStep = SerializableSkillStep(
    orderIndex = orderIndex,
    type = type.name,
    label = label,
    selectorText = selectorText,
    inputText = inputText,
    x = x,
    y = y,
    swipeDirection = swipeDirection?.name,
    waitMillis = waitMillis,
    requiresConfirmation = requiresConfirmation,
)

fun Trigger.toSerializable(): SerializableTrigger = when (this) {
    is Trigger.Manual -> SerializableTrigger.Manual
    is Trigger.Notification -> SerializableTrigger.Notification(packageName, keywordFilter)
    is Trigger.AppSwitch -> SerializableTrigger.AppSwitch(packageName, onEntry)
    is Trigger.DailySchedule -> SerializableTrigger.DailySchedule(localTime)
    is Trigger.NetworkChange -> SerializableTrigger.NetworkChange(ssid)
}
