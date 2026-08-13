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

@HiltViewModel
class ResetPasswordViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {
    private val _state = MutableStateFlow<ResetPasswordState>(ResetPasswordState.Idle)
    val state: StateFlow<ResetPasswordState>
        get() = _state

    fun onConfirmReset(oobCode: String, newPassword: String, confirmPassword: String) {
        if (newPassword.isBlank()) {
            _state.value = ResetPasswordState.Error(context.getString(R.string.validation_password_blank))
            return
        }
        if (newPassword != confirmPassword) {
            _state.value = ResetPasswordState.Error(context.getString(R.string.validation_passwords_mismatch))
            return
        }

        viewModelScope.launch {
            _state.value = ResetPasswordState.Pending
            try {
                authRepository.confirmPasswordReset(oobCode, newPassword)
                _state.value = ResetPasswordState.Success
            } catch (exception: Exception) {
                _state.value = ResetPasswordState.Error(
                    exception.message ?: context.getString(R.string.error_password_reset_failed)
                )
            }
        }
    }
}
