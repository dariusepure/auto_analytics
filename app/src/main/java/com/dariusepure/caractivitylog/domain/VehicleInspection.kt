package com.dariusepure.caractivitylog.domain

import androidx.annotation.StringRes
import com.dariusepure.caractivitylog.R
import java.util.Calendar
import java.util.Date

data class VehicleInspection(
    val id: String = "",
    val date: Date = Date(),
    val mileage: Double = 0.0,
    val durationValue: Int = 1,
    val durationUnit: InspectionDurationUnit = InspectionDurationUnit.YEARS
) {
    val expiryDate: Date
        get() {
            val calendar = Calendar.getInstance()
            calendar.time = date
            when (durationUnit) {
                InspectionDurationUnit.MONTHS -> calendar.add(Calendar.MONTH, durationValue)
                InspectionDurationUnit.YEARS -> calendar.add(Calendar.YEAR, durationValue)
            }
            return calendar.time
        }
}

enum class InspectionDurationUnit(@StringRes val labelRes: Int) {
    MONTHS(R.string.unit_months), YEARS(R.string.unit_years)
}
