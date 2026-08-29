package com.androidoctor.domain.usecase

import com.androidoctor.data.analytics.AnalyticsLogger
import com.androidoctor.domain.model.QuickScanResult
import com.androidoctor.domain.repository.DiagnosisRepository
import javax.inject.Inject

class QuickScanUseCase @Inject constructor(
    private val repository: DiagnosisRepository,
    private val analytics: AnalyticsLogger,
) {
    suspend operator fun invoke(): QuickScanResult {
        val result = repository.quickScan()
        analytics.quickScanCompleted(result.score)
        return result
    }
}
