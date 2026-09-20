package com.dariusepure.caractivitylog

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.dariusepure.caractivitylog.util.ExpiryWorker
import com.dariusepure.caractivitylog.util.NotificationHelper
import com.google.firebase.FirebaseApp
import dagger.hilt.android.HiltAndroidApp
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class AutoAnalyticsApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        
        FirebaseApp.initializeApp(this)
        AppCheckHelper.init(this)

        NotificationHelper.createNotificationChannel(this)
        scheduleExpiryChecks()

        // Set English as default if no language is selected (overrides system language on first run)
        if (AppCompatDelegate.getApplicationLocales().isEmpty) {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("en"))
        }
    }

    private fun scheduleExpiryChecks() {
        val expiryWorkRequest = PeriodicWorkRequestBuilder<ExpiryWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(1, TimeUnit.HOURS) // Start after 1 hour to not heavy load on first start
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "expiry_checks",
            ExistingPeriodicWorkPolicy.KEEP,
            expiryWorkRequest
        )
    }
}
