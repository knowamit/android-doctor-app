package com.androidoctor.domain.model

data class DeviceInfo(
    val model: String,
    val brand: String,
    val manufacturer: String,
    val androidVersion: String,
    val sdkVersion: Int,
    val chipset: String,
    val totalRamMb: Int,
    val storageType: StorageType,
    val buildDisplay: String,
)

enum class StorageType { EMMC, UFS, UNKNOWN }
