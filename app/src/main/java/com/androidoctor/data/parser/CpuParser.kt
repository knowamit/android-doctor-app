package com.androidoctor.data.parser

import com.androidoctor.domain.model.CpuDiagnosis
import com.androidoctor.domain.model.Severity

object CpuParser {

    fun parseDumpsysCpuinfo(output: String, loadavgOutput: String): CpuDiagnosis {
        val processes = mutableListOf<Pair<String, Float>>()
        var totalLoad = 0f
        var load1 = 0f; var load5 = 0f; var load15 = 0f

        for (line in output.lines()) {
            val trimmed = line.trim()
            if (trimmed.startsWith("Load:")) {
                val parts = trimmed.removePrefix("Load:").trim().split("/")
                if (parts.size >= 3) {
                    load1 = parts[0].trim().toFloatOrNull() ?: 0f
                    load5 = parts[1].trim().toFloatOrNull() ?: 0f
                    load15 = parts[2].trim().toFloatOrNull() ?: 0f
                }
                continue
            }
            if ("%" in trimmed && "/" in trimmed) {
                val pctStr = trimmed.substringBefore("%").trim()
                val cpuPct = pctStr.toFloatOrNull() ?: continue
                totalLoad += cpuPct
                val afterPct = trimmed.substringAfter("%").trim()
                val pidName = afterPct.substringBefore(":").trim()
                if ("/" in pidName) {
                    val name = pidName.substringAfter("/").trim()
                    processes.add(name to cpuPct)
                }
            }
        }

        // Fallback loadavg from /proc/loadavg
        if (load1 == 0f && loadavgOutput.isNotBlank()) {
            val parts = loadavgOutput.trim().split("\\s+".toRegex())
            if (parts.size >= 3) {
                load1 = parts[0].toFloatOrNull() ?: 0f
                load5 = parts[1].toFloatOrNull() ?: 0f
                load15 = parts[2].toFloatOrNull() ?: 0f
            }
        }

        // Normalize total load if multi-core >100%
        val normalizedLoad = if (totalLoad > 100) {
            val cores = (totalLoad / 100).toInt() + 1
            totalLoad / cores
        } else totalLoad

        val topHogs = processes.sortedByDescending { it.second }.take(5).filter { it.second > 10 }

        val findings = mutableListOf<String>()
        var score = 100

        when {
            load1 > 4.0 -> { findings.add("Load average extremely high: ${"%.1f".format(load1)} / ${"%.1f".format(load5)} / ${"%.1f".format(load15)}"); score -= 25 }
            load1 > 2.0 -> { findings.add("Load average elevated: ${"%.1f".format(load1)} / ${"%.1f".format(load5)} / ${"%.1f".format(load15)}"); score -= 15 }
            load1 > 1.0 -> { findings.add("Load average moderate: ${"%.1f".format(load1)}"); score -= 5 }
            else -> findings.add("Load average normal: ${"%.1f".format(load1)}")
        }

        if (normalizedLoad > 80) { findings.add("CPU utilization very high: ${"%.1f".format(normalizedLoad)}%"); score -= 20 }
        else if (normalizedLoad > 50) { findings.add("CPU utilization elevated: ${"%.1f".format(normalizedLoad)}%"); score -= 10 }

        for ((name, pct) in topHogs) {
            findings.add("High CPU: $name (${"%.1f".format(pct)}%)")
            score -= 5
        }

        if (findings.isEmpty()) findings.add("CPU load appears normal")

        score = score.coerceIn(0, 100)
        val severity = when {
            score >= 70 -> Severity.OK
            score >= 40 -> Severity.WARNING
            else -> Severity.CRITICAL
        }

        return CpuDiagnosis(
            severity = severity,
            totalLoadPct = normalizedLoad,
            loadAvg1 = load1,
            isThrottling = false, // updated by thermal check
            maxTempC = 0f,
            topHogs = topHogs,
            findings = findings,
            score = score,
        )
    }
}
