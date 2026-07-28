package com.dariusepure.caractivitylog.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dariusepure.caractivitylog.data.auth.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ResetPasswordViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {
    private val _state = MutableStateFlow<ResetPasswordState>(ResetPasswordState.Idle)
    val state: StateFlow<ResetPasswordState>
        get() = _state

    fun onConfirmReset(oobCode: String, newPassword: String, confirmPassword: String) {
        if (newPassword.isBlank()) {
            _state.value = ResetPasswordState.Error("Password cannot be blank")
            return
        }
        if (newPassword != confirmPassword) {
            _state.value = ResetPasswordState.Error("Passwords do not match")
            return
        }

        viewModelScope.launch {
            _state.value = ResetPasswordState.Pending
            try {
                authRepository.confirmPasswordReset(oobCode, newPassword)
                _state.value = ResetPasswordState.Success
            } catch (exception: Exception) {
                _state.value = ResetPasswordState.Error(
                    exception.message ?: "An unexpected error occurred while resetting the password"
                )
            }
        }
    }
}