package com.dariusepure.caractivitylog

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dariusepure.caractivitylog.ui.AppNavigation
import com.dariusepure.caractivitylog.ui.MainViewModel
import com.dariusepure.caractivitylog.ui.Screen
import com.dariusepure.caractivitylog.ui.theme.CarActivityLogTheme
import com.dariusepure.caractivitylog.ui.theme.ThemeViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: MainViewModel = viewModel()
            val themeViewModel: ThemeViewModel = viewModel()
            
            val isDarkMode by themeViewModel.isDarkMode.collectAsState()
            val signedIn by viewModel.signedIn.collectAsState()
            
            val systemDark = isSystemInDarkTheme()
            val useDarkTheme = isDarkMode ?: systemDark

            val startDestination = if (signedIn) {
                Screen.CarList.route
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
}
