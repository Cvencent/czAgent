package com.czagent.data

import com.czagent.core.skill.Skill
import com.czagent.core.skill.SkillParameter

class SkillRepository(
    private val skillDao: SkillDao,
    private val skillParameterDao: SkillParameterDao,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    suspend fun getAll(): List<Skill> {
        val skillEntities = skillDao.getAll()
        return skillEntities.map { entity ->
            val params = skillParameterDao.getBySkillId(entity.id)
            entity.toDomain(params)
        }
    }

    suspend fun getById(id: String): Skill? {
        val entity = skillDao.getById(id) ?: return null
        val params = skillParameterDao.getBySkillId(id)
        return entity.toDomain(params)
    }

    suspend fun getEnabled(): List<Skill> {
        val skillEntities = skillDao.getEnabled()
        return skillEntities.map { entity ->
            val params = skillParameterDao.getBySkillId(entity.id)
            entity.toDomain(params)
        }
    }

    suspend fun save(skill: Skill) {
        val entity = skill.toEntity(clock)
        skillDao.insert(entity)

        skillParameterDao.deleteBySkillId(skill.id)
        skillParameterDao.insertAll(
            skill.parameters.map { it.toEntity(skill.id) }
        )
    }

    suspend fun delete(id: String) {
        skillParameterDao.deleteBySkillId(id)
        skillDao.deleteById(id)
    }
}
