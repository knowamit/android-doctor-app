package com.androidoctor.domain.usecase

import com.androidoctor.data.analytics.AnalyticsLogger
import com.androidoctor.domain.model.BloatwareEntry
import com.androidoctor.domain.model.Impact
import com.androidoctor.domain.repository.FixRepository
import com.androidoctor.domain.repository.PanicBoostResult
import javax.inject.Inject

enum class FixLevel { SAFE, MODERATE, AGGRESSIVE }

data class FixResult(
    val packagesDisabled: Int,
    val settingsChanged: Int,
    val batteryOptimized: Int,
    val cacheFreedBytes: Long,
    val storageTrimmed: Boolean = false,
    val appsReoptimized: Int = 0,
)

class FixUseCase @Inject constructor(
    private val repository: FixRepository,
    private val analytics: AnalyticsLogger,
) {
    suspend operator fun invoke(
        bloatware: List<BloatwareEntry>,
        level: FixLevel = FixLevel.SAFE,
        topLaunchedApps: List<String> = emptyList(),
    ): FixResult {
        repository.beginSession("Fix (${level.name.lowercase()})")
        try {
            val result = runFix(bloatware, level, topLaunchedApps)
            analytics.fixApplied(level.name.lowercase(), result.packagesDisabled)
            return result
        } finally {
            repository.endSession()
        }
    }

    private suspend fun runFix(
        bloatware: List<BloatwareEntry>,
        level: FixLevel,
        topLaunchedApps: List<String>,
    ): FixResult {
        var packagesDisabled = 0
        var settingsChanged = 0
        var batteryOptimized = 0

        // Phase 1: Debloat
        val targets = bloatware.filter { entry ->
            when (level) {
                FixLevel.SAFE -> entry.impact == Impact.HIGH
                FixLevel.MODERATE -> entry.impact != Impact.LOW
                FixLevel.AGGRESSIVE -> true
            }
        }
        for (entry in targets) {
            if (repository.disablePackage(entry.packageName)) {
                packagesDisabled++
            }
        }

        // Phase 2: Settings
        if (repository.setAnimationScale(0.5f)) settingsChanged++
        if (repository.setBackgroundProcessLimit(4)) settingsChanged++

        // Phase 3: Battery
        for (entry in bloatware.filter { it.impact == Impact.HIGH }) {
            if (repository.restrictBackground(entry.packageName)) {
                batteryOptimized++
            }
        }

        // Phase 4: Cache
        val cacheFreed = repository.trimCaches()

        // Phase 5: Storage TRIM (Moderate + Aggressive)
        // Recovers write speed on worn eMMC/UFS. Real 20-40% gain on 3+ yr devices.
        val storageTrimmed = when (level) {
            FixLevel.SAFE -> false
            else -> repository.fstrimData()
        }

        // Phase 6: ART re-optimization (Aggressive only)
        // `cmd package compile -m speed -f <pkg>` re-compiles apps at speed profile.
        // Cuts cold launch time for frequently-used apps.
        val appsReoptimized = if (level == FixLevel.AGGRESSIVE && topLaunchedApps.isNotEmpty()) {
            repository.reoptimizeTopApps(topLaunchedApps)
        } else 0

        return FixResult(
            packagesDisabled = packagesDisabled,
            settingsChanged = settingsChanged,
            batteryOptimized = batteryOptimized,
            cacheFreedBytes = cacheFreed,
            storageTrimmed = storageTrimmed,
            appsReoptimized = appsReoptimized,
        )
    }
}

class RollbackUseCase @Inject constructor(
    private val repository: FixRepository,
) {
    suspend operator fun invoke(): Int = repository.rollbackLatestSession()
    suspend operator fun invoke(sessionId: Long): Int = repository.rollbackSession(sessionId)
    suspend fun hasSnapshot(): Boolean = repository.hasRollbackSnapshot()
}

class PanicBoostUseCase @Inject constructor(
    private val repository: FixRepository,
) {
    suspend operator fun invoke(): PanicBoostResult {
        repository.beginSession("Panic Boost")
        try {
            return repository.panicBoost()
        } finally {
            repository.endSession()
        }
    }
}

class CpuBoostUseCase @Inject constructor(
    private val repository: FixRepository,
) {
    suspend fun enable() = withSession("CPU Boost") { repository.setCpuGovernor("performance") }
    suspend fun disable() = withSession("CPU Boost off") { repository.setCpuGovernor("schedutil") }
    suspend fun current(): String? = repository.getCurrentCpuGovernor()

    private suspend fun <T> withSession(label: String, block: suspend () -> T): T {
        repository.beginSession(label)
        try {
            return block()
        } finally {
            repository.endSession()
        }
    }
}
