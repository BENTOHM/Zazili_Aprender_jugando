package com.example.zazili.data.repository

import com.example.zazili.data.local.dao.SessionLogDao
import com.example.zazili.data.local.entity.SessionLogEntity
import com.example.zazili.domain.model.SessionLog
import com.example.zazili.domain.repository.ProgressRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ProgressRepositoryImpl(
    private val dao: SessionLogDao
) : ProgressRepository {

    override fun observeLogs(): Flow<List<SessionLog>> =
        dao.observeAll().map { rows -> rows.map { it.toDomain() } }

    override suspend fun recordAttempt(log: SessionLog) {
        dao.insert(log.toEntity())
    }
}

private fun SessionLogEntity.toDomain() = SessionLog(
    id = id,
    moduleRoute = moduleRoute,
    activityId = activityId,
    succeeded = succeeded,
    attemptCount = attemptCount,
    timestampMillis = timestampMillis
)

private fun SessionLog.toEntity() = SessionLogEntity(
    id = id,
    moduleRoute = moduleRoute,
    activityId = activityId,
    succeeded = succeeded,
    attemptCount = attemptCount,
    timestampMillis = timestampMillis
)
