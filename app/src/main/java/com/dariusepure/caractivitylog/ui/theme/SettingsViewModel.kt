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
                sb.append("Mașină,VIN,Dată,Categorie,Detalii,Valoare / KM\n")

                carsList.forEach { car ->
                    val carName = "${car.make} ${car.model}".replace("\"", "\"\"")
                    val vin = car.vin.replace("\"", "\"\"")

                    // Mileage
                    carRepository.getMileageLogs(car.id).first().forEach { log ->
                        val date = log.date.toString()
                        sb.append("\"$carName\",\"$vin\",\"$date\",\"Kilometraj\",\"-\",\"${log.km} km\"\n")
                    }
                    
                    // Fuel
                    carRepository.getFuelLogs(car.id).first().forEach { log ->
                        val date = log.date.toString()
                        val details = "Alimentare: ${log.liters}L${if (log.isFullTank) " (Plin)" else ""}".replace("\"", "\"\"")
                        sb.append("\"$carName\",\"$vin\",\"$date\",\"Combustibil\",\"$details\",\"${log.km} km\"\n")
                    }

                    // Maintenance / Service
                    carRepository.getMaintenanceLogs(car.id).first().forEach { log ->
                        val date = log.date.toString()
                        val desc = log.description.replace("\"", "\"\"")
                        sb.append("\"$carName\",\"$vin\",\"$date\",\"Service\",\"$desc\",\"${log.km} km\"\n")
                    }

                    // Inspections (ITP)
                    carRepository.getInspections(car.id).first().forEach { insp ->
                        val date = insp.date.toString()
                        val expiry = insp.expiryDate.toString()
                        val details = "ITP efectuat (Expiră la: $expiry)".replace("\"", "\"\"")
                        sb.append("\"$carName\",\"$vin\",\"$date\",\"Inspecție Tehnică\",\"$details\",\"${insp.mileage} km\"\n")
                    }

                    // Insurances
                    carRepository.getInsurances(car.id).first().forEach { ins ->
                        val date = ins.date.toString()
                        val expiry = ins.expiryDate.toString()
                        val details = "Asigurare ${ins.provider} (Expiră la: $expiry)".replace("\"", "\"\"")
                        sb.append("\"$carName\",\"$vin\",\"$date\",\"Asigurare\",\"$details\",\"-\"\n")
                    }

                    // Vignettes
                    carRepository.getVignettes(car.id).first().forEach { vig ->
                        val date = vig.date.toString()
                        val expiry = vig.expiryDate.toString()
                        val details = "Rovinietă ${vig.country} (Expiră la: $expiry)".replace("\"", "\"\"")
                        sb.append("\"$carName\",\"$vin\",\"$date\",\"Rovinietă\",\"$details\",\"-\"\n")
                    }

                    // Tire Sets
                    carRepository.getTireSets(car.id).first().forEach { tire ->
                        val details = "Set Anvelope ${tire.season.name}: ${tire.width}/${tire.ratio}R${tire.diameter} - ${tire.brand}".replace("\"", "\"\"")
                        sb.append("\"$carName\",\"$vin\",\"-\",\"Anvelope\",\"$details\",\"-\"\n")
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
