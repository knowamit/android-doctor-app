package com.androidoctor.data.storage

import android.app.AppOpsManager
import android.app.usage.StorageStatsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Environment
import android.os.Process
import android.os.StatFs
import android.os.storage.StorageManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

data class AppStorage(
    val packageName: String,
    val label: String,
    val cacheBytes: Long,
    val totalBytes: Long,
    val daysSinceUsed: Int?, // null when unknown
)

/**
 * Label for the unused-apps list, or null when the app should not be listed.
 * With usage access granted, "no usage record in the last year" means not opened in a year.
 */
fun unusedLabel(daysSinceUsed: Int?, usageGranted: Boolean): String? = when {
    !usageGranted -> null
    daysSinceUsed == null -> "Not opened in the last year"
    daysSinceUsed >= UNUSED_AFTER_DAYS -> "Last opened $daysSinceUsed days ago"
    else -> null
}

const val UNUSED_AFTER_DAYS = 30

data class StorageScan(
    val totalBytes: Long,
    val freeBytes: Long,
    val cacheBytes: Long,
    val topCacheApps: List<AppStorage>,
    val unusedApps: List<AppStorage>,
    val usageAccessGranted: Boolean,
)

@Singleton
class StorageScanner @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun hasUsageAccess(): Boolean {
        val ops = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = ops.unsafeCheckOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName,
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    suspend fun scan(): StorageScan = withContext(Dispatchers.IO) {
        val stat = StatFs(Environment.getDataDirectory().path)
        val granted = hasUsageAccess()
        if (!granted) {
            return@withContext StorageScan(
                stat.totalBytes, stat.availableBytes, 0, emptyList(), emptyList(), false,
            )
        }
        val pm = context.packageManager
        val stats = context.getSystemService(Context.STORAGE_STATS_SERVICE) as StorageStatsManager
        val usage = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val now = System.currentTimeMillis()
        val lastUsed = usage.queryUsageStats(UsageStatsManager.INTERVAL_YEARLY, now - YEAR_MS, now)
            .associate { it.packageName to it.lastTimeUsed }

        // System apps are filtered first so no storage query is wasted on them.
        val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            .filter { it.flags and (ApplicationInfo.FLAG_SYSTEM or ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) == 0 }
            .mapNotNull { info ->
                try {
                    val s = stats.queryStatsForPackage(StorageManager.UUID_DEFAULT, info.packageName, Process.myUserHandle())
                    val last = lastUsed[info.packageName]?.takeIf { it > 0 }
                    AppStorage(
                        packageName = info.packageName,
                        label = pm.getApplicationLabel(info).toString(),
                        cacheBytes = s.cacheBytes,
                        totalBytes = s.appBytes + s.dataBytes,
                        daysSinceUsed = last?.let { ((now - it) / DAY_MS).toInt() },
                    )
                } catch (e: Exception) {
                    null
                }
            }
        StorageScan(
            totalBytes = stat.totalBytes,
            freeBytes = stat.availableBytes,
            cacheBytes = apps.sumOf { it.cacheBytes },
            topCacheApps = apps.filter { it.cacheBytes > 1_000_000 }.sortedByDescending { it.cacheBytes }.take(8),
            unusedApps = apps.filter { unusedLabel(it.daysSinceUsed, true) != null && it.packageName != context.packageName }
                // Apps with no record in a year come first, then the largest.
                .sortedWith(compareByDescending<AppStorage> { it.daysSinceUsed == null }.thenByDescending { it.totalBytes })
                .take(8),
            usageAccessGranted = true,
        )
    }

    private companion object {
        const val DAY_MS = 86_400_000L
        const val YEAR_MS = 365 * DAY_MS
    }
}
