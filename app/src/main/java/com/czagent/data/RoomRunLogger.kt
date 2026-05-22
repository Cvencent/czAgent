package com.czagent.data

import com.czagent.core.engine.RunLogger
import com.czagent.core.model.RunStatus
import com.czagent.core.model.StepLog

class RoomRunLogger(
    private val runId: Long,
    private val runDao: RunDao,
    private val clock: () -> Long = { System.currentTimeMillis() },
) : RunLogger {
    var finishedStatus: RunStatus? = null
        private set
    var finishedReason: String? = null
        private set

    override suspend fun log(entry: StepLog) {
        runDao.insertLog(
            StepLogEntity(
                runId = runId,
                stepId = entry.stepId,
                status = entry.status.name,
                message = entry.message,
                timestamp = entry.timestampMillis,
            ),
        )
    }

    override suspend fun finish(status: RunStatus, reason: String?) {
        finishedStatus = status
        finishedReason = reason
        val existing = runDao.getRun(runId)
        if (existing != null) {
            runDao.updateRun(
                existing.copy(
                    status = status.name,
                    endedAt = clock(),
                    failureReason = reason,
                ),
            )
        }
    }
}
