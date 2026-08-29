package com.androidoctor.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "fix_changes",
    foreignKeys = [
        ForeignKey(
            entity = FixSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["sessionId"], name = "idx_fix_changes_sessionId")],
)
data class FixChangeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val action: String,
    val target: String,
    val original: String,
    val newValue: String,
    val rolledBack: Boolean = false,
    val rollbackFailed: Boolean = false,
    val rollbackFailReason: String? = null,
)
