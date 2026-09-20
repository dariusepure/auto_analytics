package com.dariusepure.caractivitylog

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.dariusepure.caractivitylog.ui.AppNavigation
import com.dariusepure.caractivitylog.ui.MainViewModel
import com.dariusepure.caractivitylog.ui.Screen
import com.dariusepure.caractivitylog.ui.theme.CarActivityLogTheme
import com.dariusepure.caractivitylog.ui.theme.SettingsViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    @OptIn(androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        
        enableEdgeToEdge()
        setContent {
            val mainViewModel: MainViewModel = hiltViewModel()
            val settingsViewModel: SettingsViewModel = hiltViewModel()
            
            val isOnline by mainViewModel.isOnline.collectAsState()
            val isDarkMode by settingsViewModel.isDarkMode.collectAsState()
            
            val systemDark = isSystemInDarkTheme()
            val useDarkTheme = isDarkMode ?: systemDark

            var deepLinkRoute by remember { mutableStateOf<String?>(null) }

            // Request Notification Permission
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                val launcher = androidx.activity.compose.rememberLauncherForActivityResult(
                    androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
                ) { _ -> }
                LaunchedEffect(Unit) {
                    launcher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                }
            }

            // Handle Guest Mode Firestore Network
            val isGuestMode by settingsViewModel.isGuestMode.collectAsState()
            LaunchedEffect(isGuestMode) {
                val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                if (isGuestMode) {
                    firestore.disableNetwork()
                } else {
                    firestore.enableNetwork()
                }
            }

            // Handle Password Reset Intent
            LaunchedEffect(intent) {
                handleIntent(intent) { oobCode ->
                    deepLinkRoute = Screen.ResetPassword.createRoute(oobCode)
                }
            }

            CarActivityLogTheme(darkTheme = useDarkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        AppNavigation(
                            startDestination = deepLinkRoute,
                            mainViewModel = mainViewModel,
                            settingsViewModel = settingsViewModel,
                            windowSizeClass = calculateWindowSizeClass(this@MainActivity)
                        )

                        // Offline Banner
                        if (!isOnline && !isGuestMode) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .statusBarsPadding()
                                    .background(Color(0xFFFF9800).copy(alpha = 0.9f))
                                    .padding(vertical = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Ești offline. Datele vor fi sincronizate când te reconectezi.",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
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
