package com.androidoctor.data.shell

import android.content.ComponentName
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import android.os.RemoteException
import android.util.Log
import com.androidoctor.BuildConfig
import com.androidoctor.IShellService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import rikka.shizuku.Shizuku
import kotlin.coroutines.resume
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Executes shell commands with ADB-level permissions via a Shizuku UserService,
 * falling back to an unprivileged app-level shell when Shizuku is unavailable.
 *
 * A plain Runtime.exec from the app process always runs as the app's own uid, even
 * when Shizuku is granted, so privileged commands (pm disable-user, trim-caches,
 * appops, dumpsys) must go through [ShellUserService] running in Shizuku's process.
 */
@Singleton
class ShellExecutor @Inject constructor(private val fixLog: FixLog) {

    val isShizukuAvailable: Boolean
        get() = try {
            Shizuku.pingBinder() && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        } catch (e: Exception) {
            false
        }

    val isShizukuInstalled: Boolean
        get() = try {
            Shizuku.pingBinder()
        } catch (e: Exception) {
            false
        }

    /** Asks Shizuku (shows its dialog). Result is delivered once on the main thread. */
    fun requestShizukuPermission(onResult: (Boolean) -> Unit) {
        try {
            if (!Shizuku.pingBinder()) return onResult(false)
            if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) return onResult(true)
            if (Shizuku.shouldShowRequestPermissionRationale()) return onResult(false) // permanently denied
            val listener = object : Shizuku.OnRequestPermissionResultListener {
                override fun onRequestPermissionResult(requestCode: Int, grantResult: Int) {
                    if (requestCode != PERMISSION_REQUEST_CODE) return
                    Shizuku.removeRequestPermissionResultListener(this)
                    onResult(grantResult == PackageManager.PERMISSION_GRANTED)
                }
            }
            Shizuku.addRequestPermissionResultListener(listener)
            Shizuku.requestPermission(PERMISSION_REQUEST_CODE)
        } catch (e: Exception) {
            onResult(false)
        }
    }

    @Volatile private var service: IShellService? = null
    private val bindMutex = Mutex()

    private val userServiceArgs by lazy {
        Shizuku.UserServiceArgs(ComponentName(BuildConfig.APPLICATION_ID, ShellUserService::class.java.name))
            .processNameSuffix("shell")
            .debuggable(BuildConfig.DEBUG)
            .version(BuildConfig.VERSION_CODE)
            .daemon(false)
    }

    suspend fun exec(command: String, timeoutMs: Long = 15_000): ShellResult =
        withContext(Dispatchers.IO) {
            fixLog.line(LineKind.CMD, "$ $command")
            val r = if (isShizukuAvailable) execShizuku(command, timeoutMs) else execRuntime(command, timeoutMs)
            fixLog.result(r.exitCode, r.stdout, r.stderr)
            r
        }

    private suspend fun getService(): IShellService? {
        service?.takeIf { it.asBinder().pingBinder() }?.let { return it }
        return bindMutex.withLock {
            service?.takeIf { it.asBinder().pingBinder() } ?: withTimeoutOrNull(BIND_TIMEOUT_MS) {
                suspendCancellableCoroutine<IShellService?> { cont ->
                    val connection = object : ServiceConnection {
                        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
                            val s = binder?.takeIf { it.pingBinder() }?.let { IShellService.Stub.asInterface(it) }
                            service = s
                            // No binder calls here: this callback runs on the main thread.
                            if (cont.isActive) cont.resume(s)
                        }

                        override fun onServiceDisconnected(name: ComponentName?) {
                            service = null
                        }
                    }
                    // The 5 s timeout cancels this coroutine, so release the half-open bind.
                    cont.invokeOnCancellation {
                        runCatching { Shizuku.unbindUserService(userServiceArgs, connection, true) }
                    }
                    try {
                        Shizuku.bindUserService(userServiceArgs, connection)
                    } catch (e: Exception) {
                        if (cont.isActive) cont.resume(null)
                    }
                }
            }
        }
    }

    private suspend fun execShizuku(command: String, timeoutMs: Long): ShellResult {
        val svc = getService()
            ?: return execRuntime(command, timeoutMs).copy(stderr = "Shizuku service unavailable, ran unprivileged")
        return try {
            val r = svc.exec(command, timeoutMs)
            if (BuildConfig.DEBUG) Log.d(TAG, "exec[$command] exit=${r[2]} err=${r[1].take(80)}")
            ShellResult(
                stdout = r[0],
                stderr = r[1],
                exitCode = r[2].toIntOrNull() ?: -1,
                source = ShellSource.SHIZUKU,
            )
        } catch (e: RemoteException) {
            service = null
            ShellResult(stdout = "", stderr = e.message ?: "Shizuku error", exitCode = -1, source = ShellSource.SHIZUKU)
        }
    }

    private fun execRuntime(command: String, timeoutMs: Long): ShellResult {
        return try {
            val process = ProcessBuilder("sh", "-c", command).start()
            // stderr is drained on its own thread so a full pipe can never block stdout.
            var stderr = ""
            val errReader = Thread { stderr = runCatching { process.errorStream.bufferedReader().readText() }.getOrDefault("") }
                .apply { isDaemon = true; start() }
            var stdout = ""
            val outReader = Thread { stdout = runCatching { process.inputStream.bufferedReader().readText() }.getOrDefault("") }
                .apply { isDaemon = true; start() }
            val finished = process.waitFor(timeoutMs, java.util.concurrent.TimeUnit.MILLISECONDS)
            if (!finished) {
                process.destroyForcibly()
                errReader.join(500); outReader.join(500)
                return ShellResult(stdout, "Timed out after ${timeoutMs}ms", -1, ShellSource.RUNTIME)
            }
            errReader.join(1_000); outReader.join(1_000)
            ShellResult(stdout = stdout, stderr = stderr, exitCode = process.exitValue(), source = ShellSource.RUNTIME)
        } catch (e: Exception) {
            ShellResult(stdout = "", stderr = e.message ?: "Runtime error", exitCode = -1, source = ShellSource.RUNTIME)
        }
    }
}

private const val BIND_TIMEOUT_MS = 5_000L
private const val TAG = "ShellExecutor"
private const val PERMISSION_REQUEST_CODE = 4242

data class ShellResult(
    val stdout: String,
    val stderr: String,
    val exitCode: Int,
    val source: ShellSource,
) {
    val isSuccess: Boolean get() = exitCode == 0
    val output: String get() = stdout.trim()
}

enum class ShellSource { SHIZUKU, RUNTIME }
