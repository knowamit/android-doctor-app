package com.androidoctor.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.androidoctor.data.local.dao.BenchmarkDao
import com.androidoctor.data.local.entity.BenchmarkEntity

@Database(
    entities = [BenchmarkEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun benchmarkDao(): BenchmarkDao
}
