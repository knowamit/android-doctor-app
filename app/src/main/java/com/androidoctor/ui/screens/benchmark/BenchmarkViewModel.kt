package com.androidoctor.ui.screens.benchmark

import android.content.pm.PackageManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.androidoctor.data.parser.MemoryParser
import com.androidoctor.data.shell.ShellExecutor
import com.androidoctor.domain.model.AppLaunchResult
import com.androidoctor.domain.model.BenchmarkResult
import com.androidoctor.domain.model.LaunchStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import android.content.Context
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BenchmarkUiState(
    val isRunning: Boolean = false,
    val step: String = "",
    val result: BenchmarkResult? = null,
)

private val BENCHMARK_APPS = listOf(
    "com.google.android.gm" to "com.google.android.gm/.ConversationListActivityGmail",
    "com.google.android.apps.photos" to "com.google.android.apps.photos/.home.HomeActivity",
    "com.android.settings" to "com.android.settings/.Settings",
    "com.google.android.youtube" to "com.google.android.youtube/.HomeActivity",
    "com.whatsapp" to "com.whatsapp/.Main",
    "com.whatsapp.w4b" to "com.whatsapp.w4b/.Main",
    "com.android.chrome" to "com.android.chrome/com.google.android.apps.chrome.Main",
    "com.google.android.dialer" to "com.google.android.dialer/.extensions.GoogleDialtactsActivity",
)

@HiltViewModel
class BenchmarkViewModel @Inject constructor(
    private val shell: ShellExecutor,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _state = MutableStateFlow(BenchmarkUiState())
    val state: StateFlow<BenchmarkUiState> = _state.asStateFlow()

    fun runBenchmark() {
        viewModelScope.launch {
            _state.value = BenchmarkUiState(isRunning = true, step = "Measuring RAM & CPU...")

            // RAM
            val memRaw = shell.exec("cat /proc/meminfo").output
            val mem = MemoryParser.parseProcMeminfo(memRaw)
            val loadavg = shell.exec("cat /proc/loadavg").output
            val load1 = loadavg.split(" ").firstOrNull()?.toFloatOrNull() ?: 0f
            val swapUsed = mem.swapTotalMb - mem.swapFreeMb

            // Process count
            _state.value = _state.value.copy(step = "Counting processes...")
            val procCount = shell.exec("ps -e | wc -l").output.trim().toIntOrNull()?.minus(1) ?: 0

            // I/O
            _state.value = _state.value.copy(step = "Benchmarking storage I/O...")
            var seqRead = -1f; var seqWrite = -1f; var randRead = -1f; var randWrite = -1f
            val writeResult = shell.exec("dd if=/dev/zero of=/data/local/tmp/ad_bench bs=1048576 count=50 conv=fsync 2>&1")
            seqWrite = parseDdSpeed(writeResult.output)
            val readResult = shell.exec("dd if=/data/local/tmp/ad_bench of=/dev/null bs=1048576 2>&1")
            seqRead = parseDdSpeed(readResult.output)
            shell.exec("rm -f /data/local/tmp/ad_bench")

            // App launches
            _state.value = _state.value.copy(step = "Measuring app launch times...")
            val launches = mutableListOf<AppLaunchResult>()
            for ((pkg, activity) in BENCHMARK_APPS) {
                if (!isInstalled(pkg)) continue
                shell.exec("am force-stop $pkg")
                delay(300)
                val out = shell.exec("am start -W -n $activity 2>&1")
                val timeMatch = Regex("TotalTime:\\s*(\\d+)").find(out.output)
                if (timeMatch != null) {
                    val ms = timeMatch.groupValues[1].toInt()
                    val appName = pkg.substringAfterLast(".")
                    launches.add(AppLaunchResult(pkg, appName, ms, LaunchStatus.OK))
                    shell.exec("am force-stop $pkg")
                }
            }

            val result = BenchmarkResult(
                timestamp = System.currentTimeMillis(),
                label = "benchmark",
                ramAvailableMb = mem.availableMb,
                ramUsedPct = mem.usedPct,
                swapUsedMb = swapUsed,
                cpuLoad1 = load1,
                seqReadMbps = seqRead,
                seqWriteMbps = seqWrite,
                randReadIops = randRead,
                randWriteIops = randWrite,
                processCount = procCount,
                appLaunches = launches,
            )

            _state.value = BenchmarkUiState(result = result)
        }
    }

    private fun isInstalled(pkg: String): Boolean {
        return try {
            context.packageManager.getPackageInfo(pkg, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    private fun parseDdSpeed(output: String): Float {
        // "871 M/s" or "1.2 G/s"
        Regex("(\\d+(?:\\.\\d+)?)\\s*G/s").find(output)?.let {
            return it.groupValues[1].toFloat() * 1024
        }
        Regex("(\\d+(?:\\.\\d+)?)\\s*M(?:B)?/s").find(output)?.let {
            return it.groupValues[1].toFloat()
        }
        Regex("(\\d+(?:\\.\\d+)?)\\s*K(?:B)?/s").find(output)?.let {
            return it.groupValues[1].toFloat() / 1024
        }
        // "X bytes ... copied, Y s"
        Regex("(\\d+)\\s*bytes.*copied.*?(\\d+(?:\\.\\d+)?)\\s*s").find(output)?.let {
            val bytes = it.groupValues[1].toFloat()
            val secs = it.groupValues[2].toFloat()
            if (secs > 0) return bytes / secs / (1024 * 1024)
        }
        return -1f
    }
}
