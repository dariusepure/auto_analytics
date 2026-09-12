package com.dariusepure.caractivitylog.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dariusepure.caractivitylog.data.auth.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {
    private val _signedIn = authRepository.signedIn
    private val _isGuestMode = authRepository.isGuestMode

    val signedIn: StateFlow<Boolean?> = kotlinx.coroutines.flow.combine(
        _signedIn,
        _isGuestMode
    ) { signedIn, isGuest ->
        signedIn || isGuest
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )
}

