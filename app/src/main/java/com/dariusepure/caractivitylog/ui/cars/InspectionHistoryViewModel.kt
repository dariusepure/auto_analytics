package com.dariusepure.caractivitylog.ui.cars

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dariusepure.caractivitylog.data.cars.CarRepository
import com.dariusepure.caractivitylog.data.prefs.PreferenceRepository
import com.dariusepure.caractivitylog.domain.Car
import com.dariusepure.caractivitylog.domain.Maintenance
import com.dariusepure.caractivitylog.domain.MileageLog
import com.dariusepure.caractivitylog.domain.VehicleInspection
import com.dariusepure.caractivitylog.domain.FuelLog
import com.dariusepure.caractivitylog.domain.UnitSystem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

data class InspectionStats(
    val latestExpiryDate: Date? = null,
    val daysRemaining: Long? = null
)

sealed class InspectionHistoryUiState {
    object Loading : InspectionHistoryUiState()
    data class Success(
        val car: Car,
        val inspections: List<VehicleInspection>,
        val stats: InspectionStats,
        val mileageLogs: List<MileageLog>,
        val fuelLogs: List<FuelLog>,
        val maintenanceLogs: List<Maintenance>,
        val unitSystem: UnitSystem
    ) : InspectionHistoryUiState()
    data class Error(val message: String) : InspectionHistoryUiState()
}

@HiltViewModel
class InspectionHistoryViewModel @Inject constructor(
    private val carRepository: CarRepository,
    private val preferenceRepository: PreferenceRepository
) : ViewModel() {

    private val _state = MutableStateFlow<InspectionHistoryUiState>(InspectionHistoryUiState.Loading)
    val state = _state.asStateFlow()

    fun loadData(carId: String) {
        viewModelScope.launch {
            combine(
                carRepository.getCarFlow(carId),
                carRepository.getInspections(carId),
                carRepository.getMileageLogs(carId),
                carRepository.getFuelLogs(carId),
                carRepository.getMaintenanceLogs(carId),
                preferenceRepository.unitSystem
            ) { args: Array<Any?> ->
                val car = args[0] as? Car
                val inspections = args[1] as List<VehicleInspection>
                val mileageLogs = args[2] as List<MileageLog>
                val fuelLogs = args[3] as List<FuelLog>
                val maintenanceLogs = args[4] as List<Maintenance>
                val unitSystem = args[5] as UnitSystem

                if (car != null) {
                    val latest = inspections.maxByOrNull { it.date }
                    val stats = InspectionStats(
                        latestExpiryDate = latest?.expiryDate,
                        daysRemaining = latest?.let { (it.expiryDate.time - Date().time) / (1000 * 60 * 60 * 24) }
                    )
                    InspectionHistoryUiState.Success(
                        car, 
                        inspections.sortedByDescending { it.date }, 
                        stats, 
                        mileageLogs,
                        fuelLogs,
                        maintenanceLogs,
                        unitSystem
                    )
                } else {
                    InspectionHistoryUiState.Error("Car not found")
                }
            }.collect {
                _state.value = it
            }
        }
    }

    fun addInspection(carId: String, inspection: VehicleInspection) {
        viewModelScope.launch {
            carRepository.addInspection(carId, inspection)
        }
    }

    fun updateInspection(carId: String, inspection: VehicleInspection) {
        viewModelScope.launch {
            carRepository.updateInspection(carId, inspection)
        }
    }

    fun deleteInspection(carId: String, inspection: VehicleInspection) {
        viewModelScope.launch {
            carRepository.deleteInspection(carId, inspection)
        }
    }

    fun addBatchMileageLogs(carId: String, logs: List<MileageLog>) {
        viewModelScope.launch {
            logs.forEach { carRepository.addMileageLog(carId, it) }
        }
    }
}
