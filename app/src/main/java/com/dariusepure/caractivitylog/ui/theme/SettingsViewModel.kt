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
import kotlinx.coroutines.flow.first
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

    fun exportDataToCsv() {
        viewModelScope.launch {
            try {
                val carsList = carRepository.cars.first()
                val sb = StringBuilder()
                sb.append("Mașină,VIN,Dată,Tip,Detalii,KM\n")

                carsList.forEach { car ->
                    val mileage = carRepository.getMileageLogs(car.id).first()
                    mileage.forEach { log ->
                        sb.append("${car.make} ${car.model},${car.vin},${log.date},Kilometraj,,${log.km}\n")
                    }
                    
                    val fuel = carRepository.getFuelLogs(car.id).first()
                    fuel.forEach { log ->
                        sb.append("${car.make} ${car.model},${car.vin},${log.date},Combustibil,${log.liters}L,${log.km}\n")
                    }

                    val service = carRepository.getMaintenanceLogs(car.id).first()
                    service.forEach { log ->
                        sb.append("${car.make} ${car.model},${car.vin},${log.date},Service,${log.description},${log.km}\n")
                    }
                }

                _settingsEvent.send(SettingsEvent.DataExported(sb.toString()))
            } catch (e: Exception) {
                _settingsEvent.send(SettingsEvent.Error(e.message ?: "Export failed"))
            }
        }
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
    data class DataExported(val csvContent: String) : SettingsEvent()
    data class Error(val message: String) : SettingsEvent()
}
