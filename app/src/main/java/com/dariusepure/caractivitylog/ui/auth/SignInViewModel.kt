package com.dariusepure.caractivitylog.ui.auth

import android.content.Context
import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.dariusepure.caractivitylog.data.auth.AuthRepository
import javax.inject.Inject

@HiltViewModel
class SignInViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {
    private val _state = MutableStateFlow<SignInState>(SignInState.Idle)
    val state: StateFlow<SignInState>
        get() = _state

    val signedIn: StateFlow<Boolean> =
        authRepository.signedIn.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            false
        )

    fun onSignIn(email: String, password: String) {
        if (email.isBlank()) {
            _state.value = SignInState.Error("Email cannot be blank")
            return
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _state.value = SignInState.Error("Invalid email")
            return
        }
        if (password.isBlank()) {
            _state.value = SignInState.Error("Password cannot be blank")
            return
        }

        viewModelScope.launch {
            _state.value = SignInState.Pending
            try {
                authRepository.signIn(email, password)
            } catch (exception: Exception) {
                _state.value = SignInState.Error(exception.message ?: "An unexpected error occurred during Sign-In")
            }
        }
    }

    fun onSignInWithGoogle(context: Context) {
        viewModelScope.launch {
            _state.value = SignInState.Pending
            try {
                authRepository.signInWithGoogle(context)
            } catch (exception: Exception) {
                _state.value = SignInState.Error(exception.message ?: "An unexpected error occurred during Google Sign-In")
            }
        }
    }

    fun onContinueAsGuest() {
        authRepository.setGuestMode(true)
    }
}
