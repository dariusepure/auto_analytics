package com.dariusepure.caractivitylog.domain

import java.util.Date

data class CarReport(
    val id: String = "",
    val carId: String = "",
    val fileName: String = "", // Filename in internal storage
    val date: Date = Date()
)

