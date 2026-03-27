package com.androidoctor.data.parser

import com.androidoctor.domain.model.MemoryDiagnosis
import com.androidoctor.domain.model.Severity

object MemoryParser {

    data class RawMemory(
        val totalMb: Int = 0,
        val availableMb: Int = 0,
        val freeMb: Int = 0,
        val cachedMb: Int = 0,
        val swapTotalMb: Int = 0,
        val swapFreeMb: Int = 0,
        val usedPct: Float = 0f,
    )

    fun parseProcMeminfo(output: String): RawMemory {
        val vals = mutableMapOf<String, Long>()
        for (line in output.lines()) {
            val parts = line.split("\\s+".toRegex())
            if (parts.size >= 2) {
                val key = parts[0].trimEnd(':')
                vals[key] = parts[1].toLongOrNull() ?: continue
            }
        }
        val totalKb = vals["MemTotal"] ?: 0
        val availableKb = vals["MemAvailable"] ?: 0
        val freeKb = vals["MemFree"] ?: 0
        val cachedKb = vals["Cached"] ?: 0
        val swapTotalKb = vals["SwapTotal"] ?: 0
        val swapFreeKb = vals["SwapFree"] ?: 0
        val usedPct = if (totalKb > 0) ((totalKb - availableKb).toFloat() / totalKb * 100) else 0f

        return RawMemory(
            totalMb = (totalKb / 1024).toInt(),
            availableMb = (availableKb / 1024).toInt(),
            freeMb = (freeKb / 1024).toInt(),
            cachedMb = (cachedKb / 1024).toInt(),
            swapTotalMb = (swapTotalKb / 1024).toInt(),
            swapFreeMb = (swapFreeKb / 1024).toInt(),
            usedPct = usedPct,
        )
    }

    fun diagnose(raw: RawMemory): MemoryDiagnosis {
        val findings = mutableListOf<String>()
        var score = 100

        when {
            raw.totalMb < 2048 -> { findings.add("Total RAM: ${raw.totalMb} MB — severely insufficient"); score -= 30 }
            raw.totalMb < 3072 -> { findings.add("Total RAM: ${raw.totalMb} MB — low for modern apps"); score -= 20 }
            raw.totalMb < 4096 -> { findings.add("Total RAM: ${raw.totalMb} MB — adequate but tight"); score -= 10 }
            raw.totalMb < 6144 -> findings.add("Total RAM: ${raw.totalMb} MB — decent")
            else -> findings.add("Total RAM: ${raw.totalMb} MB — plenty")
        }

        when {
            raw.usedPct > 90 -> { findings.add("RAM usage CRITICAL: ${"%.1f".format(raw.usedPct)}% (${raw.availableMb} MB free)"); score -= 30 }
            raw.usedPct > 80 -> { findings.add("RAM usage high: ${"%.1f".format(raw.usedPct)}% (${raw.availableMb} MB free)"); score -= 20 }
            raw.usedPct > 70 -> { findings.add("RAM usage moderate: ${"%.1f".format(raw.usedPct)}% (${raw.availableMb} MB free)"); score -= 10 }
            else -> findings.add("RAM usage healthy: ${"%.1f".format(raw.usedPct)}% (${raw.availableMb} MB free)")
        }

        val swapUsed = raw.swapTotalMb - raw.swapFreeMb
        when {
            swapUsed > 1024 -> { findings.add("Heavy swap: $swapUsed MB — severe RAM pressure"); score -= 25 }
            swapUsed > 512 -> { findings.add("Moderate swap: $swapUsed MB — causing slowdowns"); score -= 15 }
            swapUsed > 100 -> { findings.add("Light swap: $swapUsed MB"); score -= 5 }
            raw.swapTotalMb > 0 -> findings.add("No swap in use")
        }

        score = score.coerceIn(0, 100)
        val severity = when {
            score >= 70 -> Severity.OK
            score >= 40 -> Severity.WARNING
            else -> Severity.CRITICAL
        }

        return MemoryDiagnosis(
            severity = severity,
            totalMb = raw.totalMb,
            availableMb = raw.availableMb,
            usedPct = raw.usedPct,
            swapUsedMb = swapUsed,
            findings = findings,
            score = score,
        )
    }
}
