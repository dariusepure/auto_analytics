package com.dariusepure.caractivitylog.ui.auth

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dariusepure.caractivitylog.data.auth.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SignUpViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _state = MutableStateFlow<SignUpState>(SignUpState.Idle)
    val state: StateFlow<SignUpState> = _state.asStateFlow()

    val signedIn = authRepository.signedIn

    fun onSignUp(email: String, password: String, confirmPassword: String) {
        if (email.isBlank() || password.isBlank()) {
            _state.value = SignUpState.Error("Email and password cannot be empty")
            return
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _state.value = SignUpState.Error("Please enter a valid email address")
            return
        }

        if (password != confirmPassword) {
            _state.value = SignUpState.Error("Passwords do not match")
            return
        }

        if (password.length < 6) {
            _state.value = SignUpState.Error("Password must be at least 6 characters")
            return
        }

        viewModelScope.launch {
            _state.value = SignUpState.Pending
            try {
                authRepository.signUp(email, password)
            } catch (e: Exception) {
                _state.value = SignUpState.Error(e.localizedMessage ?: "An error occurred during sign up")
            }
        }
    }
}
