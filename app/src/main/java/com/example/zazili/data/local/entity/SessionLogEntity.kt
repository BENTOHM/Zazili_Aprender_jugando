package com.example.zazili.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "session_logs")
data class SessionLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val moduleRoute: String,
    val activityId: String,
    val succeeded: Boolean,
    val attemptCount: Int,
    val timestampMillis: Long
)
