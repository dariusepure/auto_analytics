package com.dariusepure.caractivitylog.features.auth

sealed interface ResetPasswordState {
    data object Idle : ResetPasswordState
    data object Pending : ResetPasswordState
    data object Success : ResetPasswordState
    data class Error(val message: String) : ResetPasswordState
}
