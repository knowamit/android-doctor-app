package com.androidoctor.data.repository

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import com.androidoctor.data.parser.*
import com.androidoctor.data.shell.ShellExecutor
import com.androidoctor.domain.model.*
import com.androidoctor.domain.repository.DiagnosisRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DiagnosisRepositoryImpl @Inject constructor(
    private val shell: ShellExecutor,
    @ApplicationContext private val context: Context,
) : DiagnosisRepository {

    // No shell/Shizuku required — public Android APIs only. Runs instantly on
    // first app open, before Shizuku setup, so users see a real score before
    // hitting the privileged-access wall.
    override suspend fun quickScan(): QuickScanResult {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        am.getMemoryInfo(memInfo)
        val ramUsedPct = if (memInfo.totalMem > 0) {
            (((memInfo.totalMem - memInfo.availMem) * 100) / memInfo.totalMem).toInt()
        } else 0

        val statFs = StatFs(Environment.getDataDirectory().path)
        val totalBytes = statFs.totalBytes
        val availBytes = statFs.availableBytes
        val storageUsedPct = if (totalBytes > 0) {
            (((totalBytes - availBytes) * 100) / totalBytes).toInt()
        } else 0

        val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val batteryPct = if (level >= 0 && scale > 0) (level * 100) / scale else -1
        val tempTenths = batteryIntent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1) ?: -1
        val batteryTempC = if (tempTenths >= 0) tempTenths / 10f else -1f

        val ramScore = (100 - ramUsedPct).coerceIn(0, 100)
        val storageScore = (100 - storageUsedPct).coerceIn(0, 100)
        val thermalScore = if (batteryTempC in 0f..38f) 100 else if (batteryTempC < 0f) 100 else 40
        val score = ((ramScore * 0.45f) + (storageScore * 0.45f) + (thermalScore * 0.10f)).toInt().coerceIn(0, 100)

        val findings = buildList {
            if (ramUsedPct >= 85) add("RAM usage critical at $ramUsedPct%")
            else if (ramUsedPct >= 70) add("RAM usage elevated at $ramUsedPct%")
            if (storageUsedPct >= 90) add("Storage critically full at $storageUsedPct%")
            else if (storageUsedPct >= 80) add("Storage getting full at $storageUsedPct%")
            if (batteryTempC >= 40f) add("Battery running hot at ${batteryTempC}°C")
            if (isEmpty()) add("No major issues found in quick scan")
        }

        val severity = when {
            score < 40 -> Severity.CRITICAL
            score < 70 -> Severity.WARNING
            else -> Severity.OK
        }

        return QuickScanResult(
            score = score,
            severity = severity,
            ramUsedPct = ramUsedPct,
            storageUsedPct = storageUsedPct,
            batteryPct = batteryPct,
            batteryTemperatureC = batteryTempC,
            findings = findings,
        )
    }

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
