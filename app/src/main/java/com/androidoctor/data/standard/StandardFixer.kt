package com.androidoctor.data.standard

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.os.Build
import com.androidoctor.data.shell.FixLog
import com.androidoctor.data.shell.LineKind
import com.androidoctor.data.shell.StepState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

data class StandardFixResult(
    val memBeforeBytes: Long,
    val memAfterBytes: Long,
    val appsSignalled: Int,
    val cacheFreedBytes: Long,
)

/** Android 14 and newer ignore killBackgroundProcesses for other apps, so the step is skipped there. */
fun quickFixIncludesKillStep(sdkInt: Int): Boolean = sdkInt < Build.VERSION_CODES.UPSIDE_DOWN_CAKE

/**
 * The fix that needs no Shizuku, no developer options and no USB debugging. It only uses
 * public Android APIs (the same ones a normal app is allowed to call), so it also works on
 * phones where banking apps refuse to run with developer options on.
 *
 * Nothing here can break an app: killBackgroundProcesses only ends processes Android
 * already considers cached/background, and the cache we clear is this app's own.
 */
class StandardFixer @Inject constructor(
    @ApplicationContext private val context: Context,
    private val log: FixLog,
) {
    suspend fun run(): StandardFixResult = withContext(Dispatchers.IO) {
        log.run(
            "Quick fix",
            listOf("Measure free memory", "Ask apps to release memory", "Clear temporary files", "Measure again"),
        ) {
            val am = context.getSystemService(ActivityManager::class.java)

            log.start(0)
            val before = availMem(am)
            log.line(LineKind.CMD, "ActivityManager.getMemoryInfo()")
            log.line(LineKind.OUT, "  available: ${before / MB} MB")
            log.finish(0, summary = "${before / MB} MB free")

            log.start(1)
            var signalled = 0
            if (quickFixIncludesKillStep(Build.VERSION.SDK_INT)) {
                val targets = launchablePackages()
                for (pkg in targets) {
                    log.line(LineKind.CMD, "killBackgroundProcesses($pkg)")
                    runCatching { am.killBackgroundProcesses(pkg) }
                        .onFailure { log.line(LineKind.ERR, "  ${it.javaClass.simpleName}") }
                    signalled++
                    delay(25) // lets the log breathe so the user can follow along
                }
                log.finish(1, summary = "Asked $signalled apps to release memory")
            } else {
                log.line(LineKind.NOTE, "Android 14 and newer do not let apps stop other apps")
                log.finish(1, StepState.SKIPPED, "Skipped on Android 14 and newer")
            }

            log.start(2)
            var cacheFreed = 0L
            val dirs = listOfNotNull(context.cacheDir, context.externalCacheDir)
            for (dir in dirs) {
                log.line(LineKind.CMD, "clear ${dir.absolutePath}")
                val freed = clearDir(dir)
                cacheFreed += freed
                log.line(LineKind.OUT, "  removed ${freed / 1024} KB")
            }
            log.finish(2, summary = "${cacheFreed / 1024} KB removed")

            log.start(3)
            delay(1200) // processes die asynchronously, give the system a moment before measuring
            val after = availMem(am)
            log.line(LineKind.CMD, "ActivityManager.getMemoryInfo()")
            log.line(LineKind.OUT, "  available: ${after / MB} MB")
            log.line(LineKind.NOTE, "Free memory: ${before / MB} MB before, ${after / MB} MB after (measured)")
            log.finish(3, summary = "${after / MB} MB free now")

            StandardFixResult(memBeforeBytes = before, memAfterBytes = after, appsSignalled = signalled, cacheFreedBytes = cacheFreed)
        }
    }

    private fun availMem(am: ActivityManager): Long =
        ActivityManager.MemoryInfo().also { am.getMemoryInfo(it) }.availMem

    /** Apps with a launcher icon, minus this app and the home screen. */
    private fun launchablePackages(): List<String> {
        val pm = context.packageManager
        val home = pm.resolveActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), 0)
            ?.activityInfo?.packageName
        return pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0)
            .map { it.activityInfo.packageName }
            .distinct()
            .filter { it != context.packageName && it != home }
    }

    private fun clearDir(dir: File): Long {
        var total = 0L
        dir.listFiles()?.forEach { f ->
            total += f.walkTopDown().filter { it.isFile }.sumOf { it.length() }
            f.deleteRecursively()
        }
        return total
    }

    private companion object {
        const val MB = 1024L * 1024L
    }
}
