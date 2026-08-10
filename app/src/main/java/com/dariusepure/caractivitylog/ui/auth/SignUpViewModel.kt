/*
 * Copyright (C) 2026 Darius Epure (Darius DevWorks)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.dariusepure.caractivitylog.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dariusepure.caractivitylog.data.auth.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SignUpViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _state = MutableStateFlow<SignUpState>(SignUpState.Idle)
    val state: StateFlow<SignUpState> = _state.asStateFlow()

    val signedIn: StateFlow<Boolean?> =
        authRepository.signedIn.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            null
        )

    fun onSignUp(email: String, password: String, confirmPassword: String, fullName: String, username: String) {
        if (email.isBlank() || password.isBlank() || fullName.isBlank() || username.isBlank()) {
            _state.value = SignUpState.Error("All fields are required")
            return
        }

        if (password != confirmPassword) {
            _state.value = SignUpState.Error("Passwords do not match")
            return
        }

        if (username.length < 3) {
            _state.value = SignUpState.Error("Username must be at least 3 characters")
            return
        }

        viewModelScope.launch {
            _state.value = SignUpState.Pending
            try {
                if (!authRepository.isUsernameAvailable(username)) {
                    _state.value = SignUpState.Error("Username is already taken")
                    return@launch
                }
                authRepository.signUp(email, password, fullName, username)
            } catch (e: Exception) {
                _state.value = SignUpState.Error(e.localizedMessage ?: "An error occurred during sign up")
            }
        }
    }
}

