package com.androidoctor.data.repository

import android.os.Build
import com.androidoctor.data.parser.*
import com.androidoctor.data.shell.ShellExecutor
import com.androidoctor.domain.model.*
import com.androidoctor.domain.repository.DiagnosisRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DiagnosisRepositoryImpl @Inject constructor(
    private val shell: ShellExecutor,
) : DiagnosisRepository {

    override suspend fun getDeviceInfo(): DeviceInfo {
        val blockDevs = shell.exec("ls /sys/block/").output
        val storageType = StorageParser.detectStorageType(blockDevs)

        val meminfo = shell.exec("cat /proc/meminfo").output
        val rawMem = MemoryParser.parseProcMeminfo(meminfo)

        return DeviceInfo(
            model = Build.MODEL,
            brand = Build.BRAND,
            manufacturer = Build.MANUFACTURER,
            androidVersion = Build.VERSION.RELEASE,
            sdkVersion = Build.VERSION.SDK_INT,
            chipset = Build.HARDWARE,
            totalRamMb = rawMem.totalMb,
            storageType = storageType,
            buildDisplay = Build.DISPLAY,
        )
    }

    override suspend fun diagnoseBattery(): BatteryDiagnosis {
        val dumpsysRaw = shell.exec("dumpsys battery").output
        val dumpsys = BatteryParser.parseDumpsys(dumpsysRaw)

        val sysfsFiles = mutableMapOf<String, String>()
        for (file in listOf("charge_full", "charge_full_design", "cycle_count", "temp", "health", "status")) {
            val result = shell.exec("cat /sys/class/power_supply/battery/$file 2>/dev/null")
            if (result.isSuccess && result.output.isNotBlank() && "Permission denied" !in result.output) {
                sysfsFiles[file] = result.output
            }
        }

        val raw = BatteryParser.buildRawBattery(dumpsys, sysfsFiles)
        return BatteryParser.diagnose(raw)
    }

    override suspend fun diagnoseStorage(): StorageDiagnosis {
        val blockDevs = shell.exec("ls /sys/block/").output
        val storageType = StorageParser.detectStorageType(blockDevs)

        // Try to read health
        var lifeUsedPct = -1f
        var preEol = "unknown"
        if (storageType == StorageType.EMMC) {
            val lt = shell.exec("cat /sys/class/mmc_host/mmc0/mmc0:*/life_time 2>/dev/null")
            val eol = shell.exec("cat /sys/class/mmc_host/mmc0/mmc0:*/pre_eol_info 2>/dev/null")
            val (pct, status) = StorageParser.parseEmmcHealth(
                lt.output.takeIf { lt.isSuccess },
                eol.output.takeIf { eol.isSuccess },
            )
            lifeUsedPct = pct
            preEol = status
        }

        val dfRaw = shell.exec("df").output
        val (totalGb, availableGb, usedPct) = StorageParser.parseDf(dfRaw)

        return StorageParser.diagnose(storageType, lifeUsedPct, preEol, totalGb, availableGb, usedPct)
    }

    override suspend fun diagnoseMemory(): MemoryDiagnosis {
        val meminfo = shell.exec("cat /proc/meminfo").output
        val raw = MemoryParser.parseProcMeminfo(meminfo)
        return MemoryParser.diagnose(raw)
    }

    override suspend fun diagnoseCpu(): CpuDiagnosis {
        val cpuinfo = shell.exec("dumpsys cpuinfo").output
        val loadavg = shell.exec("cat /proc/loadavg").output
        return CpuParser.parseDumpsysCpuinfo(cpuinfo, loadavg)
    }

    override suspend fun diagnoseBloatware(): BloatwareDiagnosis {
        val systemPkgs = shell.exec("pm list packages -s").output
            .lines()
            .filter { it.startsWith("package:") }
            .map { it.removePrefix("package:").trim() }

        val disabledPkgs = shell.exec("pm list packages -d").output
            .lines()
            .filter { it.startsWith("package:") }
            .map { it.removePrefix("package:").trim() }

        return BloatwareAnalyzer.diagnose(systemPkgs, disabledPkgs, Build.BRAND)
    }

    override suspend fun computeVerdict(
        battery: BatteryDiagnosis,
        storage: StorageDiagnosis,
        memory: MemoryDiagnosis,
        cpu: CpuDiagnosis,
        bloatware: BloatwareDiagnosis,
    ): Verdict = VerdictEngine.compute(battery, storage, memory, cpu, bloatware)
}
