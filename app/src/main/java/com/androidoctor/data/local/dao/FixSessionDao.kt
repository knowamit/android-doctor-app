package com.androidoctor.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.androidoctor.data.local.entity.FixSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FixSessionDao {

    @Insert
    suspend fun insert(session: FixSessionEntity): Long

    @Query("SELECT * FROM fix_sessions ORDER BY timestamp DESC")
    fun getAll(): Flow<List<FixSessionEntity>>

    @Query("SELECT * FROM fix_sessions WHERE id = :sessionId")
    suspend fun getById(sessionId: Long): FixSessionEntity?

    @Query("SELECT * FROM fix_sessions ORDER BY timestamp DESC LIMIT 1")
    suspend fun latest(): FixSessionEntity?

    /** Newest session that still has at least one change that has not been rolled back. */
    @Query(
        """
        SELECT s.* FROM fix_sessions s
        WHERE EXISTS (SELECT 1 FROM fix_changes c WHERE c.sessionId = s.id AND c.rolledBack = 0)
        ORDER BY s.timestamp DESC, s.id DESC LIMIT 1
        """,
    )
    suspend fun latestWithActiveChanges(): FixSessionEntity?

    @Query("DELETE FROM fix_sessions WHERE id = :id AND NOT EXISTS (SELECT 1 FROM fix_changes WHERE sessionId = :id)")
    suspend fun deleteIfEmpty(id: Long)
}
