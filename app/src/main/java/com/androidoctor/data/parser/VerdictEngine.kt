package com.androidoctor.data.parser

import com.androidoctor.domain.model.*

object VerdictEngine {

    fun compute(
        battery: BatteryDiagnosis,
        storage: StorageDiagnosis,
        memory: MemoryDiagnosis,
        cpu: CpuDiagnosis,
        bloatware: BloatwareDiagnosis,
    ): Verdict {
        val overall = (
            battery.score * 0.20f +
            storage.score * 0.25f +
            memory.score * 0.25f +
            cpu.score * 0.15f +
            bloatware.score * 0.15f
        ).toInt()

        val hwPenalty = (100 - storage.score).coerceAtLeast(0) +
            if (memory.totalMb < 4096) (100 - memory.score).coerceIn(0, 30) else 0
        val thermalPenalty = (100 - battery.score).coerceAtLeast(0) +
            if (cpu.isThrottling) 20 else 0
        val swPenalty = (100 - bloatware.score).coerceAtLeast(0) +
            (100 - cpu.score).coerceAtLeast(0)

        val totalPenalty = hwPenalty + thermalPenalty + swPenalty
        val hardwarePct: Int
        val thermalPct: Int
        val softwarePct: Int
        if (totalPenalty > 0) {
            hardwarePct = (hwPenalty * 100 / totalPenalty)
            thermalPct = (thermalPenalty * 100 / totalPenalty)
            softwarePct = 100 - hardwarePct - thermalPct
        } else {
            hardwarePct = 0; thermalPct = 0; softwarePct = 0
        }

        val issues = mutableListOf<Pair<String, Int>>()
        if (storage.score < 50) issues.add("Storage degradation (NAND wear)" to (100 - storage.score))
        if (memory.score < 50) issues.add("RAM pressure (swap thrashing)" to (100 - memory.score))
        if (battery.score < 50) issues.add("Battery degradation → thermal throttling" to (100 - battery.score))
        if (bloatware.score < 70) issues.add("Bloatware (${bloatware.bloatwareFound} removable)" to (100 - bloatware.score))
        if (cpu.score < 60) issues.add("High CPU from background processes" to (100 - cpu.score))
        if (storage.score in 50..79) issues.add("Moderate storage wear" to (100 - storage.score))
        if (memory.score in 50..79) issues.add("Elevated RAM usage" to (100 - memory.score))
        if (battery.score in 50..79) issues.add("Battery showing age" to (100 - battery.score))
        issues.sortByDescending { it.second }

        val recommendation = when {
            issues.isEmpty() -> "Your phone looks healthy!"
            "storage" in issues[0].first.lowercase() -> "Primary bottleneck is storage. Debloat and reduce I/O pressure."
            "ram" in issues[0].first.lowercase() -> "Primary bottleneck is RAM. Disable bloatware to free memory."
            "battery" in issues[0].first.lowercase() -> "Battery degradation causing throttling. Consider replacement (~\$30-50)."
            "bloatware" in issues[0].first.lowercase() -> "Manufacturer bloatware is the primary issue. Run Fix to disable it."
            else -> "Multiple factors contributing. Run Fix for software optimizations."
        }

        val severity = when {
            overall >= 80 -> Severity.OK
            overall >= 60 -> Severity.WARNING
            else -> Severity.CRITICAL
        }

        return Verdict(
            overallScore = overall,
            overallSeverity = severity,
            hardwarePct = hardwarePct,
            softwarePct = softwarePct,
            thermalPct = thermalPct,
            topIssues = issues.take(5).map { it.first },
            recommendation = recommendation,
        )
    }
}
