package com.androidoctor.ads

/** Pure frequency-cap logic for the interstitial, kept free of Android types so it can be unit tested. */
fun shouldShowInterstitial(
    optimizeRuns: Int,
    lastShownAt: Long,
    now: Long,
    capMs: Long,
    loaded: Boolean,
    consent: Boolean,
): Boolean =
    loaded && consent &&
        optimizeRuns >= 2 && // never on the first Optimize of a process lifetime
        (lastShownAt == 0L || now - lastShownAt >= capMs)
