package com.dariusepure.caractivitylog.data.cars

import com.dariusepure.caractivitylog.domain.CarReport
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.Date

@Serializable
data class FirestoreCarReport(
    @SerialName("id") val id: String = "",
    @SerialName("car_id") val carId: String = "",
    @SerialName("file_name") val fileName: String = "",
    @SerialName("date") val date: Long? = null
) {
    fun toDomain(carId: String) = CarReport(
        id = id,
        carId = carId,
        fileName = fileName,
        date = if (date != null) Date(date) else Date()
    )
}

fun CarReport.toFirebase() = FirestoreCarReport(
    id = this.id,
    carId = this.carId,
    fileName = this.fileName,
    date = this.date.time
)
