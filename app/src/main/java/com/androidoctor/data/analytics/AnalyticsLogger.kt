package com.androidoctor.data.analytics

import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.logEvent
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnalyticsLogger @Inject constructor(
    private val firebaseAnalytics: FirebaseAnalytics,
) {
    fun shizukuGranted() {
        firebaseAnalytics.logEvent("shizuku_granted") {}
    }

    fun quickScanCompleted(score: Int) {
        firebaseAnalytics.logEvent("quick_scan_completed") {
            param("score", score.toLong())
        }
    }

    fun diagnosisStarted() {
        firebaseAnalytics.logEvent("diagnosis_started") {}
    }

    fun diagnosisCompleted(overallScore: Int) {
        firebaseAnalytics.logEvent("diagnosis_completed") {
            param("overall_score", overallScore.toLong())
        }
    }

    fun fixApplied(level: String, packagesDisabled: Int) {
        firebaseAnalytics.logEvent("fix_applied") {
            param("level", level)
            param("packages_disabled", packagesDisabled.toLong())
        }
    }
}
