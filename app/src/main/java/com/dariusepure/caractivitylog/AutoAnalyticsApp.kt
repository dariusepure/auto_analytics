package com.dariusepure.caractivitylog

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.google.firebase.FirebaseApp
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class AutoAnalyticsApp : Application() {

    override fun onCreate() {
        super.onCreate()
        
        FirebaseApp.initializeApp(this)
        AppCheckHelper.init(this)

        // Set English as default if no language is selected (overrides system language on first run)
        if (AppCompatDelegate.getApplicationLocales().isEmpty) {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("en"))
        }
    }
}
