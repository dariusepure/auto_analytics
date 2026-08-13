package com.dariusepure.caractivitylog.ui.auth

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dariusepure.caractivitylog.R
import com.dariusepure.caractivitylog.data.auth.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.util.Patterns

@HiltViewModel
class SignInViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {
    private val _state = MutableStateFlow<SignInState>(SignInState.Idle)
    val state: StateFlow<SignInState>
        get() = _state

    val signedIn = authRepository.signedIn

    fun onSignIn(email: String, password: String) {
        if (email.isBlank()) {
            _state.value = SignInState.Error(context.getString(R.string.validation_email_blank))
            return
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _state.value = SignInState.Error(context.getString(R.string.validation_email_invalid))
            return
        }
        if (password.isBlank()) {
            _state.value = SignInState.Error(context.getString(R.string.validation_password_blank))
            return
        }

        viewModelScope.launch {
            _state.value = SignInState.Pending
            try {
                authRepository.signIn(email, password)
                _state.value = SignInState.Idle
            } catch (exception: Exception) {
                _state.value = SignInState.Error(
                    exception.message ?: context.getString(R.string.error_signin_failed)
                )
            }
        }
    }

    fun onSignInWithGoogle(context: Context) {
        viewModelScope.launch {
            _state.value = SignInState.Pending
            try {
                authRepository.signInWithGoogle(context)
                _state.value = SignInState.Idle
            } catch (exception: Exception) {
                _state.value = SignInState.Error(
                    exception.message ?: context.getString(R.string.error_google_signin_failed)
                )
            }
        }
    }

    fun continueAsGuest() {
        authRepository.setGuestMode(true)
    }

    fun resetState() {
        _state.value = SignInState.Idle
    }
}
