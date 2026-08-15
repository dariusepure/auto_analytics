package com.dariusepure.caractivitylog.domain

import androidx.compose.runtime.Stable
import java.util.Date

@Stable
data class FuelLog(
    val id: String = "",
    val date: Date = Date(),
    val km: Double = 0.0,
    val liters: Double = 0.0,
    val isFullTank: Boolean = true,
    val mileageLogId: String = ""
)

