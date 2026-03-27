package com.androidoctor.domain.usecase

import com.androidoctor.domain.model.DiagnosisResult
import com.androidoctor.domain.repository.DiagnosisRepository
import javax.inject.Inject

class DiagnoseUseCase @Inject constructor(
    private val repository: DiagnosisRepository,
) {
    suspend operator fun invoke(): DiagnosisResult {
        val device = repository.getDeviceInfo()
        val battery = repository.diagnoseBattery()
        val storage = repository.diagnoseStorage()
        val memory = repository.diagnoseMemory()
        val cpu = repository.diagnoseCpu()
        val bloatware = repository.diagnoseBloatware()
        val verdict = repository.computeVerdict(battery, storage, memory, cpu, bloatware)

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
}
