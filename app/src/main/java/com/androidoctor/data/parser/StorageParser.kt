package com.androidoctor.data.parser

import com.androidoctor.domain.model.Severity
import com.androidoctor.domain.model.StorageDiagnosis
import com.androidoctor.domain.model.StorageType

object StorageParser {

    fun detectStorageType(blockDevices: String): StorageType {
        val devs = blockDevices.trim().split("\\s+".toRegex())
        val hasSda = devs.any { it.startsWith("sd") }
        val hasMmcblk = devs.any { it.startsWith("mmcblk") }
        return when {
            hasSda && !hasMmcblk -> StorageType.UFS
            hasMmcblk && !hasSda -> StorageType.EMMC
            else -> StorageType.UNKNOWN
        }
    }

    fun parseEmmcHealth(lifeTime: String?, preEol: String?): Pair<Float, String> {
        var lifeUsedPct = -1f
        var eolStatus = "unknown"

        if (!lifeTime.isNullOrBlank()) {
            val parts = lifeTime.trim().split("\\s+".toRegex())
            if (parts.isNotEmpty()) {
                val hex = parts[0].removePrefix("0x").toIntOrNull(16)
                if (hex != null) lifeUsedPct = hex * 10f
            }
        }

        if (!preEol.isNullOrBlank()) {
            val raw = preEol.trim().removePrefix("0x")
            val eolVal = raw.toIntOrNull(16) ?: raw.toIntOrNull()
            eolStatus = when (eolVal) {
                1 -> "normal"
                2 -> "warning"
                3 -> "urgent"
                else -> "unknown"
            }
        }

        return lifeUsedPct to eolStatus
    }

    fun parseDf(output: String): Triple<Float, Float, Float> {
        // Returns (totalGb, availableGb, usedPct) for the data partition
        for (line in output.lines()) {
            val parts = line.trim().split("\\s+".toRegex())
            if (parts.size < 4) continue
            if (parts[0] == "Filesystem") continue
            val mount = parts.last()
            if (mount in listOf("/data", "/data/media", "/storage/emulated") || parts[0].contains("data")) {
                val nums = parts.drop(1).mapNotNull { it.replace("%", "").toLongOrNull() }
                if (nums.size >= 3) {
                    val totalMb = nums[0] / 1024
                    val usedMb = nums[1] / 1024
                    val availMb = nums[2] / 1024
                    val usedPct = if (totalMb > 0) usedMb.toFloat() / totalMb * 100 else 0f
                    return Triple(totalMb / 1024f, availMb / 1024f, usedPct)
                }
            }
        }
        return Triple(0f, 0f, 0f)
    }

    fun diagnose(
        storageType: StorageType,
        lifeUsedPct: Float,
        preEol: String,
        totalGb: Float,
        availableGb: Float,
        spaceUsedPct: Float,
    ): StorageDiagnosis {
        val findings = mutableListOf<String>()
        var score = 100

        when (storageType) {
            StorageType.EMMC -> { findings.add("Storage type: eMMC (slower, degrades faster than UFS)"); score -= 10 }
            StorageType.UFS -> findings.add("Storage type: UFS (fast, good longevity)")
            StorageType.UNKNOWN -> findings.add("Storage type: could not detect")
        }

        val lifeRemaining = if (lifeUsedPct >= 0) 100 - lifeUsedPct else -1f
        when {
            lifeUsedPct >= 80 -> { findings.add("NAND flash wear CRITICAL: ${lifeUsedPct.toInt()}% consumed"); score -= 40 }
            lifeUsedPct >= 50 -> { findings.add("NAND flash showing wear: ${lifeUsedPct.toInt()}% consumed"); score -= 20 }
            lifeUsedPct >= 30 -> { findings.add("NAND flash moderate wear: ${lifeUsedPct.toInt()}% consumed"); score -= 10 }
            lifeUsedPct >= 0 -> findings.add("NAND flash healthy: only ${lifeUsedPct.toInt()}% consumed")
            else -> findings.add("NAND wear data unavailable (may need Shizuku)")
        }

        when (preEol) {
            "urgent" -> { findings.add("Storage pre-EOL: URGENT"); score -= 30 }
            "warning" -> { findings.add("Storage pre-EOL: WARNING"); score -= 15 }
            "normal" -> findings.add("Storage pre-EOL: normal")
        }

        when {
            spaceUsedPct > 95 -> { findings.add("Storage almost full: ${"%.0f".format(spaceUsedPct)}% (${"%.1f".format(availableGb)} GB free)"); score -= 25 }
            spaceUsedPct > 85 -> { findings.add("Storage getting full: ${"%.0f".format(spaceUsedPct)}% (${"%.1f".format(availableGb)} GB free)"); score -= 15 }
            spaceUsedPct > 70 -> { findings.add("Storage moderate: ${"%.0f".format(spaceUsedPct)}% (${"%.1f".format(availableGb)} GB free)"); score -= 5 }
            spaceUsedPct > 0 -> findings.add("Storage space healthy: ${"%.0f".format(spaceUsedPct)}% (${"%.1f".format(availableGb)} GB free)")
        }

        score = score.coerceIn(0, 100)
        val severity = when {
            score >= 70 -> Severity.OK
            score >= 40 -> Severity.WARNING
            else -> Severity.CRITICAL
        }

        return StorageDiagnosis(
            severity = severity,
            storageType = storageType,
            lifeRemainingPct = lifeRemaining,
            spaceUsedPct = spaceUsedPct,
            totalGb = totalGb,
            availableGb = availableGb,
            findings = findings,
            score = score,
        )
    }
}
