package com.dariusepure.caractivitylog.ui.cars

sealed interface AddCarState {
    data object Idle : AddCarState
    data object Pending : AddCarState
    data object Success : AddCarState
    data object Scanning : AddCarState
    data class Error(val message: String) : AddCarState
}

