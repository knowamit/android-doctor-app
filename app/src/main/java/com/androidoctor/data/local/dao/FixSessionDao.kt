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
}
