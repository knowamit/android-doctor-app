package com.androidoctor.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.androidoctor.data.local.dao.BenchmarkDao
import com.androidoctor.data.local.dao.FixChangeDao
import com.androidoctor.data.local.dao.FixSessionDao
import com.androidoctor.data.local.dao.ScoreSnapshotDao
import com.androidoctor.data.local.entity.BenchmarkEntity
import com.androidoctor.data.local.entity.FixChangeEntity
import com.androidoctor.data.local.entity.FixSessionEntity
import com.androidoctor.data.local.entity.ScoreSnapshotEntity

@Database(
    entities = [BenchmarkEntity::class, ScoreSnapshotEntity::class, FixSessionEntity::class, FixChangeEntity::class],
    version = 3,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun benchmarkDao(): BenchmarkDao
    abstract fun scoreSnapshotDao(): ScoreSnapshotDao
    abstract fun fixSessionDao(): FixSessionDao
    abstract fun fixChangeDao(): FixChangeDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS score_snapshots (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        timestamp INTEGER NOT NULL,
                        overall INTEGER NOT NULL,
                        battery INTEGER NOT NULL,
                        storage INTEGER NOT NULL,
                        memory INTEGER NOT NULL,
                        cpu INTEGER NOT NULL,
                        bloatware INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS idx_score_snapshots_timestamp ON score_snapshots(timestamp DESC)")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS fix_sessions (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        timestamp INTEGER NOT NULL,
                        label TEXT NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS fix_changes (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        sessionId INTEGER NOT NULL,
                        action TEXT NOT NULL,
                        target TEXT NOT NULL,
                        original TEXT NOT NULL,
                        newValue TEXT NOT NULL,
                        rolledBack INTEGER NOT NULL DEFAULT 0,
                        rollbackFailed INTEGER NOT NULL DEFAULT 0,
                        rollbackFailReason TEXT,
                        FOREIGN KEY(sessionId) REFERENCES fix_sessions(id) ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS idx_fix_changes_sessionId ON fix_changes(sessionId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS idx_fix_sessions_timestamp ON fix_sessions(timestamp DESC)")
            }
        }
    }
}
