/*
 * Copyright (C) 2026 Darius Epure (Darius DevWorks)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.dariusepure.caractivitylog.ui.auth

sealed interface ForgotPasswordState {
    data object Idle : ForgotPasswordState
    data object Pending : ForgotPasswordState
    data object Success : ForgotPasswordState
    data class Error(val message: String) : ForgotPasswordState
}
