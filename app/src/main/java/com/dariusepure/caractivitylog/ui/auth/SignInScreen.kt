package com.dariusepure.caractivitylog.ui.auth

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material3.TextButton
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.dariusepure.caractivitylog.R
import com.dariusepure.caractivitylog.ui.common.AuthFooter
import com.dariusepure.caractivitylog.ui.common.ErrorBanner
import com.dariusepure.caractivitylog.ui.common.ModernAppLogo
import com.dariusepure.caractivitylog.ui.common.LanguageSelector
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.IconButton
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dariusepure.caractivitylog.ui.theme.CarActivityLogTheme

@Composable
fun SignInScreen(
    onSignedIn: () -> Unit,
    onSignUpClick: () -> Unit,
    onForgotPasswordClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SignInViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val signedIn by viewModel.signedIn.collectAsStateWithLifecycle()

    LaunchedEffect(signedIn) {
        if (signedIn == true) onSignedIn()
    }

    SignInContent(
        state = state,
        onSignIn = viewModel::onSignIn,
        onSignInWithGoogle = viewModel::onSignInWithGoogle,
        onContinueAsGuest = viewModel::onContinueAsGuest,
        onSignUpClick = onSignUpClick,
        onForgotPasswordClick = onForgotPasswordClick,
        modifier = modifier
    )
}

@Composable
fun SignInContent(
    state: SignInState,
    onSignIn: (String, String) -> Unit,
    onSignInWithGoogle: (Context) -> Unit,
    onContinueAsGuest: () -> Unit,
    onSignUpClick: () -> Unit,
    onForgotPasswordClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val submitting = state == SignInState.Pending

    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }

    val context = LocalContext.current

    Scaffold(
        modifier = modifier,
        bottomBar = { AuthFooter() }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            ModernAppLogo(
                modifier = Modifier.padding(bottom = 16.dp)
            )
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.displaySmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 16.dp),
            )
            Text(
                text = stringResource(R.string.auth_signin_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp),
            )

            (state as? SignInState.Error)?.let { error ->
                ErrorBanner(
                    message = error.message,
                    modifier = Modifier.padding(top = 24.dp),
                )
            }

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text(stringResource(R.string.auth_email_label)) },
                singleLine = true,
                enabled = !submitting,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp),
            )
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text(stringResource(R.string.auth_password_label)) },
                singleLine = true,
                enabled = !submitting,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    val image = if (passwordVisible)
                        Icons.Filled.Visibility
                    else Icons.Filled.VisibilityOff

                    val description = if (passwordVisible) stringResource(R.string.auth_hide_password) else stringResource(R.string.auth_show_password)

                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(imageVector = image, description)
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
            )

            TextButton(
                onClick = onForgotPasswordClick,
                enabled = !submitting,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text(stringResource(R.string.auth_forgot_password_title))
            }

            Button(
                onClick = { onSignIn(email, password) },
                enabled = !submitting,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp),
            ) {
                if (submitting) {
                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Text(stringResource(R.string.auth_signin_button))
                }
            }

            OutlinedButton(
                onClick = { onSignInWithGoogle(context)  },
                enabled = !submitting,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
            ) {
                Text(stringResource(R.string.auth_continue_google))
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                TextButton(
                    onClick = onContinueAsGuest,
                    enabled = !submitting,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.auth_continue_offline))
                }
            }

            Row(
                modifier = Modifier.padding(top = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.auth_no_account))
                TextButton(
                    onClick = onSignUpClick,
                    enabled = !submitting
                ) {
                    Text(stringResource(R.string.auth_signup_button))
                }
            }

            LanguageSelector(
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SignInScreenPreview() {
    CarActivityLogTheme {
        SignInContent(
            state = SignInState.Idle,
            onSignIn = { _, _ -> },
            onSignInWithGoogle = {},
            onContinueAsGuest = {},
            onSignUpClick = {},
            onForgotPasswordClick = {}
        )
    }
}
