package com.dariusepure.caractivitylog.domain

import androidx.annotation.StringRes
import com.dariusepure.caractivitylog.R
import java.util.Calendar
import androidx.compose.runtime.Stable
import java.util.Date

@Stable
data class VehicleInspection(
    val id: String = "",
    val date: Date = Date(),
    val mileage: Double = 0.0,
    val durationValue: Int = 1,
    val durationUnit: InspectionDurationUnit = InspectionDurationUnit.YEARS,
    val mileageLogId: String = ""
) {
    val expiryDate: Date
        get() {
            val calendar = Calendar.getInstance()
            calendar.time = date
            when (durationUnit) {
                InspectionDurationUnit.DAYS -> calendar.add(Calendar.DAY_OF_YEAR, durationValue)
                InspectionDurationUnit.MONTHS -> calendar.add(Calendar.MONTH, durationValue)
                InspectionDurationUnit.YEARS -> calendar.add(Calendar.YEAR, durationValue)
            }
            return calendar.time
        }
}

enum class InspectionDurationUnit(@StringRes val labelRes: Int) {
    DAYS(R.string.unit_days), MONTHS(R.string.unit_months), YEARS(R.string.unit_years)
}

