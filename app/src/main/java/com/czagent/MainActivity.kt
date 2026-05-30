package com.czagent

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.room.Room
import com.czagent.android.automation.AndroidActionExecutor
import com.czagent.android.observation.AndroidScreenObserver
import com.czagent.android.permissions.AndroidPermissionChecker
import com.czagent.android.scheduler.TaskScheduler
import com.czagent.core.engine.ActionExecutor
import com.czagent.core.engine.ScreenObserver
import com.czagent.data.AppDatabase
import com.czagent.data.RunDao
import com.czagent.data.SkillRepository
import com.czagent.data.TaskRepository
import com.czagent.ui.AppState
import com.czagent.ui.MobileAgentApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val database = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "czagent.db",
        ).fallbackToDestructiveMigration().build()
        val taskRepository = TaskRepository(database.taskDao())
        val skillRepository = SkillRepository(database.skillDao(), database.skillParameterDao())
        val factory = AppStateFactory(
            taskRepository = taskRepository,
            runDao = database.runDao(),
            screenObserver = AndroidScreenObserver(),
            actionExecutor = AndroidActionExecutor(applicationContext),
            taskScheduler = TaskScheduler(applicationContext, taskRepository),
            permissionChecker = AndroidPermissionChecker(applicationContext),
            skillRepository = skillRepository,
            skillRunDao = database.skillRunDao(),
        )
        setContent {
            MobileAgentApp(factory)
        }
    }
}

class AppStateFactory(
    private val taskRepository: TaskRepository,
    private val runDao: RunDao,
    private val screenObserver: ScreenObserver,
    private val actionExecutor: ActionExecutor,
    private val taskScheduler: TaskScheduler,
    private val permissionChecker: AndroidPermissionChecker,
    private val skillRepository: SkillRepository? = null,
    private val skillRunDao: com.czagent.data.SkillRunDao? = null,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AppState::class.java)) {
            return AppState(taskRepository, runDao, screenObserver, actionExecutor, taskScheduler, permissionChecker, skillRepository, skillRunDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
