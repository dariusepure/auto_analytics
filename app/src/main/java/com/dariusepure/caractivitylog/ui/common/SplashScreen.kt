package com.dariusepure.caractivitylog.ui.common

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    signedIn: Boolean?,
    onNavigateToSignIn: () -> Unit,
    onNavigateToMain: () -> Unit
) {
    var navigated by remember { mutableStateOf(false) }

    // Wait until the authentication state is determined (not null)
    LaunchedEffect(signedIn) {
        if (signedIn != null && !navigated) {
            // Settle delay for Firebase/Credential Manager on older devices
            delay(300)
            navigated = true
            if (signedIn) {
                onNavigateToMain()
            } else {
                onNavigateToSignIn()
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        ModernAppLogo(size = 120.dp, iconSize = 72.dp)
    }
}
