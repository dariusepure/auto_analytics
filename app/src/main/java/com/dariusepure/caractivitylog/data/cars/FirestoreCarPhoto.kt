package com.dariusepure.caractivitylog.data.cars

import com.dariusepure.caractivitylog.domain.CarPhoto
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class FirestoreCarPhoto(
    @DocumentId val id: String = "",
    val fileName: String = "",
    @ServerTimestamp val timestamp: Date? = null
) {
    fun toDomain(carId: String) = CarPhoto(
        id = id,
        carId = carId,
        fileName = fileName,
        timestamp = timestamp ?: Date()
    )
}

fun CarPhoto.toFirebase() = mapOf(
    "fileName" to fileName,
    "timestamp" to com.google.firebase.Timestamp(timestamp)
)
