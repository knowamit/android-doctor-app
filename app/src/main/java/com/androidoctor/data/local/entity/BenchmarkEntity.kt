package com.androidoctor.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "benchmarks")
data class BenchmarkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val label: String,
    val ramAvailableMb: Int,
    val ramUsedPct: Float,
    val swapUsedMb: Int,
    val cpuLoad1: Float,
    val seqReadMbps: Float,
    val seqWriteMbps: Float,
    val randReadIops: Float,
    val randWriteIops: Float,
    val processCount: Int,
    val appLaunchesJson: String, // JSON serialized list
)
