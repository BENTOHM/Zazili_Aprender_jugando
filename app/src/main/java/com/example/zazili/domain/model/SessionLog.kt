package com.example.zazili.domain.model

data class SessionLog(
    val id: Long = 0,
    val moduleRoute: String,
    val activityId: String,
    val succeeded: Boolean,
    val attemptCount: Int,
    val timestampMillis: Long
)
