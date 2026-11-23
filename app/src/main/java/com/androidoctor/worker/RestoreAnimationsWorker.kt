package com.androidoctor.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.androidoctor.data.shell.ShellExecutor
import com.androidoctor.domain.repository.FixRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

/**
 * Puts animation speed back after Panic Boost turned it off. Rolls back the Panic Boost
 * session only, so changes from other fixes are untouched.
 */
@HiltWorker
class RestoreAnimationsWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val repository: FixRepository,
    private val shell: ShellExecutor,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val sessionId = inputData.getLong(KEY_SESSION_ID, 0L)
        if (sessionId == 0L) return Result.success()
        if (!shell.isShizukuAvailable) {
            // Shizuku not running right now: retry a few times, then leave the change in Fix History.
            return if (runAttemptCount < MAX_ATTEMPTS) Result.retry() else Result.success()
        }
        return runCatching {
            repository.rollbackSession(sessionId) // returns 0 when the user already undid it
            Result.success()
        }.getOrElse { if (runAttemptCount < MAX_ATTEMPTS) Result.retry() else Result.success() }
    }

    companion object {
        const val UNIQUE_NAME = "panic_restore"
        const val KEY_SESSION_ID = "sessionId"
        const val RESTORE_DELAY_MINUTES = 5L
        private const val MAX_ATTEMPTS = 3

        fun schedule(context: Context, sessionId: Long, delayMinutes: Long = RESTORE_DELAY_MINUTES) {
            val request = OneTimeWorkRequestBuilder<RestoreAnimationsWorker>()
                .setInitialDelay(delayMinutes, TimeUnit.MINUTES)
                .setInputData(workDataOf(KEY_SESSION_ID to sessionId))
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(UNIQUE_NAME, ExistingWorkPolicy.REPLACE, request)
        }
    }
}
