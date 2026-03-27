package com.androidoctor.data.parser

import com.androidoctor.domain.model.*

/**
 * Bloatware detection using an embedded database of known packages.
 * Matches installed system packages against known bloatware by OEM.
 */
object BloatwareAnalyzer {

    private data class BloatEntry(
        val pkg: String,
        val name: String,
        val category: String,
        val impact: Impact,
        val description: String,
    )

    // Curated subset — the full 113-entry DB from the CLI tool
    private val DB: Map<String, List<BloatEntry>> by lazy { buildDatabase() }

    fun diagnose(
        systemPackages: List<String>,
        disabledPackages: List<String>,
        brand: String,
    ): BloatwareDiagnosis {
        val findings = mutableListOf<String>()
        val oem = detectOem(systemPackages, brand)
        findings.add("Detected OEM: $oem")
        findings.add("Total system packages: ${systemPackages.size}")

        val allBloat = mutableMapOf<String, BloatEntry>()
        for (section in listOf("google", oem, "common")) {
            DB[section]?.forEach { allBloat[it.pkg] = it }
        }

        val disabledSet = disabledPackages.toSet()
        val found = mutableListOf<BloatwareEntry>()
        val alreadyDisabled = mutableListOf<String>()

        for (pkg in systemPackages) {
            val entry = allBloat[pkg] ?: continue
            if (pkg in disabledSet) {
                alreadyDisabled.add(pkg)
            } else {
                found.add(BloatwareEntry(pkg, entry.name, entry.category, entry.impact, entry.description))
            }
        }

        found.sortBy { when (it.impact) { Impact.HIGH -> 0; Impact.MEDIUM -> 1; Impact.LOW -> 2 } }

        val highCount = found.count { it.impact == Impact.HIGH }
        val medCount = found.count { it.impact == Impact.MEDIUM }

        if (found.isNotEmpty()) {
            findings.add("Removable bloatware: ${found.size} ($highCount high, $medCount medium)")
        } else {
            findings.add("No known bloatware detected")
        }
        if (alreadyDisabled.isNotEmpty()) {
            findings.add("Already disabled: ${alreadyDisabled.size}")
        }

        var score = 100
        score -= highCount * 8
        score -= medCount * 4
        score -= (found.size - 10).coerceAtLeast(0) * 2
        score = score.coerceIn(0, 100)

        val severity = when {
            score >= 70 -> Severity.OK
            score >= 40 -> Severity.WARNING
            else -> Severity.CRITICAL
        }

        return BloatwareDiagnosis(severity, systemPackages.size, found.size, highCount, found, alreadyDisabled, findings, score)
    }

    private fun detectOem(packages: List<String>, brand: String): String {
        val b = brand.lowercase()
        if (b in listOf("samsung", "xiaomi", "oppo", "vivo", "oneplus", "huawei", "google")) return b
        val prefixes = mapOf(
            "samsung" to listOf("com.samsung.", "com.sec."),
            "xiaomi" to listOf("com.miui.", "com.xiaomi."),
            "oppo" to listOf("com.coloros.", "com.heytap."),
            "vivo" to listOf("com.vivo.", "com.bbk."),
            "oneplus" to listOf("com.oneplus."),
            "huawei" to listOf("com.huawei."),
        )
        val counts = mutableMapOf<String, Int>()
        for (pkg in packages) {
            for ((oem, pfs) in prefixes) {
                if (pfs.any { pkg.startsWith(it) }) counts[oem] = (counts[oem] ?: 0) + 1
            }
        }
        return counts.maxByOrNull { it.value }?.key ?: "unknown"
    }

    private fun buildDatabase(): Map<String, List<BloatEntry>> = mapOf(
        "samsung" to listOf(
            BloatEntry("com.samsung.android.bixby.agent", "Bixby Voice", "assistant", Impact.HIGH, "Constant background CPU and RAM"),
            BloatEntry("com.samsung.android.bixby.service", "Bixby Service", "assistant", Impact.HIGH, "Background service"),
            BloatEntry("com.samsung.android.app.spage", "Samsung Free", "media", Impact.HIGH, "Heavy background sync"),
            BloatEntry("com.samsung.android.mobileservice", "Samsung Experience Service", "telemetry", Impact.HIGH, "Account sync and telemetry"),
            BloatEntry("com.samsung.android.samsungpay", "Samsung Pay", "other", Impact.HIGH, "Heavy background activity"),
            BloatEntry("com.samsung.android.spay", "Samsung Pay Framework", "other", Impact.HIGH, "Payment framework"),
            BloatEntry("com.samsung.android.visionintelligence", "Bixby Vision", "assistant", Impact.MEDIUM, "Camera AI"),
            BloatEntry("com.samsung.android.game.gamehome", "Game Launcher", "other", Impact.MEDIUM, "Game tools"),
            BloatEntry("com.samsung.android.aremoji", "AR Emoji", "media", Impact.MEDIUM, "AR emoji"),
            BloatEntry("com.samsung.android.arzone", "AR Zone", "media", Impact.MEDIUM, "AR feature hub"),
            BloatEntry("com.samsung.android.forest", "Samsung Health", "other", Impact.MEDIUM, "Background sensors"),
            BloatEntry("com.samsung.android.app.social", "What's New", "ads", Impact.MEDIUM, "Promotional content"),
            BloatEntry("com.samsung.android.smartsuggestions", "Smart Suggestions", "telemetry", Impact.MEDIUM, "Background processing"),
            BloatEntry("com.sec.android.app.sbrowser", "Samsung Internet", "duplicate", Impact.MEDIUM, "Duplicate browser"),
            BloatEntry("com.sec.android.daemonapp", "Samsung Weather", "other", Impact.MEDIUM, "Frequent background syncs"),
        ),
        "xiaomi" to listOf(
            BloatEntry("com.miui.analytics", "MIUI Analytics", "telemetry", Impact.HIGH, "Constant data collection"),
            BloatEntry("com.xiaomi.joyose", "Joyose", "telemetry", Impact.HIGH, "Ad targeting telemetry"),
            BloatEntry("com.miui.msa.global", "MSA (System Ads)", "ads", Impact.HIGH, "System ad framework"),
            BloatEntry("com.miui.daemon", "MIUI Daemon", "telemetry", Impact.HIGH, "System daemon"),
            BloatEntry("com.miui.hybrid", "Quick Apps", "other", Impact.MEDIUM, "Instant apps"),
            BloatEntry("com.miui.personalassistant", "App Vault", "other", Impact.MEDIUM, "Left swipe panel"),
            BloatEntry("com.mi.globalbrowser", "Mi Browser", "duplicate", Impact.MEDIUM, "Duplicate browser"),
            BloatEntry("com.miui.cleanmaster", "Cleaner", "ads", Impact.MEDIUM, "Cleaner with ads"),
            BloatEntry("com.miui.cloudservice", "Mi Cloud", "other", Impact.MEDIUM, "Cloud sync"),
        ),
        "oppo" to listOf(
            BloatEntry("com.coloros.bootreg", "Boot Registration", "telemetry", Impact.HIGH, "Boot telemetry"),
            BloatEntry("com.nearme.statistics.rom", "ROM Statistics", "telemetry", Impact.HIGH, "Usage statistics"),
            BloatEntry("com.heytap.browser", "HeyTap Browser", "duplicate", Impact.MEDIUM, "Duplicate browser"),
            BloatEntry("com.heytap.market", "HeyTap App Market", "other", Impact.MEDIUM, "App store"),
        ),
        "google" to listOf(
            BloatEntry("com.google.android.apps.wellbeing", "Digital Wellbeing", "telemetry", Impact.MEDIUM, "Constant background tracking"),
            BloatEntry("com.google.android.apps.magazines", "Google News", "media", Impact.MEDIUM, "Background sync"),
            BloatEntry("com.google.android.videos", "Google TV", "media", Impact.LOW, "Video store"),
            BloatEntry("com.google.android.printservice.recommendation", "Print Service", "other", Impact.LOW, "Print recommendation"),
            BloatEntry("com.google.ar.core", "ARCore", "other", Impact.LOW, "AR framework"),
        ),
        "common" to listOf(
            BloatEntry("com.facebook.services", "Facebook Services", "telemetry", Impact.HIGH, "Heavy battery and data drain"),
            BloatEntry("com.facebook.system", "Facebook System", "telemetry", Impact.HIGH, "Pre-installed Meta service"),
            BloatEntry("com.facebook.appmanager", "Facebook App Manager", "telemetry", Impact.HIGH, "Auto-updates Facebook"),
            BloatEntry("com.microsoft.skydrive", "OneDrive", "other", Impact.MEDIUM, "Background sync"),
            BloatEntry("com.linkedin.android", "LinkedIn", "social", Impact.MEDIUM, "Pre-installed"),
        ),
    )
}
