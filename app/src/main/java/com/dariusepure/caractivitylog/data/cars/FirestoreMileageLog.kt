package com.dariusepure.caractivitylog.data.cars

import com.dariusepure.caractivitylog.domain.MileageLog
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.Date

@Serializable
data class FirestoreMileageLog(
    @SerialName("id") val id: String = "",
    @SerialName("car_id") val carId: String = "",
    @SerialName("km") val km: Double = 0.0,
    @SerialName("date") val date: Long = System.currentTimeMillis()
)

fun MileageLog.toFirebase() = FirestoreMileageLog(
    id = this.id,
    km = this.km,
    date = this.date.time
)

fun FirestoreMileageLog.fromFirebase() = MileageLog(
    id = this.id,
    km = this.km,
    date = Date(this.date)
)
