package com.dariusepure.caractivitylog.domain

import java.util.Date

data class CarPhoto(
    val id: String = "",
    val carId: String = "",
    val fileName: String = "", // Filename in internal storage
    val timestamp: Date = Date()
)
