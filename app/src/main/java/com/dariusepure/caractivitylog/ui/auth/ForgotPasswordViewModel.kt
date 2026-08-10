/*
 * Copyright (C) 2026 Darius Epure (Darius DevWorks)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.dariusepure.caractivitylog.ui.auth

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dariusepure.caractivitylog.data.auth.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ForgotPasswordViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {
    private val _state = MutableStateFlow<ForgotPasswordState>(ForgotPasswordState.Idle)
    val state: StateFlow<ForgotPasswordState>
        get() = _state

    fun onSendResetEmail(email: String) {
        if (email.isBlank()) {
            _state.value = ForgotPasswordState.Error("Email cannot be blank")
            return
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _state.value = ForgotPasswordState.Error("Invalid email")
            return
        }

        viewModelScope.launch {
            _state.value = ForgotPasswordState.Pending
            try {
                authRepository.sendPasswordResetEmail(email)
                _state.value = ForgotPasswordState.Success
            } catch (exception: Exception) {
                _state.value = ForgotPasswordState.Error(
                    exception.message ?: "An unexpected error occurred while sending the reset email"
                )
            }
        }
    }
    
    fun resetState() {
        _state.value = ForgotPasswordState.Idle
    }
}
