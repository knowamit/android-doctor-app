package com.androidoctor.ads

import android.app.Activity
import android.content.Context
import android.os.SystemClock
import android.util.Log
import com.androidoctor.BuildConfig
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A standard, closable interstitial shown at a natural break (leaving the result screen).
 * Never before the result, never on the first Optimize, at most one per [CAP_MS].
 * The Activity is only ever a parameter of [showIfAllowed], never stored.
 */
@Singleton
class InterstitialAdController @Inject constructor(
    @ApplicationContext private val app: Context,
    private val consent: ConsentManager,
) {
    private var ad: InterstitialAd? = null
    private var loading = false
    private var lastShownAt = 0L
    private var optimizeRuns = 0

    init {
        // Preload as soon as consent allows ad requests (and again after later consent changes).
        consent.canRequestAds
            .onEach { if (it) preload() }
            .launchIn(CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate))
    }

    fun preload() {
        val unit = unitId()
        if (unit.isEmpty() || !consent.canRequestAds.value || ad != null || loading) return
        loading = true
        InterstitialAd.load(
            app,
            unit,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(a: InterstitialAd) {
                    ad = a
                    loading = false
                    if (BuildConfig.DEBUG) Log.d(TAG, "interstitial loaded")
                }

                override fun onAdFailedToLoad(e: LoadAdError) {
                    loading = false
                    if (BuildConfig.DEBUG) Log.d(TAG, "interstitial failed to load: ${e.code}")
                }
            },
        )
    }

    fun onOptimizeFinished() {
        optimizeRuns++
        preload()
    }

    /** Shows the ad if every condition holds, then always calls [onDone] exactly once. */
    fun showIfAllowed(activity: Activity, onDone: () -> Unit) {
        val now = SystemClock.elapsedRealtime()
        val current = ad
        val allowed = shouldShowInterstitial(
            optimizeRuns, lastShownAt, now, CAP_MS, loaded = current != null, consent = consent.canRequestAds.value,
        ) && !activity.isFinishing && !activity.isDestroyed
        if (!allowed || current == null) {
            onDone()
            return
        }
        ad = null
        current.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                lastShownAt = SystemClock.elapsedRealtime()
                onDone()
                preload()
            }

            override fun onAdFailedToShowFullScreenContent(e: AdError) {
                onDone()
                preload()
            }
        }
        current.show(activity)
    }

    private fun unitId() = if (BuildConfig.DEBUG) TEST_INTERSTITIAL else PROD_INTERSTITIAL

    companion object {
        private const val TAG = "Ads"
        const val TEST_INTERSTITIAL = "ca-app-pub-3940256099942544/1033173712" // Google's documented test ID

        // OWNER STEP: paste the new Interstitial ad unit ID from AdMob here. Empty means release
        // builds never request an interstitial. Do not reuse the rewarded or native unit IDs.
        const val PROD_INTERSTITIAL = ""
        const val CAP_MS = 3 * 60_000L
    }
}
