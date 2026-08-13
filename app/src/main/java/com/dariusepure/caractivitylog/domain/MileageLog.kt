package com.dariusepure.caractivitylog.domain

import java.util.Date

data class MileageLog(
    val id: String = "",
    val km: Double = 0.0,
    val date: Date = Date()
)

