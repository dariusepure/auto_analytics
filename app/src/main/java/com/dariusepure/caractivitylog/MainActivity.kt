package com.dariusepure.caractivitylog

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.dariusepure.caractivitylog.ui.AppNavigation
import com.dariusepure.caractivitylog.ui.MainViewModel
import com.dariusepure.caractivitylog.ui.Screen
import com.dariusepure.caractivitylog.ui.theme.CarActivityLogTheme
import com.dariusepure.caractivitylog.ui.theme.ThemeViewModel
import com.dariusepure.caractivitylog.util.NotificationHelper
import com.dariusepure.caractivitylog.worker.InspectionReminderWorker
import dagger.hilt.android.AndroidEntryPoint
import java.util.concurrent.TimeUnit

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        NotificationHelper.createNotificationChannel(this)

        setContent {
            val viewModel: MainViewModel = viewModel()
            val themeViewModel: ThemeViewModel = viewModel()
            val context = LocalContext.current
            
            val isDarkMode by themeViewModel.isDarkMode.collectAsState()
            val signedIn by viewModel.signedIn.collectAsState()
            val isEmailVerified by viewModel.isEmailVerified.collectAsState()
            
            val systemDark = isSystemInDarkTheme()
            val useDarkTheme = isDarkMode ?: systemDark

            val launcher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { isGranted ->
                if (isGranted) {
                    scheduleInspectionReminders()
                }
            }

            LaunchedEffect(Unit) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.POST_NOTIFICATIONS
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        scheduleInspectionReminders()
                    }
                } else {
                    scheduleInspectionReminders()
                }
            }

            val startDestination = if (signedIn) {
                if (isEmailVerified || viewModel.isGuestMode) Screen.CarList.route else Screen.EmailVerification.route
            } else {
                Screen.SignIn.route
            }

            CarActivityLogTheme(darkTheme = useDarkTheme) {
                AppNavigation(
                    startDestination = startDestination,
                    themeViewModel = themeViewModel
                )
            }
        }
    }

    private fun scheduleInspectionReminders() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val workRequest = PeriodicWorkRequestBuilder<InspectionReminderWorker>(24, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "InspectionReminder",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }
}
