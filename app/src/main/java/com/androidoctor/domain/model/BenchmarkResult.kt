package com.androidoctor.domain.model

data class BenchmarkResult(
    val timestamp: Long,
    val label: String,
    val ramAvailableMb: Int,
    val ramUsedPct: Float,
    val swapUsedMb: Int,
    val cpuLoad1: Float,
    val seqReadMbps: Float,
    val seqWriteMbps: Float,
    val randReadIops: Float,
    val randWriteIops: Float,
    val processCount: Int,
    val appLaunches: List<AppLaunchResult>,
)

data class AppLaunchResult(
    val packageName: String,
    val appName: String,
    val totalTimeMs: Int,
    val status: LaunchStatus,
)

enum class LaunchStatus { OK, NOT_INSTALLED, ERROR }
