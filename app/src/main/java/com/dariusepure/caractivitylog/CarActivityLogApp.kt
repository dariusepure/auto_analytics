package com.dariusepure.caractivitylog

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.dariusepure.caractivitylog.util.NotificationHelper
import com.dariusepure.caractivitylog.worker.ItpCheckWorker
import dagger.hilt.android.HiltAndroidApp
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class CarActivityLogApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        
        AppCheckHelper.init(this)
        NotificationHelper.createNotificationChannel(this)
        scheduleItpChecks()
    }

    private fun scheduleItpChecks() {
        val workRequest = PeriodicWorkRequestBuilder<ItpCheckWorker>(1, TimeUnit.DAYS)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "itp_check_work",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }
}
