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
        val type = preferenceRepository.getLastScreenType()
        val carId = preferenceRepository.getLastCarId() ?: ""
        return when (type) {
            "car_details" -> if (carId.isNotBlank()) Screen.CarDetails.createRoute(carId) else Screen.CarList.route
            "mileage_history" -> if (carId.isNotBlank()) Screen.MileageHistory.createRoute(carId) else Screen.CarList.route
            "inspection_history" -> if (carId.isNotBlank()) Screen.InspectionHistory.createRoute(carId) else Screen.CarList.route
            "insurance_history" -> if (carId.isNotBlank()) Screen.InsuranceHistory.createRoute(carId) else Screen.CarList.route
            "vignette_history" -> if (carId.isNotBlank()) Screen.VignetteHistory.createRoute(carId) else Screen.CarList.route
            "tire_history" -> if (carId.isNotBlank()) Screen.TireHistory.createRoute(carId) else Screen.CarList.route
            "service_history" -> if (carId.isNotBlank()) Screen.ServiceHistory.createRoute(carId) else Screen.CarList.route
            "fuel_history" -> if (carId.isNotBlank()) Screen.FuelHistory.createRoute(carId) else Screen.CarList.route
            "technical_sheet" -> if (carId.isNotBlank()) Screen.TechnicalSheet.createRoute(carId) else Screen.CarList.route
            "diagnosis" -> if (carId.isNotBlank()) Screen.Diagnosis.createRoute(carId) else Screen.CarList.route
            "car_reports" -> if (carId.isNotBlank()) Screen.CarReports.createRoute(carId) else Screen.CarList.route
            "settings" -> Screen.Settings.route
            else -> Screen.CarList.route
        }
    }
}

