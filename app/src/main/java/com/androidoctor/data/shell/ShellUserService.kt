package com.androidoctor.data.shell

import com.androidoctor.IShellService
import kotlin.concurrent.thread
import kotlin.system.exitProcess

/**
 * Instantiated by Shizuku inside its own shell-privileged process (uid 2000),
 * not inside the app process. Commands run here have ADB-level permissions.
 */
class ShellUserService : IShellService.Stub() {

    override fun destroy() {
        exitProcess(0)
    }

    override fun exec(command: String, timeoutMs: Long): Array<String> {
        return try {
            val process = ProcessBuilder("sh", "-c", command).start()
            var out = ""
            var err = ""
            val outReader = thread(isDaemon = true) { out = process.inputStream.bufferedReader().readCapped() }
            val errReader = thread(isDaemon = true) { err = process.errorStream.bufferedReader().readCapped() }

            val finished = process.waitFor(timeoutMs, java.util.concurrent.TimeUnit.MILLISECONDS)
            if (!finished) process.destroyForcibly()
            outReader.join(1_000)
            errReader.join(1_000)

            arrayOf(out, err, if (finished) process.exitValue().toString() else "-1")
        } catch (e: Exception) {
            arrayOf("", e.message ?: "exec failed", "-1")
        }
    }

    private fun java.io.BufferedReader.readCapped(): String {
        val sb = StringBuilder()
        val buf = CharArray(8192)
        use { reader ->
            while (true) {
                val n = reader.read(buf)
                if (n < 0) break
                // Keep draining so the process never blocks on a full pipe,
                // but stop storing once the binder-safe cap is reached.
                if (sb.length < MAX_CHARS) sb.append(buf, 0, minOf(n, MAX_CHARS - sb.length))
            }
        }
        return sb.toString()
    }

    private companion object {
        const val MAX_CHARS = 400_000
    }
}
