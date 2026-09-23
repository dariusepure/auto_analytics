package com.dariusepure.caractivitylog.ui.cars

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dariusepure.caractivitylog.R
import com.dariusepure.caractivitylog.data.cars.CarRepository
import com.dariusepure.caractivitylog.data.prefs.PreferenceRepository
import com.dariusepure.caractivitylog.domain.FuelLog
import com.dariusepure.caractivitylog.domain.UnitSystem
import com.dariusepure.caractivitylog.domain.Car
import com.dariusepure.caractivitylog.domain.Maintenance
import com.dariusepure.caractivitylog.domain.VehicleInspection
import com.dariusepure.caractivitylog.domain.MileageLog
import com.dariusepure.caractivitylog.ui.common.CarFormatters
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

data class FuelLogWithConsumption(
    val log: FuelLog,
    val consumption: Double?
)

data class FuelStats(
    val avgConsumption: Double?,
    val totalDistance: Double,
    val totalLiters: Double
)

sealed class FuelHistoryUiState {
    object Loading : FuelHistoryUiState()
    data class Success(
        val logs: List<FuelLogWithConsumption>,
        val rawFuelLogs: List<FuelLog>,
        val maintenanceLogs: List<Maintenance>,
        val inspections: List<VehicleInspection>,
        val mileageLogs: List<MileageLog>,
        val stats: FuelStats,
        val unitSystem: UnitSystem
    ) : FuelHistoryUiState()
    data class Error(val message: String) : FuelHistoryUiState()
}

@HiltViewModel
class FuelHistoryViewModel @Inject constructor(
    private val carRepository: CarRepository,
    private val preferenceRepository: PreferenceRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _state = MutableStateFlow<FuelHistoryUiState>(FuelHistoryUiState.Loading)
    val state = _state.asStateFlow()

    fun loadData(carId: String) {
        viewModelScope.launch {
            _state.value = FuelHistoryUiState.Loading
            try {
                val carFlow = carRepository.getCarFlow(carId)
                val fuelFlow = carRepository.getFuelLogs(carId)
                val maintenanceFlow = carRepository.getMaintenanceLogs(carId)
                val inspectionsFlow = carRepository.getInspections(carId)
                val mileageFlow = carRepository.getMileageLogs(carId)
                val unitSystemFlow = preferenceRepository.unitSystem

                combine(
                    carFlow, fuelFlow, maintenanceFlow, inspectionsFlow, mileageFlow, unitSystemFlow
                ) { args: Array<Any?> ->
                    val car = args[0] as? Car
                    val fuel = args[1] as List<FuelLog>
                    val maintenance = args[2] as List<Maintenance>
                    val inspections = args[3] as List<VehicleInspection>
                    val mileage = args[4] as List<MileageLog>
                    val unitSystem = args[5] as UnitSystem

                    if (car != null) {
                        val sortedFuel = fuel.sortedByDescending { it.date }
                        val logsWithCons = calculateConsumptions(sortedFuel, unitSystem)
                        val stats = calculateStats(logsWithCons, unitSystem)
                        FuelHistoryUiState.Success(
                            logsWithCons, sortedFuel, maintenance, inspections, mileage, stats, unitSystem
                        )
                    } else {
                        FuelHistoryUiState.Error(context.getString(R.string.error_car_not_found))
                    }
                }.collect {
                    _state.value = it
                }
            } catch (e: Exception) {
                _state.value = FuelHistoryUiState.Error(e.localizedMessage ?: context.getString(R.string.error_generic))
            }
        }
    }

    private fun calculateConsumptions(logs: List<FuelLog>, unitSystem: UnitSystem): List<FuelLogWithConsumption> {
        val result = mutableListOf<FuelLogWithConsumption>()
        val usesMiles = unitSystem == UnitSystem.IMPERIAL
        
        for (i in logs.indices) {
            val current = logs[i]
            val nextFull = logs.drop(i + 1).firstOrNull { it.isFullTank }
            
            val consumption = if (current.isFullTank && nextFull != null) {
                val dist = current.km - nextFull.km
                if (dist > 0) {
                    val liters = logs.subList(i, logs.indexOf(nextFull)).sumOf { it.liters }
                    CarFormatters.calculateConsumption(liters, dist, usesMiles)
                } else null
            } else null
            
            result.add(FuelLogWithConsumption(current, consumption))
        }
        return result
    }

    private fun calculateStats(logs: List<FuelLogWithConsumption>, unitSystem: UnitSystem): FuelStats {
        val fullTankLogs = logs.filter { it.log.isFullTank }
        val avg = if (fullTankLogs.size >= 2) {
            val latest = fullTankLogs.first()
            val oldest = fullTankLogs.last()
            val totalDist = latest.log.km - oldest.log.km
            val totalLiters = logs.subList(logs.indexOf(latest), logs.indexOf(oldest)).sumOf { it.log.liters }
            if (totalDist > 0) CarFormatters.calculateConsumption(totalLiters, totalDist, unitSystem == UnitSystem.IMPERIAL) else null
        } else null

        val totalDist = if (logs.isNotEmpty()) logs.first().log.km - logs.last().log.km else 0.0
        val totalLiters = logs.sumOf { it.log.liters }
        
        return FuelStats(
            avgConsumption = avg,
            totalDistance = CarFormatters.fromCanonicalDistance(totalDist, unitSystem == UnitSystem.IMPERIAL),
            totalLiters = CarFormatters.fromCanonicalVolume(totalLiters, unitSystem == UnitSystem.IMPERIAL)
        )
    }

    fun addFuelLog(carId: String, km: Double, liters: Double, isFullTank: Boolean, date: Date, usesMiles: Boolean) {
        viewModelScope.launch {
            try {
                val canonicalLiters = CarFormatters.toCanonicalVolume(liters, usesMiles)
                val log = FuelLog(km = km, liters = canonicalLiters, isFullTank = isFullTank, date = date)
                carRepository.addFuelLog(carId, log)
            } catch (e: Exception) {
                _state.value = FuelHistoryUiState.Error(e.localizedMessage ?: context.getString(R.string.error_generic))
            }
        }
    }

    fun updateFuelLog(carId: String, log: FuelLog, litersInput: Double, usesMiles: Boolean) {
        viewModelScope.launch {
            try {
                val canonicalLiters = CarFormatters.toCanonicalVolume(litersInput, usesMiles)
                carRepository.updateFuelLog(carId, log.copy(liters = canonicalLiters))
            } catch (e: Exception) {
                _state.value = FuelHistoryUiState.Error(e.localizedMessage ?: context.getString(R.string.error_generic))
            }
        }
    }

    fun deleteFuelLog(carId: String, log: FuelLog) {
        viewModelScope.launch {
            try {
                carRepository.deleteFuelLog(carId, log)
            } catch (e: Exception) {
                _state.value = FuelHistoryUiState.Error(e.localizedMessage ?: context.getString(R.string.error_generic))
            }
        }
    }
}
