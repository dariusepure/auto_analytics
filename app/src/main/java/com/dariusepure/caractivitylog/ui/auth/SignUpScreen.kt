package com.dariusepure.caractivitylog.ui.auth

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
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.res.stringResource
import com.dariusepure.caractivitylog.R
import com.dariusepure.caractivitylog.ui.common.AuthFooter
import com.dariusepure.caractivitylog.ui.common.ErrorBanner
import com.dariusepure.caractivitylog.ui.common.ModernAppLogo
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
fun SignUpScreen(
    onSignedIn: () -> Unit,
    onBackToSignIn: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SignUpViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val signedIn by viewModel.signedIn.collectAsStateWithLifecycle(initialValue = false)

    LaunchedEffect(signedIn) {
        if (signedIn) onSignedIn()
    }

    SignUpContent(
        state = state,
        onSignUp = viewModel::onSignUp,
        onBackToSignIn = onBackToSignIn,
        modifier = modifier
    )
}

@Composable
fun SignUpContent(
    state: SignUpState,
    onSignUp: (String, String, String, String, String) -> Unit,
    onBackToSignIn: () -> Unit,
    modifier: Modifier = Modifier
) {
    val submitting = state == SignUpState.Pending

    var email by rememberSaveable { mutableStateOf("") }
    var fullName by rememberSaveable { mutableStateOf("") }
    var username by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    var confirmPasswordVisible by rememberSaveable { mutableStateOf(false) }

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
                text = stringResource(R.string.auth_signup_title),
                style = MaterialTheme.typography.displaySmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 16.dp),
            )
            Text(
                text = stringResource(R.string.auth_signup_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp),
            )

            (state as? SignUpState.Error)?.let { error ->
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
                modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
            )
            OutlinedTextField(
                value = fullName,
                onValueChange = { fullName = it },
                label = { Text(stringResource(R.string.auth_full_name_label)) },
                singleLine = true,
                enabled = !submitting,
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            )
            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text(stringResource(R.string.auth_username_label)) },
                singleLine = true,
                enabled = !submitting,
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
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

                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(imageVector = image, contentDescription = null)
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            )
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = { Text(stringResource(R.string.auth_confirm_password_label)) },
                singleLine = true,
                enabled = !submitting,
                visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    val image = if (confirmPasswordVisible)
                        Icons.Filled.Visibility
                    else Icons.Filled.VisibilityOff

                    IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                        Icon(imageVector = image, contentDescription = null)
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                isError = password.isNotEmpty() && confirmPassword.isNotEmpty() && password != confirmPassword
            )

            Button(
                onClick = {
                    onSignUp(email, password, confirmPassword, fullName, username)
                },
                enabled = !submitting && email.isNotBlank() && password.isNotBlank() && 
                        confirmPassword.isNotBlank() && fullName.isNotBlank() && username.isNotBlank(),
                modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
            ) {
                if (submitting) {
                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Text(stringResource(R.string.auth_signup_button))
                }
            }

            Row(
                modifier = Modifier.padding(top = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.auth_have_account))
                TextButton(onClick = onBackToSignIn) {
                    Text(stringResource(R.string.auth_signin_button))
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SignUpScreenPreview() {
    CarActivityLogTheme {
        SignUpContent(
            state = SignUpState.Idle,
            onSignUp = { _, _, _, _, _ -> },
            onBackToSignIn = {}
        )
    }
}
