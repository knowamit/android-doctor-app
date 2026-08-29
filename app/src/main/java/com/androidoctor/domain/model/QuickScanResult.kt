package com.androidoctor.domain.model

data class QuickScanResult(
    val score: Int,
    val severity: Severity,
    val ramUsedPct: Int,
    val storageUsedPct: Int,
    val batteryPct: Int,
    val batteryTemperatureC: Float,
    val findings: List<String>,
)
