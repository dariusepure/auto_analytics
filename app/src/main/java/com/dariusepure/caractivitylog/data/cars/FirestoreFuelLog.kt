package com.dariusepure.caractivitylog.data.cars

import com.dariusepure.caractivitylog.domain.FuelLog
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.Date

@Serializable
data class FirestoreFuelLog(
    @SerialName("id") val id: String = "",
    @SerialName("car_id") val carId: String = "",
    @SerialName("date") val date: Long = System.currentTimeMillis(),
    @SerialName("km") val km: Double = 0.0,
    @SerialName("liters") val liters: Double = 0.0,
    @SerialName("is_full_tank") val isFullTank: Boolean = true,
    @SerialName("mileage_log_id") val mileageLogId: String = ""
)

fun FuelLog.toFirebase() = FirestoreFuelLog(
    id = this.id,
    date = this.date.time,
    km = this.km,
    liters = this.liters,
    isFullTank = this.isFullTank,
    mileageLogId = this.mileageLogId
)

fun FirestoreFuelLog.fromFirebase() = FuelLog(
    id = this.id,
    date = Date(this.date),
    km = this.km,
    liters = this.liters,
    isFullTank = this.isFullTank,
    mileageLogId = this.mileageLogId
)
