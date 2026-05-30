package com.czagent.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "skills")
data class SkillEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val tags: String, // JSON array
    val version: Int,
    val stepsJson: String, // JSON serialized steps
    val triggersJson: String, // JSON serialized triggers
    val enabled: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(tableName = "skill_parameters")
data class SkillParameterEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val skillId: String,
    val name: String,
    val displayName: String,
    val type: String,
    val defaultValue: String?,
    val required: Boolean,
)

@Entity(tableName = "skill_runs")
data class SkillRunEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val skillId: String,
    val skillName: String,
    val status: String,
    val startedAt: Long,
    val endedAt: Long?,
    val failureReason: String?,
    val paramsJson: String?,
)
