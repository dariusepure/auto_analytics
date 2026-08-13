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
class ForgotPasswordViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {
    private val _state = MutableStateFlow<ForgotPasswordState>(ForgotPasswordState.Idle)
    val state: StateFlow<ForgotPasswordState>
        get() = _state

    fun onSendResetEmail(email: String) {
        if (email.isBlank()) {
            _state.value = ForgotPasswordState.Error(context.getString(R.string.validation_email_blank))
            return
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _state.value = ForgotPasswordState.Error(context.getString(R.string.validation_email_invalid))
            return
        }

        viewModelScope.launch {
            _state.value = ForgotPasswordState.Pending
            try {
                authRepository.sendPasswordResetEmail(email)
                _state.value = ForgotPasswordState.Success
            } catch (exception: Exception) {
                _state.value = ForgotPasswordState.Error(
                    exception.message ?: context.getString(R.string.error_reset_email_failed)
                )
            }
        }
    }
}
