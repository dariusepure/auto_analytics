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

