package com.dariusepure.caractivitylog.ui.cars

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dariusepure.caractivitylog.data.cars.CarRepository
import com.dariusepure.caractivitylog.data.prefs.PreferenceRepository
import com.dariusepure.caractivitylog.domain.Car
import com.dariusepure.caractivitylog.domain.Maintenance
import com.dariusepure.caractivitylog.domain.MileageLog
import com.dariusepure.caractivitylog.domain.UnitSystem
import com.dariusepure.caractivitylog.ui.common.CarFormatters
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

data class ServiceStats(
    val totalServices: Int = 0,
    val lastServiceKm: Double? = null,
    val averageIntervalKm: Double? = null
)

sealed class ServiceHistoryUiState {
    object Loading : ServiceHistoryUiState()
    data class Success(
        val car: Car,
        val logs: List<Maintenance>,
        val stats: ServiceStats,
        val mileageLogs: List<MileageLog>,
        val unitSystem: UnitSystem
    ) : ServiceHistoryUiState()
    data class Error(val message: String) : ServiceHistoryUiState()
}

@HiltViewModel
class ServiceHistoryViewModel @Inject constructor(
    private val carRepository: CarRepository,
    private val preferenceRepository: PreferenceRepository
) : ViewModel() {

    private val _state = MutableStateFlow<ServiceHistoryUiState>(ServiceHistoryUiState.Loading)
    val state = _state.asStateFlow()

    fun loadData(carId: String) {
        viewModelScope.launch {
            combine(
                carRepository.getCarFlow(carId),
                carRepository.getMaintenanceLogs(carId),
                carRepository.getMileageLogs(carId),
                preferenceRepository.unitSystem
            ) { car, logs, mileageLogs, unitSystem ->
                if (car != null) {
                    val usesMiles = unitSystem == UnitSystem.IMPERIAL
                    val stats = calculateStats(logs, usesMiles)
                    ServiceHistoryUiState.Success(car, logs.sortedByDescending { it.date }, stats, mileageLogs, unitSystem)
                } else {
                    ServiceHistoryUiState.Error("Car not found")
                }
            }.collect {
                _state.value = it
            }
        }
    }

    private fun calculateStats(logs: List<Maintenance>, usesMiles: Boolean): ServiceStats {
        if (logs.isEmpty()) return ServiceStats()
        
        val sorted = logs.sortedBy { it.date }
        val lastService = sorted.last()
        
        var avgInterval: Double? = null
        if (sorted.size >= 2) {
            val totalInterval = sorted.last().km - sorted.first().km
            avgInterval = totalInterval / (sorted.size - 1)
        }
        
        return ServiceStats(
            totalServices = logs.size,
            lastServiceKm = CarFormatters.fromCanonicalDistance(lastService.km, usesMiles),
            averageIntervalKm = avgInterval?.let { CarFormatters.fromCanonicalDistance(it, usesMiles) }
        )
    }

    fun addMaintenance(carId: String, log: Maintenance) {
        viewModelScope.launch {
            carRepository.addMaintenanceLog(carId, log)
        }
    }

    fun updateMaintenance(carId: String, log: Maintenance) {
        viewModelScope.launch {
            carRepository.updateMaintenanceLog(carId, log)
        }
    }

    fun deleteMaintenance(carId: String, log: Maintenance) {
        viewModelScope.launch {
            carRepository.deleteMaintenanceLog(carId, log)
        }
    }
}
