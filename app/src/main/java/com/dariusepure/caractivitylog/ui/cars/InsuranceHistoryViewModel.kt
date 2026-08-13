package com.dariusepure.caractivitylog.ui.cars

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dariusepure.caractivitylog.data.cars.CarRepository
import com.dariusepure.caractivitylog.data.prefs.PreferenceRepository
import com.dariusepure.caractivitylog.domain.Car
import com.dariusepure.caractivitylog.domain.Insurance
import com.dariusepure.caractivitylog.domain.UnitSystem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

data class InsuranceStats(
    val latestExpiryDate: Date? = null,
    val daysRemaining: Long? = null
)

sealed class InsuranceHistoryUiState {
    object Loading : InsuranceHistoryUiState()
    data class Success(
        val car: Car,
        val insurances: List<Insurance>,
        val stats: InsuranceStats,
        val unitSystem: UnitSystem
    ) : InsuranceHistoryUiState()
    data class Error(val message: String) : InsuranceHistoryUiState()
}

@HiltViewModel
class InsuranceHistoryViewModel @Inject constructor(
    private val carRepository: CarRepository,
    private val preferenceRepository: PreferenceRepository
) : ViewModel() {

    private val _state = MutableStateFlow<InsuranceHistoryUiState>(InsuranceHistoryUiState.Loading)
    val state = _state.asStateFlow()

    fun loadData(carId: String) {
        viewModelScope.launch {
            combine(
                carRepository.getCarFlow(carId),
                carRepository.getInsurances(carId),
                preferenceRepository.unitSystem
            ) { car, insurances, unitSystem ->
                if (car != null) {
                    val latest = insurances.maxByOrNull { it.date }
                    val stats = InsuranceStats(
                        latestExpiryDate = latest?.expiryDate,
                        daysRemaining = latest?.let { (it.expiryDate.time - Date().time) / (1000 * 60 * 60 * 24) }
                    )
                    InsuranceHistoryUiState.Success(car, insurances.sortedByDescending { it.date }, stats, unitSystem)
                } else {
                    InsuranceHistoryUiState.Error("Car not found")
                }
            }.collect {
                _state.value = it
            }
        }
    }

    fun addInsurance(carId: String, insurance: Insurance) {
        viewModelScope.launch {
            carRepository.addInsurance(carId, insurance)
        }
    }

    fun updateInsurance(carId: String, insurance: Insurance) {
        viewModelScope.launch {
            carRepository.updateInsurance(carId, insurance)
        }
    }

    fun deleteInsurance(carId: String, insuranceId: String) {
        viewModelScope.launch {
            carRepository.deleteInsurance(carId, insuranceId)
        }
    }
}
