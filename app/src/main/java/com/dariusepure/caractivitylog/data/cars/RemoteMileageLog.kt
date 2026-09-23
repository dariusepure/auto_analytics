package com.dariusepure.caractivitylog.data.cars

import com.dariusepure.caractivitylog.domain.MileageLog
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.Date
import java.util.UUID

@Serializable
data class RemoteMileageLog(
    @SerialName("id") val id: String = "",
    @SerialName("car_id") val carId: String = "",
    @SerialName("mileage") val km: Double = 0.0,
    @SerialName("date") val date: Long = System.currentTimeMillis()
)

fun MileageLog.toRemote() = RemoteMileageLog(
    id = if (this.id.isBlank()) UUID.randomUUID().toString() else this.id,
    km = this.km,
    date = this.date.time
)

fun RemoteMileageLog.fromRemote() = MileageLog(
    id = this.id,
    km = this.km,
    date = Date(this.date)
)
