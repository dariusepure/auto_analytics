package com.dariusepure.caractivitylog.domain

import java.util.Date

data class FuelLog(
    val id: String = "",
    val date: Date = Date(),
    val km: Double = 0.0,
    val liters: Double = 0.0,
    val isFullTank: Boolean = true,
    val mileageLogId: String = ""
)

