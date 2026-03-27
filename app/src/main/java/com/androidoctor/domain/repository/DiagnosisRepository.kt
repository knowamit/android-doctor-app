package com.androidoctor.domain.repository

import com.androidoctor.domain.model.*
import kotlinx.coroutines.flow.Flow

interface DiagnosisRepository {
    suspend fun getDeviceInfo(): DeviceInfo
    suspend fun diagnoseBattery(): BatteryDiagnosis
    suspend fun diagnoseStorage(): StorageDiagnosis
    suspend fun diagnoseMemory(): MemoryDiagnosis
    suspend fun diagnoseCpu(): CpuDiagnosis
    suspend fun diagnoseBloatware(): BloatwareDiagnosis
    suspend fun computeVerdict(
        battery: BatteryDiagnosis,
        storage: StorageDiagnosis,
        memory: MemoryDiagnosis,
        cpu: CpuDiagnosis,
        bloatware: BloatwareDiagnosis,
    ): Verdict
}

interface FixRepository {
    suspend fun disablePackage(packageName: String): Boolean
    suspend fun enablePackage(packageName: String): Boolean
    suspend fun setAnimationScale(scale: Float): Boolean
    suspend fun setBackgroundProcessLimit(limit: Int): Boolean
    suspend fun restrictBackground(packageName: String): Boolean
    suspend fun trimCaches(): Long // returns bytes freed
    suspend fun rollbackAll(): Int // returns number of changes reverted
    fun hasRollbackSnapshot(): Boolean
}

interface BenchmarkRepository {
    suspend fun runBenchmark(label: String): BenchmarkResult
    suspend fun saveBenchmark(result: BenchmarkResult)
    fun getHistory(): Flow<List<BenchmarkResult>>
}
