package com.androidoctor.ads

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAdLoadCallback
import javax.inject.Inject
import javax.inject.Singleton

// Android Doctor's real rewarded interstitial ad unit (Ammroid AdMob account).
// Rewarded interstitial is the only AdMob format where the no-skip duration
// is a real, Google-enforced timer (a plain Interstitial's skip timer is
// short and fixed by Google — publishers can't extend it, so it's the wrong
// format for "force a minimum watch time").
private const val REWARDED_INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3519607474127563/3716535642"

@Singleton
class ScanCompleteAdController @Inject constructor() {
    private var ad: RewardedInterstitialAd? = null
    private var loading = false

    fun preload(context: Context) {
        if (ad != null || loading) return
        loading = true
        RewardedInterstitialAd.load(
            context,
            REWARDED_INTERSTITIAL_AD_UNIT_ID,
            AdRequest.Builder().build(),
            object : RewardedInterstitialAdLoadCallback() {
                override fun onAdLoaded(loadedAd: RewardedInterstitialAd) {
                    ad = loadedAd
                    loading = false
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    ad = null
                    loading = false
                }
            },
        )
    }

    // Shows the full-screen ad if one is ready; otherwise calls onDismissed
    // immediately so the scan result is never blocked by an ad failure.
    fun show(activity: Activity, onDismissed: () -> Unit) {
        val current = ad
        if (current == null) {
            preload(activity)
            onDismissed()
            return
        }
        current.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                ad = null
                preload(activity)
                onDismissed()
            }

            override fun onAdFailedToShowFullScreenContent(error: com.google.android.gms.ads.AdError) {
                ad = null
                preload(activity)
                onDismissed()
            }
        }
        current.show(activity) { /* no in-app currency reward — used purely as a forced-view gate */ }
    }
}
