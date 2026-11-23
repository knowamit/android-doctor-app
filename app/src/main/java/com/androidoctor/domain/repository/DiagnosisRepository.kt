package com.androidoctor.domain.repository

import com.androidoctor.domain.model.*
import kotlinx.coroutines.flow.Flow

interface DiagnosisRepository {
    suspend fun quickScan(): QuickScanResult
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
    suspend fun beginSession(label: String): Long
    suspend fun endSession()
    suspend fun disablePackage(packageName: String): Boolean
    suspend fun enablePackage(packageName: String): Boolean
    suspend fun setAnimationScale(scale: Float): Boolean
    suspend fun setBackgroundProcessLimit(limit: Int): Boolean
    suspend fun restrictBackground(packageName: String): Boolean
    suspend fun trimCaches(): Long // returns bytes freed
    suspend fun fstrimData(): Boolean // storage TRIM on /data
    suspend fun killAllApps(): Int // returns processes killed
    suspend fun dropKernelCaches(): Boolean // echo 3 > /proc/sys/vm/drop_caches
    suspend fun reoptimizeTopApps(packages: List<String>): Int // `cmd package compile` speed profile
    suspend fun setCpuGovernor(governor: String): Int // returns cpu cores set
    suspend fun getCurrentCpuGovernor(): String?
    suspend fun panicBoost(): PanicBoostResult
    suspend fun rollbackLatestSession(): Int // returns number of changes reverted
    suspend fun rollbackLatest(): RollbackOutcome // newest session that still has active changes
    suspend fun rollbackSession(sessionId: Long): Int // returns number of changes reverted
    suspend fun hasRollbackSnapshot(): Boolean
    suspend fun availableRamBytes(): Long // MemAvailable from /proc/meminfo
    suspend fun runningProcessCount(): Int
}

data class RollbackOutcome(val restored: Int, val kept: Int)

data class PanicBoostResult(
    val processesKilled: Int,
    val cacheFreedBytes: Long,
    val kernelCachesDropped: Boolean,
    val animationsZeroed: Boolean,
    val sessionId: Long = 0,
)

interface BenchmarkRepository {
    suspend fun runBenchmark(label: String): BenchmarkResult
    suspend fun saveBenchmark(result: BenchmarkResult)
    fun getHistory(): Flow<List<BenchmarkResult>>
}
