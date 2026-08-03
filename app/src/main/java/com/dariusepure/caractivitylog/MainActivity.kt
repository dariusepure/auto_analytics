package com.dariusepure.caractivitylog

import android.Manifest
import android.content.pm.PackageManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.dariusepure.caractivitylog.ui.AppNavigation
import com.dariusepure.caractivitylog.ui.Screen
import com.dariusepure.caractivitylog.ui.theme.CarActivityLogTheme
import com.dariusepure.caractivitylog.ui.theme.ThemeViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    @OptIn(androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }

        enableEdgeToEdge()
        setContent {
            val themeViewModel: ThemeViewModel = hiltViewModel()
            val isDarkMode by themeViewModel.isDarkMode.collectAsState()
            
            val systemDark = isSystemInDarkTheme()
            val useDarkTheme = isDarkMode ?: systemDark

            var deepLinkRoute by remember { mutableStateOf<String?>(null) }

            // Handle Password Reset Intent
            LaunchedEffect(intent) {
                handleIntent(intent) { oobCode ->
                    deepLinkRoute = Screen.ResetPassword.createRoute(oobCode)
                }
            }

            CarActivityLogTheme(darkTheme = useDarkTheme) {
                AppNavigation(
                    startDestination = deepLinkRoute,
                    themeViewModel = themeViewModel,
                    windowSizeClass = calculateWindowSizeClass(this)
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
