package com.androidoctor

import com.androidoctor.ads.shouldShowInterstitial
import com.androidoctor.data.standard.quickFixIncludesKillStep
import com.androidoctor.data.storage.unusedLabel
import com.androidoctor.domain.usecase.OptimizeMode
import com.androidoctor.domain.usecase.OptimizeResult
import com.androidoctor.ui.screens.result.resultRows
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PolicyTests {
    private val cap = 180_000L

    @Test fun interstitialNeverOnFirstOptimize() =
        assertFalse(shouldShowInterstitial(1, 0, 1_000_000, cap, loaded = true, consent = true))

    @Test fun interstitialShownOnSecondOptimizeWhenLoaded() =
        assertTrue(shouldShowInterstitial(2, 0, 1_000_000, cap, loaded = true, consent = true))

    @Test fun interstitialRespectsCap() {
        assertFalse(shouldShowInterstitial(5, 900_000, 1_000_000, cap, loaded = true, consent = true))
        assertTrue(shouldShowInterstitial(5, 800_000, 1_000_000, cap, loaded = true, consent = true))
    }

    @Test fun interstitialNeedsConsentAndLoadedAd() {
        assertFalse(shouldShowInterstitial(5, 0, 1_000_000, cap, loaded = false, consent = true))
        assertFalse(shouldShowInterstitial(5, 0, 1_000_000, cap, loaded = true, consent = false))
    }

    @Test fun unusedLabelCases() {
        assertNull(unusedLabel(null, usageGranted = false))
        assertNull(unusedLabel(100, usageGranted = false))
        assertEquals("Not opened in the last year", unusedLabel(null, usageGranted = true))
        assertEquals("Last opened 45 days ago", unusedLabel(45, usageGranted = true))
        assertNull(unusedLabel(29, usageGranted = true))
        assertEquals("Last opened 30 days ago", unusedLabel(30, usageGranted = true))
    }

    @Test fun killStepSkippedOnAndroid14Plus() {
        assertTrue(quickFixIncludesKillStep(33))
        assertFalse(quickFixIncludesKillStep(34))
        assertFalse(quickFixIncludesKillStep(35))
    }

    private fun result(mode: OptimizeMode, before: Long = 0, after: Long = 0, signalled: Int = 0, pBefore: Int = 0) =
        OptimizeResult(mode, 1_000_000, before, after, pBefore, 3, 2, signalled)

    @Test fun standardRowsHideUnmeasuredMemory() {
        val labels = resultRows(result(OptimizeMode.STANDARD), 33).map { it.label }
        assertFalse("Free memory before" in labels)
        assertTrue("Temporary files cleared" in labels)
    }

    @Test fun standardRowsHideSignalledAppsOnAndroid14() {
        val r = result(OptimizeMode.STANDARD, 100, 200, signalled = 12)
        assertTrue(resultRows(r, 33).any { it.label == "Apps asked to release memory" })
        assertFalse(resultRows(r, 34).any { it.label == "Apps asked to release memory" })
    }

    @Test fun deepRowsShowProcessesOnlyWhenMeasured() {
        assertFalse(resultRows(result(OptimizeMode.DEEP), 33).any { it.label == "Processes ended" })
        assertTrue(resultRows(result(OptimizeMode.DEEP, pBefore = 40), 33).any { it.label == "Processes ended" })
    }
}
