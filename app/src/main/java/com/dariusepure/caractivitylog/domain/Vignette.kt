/*
 * Copyright (C) 2026 Darius Epure (Darius DevWorks)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.dariusepure.caractivitylog.domain

import java.util.Calendar
import java.util.Date

data class Vignette(
    val id: String = "",
    val date: Date = Date(),
    val durationValue: Int = 1,
    val durationUnit: InspectionDurationUnit = InspectionDurationUnit.MONTHS,
    val country: String = ""
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

