package com.dariusepure.caractivitylog.features.auth

sealed interface SignInState {
    data object Idle : SignInState
    data object Pending : SignInState
    data class Error(val message: String) : SignInState
}
