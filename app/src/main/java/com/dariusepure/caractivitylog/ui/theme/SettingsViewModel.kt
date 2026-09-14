package com.dariusepure.caractivitylog.ui.theme

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dariusepure.caractivitylog.data.auth.AuthRepository
import com.dariusepure.caractivitylog.data.prefs.PreferenceRepository
import com.dariusepure.caractivitylog.domain.UnitSystem
import com.dariusepure.caractivitylog.domain.User
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferenceRepository: PreferenceRepository,
    private val authRepository: AuthRepository
) : ViewModel() {
    
    val isDarkMode = preferenceRepository.isDarkMode
    val unitSystem = preferenceRepository.unitSystem
    val isGuestMode = preferenceRepository.isGuestMode
    
    val userEmail: StateFlow<String?> = authRepository.userEmailFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), authRepository.currentUserEmail)
        
    val isAnonymous: StateFlow<Boolean> = authRepository.isAnonymousFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), authRepository.isCurrentlyGuest || authRepository.isAnonymous)

    val isPasswordUser = authRepository.isPasswordUser()

    private val _settingsEvent = Channel<SettingsEvent>()
    val settingsEvent = _settingsEvent.receiveAsFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val userData: StateFlow<User?> = authRepository.userId
        .flatMapLatest { uid ->
            if (uid != null) authRepository.getUserData(uid)
            else flowOf(null)
        }
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

    fun changePassword(currentPassword: String, newPassword: String) {
        viewModelScope.launch {
            try {
                authRepository.reauthenticate(currentPassword)
                authRepository.updatePassword(newPassword)
                _settingsEvent.send(SettingsEvent.PasswordChanged)
            } catch (e: Exception) {
                _settingsEvent.send(SettingsEvent.Error(e.message ?: "Unknown error"))
            }
        }
    }

    fun deleteAccount(password: String?) {
        viewModelScope.launch {
            try {
                if (isPasswordUser && password != null) {
                    authRepository.reauthenticate(password)
                }
                authRepository.deleteAccount()
                _settingsEvent.send(SettingsEvent.AccountDeleted)
            } catch (e: Exception) {
                _settingsEvent.send(SettingsEvent.Error(e.message ?: "Unknown error"))
            }
        }
    }
}

sealed class SettingsEvent {
    data object PasswordChanged : SettingsEvent()
    data object AccountDeleted : SettingsEvent()
    data class Error(val message: String) : SettingsEvent()
}
