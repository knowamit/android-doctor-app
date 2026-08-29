package com.androidoctor.ui.components

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.androidoctor.ui.theme.Copper
import com.androidoctor.ui.theme.Zinc100
import com.androidoctor.ui.theme.Zinc400
import com.androidoctor.ui.theme.Zinc800
import com.androidoctor.ui.theme.Zinc900
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.nativead.NativeAdView

// Android Doctor's real native ad unit (Ammroid AdMob account).
private const val NATIVE_AD_UNIT_ID = "ca-app-pub-3519607474127563/5063713230"

@Composable
fun NativeAdCard(
    modifier: Modifier = Modifier,
    adUnitId: String = NATIVE_AD_UNIT_ID,
) {
    val context = LocalContext.current
    var nativeAd by remember { mutableStateOf<NativeAd?>(null) }
    var failed by remember { mutableStateOf(false) }

    DisposableEffect(adUnitId) {
        val loader = AdLoader.Builder(context, adUnitId)
            .forNativeAd { ad -> nativeAd = ad }
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(error: com.google.android.gms.ads.LoadAdError) {
                    failed = true
                }
            })
            .withNativeAdOptions(NativeAdOptions.Builder().build())
            .build()
        loader.loadAd(AdRequest.Builder().build())
        onDispose { nativeAd?.destroy() }
    }

    val ad = nativeAd
    if (ad == null || failed) return

    val headlineColor = Zinc100.toArgb()
    val bodyColor = Zinc400.toArgb()
    val ctaTextColor = Copper.toArgb()
    val ctaBorderColor = Copper.toArgb()
    val surfaceColor = Zinc900.toArgb()
    val borderColor = Zinc800.toArgb()

    AndroidView(
        modifier = modifier.fillMaxWidth().height(112.dp),
        factory = { ctx -> buildNativeAdView(ctx, headlineColor, bodyColor, ctaTextColor, ctaBorderColor, surfaceColor, borderColor) },
        update = { view -> bindNativeAd(view, ad) },
    )
}

private class AdViewHolder(
    val root: NativeAdView,
    val label: TextView,
    val icon: ImageView,
    val headline: TextView,
    val body: TextView,
    val cta: Button,
)

private fun buildNativeAdView(
    context: Context,
    headlineColor: Int,
    bodyColor: Int,
    ctaTextColor: Int,
    ctaBorderColor: Int,
    surfaceColor: Int,
    borderColor: Int,
): NativeAdView {
    val density = context.resources.displayMetrics.density
    fun dp(v: Int) = (v * density).toInt()

    val root = NativeAdView(context).apply {
        background = GradientDrawable().apply {
            setColor(surfaceColor)
            setStroke(dp(1), borderColor)
            cornerRadius = dp(4).toFloat()
        }
    }

    val container = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(16), dp(12), dp(16), dp(12))
    }

    val label = TextView(context).apply {
        text = "ADVERTISEMENT"
        setTextColor(bodyColor)
        textSize = 9f
        letterSpacing = 0.15f
        setTypeface(typeface, android.graphics.Typeface.BOLD)
    }
    container.addView(label)

    val row = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding(0, dp(10), 0, 0)
    }

    val icon = ImageView(context).apply {
        layoutParams = LinearLayout.LayoutParams(dp(40), dp(40))
        scaleType = ImageView.ScaleType.CENTER_CROP
        background = GradientDrawable().apply {
            setColor(borderColor)
            cornerRadius = dp(4).toFloat()
        }
        clipToOutline = true
    }
    row.addView(icon)

    val textCol = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
            marginStart = dp(12)
        }
    }
    val headline = TextView(context).apply {
        setTextColor(headlineColor)
        textSize = 14f
        setTypeface(typeface, android.graphics.Typeface.BOLD)
        maxLines = 1
    }
    val body = TextView(context).apply {
        setTextColor(bodyColor)
        textSize = 12f
        maxLines = 1
        setPadding(0, dp(2), 0, 0)
    }
    textCol.addView(headline)
    textCol.addView(body)
    row.addView(textCol)

    val cta = Button(context).apply {
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, dp(32))
        setTextColor(ctaTextColor)
        textSize = 12f
        isAllCaps = false
        setPadding(dp(14), 0, dp(14), 0)
        background = GradientDrawable().apply {
            setColor(android.graphics.Color.TRANSPARENT)
            setStroke(dp(1), ctaBorderColor)
            cornerRadius = dp(4).toFloat()
        }
        stateListAnimator = null
        isClickable = false // NativeAdView's own click handling drives the CTA
    }
    row.addView(cta)

    container.addView(row)
    root.addView(container)

    root.headlineView = headline
    root.bodyView = body
    root.iconView = icon
    root.callToActionView = cta

    root.tag = AdViewHolder(root, label, icon, headline, body, cta)
    return root
}

private fun bindNativeAd(view: NativeAdView, ad: NativeAd) {
    val holder = view.tag as? AdViewHolder ?: return

    holder.headline.text = ad.headline
    holder.body.text = ad.body ?: ad.advertiser.orEmpty()
    holder.body.visibility = if (holder.body.text.isNullOrBlank()) View.GONE else View.VISIBLE

    val iconDrawable = ad.icon?.drawable
    if (iconDrawable != null) {
        holder.icon.setImageDrawable(iconDrawable)
        holder.icon.visibility = View.VISIBLE
    } else {
        holder.icon.visibility = View.GONE
    }

    val cta = ad.callToAction
    if (cta != null) {
        holder.cta.text = cta
        holder.cta.visibility = View.VISIBLE
    } else {
        holder.cta.visibility = View.GONE
    }

    view.setNativeAd(ad)
}
