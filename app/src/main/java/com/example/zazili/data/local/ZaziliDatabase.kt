package com.example.zazili.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.zazili.data.local.dao.SessionLogDao
import com.example.zazili.data.local.entity.SessionLogEntity

@Database(
    entities = [SessionLogEntity::class],
    version = 1,
    exportSchema = true
)
abstract class ZaziliDatabase : RoomDatabase() {
    abstract fun sessionLogDao(): SessionLogDao
}
