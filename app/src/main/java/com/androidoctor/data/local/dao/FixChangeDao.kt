package com.androidoctor.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.androidoctor.data.local.entity.FixChangeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FixChangeDao {

    @Insert
    suspend fun insert(change: FixChangeEntity): Long

    @Query("SELECT * FROM fix_changes WHERE sessionId = :sessionId ORDER BY id ASC")
    fun getForSession(sessionId: Long): Flow<List<FixChangeEntity>>

    @Query("SELECT * FROM fix_changes WHERE sessionId = :sessionId ORDER BY id ASC")
    suspend fun getForSessionOnce(sessionId: Long): List<FixChangeEntity>

    // Any not-yet-rolled-back change to the same target from a newer session (higher id
    // correlates with later insertion) means restoring this change's `original` value
    // would stomp state a later session already changed on purpose.
    @Query(
        """
        SELECT EXISTS(
            SELECT 1 FROM fix_changes
            WHERE target = :target AND sessionId > :sessionId AND rolledBack = 0
        )
        """,
    )
    suspend fun hasNewerActiveChange(target: String, sessionId: Long): Boolean

    @Query("UPDATE fix_changes SET rolledBack = 1, rollbackFailed = 0, rollbackFailReason = NULL WHERE id = :id")
    suspend fun markRolledBack(id: Long)

    @Query("UPDATE fix_changes SET rollbackFailed = 1, rollbackFailReason = :reason WHERE id = :id")
    suspend fun markRollbackFailed(id: Long, reason: String)

    @Query("SELECT EXISTS(SELECT 1 FROM fix_changes WHERE rolledBack = 0)")
    suspend fun hasAnyActive(): Boolean
}
