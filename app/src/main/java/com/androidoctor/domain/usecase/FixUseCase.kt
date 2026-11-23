package com.androidoctor.domain.usecase

import com.androidoctor.data.analytics.AnalyticsLogger
import com.androidoctor.data.shell.FixLog
import com.androidoctor.data.shell.LineKind
import com.androidoctor.data.shell.StepState
import com.androidoctor.domain.model.BloatwareEntry
import com.androidoctor.domain.model.Impact
import com.androidoctor.domain.repository.FixRepository
import com.androidoctor.domain.repository.PanicBoostResult
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
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
    private val log: FixLog,
) {
    suspend operator fun invoke(
        bloatware: List<BloatwareEntry>,
        level: FixLevel = FixLevel.SAFE,
        topLaunchedApps: List<String> = emptyList(),
    ): FixResult {
        return log.run(
            "Fix (${level.name.lowercase()})",
            listOf("Disable removable apps", "Tune system settings", "Restrict background activity", "Trim caches", "Storage TRIM", "Re-optimize apps"),
        ) {
            repository.beginSession("Fix (${level.name.lowercase()})")
            try {
                val result = runFix(bloatware, level, topLaunchedApps)
                analytics.fixApplied(level.name.lowercase(), result.packagesDisabled)
                result
            } finally {
                withContext(NonCancellable) { repository.endSession() }
            }
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
        log.start(0)
        for (entry in targets) {
            if (repository.disablePackage(entry.packageName)) {
                packagesDisabled++
            }
        }
        log.finish(0, if (targets.isEmpty()) StepState.SKIPPED else StepState.DONE, "$packagesDisabled of ${targets.size} disabled")

        // Phase 2: Settings
        log.start(1)
        if (repository.setAnimationScale(0.5f)) settingsChanged++
        // Limiting background processes affects every app, so only the most aggressive level does it.
        if (level == FixLevel.AGGRESSIVE && repository.setBackgroundProcessLimit(4)) settingsChanged++
        log.finish(1, summary = "$settingsChanged changed")

        // Phase 3: Battery
        log.start(2)
        val heavy = bloatware.filter { it.impact == Impact.HIGH }
        for (entry in heavy) {
            if (repository.restrictBackground(entry.packageName)) {
                batteryOptimized++
            }
        }
        log.finish(2, if (heavy.isEmpty()) StepState.SKIPPED else StepState.DONE, "$batteryOptimized restricted")

        // Phase 4: Cache
        log.start(3)
        val cacheFreed = repository.trimCaches()
        log.finish(3, summary = "${cacheFreed / (1024 * 1024)} MB freed")

        // Phase 5: Storage TRIM (Moderate + Aggressive)
        // Recovers write speed on worn eMMC/UFS. Real 20-40% gain on 3+ yr devices.
        log.start(4)
        val storageTrimmed = when (level) {
            FixLevel.SAFE -> false
            else -> repository.fstrimData()
        }
        log.finish(4, if (level == FixLevel.SAFE) StepState.SKIPPED else if (storageTrimmed) StepState.DONE else StepState.FAILED,
            if (level == FixLevel.SAFE) "Only in Moderate and Aggressive" else null)

        // Phase 6: ART re-optimization (Aggressive only)
        // `cmd package compile -m speed -f <pkg>` re-compiles apps at speed profile.
        // Cuts cold launch time for frequently-used apps.
        log.start(5)
        val appsReoptimized = if (level == FixLevel.AGGRESSIVE && topLaunchedApps.isNotEmpty()) {
            repository.reoptimizeTopApps(topLaunchedApps)
        } else 0
        log.finish(5, if (appsReoptimized > 0) StepState.DONE else StepState.SKIPPED,
            if (appsReoptimized > 0) "$appsReoptimized apps" else "Only in Aggressive")
        log.line(LineKind.NOTE, "Done. Every change can be undone from Fix History.")

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
    suspend fun latest(): com.androidoctor.domain.repository.RollbackOutcome = repository.rollbackLatest()
    suspend operator fun invoke(sessionId: Long): Int = repository.rollbackSession(sessionId)
    suspend fun hasSnapshot(): Boolean = repository.hasRollbackSnapshot()
}

class PanicBoostUseCase @Inject constructor(
    private val repository: FixRepository,
    private val log: FixLog,
) {
    suspend operator fun invoke(): PanicBoostResult = log.run(
        "Panic Boost",
        listOf("Stop background apps", "Trim caches", "Drop system caches", "Turn animations off"),
    ) {
        val sessionId = repository.beginSession("Panic Boost")
        try {
            log.start(0)
            val ended = repository.killAllApps()
            log.finish(0, summary = "$ended processes ended")
            log.start(1)
            val freed = repository.trimCaches()
            log.finish(1, summary = "${freed / (1024 * 1024)} MB freed")
            log.start(2)
            val dropped = repository.dropKernelCaches()
            log.finish(2, if (dropped) StepState.DONE else StepState.SKIPPED, if (dropped) null else "Blocked by Android on this phone")
            log.start(3)
            val anim = repository.setAnimationScale(0.0f)
            log.finish(3, if (anim) StepState.DONE else StepState.FAILED)
            PanicBoostResult(ended, freed, dropped, anim, sessionId)
        } finally {
            withContext(NonCancellable) { repository.endSession() }
        }
    }
}

class CpuBoostUseCase @Inject constructor(
    private val repository: FixRepository,
) {
    /** Returns how many CPU cores accepted the governor (0 means unsupported on this phone). */
    suspend fun enable(): Int = withSession("CPU Boost") { repository.setCpuGovernor("performance") }
    suspend fun disable(): Int = withSession("CPU Boost off") { repository.setCpuGovernor("schedutil") }
    suspend fun current(): String? = repository.getCurrentCpuGovernor()

    private suspend fun <T> withSession(label: String, block: suspend () -> T): T {
        repository.beginSession(label)
        try {
            return block()
        } finally {
            withContext(NonCancellable) { repository.endSession() }
        }
    }
}
