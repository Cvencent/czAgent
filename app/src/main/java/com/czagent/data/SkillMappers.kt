package com.czagent.data

import com.czagent.core.model.StepType
import com.czagent.core.model.SwipeDirection
import com.czagent.core.skill.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
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

// JSON Import/Export
fun Skill.toJson(): String {
    val exportDto = SkillExportDto(
        name = name,
        description = description,
        tags = tags,
        version = 1,
        parameters = parameters.map { SkillParameterExportDto(it.name, it.displayName, it.type.name, it.defaultValue, it.required) },
        steps = steps.map { it.toSerializable() },
        triggers = triggers.map { it.toSerializable() },
    )
    return json.encodeToString(exportDto)
}

fun Skill.Companion.fromJson(jsonString: String, id: String, clock: () -> Long = System::currentTimeMillis): Skill {
    val dto = json.decodeFromString<SkillExportDto>(jsonString)
    val params = dto.parameters.map { SkillParameter(it.name, it.displayName, ParamType.valueOf(it.type), it.defaultValue, it.required) }
    val steps = dto.steps.map { it.toDomain() }
    val triggers = dto.triggers.map { it.toDomain() }

    return Skill(
        id = id,
        name = dto.name,
        description = dto.description,
        tags = dto.tags,
        version = dto.version,
        parameters = params,
        steps = steps,
        triggers = triggers,
        enabled = true,
        createdAt = clock(),
        updatedAt = clock(),
    )
}

@Serializable
private data class SkillExportDto(
    val name: String,
    val description: String,
    val tags: List<String>,
    val version: Int,
    val parameters: List<SkillParameterExportDto>,
    val steps: List<SerializableSkillStep>,
    val triggers: List<SerializableTrigger>,
)

@Serializable
private data class SkillParameterExportDto(
    val name: String,
    val displayName: String,
    val type: String,
    val defaultValue: String?,
    val required: Boolean,
)
