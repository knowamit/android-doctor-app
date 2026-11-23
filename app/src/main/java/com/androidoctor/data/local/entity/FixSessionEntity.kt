package com.androidoctor.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "fix_sessions",
    indices = [
        Index(value = ["timestamp"], name = "idx_fix_sessions_timestamp", orders = [Index.Order.DESC]),
    ],
)
data class FixSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val label: String,
)
