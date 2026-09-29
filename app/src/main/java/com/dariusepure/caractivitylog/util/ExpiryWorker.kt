package com.dariusepure.caractivitylog.util

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.dariusepure.caractivitylog.R
import com.dariusepure.caractivitylog.data.cars.CarRepository
import com.dariusepure.caractivitylog.domain.displayName
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.util.Calendar
import java.util.Date
import java.util.concurrent.TimeUnit

@HiltWorker
class ExpiryWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val carRepository: CarRepository,
    private val preferenceRepository: com.dariusepure.caractivitylog.data.prefs.PreferenceRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        try {
            val cars = carRepository.cars.first()
            val now = Date()
            
            val notifyItp = preferenceRepository.notifyItp.first()
            val notifyInsurance = preferenceRepository.notifyInsurance.first()
            val notifyVignette = preferenceRepository.notifyVignette.first()

            val itpLabel = applicationContext.getString(R.string.common_inspection)
            val insuranceLabel = applicationContext.getString(R.string.car_insurance_title)
            val vignetteLabel = applicationContext.getString(R.string.car_vignette_title)

            cars.forEach { car ->
                // Check Inspections
                if (notifyItp) {
                    val inspections = carRepository.getInspections(car.id).first()
                    inspections.firstOrNull()?.let { inspection ->
                        checkAndNotify(car.displayName, itpLabel, inspection.expiryDate, now)
                    }
                }

                // Check Insurances
                if (notifyInsurance) {
                    val insurances = carRepository.getInsurances(car.id).first()
                    insurances.firstOrNull()?.let { insurance ->
                        checkAndNotify(car.displayName, insuranceLabel, insurance.expiryDate, now)
                    }
                }

                // Check Vignettes
                if (notifyVignette) {
                    val vignettes = carRepository.getVignettes(car.id).first()
                    vignettes.firstOrNull()?.let { vignette ->
                        checkAndNotify(car.displayName, vignetteLabel, vignette.expiryDate, now)
                    }
                }
            }

            return Result.success()
        } catch (e: Exception) {
            return Result.retry()
        }
    }

    private fun checkAndNotify(carName: String, docType: String, expiryDate: Date, now: Date) {
        val diffInMs = expiryDate.time - now.time
        val diffInDays = TimeUnit.MILLISECONDS.toDays(diffInMs)

        if (diffInDays in listOf(30L, 7L, 1L, 0L)) {
            val title = applicationContext.getString(R.string.notification_warning_title, docType)
            val message = when (diffInDays) {
                0L -> applicationContext.getString(R.string.notification_msg_today, docType, carName)
                1L -> applicationContext.getString(R.string.notification_msg_tomorrow, docType, carName)
                else -> applicationContext.getString(R.string.notification_msg_days, docType, carName, diffInDays)
            }
            
            val notificationId = (carName + docType + diffInDays).hashCode()
            NotificationHelper.showNotification(applicationContext, notificationId, title, message)
        }
    }
}
