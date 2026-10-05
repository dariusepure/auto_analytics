package com.dariusepure.caractivitylog.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dariusepure.caractivitylog.data.auth.AuthRepository
import com.dariusepure.caractivitylog.data.prefs.PreferenceRepository
import com.dariusepure.caractivitylog.util.NetworkMonitor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val preferenceRepository: PreferenceRepository,
    networkMonitor: NetworkMonitor
) : ViewModel() {
    private val _signedIn = authRepository.signedIn
    private val _isGuestMode = authRepository.isGuestMode

    val isOnline: StateFlow<Boolean> = networkMonitor.isOnline
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val signedIn: StateFlow<Boolean> = combine(
        _signedIn,
        _isGuestMode
    ) { signedIn, isGuest ->
        signedIn || isGuest
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = authRepository.initialSignedIn
    )

    fun setLastNavigation(screenType: String, carId: String) {
        preferenceRepository.setLastNavigation(screenType, carId)
    }

    fun getSavedStartDestination(): String {
        return Screen.CarList.route
    }
}

