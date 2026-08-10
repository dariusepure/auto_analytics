/*
 * Copyright (C) 2026 Darius Epure (Darius DevWorks)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.dariusepure.caractivitylog.ui.cars

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dariusepure.caractivitylog.data.cars.CarRepository
import com.dariusepure.caractivitylog.domain.Car
import com.dariusepure.caractivitylog.domain.FuelLog
import com.dariusepure.caractivitylog.domain.MileageLog
import com.dariusepure.caractivitylog.ui.common.CarFormatters
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

data class FuelStats(
    val avgConsumption: Double? = null,
    val totalLiters: Double = 0.0,
    val totalDistance: Double = 0.0
)

data class FuelLogWithConsumption(
    val log: FuelLog,
    val consumption: Double? = null
)

sealed class FuelHistoryUiState {
    object Loading : FuelHistoryUiState()
    data class Success(
        val car: Car,
        val logs: List<FuelLogWithConsumption>,
        val stats: FuelStats,
        val mileageLogs: List<MileageLog>
    ) : FuelHistoryUiState()
    data class Error(val message: String) : FuelHistoryUiState()
}

@HiltViewModel
class FuelHistoryViewModel @Inject constructor(
    private val carRepository: CarRepository
) : ViewModel() {

    private val _state = MutableStateFlow<FuelHistoryUiState>(FuelHistoryUiState.Loading)
    val state = _state.asStateFlow()

    fun loadData(carId: String) {
        viewModelScope.launch {
            combine(
                carRepository.getCarFlow(carId),
                carRepository.getFuelLogs(carId),
                carRepository.getMileageLogs(carId)
            ) { car, fuelLogs, mileageLogs ->
                if (car != null) {
                    val usesMiles = europeanCountries.find { it.code == car.plateCountry }?.usesMiles == true
                    val processedLogs = calculateConsumption(fuelLogs, usesMiles)
                    val stats = calculateStats(fuelLogs, usesMiles)
                    FuelHistoryUiState.Success(car, processedLogs, stats, mileageLogs)
                } else {
                    FuelHistoryUiState.Error("Car not found")
                }
            }.collect {
                _state.value = it
            }
        }
    }

    private fun calculateConsumption(logs: List<FuelLog>, usesMiles: Boolean): List<FuelLogWithConsumption> {
        if (logs.isEmpty()) return emptyList()
        
        // Logs are stored in canonical KM
        val sortedLogs = logs.sortedBy { it.date }
        val result = mutableListOf<FuelLogWithConsumption>()
        
        for (i in sortedLogs.indices) {
            val currentLog = sortedLogs[i]
            var consumption: Double? = null
            
            if (currentLog.isFullTank) {
                // Look back for the previous full tank
                var previousFullTankIndex = -1
                for (j in i - 1 downTo 0) {
                    if (sortedLogs[j].isFullTank) {
                        previousFullTankIndex = j
                        break
                    }
                }
                
                if (previousFullTankIndex != -1) {
                    val distKm = currentLog.km - sortedLogs[previousFullTankIndex].km
                    if (distKm > 0) {
                        var litersSum = 0.0
                        for (k in previousFullTankIndex + 1..i) {
                            litersSum += sortedLogs[k].liters
                        }
                        
                        consumption = if (usesMiles) {
                            val miles = CarFormatters.fromCanonicalDistance(distKm, true)
                            val gallonsUK = litersSum / 4.54609
                            if (gallonsUK > 0) miles / gallonsUK else null
                        } else {
                            (litersSum / distKm) * 100
                        }
                    }
                }
            }
            
            result.add(FuelLogWithConsumption(currentLog, consumption))
        }
        
        return result.reversed() // Descending for UI
    }

    private fun calculateStats(logs: List<FuelLog>, usesMiles: Boolean): FuelStats {
        if (logs.isEmpty()) return FuelStats()
        
        val sorted = logs.sortedBy { it.date }
        val totalLiters = logs.sumOf { it.liters }
        
        // Avg consumption overall (from first full tank to last full tank)
        val fullTanks = sorted.filter { it.isFullTank }
        var avgConsumption: Double? = null
        var totalDistanceCanonical = 0.0
        
        if (fullTanks.size >= 2) {
            val firstFull = fullTanks.first()
            val lastFull = fullTanks.last()
            val distKm = lastFull.km - firstFull.km
            if (distKm > 0) {
                totalDistanceCanonical = distKm
                val startIndex = sorted.indexOf(firstFull)
                val endIndex = sorted.indexOf(lastFull)
                val litersInBetween = sorted.subList(startIndex + 1, endIndex + 1).sumOf { it.liters }
                
                avgConsumption = if (usesMiles) {
                    val miles = CarFormatters.fromCanonicalDistance(distKm, true)
                    val gallonsUK = litersInBetween / 4.54609
                    if (gallonsUK > 0) miles / gallonsUK else null
                } else {
                    (litersInBetween / distKm) * 100
                }
            }
        }
        
        return FuelStats(
            avgConsumption = avgConsumption,
            totalLiters = totalLiters,
            totalDistance = CarFormatters.fromCanonicalDistance(totalDistanceCanonical, usesMiles)
        )
    }

    fun addFuelLog(carId: String, kmCanonical: Double, liters: Double, isFullTank: Boolean, date: Date) {
        viewModelScope.launch {
            carRepository.addFuelLog(carId, FuelLog(km = kmCanonical, liters = liters, isFullTank = isFullTank, date = date))
        }
    }

    fun updateFuelLog(carId: String, log: FuelLog) {
        viewModelScope.launch {
            carRepository.updateFuelLog(carId, log)
        }
    }

    fun deleteFuelLog(carId: String, log: FuelLog) {
        viewModelScope.launch {
            carRepository.deleteFuelLog(carId, log)
        }
    }
}

