package com.czagent.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        TaskEntity::class,
        TaskStepEntity::class,
        TaskScheduleEntity::class,
        ShortcutEntity::class,
        TaskRunEntity::class,
        StepLogEntity::class,
        SkillEntity::class,
        SkillParameterEntity::class,
        SkillRunEntity::class,
    ],
    version = 3,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun runDao(): RunDao
    abstract fun skillDao(): SkillDao
    abstract fun skillParameterDao(): SkillParameterDao
    abstract fun skillRunDao(): SkillRunDao
}
