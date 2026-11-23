package com.androidoctor.ads

import android.app.Activity
import android.content.Context
import com.androidoctor.BuildConfig
import com.google.android.gms.ads.MobileAds
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Google UMP consent. No ad request may be made until [canRequestAds] is true, and the
 * Mobile Ads SDK is initialized here, once, and nowhere else.
 */
@Singleton
class ConsentManager @Inject constructor(@ApplicationContext private val app: Context) {
    private val info = UserMessagingPlatform.getConsentInformation(app)
    private val _canRequestAds = MutableStateFlow(false)
    val canRequestAds: StateFlow<Boolean> = _canRequestAds.asStateFlow()
    private val _privacyOptionsRequired = MutableStateFlow(false)
    val privacyOptionsRequired: StateFlow<Boolean> = _privacyOptionsRequired.asStateFlow()
    private val sdkStarted = AtomicBoolean(false)

    fun gather(activity: Activity) {
        val params = ConsentRequestParameters.Builder().apply {
            if (BuildConfig.DEBUG) {
                setConsentDebugSettings(
                    ConsentDebugSettings.Builder(activity)
                        .setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA)
                        .addTestDeviceHashedId(DEBUG_DEVICE_ID)
                        .build(),
                )
            }
        }.build()
        // Consent given on a previous launch lets returning users load ads without waiting.
        publish()
        info.requestConsentInfoUpdate(
            activity,
            params,
            { UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { publish() } },
            { publish() }, // errors never block the app; ads load only if canRequestAds() is true
        )
    }

    fun showPrivacyOptions(activity: Activity) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { publish() }
    }

    private fun publish() {
        val ok = info.canRequestAds()
        if (ok && sdkStarted.compareAndSet(false, true)) MobileAds.initialize(app)
        _privacyOptionsRequired.value =
            info.privacyOptionsRequirementStatus == ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
        _canRequestAds.value = ok
    }

    private companion object {
        // Same device ID DoctorApp uses for AdMob test ads. UMP logs its own hashed ID if this differs.
        const val DEBUG_DEVICE_ID = "86D158D7D24E80D5DA03D74873F7DEFC"
    }
}
