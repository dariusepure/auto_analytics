/*
 * Copyright (C) 2026 Darius Epure (Darius DevWorks)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.dariusepure.caractivitylog.ui.cars

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dariusepure.caractivitylog.data.cars.CarRepository
import com.dariusepure.caractivitylog.domain.CarReport
import com.dariusepure.caractivitylog.util.PdfReportGenerator
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class CarReportsViewModel @Inject constructor(
    private val carRepository: CarRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _reports = MutableStateFlow<List<CarReport>>(emptyList())
    val reports: StateFlow<List<CarReport>> = _reports.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating = _isGenerating.asStateFlow()

    fun loadReports(carId: String) {
        viewModelScope.launch {
            carRepository.getCarReports(carId).collect {
                _reports.value = it
            }
        }
    }

    fun generateReport(carId: String) {
        viewModelScope.launch {
            _isGenerating.value = true
            try {
                val car = carRepository.getCar(carId) ?: return@launch
                val mileage = carRepository.getMileageLogs(carId).first()
                val inspections = carRepository.getInspections(carId).first()
                val fuel = carRepository.getFuelLogs(carId).first()
                val tires = carRepository.getTireSets(carId).first()
                val maintenance = carRepository.getMaintenanceLogs(carId).first()
                val insurances = carRepository.getInsurances(carId).first()
                val vignettes = carRepository.getVignettes(carId).first()

                val timestamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.ROOT).format(Date())
                val fileName = "Report_${car.make}_${car.model}_$timestamp.pdf".replace(" ", "_")
                val destFile = File(context.filesDir, "reports/$fileName")
                destFile.parentFile?.mkdirs()

                destFile.outputStream().use { os ->
                    PdfReportGenerator.generateReport(
                        context, car, mileage, inspections, fuel, tires, maintenance, insurances, vignettes, os
                    )
                }

                if (destFile.exists()) {
                    carRepository.addCarReport(carId, CarReport(carId = carId, fileName = fileName))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isGenerating.value = false
            }
        }
    }

    fun deleteReport(carId: String, report: CarReport) {
        viewModelScope.launch {
            try {
                val file = File(context.filesDir, "reports/${report.fileName}")
                if (file.exists()) {
                    file.delete()
                }
                carRepository.deleteCarReport(carId, report.id)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

