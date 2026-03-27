package com.androidoctor.domain.model

data class DiagnosisResult(
    val device: DeviceInfo,
    val battery: BatteryDiagnosis,
    val storage: StorageDiagnosis,
    val memory: MemoryDiagnosis,
    val cpu: CpuDiagnosis,
    val bloatware: BloatwareDiagnosis,
    val verdict: Verdict,
)

data class BatteryDiagnosis(
    val severity: Severity,
    val healthPct: Float,
    val cycleCount: Int,
    val temperatureC: Float,
    val isThrottling: Boolean,
    val findings: List<String>,
    val score: Int,
)

data class StorageDiagnosis(
    val severity: Severity,
    val storageType: StorageType,
    val lifeRemainingPct: Float,
    val spaceUsedPct: Float,
    val totalGb: Float,
    val availableGb: Float,
    val findings: List<String>,
    val score: Int,
)

data class MemoryDiagnosis(
    val severity: Severity,
    val totalMb: Int,
    val availableMb: Int,
    val usedPct: Float,
    val swapUsedMb: Int,
    val findings: List<String>,
    val score: Int,
)

data class CpuDiagnosis(
    val severity: Severity,
    val totalLoadPct: Float,
    val loadAvg1: Float,
    val isThrottling: Boolean,
    val maxTempC: Float,
    val topHogs: List<Pair<String, Float>>,
    val findings: List<String>,
    val score: Int,
)

data class BloatwareEntry(
    val packageName: String,
    val name: String,
    val category: String,
    val impact: Impact,
    val description: String,
)

data class BloatwareDiagnosis(
    val severity: Severity,
    val totalSystemPackages: Int,
    val bloatwareFound: Int,
    val highImpactCount: Int,
    val removable: List<BloatwareEntry>,
    val alreadyDisabled: List<String>,
    val findings: List<String>,
    val score: Int,
)

data class Verdict(
    val overallScore: Int,
    val overallSeverity: Severity,
    val hardwarePct: Int,
    val softwarePct: Int,
    val thermalPct: Int,
    val topIssues: List<String>,
    val recommendation: String,
)

enum class Severity { OK, WARNING, CRITICAL }

enum class Impact { HIGH, MEDIUM, LOW }
