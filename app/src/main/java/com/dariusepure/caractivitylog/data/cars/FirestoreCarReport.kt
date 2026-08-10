/*
 * Copyright (C) 2026 Darius Epure (Darius DevWorks)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.dariusepure.caractivitylog.data.cars

import com.dariusepure.caractivitylog.domain.CarReport
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class FirestoreCarReport(
    @DocumentId val id: String = "",
    val fileName: String = "",
    @ServerTimestamp val date: Date? = null
) {
    fun toDomain(carId: String) = CarReport(
        id = id,
        carId = carId,
        fileName = fileName,
        date = date ?: Date()
    )
}

fun CarReport.toFirebase() = mapOf(
    "fileName" to fileName,
    "date" to com.google.firebase.Timestamp(date)
)

