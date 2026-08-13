package com.dariusepure.caractivitylog.ui.cars

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dariusepure.caractivitylog.R
import com.dariusepure.caractivitylog.data.cars.CarRepository
import com.dariusepure.caractivitylog.data.prefs.PreferenceRepository
import com.dariusepure.caractivitylog.domain.VehicleInspection
import com.dariusepure.caractivitylog.domain.UnitSystem
import com.dariusepure.caractivitylog.domain.MileageLog
import com.dariusepure.caractivitylog.domain.Car
import com.dariusepure.caractivitylog.domain.FuelLog
import com.dariusepure.caractivitylog.domain.Maintenance
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

sealed class InspectionHistoryUiState {
    object Loading : InspectionHistoryUiState()
    data class Success(
        val inspections: List<VehicleInspection>,
        val fuelLogs: List<FuelLog>,
        val maintenanceLogs: List<Maintenance>,
        val mileageLogs: List<MileageLog>,
        val stats: InspectionStats,
        val unitSystem: UnitSystem
    ) : InspectionHistoryUiState()
    data class Error(val message: String) : InspectionHistoryUiState()
}

data class InspectionStats(
    val latestExpiryDate: Date?,
    val daysRemaining: Long?
)

@HiltViewModel
class InspectionHistoryViewModel @Inject constructor(
    private val carRepository: CarRepository,
    private val preferenceRepository: PreferenceRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _state = MutableStateFlow<InspectionHistoryUiState>(InspectionHistoryUiState.Loading)
    val state = _state.asStateFlow()

    fun loadData(carId: String) {
        viewModelScope.launch {
            _state.value = InspectionHistoryUiState.Loading
            try {
                val carFlow = carRepository.getCarFlow(carId)
                val inspectionsFlow = carRepository.getInspections(carId)
                val fuelFlow = carRepository.getFuelLogs(carId)
                val maintenanceFlow = carRepository.getMaintenanceLogs(carId)
                val mileageFlow = carRepository.getMileageLogs(carId)
                val unitSystemFlow = preferenceRepository.unitSystem

                combine(
                    carFlow, inspectionsFlow, fuelFlow, maintenanceFlow, mileageFlow, unitSystemFlow
                ) { args: Array<Any?> ->
                    val car = args[0] as? Car
                    val inspections = args[1] as List<VehicleInspection>
                    val fuel = args[2] as List<FuelLog>
                    val maintenance = args[3] as List<Maintenance>
                    val mileage = args[4] as List<MileageLog>
                    val unitSystem = args[5] as UnitSystem

                    if (car != null) {
                        val sortedInspections = inspections.sortedByDescending { it.date }
                        val latest = sortedInspections.firstOrNull()
                        val days = latest?.let {
                            val diff = it.expiryDate.time - Date().time
                            diff / (1000 * 60 * 60 * 24)
                        }
                        val stats = InspectionStats(latest?.expiryDate, days)
                        InspectionHistoryUiState.Success(
                            sortedInspections, fuel, maintenance, mileage, stats, unitSystem
                        )
                    } else {
                        InspectionHistoryUiState.Error(context.getString(R.string.error_car_not_found))
                    }
                }.collect {
                    _state.value = it
                }
            } catch (e: Exception) {
                _state.value = InspectionHistoryUiState.Error(e.localizedMessage ?: context.getString(R.string.error_generic))
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
            logs.forEach { log ->
                carRepository.addMileageLog(carId, log)
            }
        }
    }
}
