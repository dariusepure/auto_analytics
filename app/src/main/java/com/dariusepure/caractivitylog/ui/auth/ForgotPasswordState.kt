package com.dariusepure.caractivitylog.ui.auth

sealed interface ForgotPasswordState {
    data object Idle : ForgotPasswordState
    data object Pending : ForgotPasswordState
    data object Success : ForgotPasswordState
    data class Error(val message: String) : ForgotPasswordState
}
