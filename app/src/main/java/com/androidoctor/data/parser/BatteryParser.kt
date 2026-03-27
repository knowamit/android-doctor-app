package com.androidoctor.data.parser

import com.androidoctor.domain.model.BatteryDiagnosis
import com.androidoctor.domain.model.Severity

object BatteryParser {

    data class RawBattery(
        val level: Int = 0,
        val temperatureC: Float = 0f,
        val voltageMv: Int = 0,
        val health: String = "unknown",
        val status: String = "unknown",
        val cycleCount: Int = -1,
        val chargeFull: Int = -1,
        val chargeFullDesign: Int = -1,
        val healthPct: Float = -1f,
    )

    fun parseDumpsys(output: String): Map<String, String> {
        val props = mutableMapOf<String, String>()
        for (line in output.lines()) {
            val trimmed = line.trim()
            val idx = trimmed.indexOf(':')
            if (idx > 0) {
                props[trimmed.substring(0, idx).trim().lowercase()] = trimmed.substring(idx + 1).trim()
            }
        }
        return props
    }

    fun parseSysfs(files: Map<String, String>): Map<String, String> = files

    fun buildRawBattery(dumpsys: Map<String, String>, sysfs: Map<String, String>): RawBattery {
        val level = dumpsys["level"]?.toIntOrNull() ?: 0
        val rawTemp = dumpsys["temperature"]?.toIntOrNull() ?: 0
        val tempC = rawTemp / 10f
        val voltageMv = dumpsys["voltage"]?.toIntOrNull() ?: 0

        val healthCode = dumpsys["health"] ?: "unknown"
        val healthMap = mapOf("2" to "good", "3" to "overheat", "4" to "dead", "5" to "over-voltage", "6" to "failure", "7" to "cold")
        val health = healthMap[healthCode] ?: healthCode

        val cycleCount = sysfs["cycle_count"]?.toIntOrNull() ?: -1
        val chargeFull = sysfs["charge_full"]?.toIntOrNull() ?: -1
        val chargeFullDesign = sysfs["charge_full_design"]?.toIntOrNull() ?: -1
        val healthPct = if (chargeFull > 0 && chargeFullDesign > 0) {
            (chargeFull.toFloat() / chargeFullDesign * 100f)
        } else -1f

        return RawBattery(
            level = level,
            temperatureC = tempC,
            voltageMv = voltageMv,
            health = health,
            cycleCount = cycleCount,
            chargeFull = chargeFull,
            chargeFullDesign = chargeFullDesign,
            healthPct = healthPct,
        )
    }

    fun diagnose(raw: RawBattery, maxThermalTempC: Float = 0f): BatteryDiagnosis {
        val findings = mutableListOf<String>()
        var score = 100

        // Capacity
        if (raw.healthPct > 0) {
            when {
                raw.healthPct < 70 -> { findings.add("Battery capacity severely degraded: ${raw.healthPct.format(1)}%"); score -= 40 }
                raw.healthPct < 80 -> { findings.add("Battery capacity degraded: ${raw.healthPct.format(1)}%"); score -= 25 }
                raw.healthPct < 90 -> { findings.add("Battery showing wear: ${raw.healthPct.format(1)}%"); score -= 10 }
                else -> findings.add("Battery capacity healthy: ${raw.healthPct.format(1)}%")
            }
        } else {
            findings.add("Battery capacity data unavailable")
        }

        // Cycles
        when {
            raw.cycleCount > 800 -> { findings.add("Very high cycle count: ${raw.cycleCount}"); score -= 20 }
            raw.cycleCount > 500 -> { findings.add("High cycle count: ${raw.cycleCount}"); score -= 10 }
            raw.cycleCount > 300 -> { findings.add("Moderate cycle count: ${raw.cycleCount}"); score -= 5 }
            raw.cycleCount >= 0 -> findings.add("Low cycle count: ${raw.cycleCount}")
        }

        // Temperature
        var isThrottling = false
        when {
            raw.temperatureC > 45 -> { findings.add("Battery temperature CRITICAL: ${raw.temperatureC.format(1)}C"); isThrottling = true; score -= 25 }
            raw.temperatureC > 40 -> { findings.add("Battery temperature elevated: ${raw.temperatureC.format(1)}C"); isThrottling = true; score -= 15 }
            raw.temperatureC > 35 -> { findings.add("Battery temperature warm: ${raw.temperatureC.format(1)}C"); score -= 5 }
            raw.temperatureC > 0 -> findings.add("Battery temperature normal: ${raw.temperatureC.format(1)}C")
        }

        // Health status
        if (raw.health !in listOf("good", "unknown")) {
            if (raw.health == "failure" && raw.healthPct > 85) {
                findings.add("System reports health: ${raw.health} (likely driver quirk — capacity is ${raw.healthPct.format(1)}%)")
                score -= 5
            } else {
                findings.add("System reports battery health: ${raw.health}")
                score -= 20
            }
        }

        score = score.coerceIn(0, 100)
        val severity = when {
            score >= 70 -> Severity.OK
            score >= 40 -> Severity.WARNING
            else -> Severity.CRITICAL
        }

        return BatteryDiagnosis(
            severity = severity,
            healthPct = raw.healthPct,
            cycleCount = raw.cycleCount,
            temperatureC = raw.temperatureC,
            isThrottling = isThrottling,
            findings = findings,
            score = score,
        )
    }

    private fun Float.format(decimals: Int) = "%.${decimals}f".format(this)
}
