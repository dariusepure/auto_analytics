package com.dariusepure.caractivitylog.domain

import androidx.compose.runtime.Stable
import java.util.Date

@Stable
data class MileageLog(
    val id: String = "",
    val km: Double = 0.0,
    val date: Date = Date()
)

