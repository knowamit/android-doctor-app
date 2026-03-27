package com.androidoctor.domain.usecase

import com.androidoctor.domain.model.BloatwareEntry
import com.androidoctor.domain.model.Impact
import com.androidoctor.domain.repository.FixRepository
import javax.inject.Inject

enum class FixLevel { SAFE, MODERATE, AGGRESSIVE }

data class FixResult(
    val packagesDisabled: Int,
    val settingsChanged: Int,
    val batteryOptimized: Int,
    val cacheFreedBytes: Long,
)

class FixUseCase @Inject constructor(
    private val repository: FixRepository,
) {
    suspend operator fun invoke(
        bloatware: List<BloatwareEntry>,
        level: FixLevel = FixLevel.SAFE,
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

        return FixResult(
            packagesDisabled = packagesDisabled,
            settingsChanged = settingsChanged,
            batteryOptimized = batteryOptimized,
            cacheFreedBytes = cacheFreed,
        )
    }
}

class RollbackUseCase @Inject constructor(
    private val repository: FixRepository,
) {
    suspend operator fun invoke(): Int = repository.rollbackAll()
    fun hasSnapshot(): Boolean = repository.hasRollbackSnapshot()
}
