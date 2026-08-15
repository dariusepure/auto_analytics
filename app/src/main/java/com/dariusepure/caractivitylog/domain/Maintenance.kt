package com.dariusepure.caractivitylog.domain

import androidx.compose.runtime.Stable
import java.util.Date

@Stable
data class Maintenance(
    val id: String = "",
    val date: Date = Date(),
    val km: Double = 0.0,
    val description: String = "",
    val mileageLogId: String = "",
    val category: String = "General"
)

