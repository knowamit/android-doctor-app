package com.androidoctor.domain.usecase

import com.androidoctor.data.shell.FixLog
import com.androidoctor.data.shell.LineKind
import com.androidoctor.data.shell.ShellExecutor
import com.androidoctor.data.shell.StepState
import com.androidoctor.data.standard.StandardFixer
import com.androidoctor.domain.repository.FixRepository
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton

enum class OptimizeMode { DEEP, STANDARD }

/** Everything here is measured before/after, nothing is estimated. */
data class OptimizeResult(
    val mode: OptimizeMode,
    val storageFreedBytes: Long,
    val memBeforeBytes: Long,
    val memAfterBytes: Long,
    val processesBefore: Int,
    val processesStopped: Int,
    val settingsChanged: Int,
    val appsSignalled: Int,
)

/** Holds the last result so the result screen needs no navigation arguments. */
@Singleton
class OptimizeResultHolder @Inject constructor() {
    private val ref = AtomicReference<OptimizeResult?>(null)
    var last: OptimizeResult?
        get() = ref.get()
        set(value) = ref.set(value)
}

/**
 * "Optimize Now". With Shizuku it runs the deep, reversible set of actions (all recorded in
 * Fix History). Without Shizuku it falls back to [StandardFixer], which needs no setup.
 */
class OptimizeUseCase @Inject constructor(
    private val repository: FixRepository,
    private val shell: ShellExecutor,
    private val standard: StandardFixer,
    private val log: FixLog,
    private val holder: OptimizeResultHolder,
) {
    suspend operator fun invoke(): OptimizeResult {
        val result = if (shell.isShizukuAvailable) deep() else quick()
        holder.last = result
        return result
    }

    private suspend fun quick(): OptimizeResult {
        val r = standard.run()
        return OptimizeResult(
            mode = OptimizeMode.STANDARD,
            storageFreedBytes = r.cacheFreedBytes,
            memBeforeBytes = r.memBeforeBytes,
            memAfterBytes = r.memAfterBytes,
            processesBefore = 0,
            processesStopped = 0,
            settingsChanged = 0,
            appsSignalled = r.appsSignalled,
        )
    }

    private suspend fun deep(): OptimizeResult = log.run(
        "Optimize Now",
        listOf("Measure memory and processes", "Trim app caches", "Stop background apps", "Tune animation speed", "Measure again"),
    ) {
        repository.beginSession("Optimize Now")
        try {
            log.start(0)
            val ramBefore = repository.availableRamBytes()
            val procsBefore = repository.runningProcessCount()
            log.finish(0, summary = "${ramBefore / MB} MB free, $procsBefore processes")

            log.start(1)
            val storageFreed = repository.trimCaches()
            log.finish(1, summary = "${storageFreed / MB} MB freed")

            log.start(2)
            repository.killAllApps()
            log.finish(2)

            log.start(3)
            val tuned = repository.setAnimationScale(0.5f)
            val settings = if (tuned) 3 else 0
            log.finish(3, if (tuned) StepState.DONE else StepState.FAILED, if (tuned) "3 settings changed" else "Could not change settings")

            log.start(4)
            val ramAfter = repository.availableRamBytes()
            val procsAfter = repository.runningProcessCount()
            log.line(LineKind.NOTE, "Free memory: ${ramBefore / MB} MB before, ${ramAfter / MB} MB after (measured)")
            log.finish(4, summary = "${ramAfter / MB} MB free now")

            OptimizeResult(
                mode = OptimizeMode.DEEP,
                storageFreedBytes = storageFreed,
                memBeforeBytes = ramBefore,
                memAfterBytes = ramAfter,
                processesBefore = procsBefore,
                processesStopped = (procsBefore - procsAfter).coerceAtLeast(0),
                settingsChanged = settings,
                appsSignalled = 0,
            )
        } finally {
            // Must still run when the caller was cancelled, or the session id stays set.
            kotlinx.coroutines.withContext(kotlinx.coroutines.NonCancellable) { repository.endSession() }
        }
    }

    private companion object { const val MB = 1024L * 1024L }
}
