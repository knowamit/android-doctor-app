package com.androidoctor.data.parser

import android.content.Context
import android.os.SystemClock
import com.androidoctor.data.shell.ShellExecutor
import com.androidoctor.domain.model.AppBatteryUsage
import com.androidoctor.domain.model.BatteryAttribution
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

// Per-app battery attribution via `dumpsys batterystats --checkin`.
// Works on all API levels if Shizuku is available. Pre-Android 12 needs
// BATTERY_STATS signature permission granted through Shizuku.
//
// Checkin format reference: https://source.android.com/docs/core/power/batterystats
// Fields parsed:
//   <uid>,l,pkg,...,<packageName>,...   (package-to-uid mapping)
//   <uid>,l,pwi,<name>,<mah>,...        (per-uid power consumption)
@Singleton
class BatteryAttributionParser @Inject constructor(
    @ApplicationContext private val context: Context,
    private val shell: ShellExecutor,
) {
    suspend fun attribute(): BatteryAttribution = withContext(Dispatchers.IO) {
        parseCheckin() ?: unavailable()
    }

    private suspend fun parseCheckin(): BatteryAttribution? = runCatching {
        val result = shell.exec("dumpsys batterystats --checkin")
        if (result.exitCode != 0 || result.stdout.isBlank()) return@runCatching null

        val uidToPkg = mutableMapOf<Int, String>()
        val uidToPower = mutableMapOf<Int, Double>()

        // Real format: "9,<uid>,l,pwi,uid,<mah>,..." and "9,0,i,uid,<uid>,<package>".
        result.stdout.lineSequence().forEach { line ->
            val parts = line.split(",")
            if (parts.size < 6) return@forEach
            if (parts[2] == "i" && parts[3] == "uid") {
                val uid = parts[4].toIntOrNull() ?: return@forEach
                uidToPkg.putIfAbsent(uid, parts[5])
            } else if (parts[2] == "l" && parts[3] == "pwi" && parts[4] == "uid") {
                val uid = parts[1].toIntOrNull() ?: return@forEach
                val power = parts[5].toDoubleOrNull() ?: return@forEach
                uidToPower[uid] = (uidToPower[uid] ?: 0.0) + power
            }
        }

        if (uidToPower.isEmpty()) return@runCatching null

        val total = uidToPower.values.sum().coerceAtLeast(0.001)
        val pm = context.packageManager
        val apps = uidToPower.entries
            .sortedByDescending { it.value }
            .take(20)
            .mapNotNull { (uid, power) ->
                val pkg = uidToPkg[uid] ?: return@mapNotNull null
                val label = runCatching {
                    pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
                }.getOrDefault(pkg)
                AppBatteryUsage(
                    packageName = pkg,
                    appLabel = label,
                    consumedPowerMah = power,
                    percentOfTotal = (power / total) * 100.0,
                )
            }

        if (apps.isEmpty()) return@runCatching null

        BatteryAttribution(
            apps = apps,
            source = BatteryAttribution.Source.SHIZUKU_DUMPSYS,
            sinceMs = SystemClock.elapsedRealtime(),
            totalPowerMah = total,
        )
    }.getOrNull()

    private fun unavailable() = BatteryAttribution(
        apps = emptyList(),
        source = BatteryAttribution.Source.UNAVAILABLE,
        sinceMs = 0L,
        totalPowerMah = 0.0,
    )
}
