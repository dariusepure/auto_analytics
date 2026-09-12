package com.dariusepure.caractivitylog.ui.theme

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dariusepure.caractivitylog.data.auth.AuthRepository
import com.dariusepure.caractivitylog.data.prefs.PreferenceRepository
import com.dariusepure.caractivitylog.domain.UnitSystem
import com.dariusepure.caractivitylog.domain.User
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferenceRepository: PreferenceRepository,
    private val authRepository: AuthRepository
) : ViewModel() {
    
    val isDarkMode = preferenceRepository.isDarkMode
    val unitSystem = preferenceRepository.unitSystem
    val isGuestMode = preferenceRepository.isGuestMode
    val userEmail = authRepository.currentUserEmail
    val isAnonymous = authRepository.isCurrentlyGuest || authRepository.isAnonymous

    val userData: StateFlow<User?> = (authRepository.getUserId()?.let { authRepository.getUserData(it) } ?: flowOf(null))
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    fun toggleTheme(currentDark: Boolean) {
        preferenceRepository.setDarkMode(!currentDark)
    }

    fun setDarkMode(enabled: Boolean?) {
        preferenceRepository.setDarkMode(enabled)
    }

    fun setUnitSystem(system: UnitSystem) {
        preferenceRepository.setUnitSystem(system)
    }

    fun signOut() {
        authRepository.signOut()
        preferenceRepository.setGuestMode(false)
    }
}
