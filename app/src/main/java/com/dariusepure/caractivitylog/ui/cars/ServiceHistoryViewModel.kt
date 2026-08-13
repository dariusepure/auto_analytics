package com.dariusepure.caractivitylog.ui.cars

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dariusepure.caractivitylog.R
import com.dariusepure.caractivitylog.data.cars.CarRepository
import com.dariusepure.caractivitylog.data.prefs.PreferenceRepository
import com.dariusepure.caractivitylog.domain.Maintenance
import com.dariusepure.caractivitylog.domain.UnitSystem
import com.dariusepure.caractivitylog.domain.MileageLog
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class ServiceHistoryUiState {
    object Loading : ServiceHistoryUiState()
    data class Success(
        val logs: List<Maintenance>,
        val mileageLogs: List<MileageLog>,
        val stats: ServiceStats,
        val unitSystem: UnitSystem
    ) : ServiceHistoryUiState()
    data class Error(val message: String) : ServiceHistoryUiState()
}

data class ServiceStats(
    val totalServices: Int,
    val lastServiceKm: Double?,
    val averageIntervalKm: Double?
)

@HiltViewModel
class ServiceHistoryViewModel @Inject constructor(
    private val carRepository: CarRepository,
    private val preferenceRepository: PreferenceRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _state = MutableStateFlow<ServiceHistoryUiState>(ServiceHistoryUiState.Loading)
    val state = _state.asStateFlow()

    fun loadData(carId: String) {
        viewModelScope.launch {
            _state.value = ServiceHistoryUiState.Loading
            try {
                val carFlow = carRepository.getCarFlow(carId)
                val maintenanceFlow = carRepository.getMaintenanceLogs(carId)
                val mileageFlow = carRepository.getMileageLogs(carId)
                val unitSystemFlow = preferenceRepository.unitSystem

                combine(carFlow, maintenanceFlow, mileageFlow, unitSystemFlow) { car, logs, mileageLogs, unitSystem ->
                    if (car != null) {
                        val sortedLogs = logs.sortedByDescending { it.date }
                        val stats = calculateStats(sortedLogs)
                        ServiceHistoryUiState.Success(sortedLogs, mileageLogs, stats, unitSystem)
                    } else {
                        ServiceHistoryUiState.Error(context.getString(R.string.error_car_not_found))
                    }
                }.collect {
                    _state.value = it
                }
            } catch (e: Exception) {
                _state.value = ServiceHistoryUiState.Error(e.localizedMessage ?: context.getString(R.string.error_generic))
            }
        }
    }

    private fun calculateStats(logs: List<Maintenance>): ServiceStats {
        if (logs.isEmpty()) return ServiceStats(0, null, null)

        val total = logs.size
        val lastKm = logs.maxByOrNull { it.date }?.km
        
        val intervals = mutableListOf<Double>()
        val sortedAsc = logs.sortedBy { it.date }
        for (i in 0 until sortedAsc.size - 1) {
            val diff = sortedAsc[i + 1].km - sortedAsc[i].km
            if (diff > 0) intervals.add(diff)
        }
        
        val avg = if (intervals.isNotEmpty()) intervals.average() else null
        
        return ServiceStats(total, lastKm, avg)
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
