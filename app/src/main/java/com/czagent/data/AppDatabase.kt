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
    ],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun runDao(): RunDao
}
