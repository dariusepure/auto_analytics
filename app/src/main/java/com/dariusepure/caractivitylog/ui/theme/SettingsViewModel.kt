package com.dariusepure.caractivitylog.ui.theme

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dariusepure.caractivitylog.data.auth.AuthRepository
import com.dariusepure.caractivitylog.data.prefs.PreferenceRepository
import com.dariusepure.caractivitylog.domain.Car
import com.dariusepure.caractivitylog.domain.FuelLog
import com.dariusepure.caractivitylog.domain.Insurance
import com.dariusepure.caractivitylog.domain.Maintenance
import com.dariusepure.caractivitylog.domain.MileageLog
import com.dariusepure.caractivitylog.domain.TireSeason
import com.dariusepure.caractivitylog.domain.TireSet
import com.dariusepure.caractivitylog.domain.UnitSystem
import com.dariusepure.caractivitylog.domain.User
import com.dariusepure.caractivitylog.domain.VehicleInspection
import com.dariusepure.caractivitylog.domain.Vignette
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferenceRepository: PreferenceRepository,
    private val authRepository: AuthRepository,
    private val carRepository: com.dariusepure.caractivitylog.data.cars.CarRepository
) : ViewModel() {
    
    val isDarkMode = preferenceRepository.isDarkMode
    val unitSystem = preferenceRepository.unitSystem
    val isGuestMode = preferenceRepository.isGuestMode
    
    val notifyItp = preferenceRepository.notifyItp
    val notifyInsurance = preferenceRepository.notifyInsurance
    val notifyVignette = preferenceRepository.notifyVignette
    
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

    fun setNotifyItp(enabled: Boolean) {
        preferenceRepository.setNotifyItp(enabled)
    }

    fun setNotifyInsurance(enabled: Boolean) {
        preferenceRepository.setNotifyInsurance(enabled)
    }

    fun setNotifyVignette(enabled: Boolean) {
        preferenceRepository.setNotifyVignette(enabled)
    }

    fun signOut() {
        authRepository.signOut()
        preferenceRepository.setGuestMode(false)
    }

    fun changePassword(currentPassword: String, newPassword: String, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                authRepository.reauthenticate(currentPassword)
                authRepository.updatePassword(newPassword)
                _settingsEvent.send(SettingsEvent.PasswordChanged)
            } catch (e: Exception) {
                onError(e.message ?: "Unknown error")
            }
        }
    }

    fun deleteAccount(password: String?, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                if (isPasswordUser && password != null) {
                    authRepository.reauthenticate(password)
                }
                authRepository.deleteAccount()
                _settingsEvent.send(SettingsEvent.AccountDeleted)
            } catch (e: Exception) {
                onError(e.message ?: "Unknown error")
            }
        }
    }
}

sealed class SettingsEvent {
    data object PasswordChanged : SettingsEvent()
    data object AccountDeleted : SettingsEvent()
    data class Error(val message: String) : SettingsEvent()
}
