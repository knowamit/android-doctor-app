package com.androidoctor

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.androidoctor.worker.WeeklyHealthWorker
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import dagger.hilt.android.HiltAndroidApp
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class DoctorApp : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        scheduleWeeklyHealth()
        if (BuildConfig.DEBUG) {
            // Dev devices only - forces test creatives so local runs never
            // count as real ad traffic against the live ad units above.
            MobileAds.setRequestConfiguration(
                RequestConfiguration.Builder()
                    .setTestDeviceIds(listOf("86D158D7D24E80D5DA03D74873F7DEFC"))
                    .build(),
            )
        }
        // MobileAds.initialize is called by ConsentManager once consent allows ad requests.
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                WEEKLY_HEALTH_CHANNEL_ID,
                getString(R.string.weekly_notif_channel_name),
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = getString(R.string.weekly_notif_channel_desc)
                enableVibration(false)
                setShowBadge(false)
            }
            nm.createNotificationChannel(channel)
        }
    }

    private fun scheduleWeeklyHealth() {
        val request = PeriodicWorkRequestBuilder<WeeklyHealthWorker>(7, TimeUnit.DAYS)
            .setInitialDelay(7, TimeUnit.DAYS)
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            WEEKLY_HEALTH_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }

    companion object {
        const val WEEKLY_HEALTH_CHANNEL_ID = "weekly_health"
        const val WEEKLY_HEALTH_WORK_NAME = "weekly_health_periodic"
    }
}
