package com.dariusepure.caractivitylog.domain

import java.util.Calendar
import java.util.Date

data class Insurance(
    val id: String = "",
    val date: Date = Date(),
    val durationValue: Int = 6,
    val durationUnit: InspectionDurationUnit = InspectionDurationUnit.MONTHS,
    val provider: String = "",
    val cost: Double = 0.0
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
