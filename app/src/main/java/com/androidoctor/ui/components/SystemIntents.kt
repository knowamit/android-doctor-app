package com.androidoctor.ui.components

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings

/** Opens the first system screen that exists, falling back to plain Settings. */
fun openSystem(ctx: Context, vararg actions: String) {
    for (a in actions.toList() + Settings.ACTION_SETTINGS) {
        val ok = runCatching { ctx.startActivity(Intent(a).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }.isSuccess
        if (ok) return
    }
}

/** Opens Android's own app info page, where the user can clear cache, disable or uninstall. */
fun openAppSettings(ctx: Context, packageName: String) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName"))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { ctx.startActivity(intent) }
}

/** Opens Developer options. Returns false when they are off and About phone was opened instead. */
fun openDeveloperOptions(ctx: Context): Boolean {
    return try {
        ctx.startActivity(Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    } catch (e: ActivityNotFoundException) {
        openSystem(ctx, Settings.ACTION_DEVICE_INFO_SETTINGS)
        false
    }
}
