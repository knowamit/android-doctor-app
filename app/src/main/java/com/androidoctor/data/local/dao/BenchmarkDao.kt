package com.androidoctor.data.local.dao

import androidx.room.*
import com.androidoctor.data.local.entity.BenchmarkEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BenchmarkDao {

    @Insert
    suspend fun insert(benchmark: BenchmarkEntity)

    @Query("SELECT * FROM benchmarks ORDER BY timestamp DESC LIMIT 100")
    fun getAll(): Flow<List<BenchmarkEntity>>

    @Query("SELECT * FROM benchmarks WHERE label = :label ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestByLabel(label: String): BenchmarkEntity?

    @Query("DELETE FROM benchmarks")
    suspend fun deleteAll()
}
