package com.example.zazili.domain.repository

import com.example.zazili.domain.model.SessionLog
import kotlinx.coroutines.flow.Flow

interface ProgressRepository {
    fun observeLogs(): Flow<List<SessionLog>>
    suspend fun recordAttempt(log: SessionLog)
}
