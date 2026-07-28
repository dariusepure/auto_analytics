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
                val inspections = carRepository.getInspections(car.id).first()
                val latestInspection = inspections.maxByOrNull { it.date } ?: continue
                
                val expiryDate = latestInspection.expiryDate
                val today = Calendar.getInstance().time
                
                val diffInMillis = expiryDate.time - today.time
                val daysLeft = TimeUnit.MILLISECONDS.toDays(diffInMillis).toInt()

                // Check thresholds: 30 days, 7 days, 1 day, 0 days, or expired
                if (daysLeft in listOf(30, 7, 1, 0) || (daysLeft < 0 && daysLeft > -7)) {
                    NotificationHelper.showItpNotification(applicationContext, car.displayName, daysLeft)
                }
            }
            return ListenableWorker.Result.success()
        } catch (e: Exception) {
            return ListenableWorker.Result.retry()
        }
    }
}
