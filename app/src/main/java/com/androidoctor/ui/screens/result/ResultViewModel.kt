package com.androidoctor.ui.screens.result

import android.app.Activity
import androidx.lifecycle.ViewModel
import com.androidoctor.ads.InterstitialAdController
import com.androidoctor.domain.usecase.OptimizeResult
import com.androidoctor.domain.usecase.OptimizeResultHolder
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ResultViewModel @Inject constructor(
    holder: OptimizeResultHolder,
    private val ads: InterstitialAdController,
) : ViewModel() {
    val result: OptimizeResult? = holder.last

    /** Shows the interstitial when allowed, then calls [onDone]. The Activity is not retained. */
    fun showAdThen(activity: Activity?, onDone: () -> Unit) {
        if (activity == null) onDone() else ads.showIfAllowed(activity, onDone)
    }
}
