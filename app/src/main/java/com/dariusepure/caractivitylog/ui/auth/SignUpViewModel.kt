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
class SignUpViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {
    private val _state = MutableStateFlow<SignUpState>(SignUpState.Idle)
    val state: StateFlow<SignUpState>
        get() = _state

    val signedIn = authRepository.signedIn

    fun onSignUp(email: String, password: String, confirmPassword: String) {
        if (email.isBlank() || password.isBlank() || confirmPassword.isBlank()) {
            _state.value = SignUpState.Error(context.getString(R.string.validation_fields_required))
            return
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _state.value = SignUpState.Error(context.getString(R.string.validation_email_invalid))
            return
        }
        if (password != confirmPassword) {
            _state.value = SignUpState.Error(context.getString(R.string.validation_passwords_mismatch))
            return
        }

        viewModelScope.launch {
            _state.value = SignUpState.Pending
            try {
                authRepository.signUp(email, password)
                _state.value = SignUpState.Idle
            } catch (e: Exception) {
                _state.value = SignUpState.Error(
                    e.localizedMessage ?: context.getString(R.string.error_signup_failed)
                )
            }
        }
    }

    fun resetState() {
        _state.value = SignUpState.Idle
    }
}
