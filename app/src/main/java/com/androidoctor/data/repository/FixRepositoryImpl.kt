package com.androidoctor.data.repository

import android.content.Context
import com.androidoctor.data.shell.ShellExecutor
import com.androidoctor.domain.repository.FixRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FixRepositoryImpl @Inject constructor(
    private val shell: ShellExecutor,
    @ApplicationContext private val context: Context,
) : FixRepository {

    private val snapshotFile: File
        get() = File(context.filesDir, "rollback_snapshot.json")

    private val changes = mutableListOf<JSONObject>()

    private fun recordChange(action: String, target: String, original: String, newValue: String) {
        val change = JSONObject().apply {
            put("action", action)
            put("target", target)
            put("original", original)
            put("new", newValue)
        }
        changes.add(change)
        saveSnapshot()
    }

    private fun saveSnapshot() {
        val arr = JSONArray()
        changes.forEach { arr.put(it) }
        snapshotFile.writeText(arr.toString(2))
    }

    override suspend fun disablePackage(packageName: String): Boolean {
        val result = shell.exec("pm disable-user --user 0 $packageName")
        if (result.isSuccess || "disabled" in result.output.lowercase()) {
            shell.exec("am force-stop $packageName")
            recordChange("disable_package", packageName, "enabled", "disabled")
            return true
        }
        return false
    }

    override suspend fun enablePackage(packageName: String): Boolean {
        val result = shell.exec("pm enable $packageName")
        return result.isSuccess || "enabled" in result.output.lowercase()
    }

    override suspend fun setAnimationScale(scale: Float): Boolean {
        var success = true
        for (setting in listOf("window_animation_scale", "transition_animation_scale", "animator_duration_scale")) {
            val original = shell.exec("settings get global $setting").output
            val result = shell.exec("settings put global $setting $scale")
            if (result.isSuccess) {
                recordChange("set_setting", "global:$setting", original, scale.toString())
            } else {
                success = false
            }
        }
        return success
    }

    override suspend fun setBackgroundProcessLimit(limit: Int): Boolean {
        val original = shell.exec("settings get global background_process_limit").output
        val result = shell.exec("settings put global background_process_limit $limit")
        if (result.isSuccess) {
            recordChange("set_setting", "global:background_process_limit", original, limit.toString())
            return true
        }
        return false
    }

    override suspend fun restrictBackground(packageName: String): Boolean {
        val result = shell.exec("cmd appops set $packageName RUN_IN_BACKGROUND deny")
        if (result.isSuccess) {
            recordChange("restrict_background", packageName, "allowed", "denied")
            return true
        }
        return false
    }

    override suspend fun trimCaches(): Long {
        val dfBefore = shell.exec("df /data").output
        shell.exec("pm trim-caches 1099511627776")
        val dfAfter = shell.exec("df /data").output

        // Parse available space difference
        fun parseAvail(df: String): Long {
            for (line in df.lines()) {
                val parts = line.trim().split("\\s+".toRegex())
                if (parts.size >= 4 && parts[0] != "Filesystem") {
                    return parts[3].toLongOrNull() ?: continue
                }
            }
            return 0
        }

        val before = parseAvail(dfBefore)
        val after = parseAvail(dfAfter)
        return (after - before).coerceAtLeast(0) * 1024 // KB to bytes
    }

    override suspend fun rollbackAll(): Int {
        if (!snapshotFile.exists()) return 0

        val arr = JSONArray(snapshotFile.readText())
        var restored = 0

        for (i in arr.length() - 1 downTo 0) {
            val change = arr.getJSONObject(i)
            val action = change.getString("action")
            val target = change.getString("target")
            val original = change.getString("original")

            val success = when (action) {
                "disable_package" -> enablePackage(target)
                "set_setting" -> {
                    val (ns, key) = target.split(":", limit = 2)
                    if (original in listOf("-1", "null")) {
                        shell.exec("settings delete $ns $key").isSuccess
                    } else {
                        shell.exec("settings put $ns $key $original").isSuccess
                    }
                }
                "restrict_background" -> {
                    shell.exec("cmd appops set $target RUN_IN_BACKGROUND allow").isSuccess
                }
                else -> false
            }
            if (success) restored++
        }

        snapshotFile.delete()
        changes.clear()
        return restored
    }

    override fun hasRollbackSnapshot(): Boolean = snapshotFile.exists() && snapshotFile.length() > 2
}
