package com.dariusepure.caractivitylog.ui.cars

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dariusepure.caractivitylog.data.cars.CarRepository
import com.dariusepure.caractivitylog.data.prefs.PreferenceRepository
import com.dariusepure.caractivitylog.domain.Car
import com.dariusepure.caractivitylog.domain.FuelLog
import com.dariusepure.caractivitylog.domain.Maintenance
import com.dariusepure.caractivitylog.domain.MileageLog
import com.dariusepure.caractivitylog.domain.VehicleInspection
import com.dariusepure.caractivitylog.domain.UnitSystem
import com.dariusepure.caractivitylog.ui.common.CarFormatters
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MileageStats(
    val currentMileage: Double = 0.0,
    val totalRecords: Int = 0
)

sealed class MileageHistoryUiState {
    object Loading : MileageHistoryUiState()
    data class Success(
        val car: Car,
        val mileageLogs: List<MileageLog>,
        val stats: MileageStats,
        val unitSystem: UnitSystem,
        val fuelLogs: List<FuelLog>,
        val maintenanceLogs: List<Maintenance>,
        val inspections: List<VehicleInspection>
    ) : MileageHistoryUiState()
    data class Error(val message: String) : MileageHistoryUiState()
}

@HiltViewModel
class MileageHistoryViewModel @Inject constructor(
    private val carRepository: CarRepository,
    private val preferenceRepository: PreferenceRepository
) : ViewModel() {

    private val _state = MutableStateFlow<MileageHistoryUiState>(MileageHistoryUiState.Loading)
    val state = _state.asStateFlow()

    fun loadData(carId: String) {
        viewModelScope.launch {
            combine(
                carRepository.getCarFlow(carId),
                carRepository.getMileageLogs(carId),
                carRepository.getFuelLogs(carId),
                carRepository.getMaintenanceLogs(carId),
                carRepository.getInspections(carId),
                preferenceRepository.unitSystem
            ) { args: Array<Any?> ->
                val car = args[0] as? Car
                val logs = args[1] as List<MileageLog>
                val fuel = args[2] as List<FuelLog>
                val maintenance = args[3] as List<Maintenance>
                val inspections = args[4] as List<VehicleInspection>
                val unitSystem = args[5] as UnitSystem

                if (car != null) {
                    val usesMiles = unitSystem == UnitSystem.IMPERIAL
                    val sorted = logs.sortedByDescending { it.date }
                    val currentMileage = sorted.firstOrNull()?.km ?: 0.0
                    val stats = MileageStats(
                        currentMileage = CarFormatters.fromCanonicalDistance(currentMileage, usesMiles),
                        totalRecords = logs.size
                    )
                    MileageHistoryUiState.Success(car, sorted, stats, unitSystem, fuel, maintenance, inspections)
                } else {
                    MileageHistoryUiState.Error("Car not found")
                }
            }.collect {
                _state.value = it
            }
        }
    }

    fun addMileage(carId: String, kmCanonical: Double, date: java.util.Date) {
        viewModelScope.launch {
            carRepository.addMileageLog(carId, MileageLog(km = kmCanonical, date = date))
        }
    }

    fun addBatchMileageLogs(carId: String, logs: List<MileageLog>) {
        viewModelScope.launch {
            logs.forEach { carRepository.addMileageLog(carId, it) }
        }
    }

    fun updateMileage(carId: String, log: MileageLog) {
        viewModelScope.launch {
            carRepository.updateMileageLog(carId, log)
        }
    }

    fun deleteMileage(carId: String, logId: String) {
        viewModelScope.launch {
            carRepository.deleteMileageLog(carId, logId)
        }
    }
}
