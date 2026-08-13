package com.dariusepure.caractivitylog.ui.cars

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dariusepure.caractivitylog.R
import com.dariusepure.caractivitylog.data.cars.CarRepository
import com.dariusepure.caractivitylog.data.prefs.PreferenceRepository
import com.dariusepure.caractivitylog.domain.MileageLog
import com.dariusepure.caractivitylog.domain.UnitSystem
import com.dariusepure.caractivitylog.domain.FuelLog
import com.dariusepure.caractivitylog.domain.Maintenance
import com.dariusepure.caractivitylog.domain.VehicleInspection
import com.dariusepure.caractivitylog.domain.Car
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

sealed class MileageHistoryUiState {
    object Loading : MileageHistoryUiState()
    data class Success(
        val mileageLogs: List<MileageLog>,
        val fuelLogs: List<FuelLog>,
        val maintenanceLogs: List<Maintenance>,
        val inspections: List<VehicleInspection>,
        val stats: MileageStats,
        val unitSystem: UnitSystem
    ) : MileageHistoryUiState()
    data class Error(val message: String) : MileageHistoryUiState()
}

data class MileageStats(
    val currentMileage: Double,
    val totalRecords: Int
)

@HiltViewModel
class MileageHistoryViewModel @Inject constructor(
    private val carRepository: CarRepository,
    private val preferenceRepository: PreferenceRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _state = MutableStateFlow<MileageHistoryUiState>(MileageHistoryUiState.Loading)
    val state = _state.asStateFlow()

    fun loadData(carId: String) {
        viewModelScope.launch {
            _state.value = MileageHistoryUiState.Loading
            try {
                val carFlow = carRepository.getCarFlow(carId)
                val mileageFlow = carRepository.getMileageLogs(carId)
                val fuelFlow = carRepository.getFuelLogs(carId)
                val maintenanceFlow = carRepository.getMaintenanceLogs(carId)
                val inspectionsFlow = carRepository.getInspections(carId)
                val unitSystemFlow = preferenceRepository.unitSystem

                combine(
                    carFlow, mileageFlow, fuelFlow, maintenanceFlow, inspectionsFlow, unitSystemFlow
                ) { args: Array<Any?> ->
                    val car = args[0] as? Car
                    val logs = args[1] as List<MileageLog>
                    val fuel = args[2] as List<FuelLog>
                    val maintenance = args[3] as List<Maintenance>
                    val inspections = args[4] as List<VehicleInspection>
                    val unitSystem = args[5] as UnitSystem

                    if (car != null) {
                        val sortedLogs = logs.sortedByDescending { it.date }
                        val currentKm = sortedLogs.firstOrNull()?.km ?: 0.0
                        val stats = MileageStats(currentKm, sortedLogs.size)
                        MileageHistoryUiState.Success(
                            sortedLogs, fuel, maintenance, inspections, stats, unitSystem
                        )
                    } else {
                        MileageHistoryUiState.Error(context.getString(R.string.error_car_not_found))
                    }
                }.collect {
                    _state.value = it
                }
            } catch (e: Exception) {
                _state.value = MileageHistoryUiState.Error(e.localizedMessage ?: context.getString(R.string.error_generic))
            }
        }
    }

    fun addMileage(carId: String, km: Double, date: Date) {
        viewModelScope.launch {
            carRepository.addMileageLog(carId, MileageLog(km = km, date = date))
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

    fun addBatchMileageLogs(carId: String, logs: List<MileageLog>) {
        viewModelScope.launch {
            logs.forEach { log ->
                carRepository.addMileageLog(carId, log)
            }
        }
    }
}
