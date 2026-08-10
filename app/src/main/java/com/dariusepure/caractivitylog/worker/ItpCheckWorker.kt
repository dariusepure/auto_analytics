/*
 * Copyright (C) 2026 Darius Epure (Darius DevWorks)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.dariusepure.caractivitylog.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import com.dariusepure.caractivitylog.data.auth.AuthRepository
import com.dariusepure.caractivitylog.data.cars.CarRepository
import com.dariusepure.caractivitylog.domain.displayName
import com.dariusepure.caractivitylog.util.NotificationHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.util.Calendar
import java.util.concurrent.TimeUnit

@HiltWorker
class ItpCheckWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val carRepository: CarRepository,
    private val authRepository: AuthRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): ListenableWorker.Result {
        val uid = authRepository.getUserId()
        if (uid == null || uid == "guest_user") return ListenableWorker.Result.success()

        try {
            val cars = carRepository.cars.first()
            for (car in cars) {
                val today = Calendar.getInstance().time

                // 1. ITP Check
                val inspections = carRepository.getInspections(car.id).first()
                val latestInspection = inspections.maxByOrNull { it.date }
                if (latestInspection != null) {
                    val daysLeft = TimeUnit.MILLISECONDS.toDays(latestInspection.expiryDate.time - today.time).toInt()
                    if (daysLeft in listOf(30, 7, 1, 0) || (daysLeft < 0 && daysLeft > -7)) {
                        NotificationHelper.showItpNotification(applicationContext, car.displayName, daysLeft)
                    }
                }

                // 2. Insurance Check
                val insurances = carRepository.getInsurances(car.id).first()
                val latestInsurance = insurances.maxByOrNull { it.date }
                if (latestInsurance != null) {
                    val daysLeft = TimeUnit.MILLISECONDS.toDays(latestInsurance.expiryDate.time - today.time).toInt()
                    if (daysLeft in listOf(30, 7, 1, 0) || (daysLeft < 0 && daysLeft > -7)) {
                        NotificationHelper.showInsuranceNotification(applicationContext, car.displayName, daysLeft)
                    }
                }

                // 3. Vignette Check
                val vignettes = carRepository.getVignettes(car.id).first()
                val latestVignette = vignettes.maxByOrNull { it.date }
                if (latestVignette != null) {
                    val daysLeft = TimeUnit.MILLISECONDS.toDays(latestVignette.expiryDate.time - today.time).toInt()
                    if (daysLeft in listOf(7, 1, 0) || (daysLeft < 0 && daysLeft > -3)) { // Thresholds shorter for vignettes usually
                        NotificationHelper.showVignetteNotification(applicationContext, car.displayName, daysLeft)
                    }
                }
            }
            return ListenableWorker.Result.success()
        } catch (e: Exception) {
            return ListenableWorker.Result.retry()
        }
    }
}

