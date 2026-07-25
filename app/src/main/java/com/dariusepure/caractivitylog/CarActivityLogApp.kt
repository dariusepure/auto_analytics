package com.dariusepure.caractivitylog

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.google.firebase.FirebaseApp
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class CarActivityLogApp : Application(), Configuration.Provider {
    @Inject lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        android.util.Log.d("CarActivityLogApp", "Initializing application...")
        FirebaseApp.initializeApp(this)
        android.util.Log.d("CarActivityLogApp", "FirebaseApp initialized")
        AppCheckHelper.init(this)
        android.util.Log.d("CarActivityLogApp", "AppCheck initialized")
    }
}
