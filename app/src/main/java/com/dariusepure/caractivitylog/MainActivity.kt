package com.dariusepure.caractivitylog

import android.Manifest
import android.content.pm.PackageManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dariusepure.caractivitylog.ui.AppNavigation
import com.dariusepure.caractivitylog.ui.MainViewModel
import com.dariusepure.caractivitylog.ui.Screen
import com.dariusepure.caractivitylog.ui.theme.CarActivityLogTheme
import com.dariusepure.caractivitylog.ui.theme.ThemeViewModel
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }

        enableEdgeToEdge()
        setContent {
            val viewModel: MainViewModel = viewModel()
            val themeViewModel: ThemeViewModel = viewModel()
            
            val isDarkMode by themeViewModel.isDarkMode.collectAsState()
            val signedIn by viewModel.signedIn.collectAsState()
            
            val systemDark = isSystemInDarkTheme()
            val useDarkTheme = isDarkMode ?: systemDark

            var startRoute by remember(signedIn) {
                mutableStateOf(if (signedIn) Screen.CarList.route else Screen.SignIn.route)
            }

            // Handle Password Reset Intent
            LaunchedEffect(intent) {
                handleIntent(intent) { oobCode ->
                    startRoute = Screen.ResetPassword.createRoute(oobCode)
                }
            }

            CarActivityLogTheme(darkTheme = useDarkTheme) {
                AppNavigation(
                    startDestination = startRoute,
                    themeViewModel = themeViewModel
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    private fun handleIntent(intent: Intent?, onReset: (String) -> Unit) {
        val data: Uri? = intent?.data
        if (data != null && data.toString().contains("oobCode")) {
            val oobCode = data.getQueryParameter("oobCode")
            if (oobCode != null) {
                onReset(oobCode)
            }
        }
    }
}
