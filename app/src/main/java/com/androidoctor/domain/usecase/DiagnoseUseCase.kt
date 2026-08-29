package com.androidoctor.domain.usecase

import com.androidoctor.data.analytics.AnalyticsLogger
import com.androidoctor.data.local.dao.ScoreSnapshotDao
import com.androidoctor.data.local.entity.ScoreSnapshotEntity
import com.androidoctor.domain.model.DiagnosisResult
import com.androidoctor.domain.repository.DiagnosisRepository
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class DiagnoseUseCase @Inject constructor(
    private val repository: DiagnosisRepository,
    private val snapshotDao: ScoreSnapshotDao,
    private val analytics: AnalyticsLogger,
) {
    suspend operator fun invoke(): DiagnosisResult {
        analytics.diagnosisStarted()
        val device = repository.getDeviceInfo()
        val battery = repository.diagnoseBattery()
        val storage = repository.diagnoseStorage()
        val memory = repository.diagnoseMemory()
        val cpu = repository.diagnoseCpu()
        val bloatware = repository.diagnoseBloatware()
        val verdict = repository.computeVerdict(battery, storage, memory, cpu, bloatware)

        persistSnapshot(verdict.overallScore, battery.score, storage.score, memory.score, cpu.score, bloatware.score)
        analytics.diagnosisCompleted(verdict.overallScore)

        return DiagnosisResult(
            device = device,
            battery = battery,
            storage = storage,
            memory = memory,
            cpu = cpu,
            bloatware = bloatware,
            verdict = verdict,
        )
    }

    private suspend fun persistSnapshot(
        overall: Int, battery: Int, storage: Int, memory: Int, cpu: Int, bloatware: Int,
    ) {
        runCatching {
            snapshotDao.insert(
                ScoreSnapshotEntity(
                    timestamp = System.currentTimeMillis(),
                    overall = overall, battery = battery, storage = storage,
                    memory = memory, cpu = cpu, bloatware = bloatware,
                )
            )
            val cutoff = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(90)
            snapshotDao.pruneBefore(cutoff)
            val count = snapshotDao.count()
            if (count > 500) snapshotDao.deleteOldest(count - 500)
        }
    }
}
