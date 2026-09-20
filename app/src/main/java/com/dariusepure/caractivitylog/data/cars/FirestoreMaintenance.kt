package com.dariusepure.caractivitylog.data.cars

import com.dariusepure.caractivitylog.domain.Maintenance
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.Date

@Serializable
data class FirestoreMaintenance(
    @SerialName("id") val id: String = "",
    @SerialName("car_id") val carId: String = "",
    @SerialName("date") val date: Long = System.currentTimeMillis(),
    @SerialName("km") val km: Double = 0.0,
    @SerialName("description") val description: String = "",
    @SerialName("mileage_log_id") val mileageLogId: String = "",
    @SerialName("category") val category: String = "General"
)

fun Maintenance.toFirebase() = FirestoreMaintenance(
    id = this.id,
    date = this.date.time,
    km = this.km,
    description = this.description,
    mileageLogId = this.mileageLogId,
    category = this.category
)

fun FirestoreMaintenance.fromFirebase() = Maintenance(
    id = this.id,
    date = Date(this.date),
    km = this.km,
    description = this.description,
    mileageLogId = this.mileageLogId,
    category = this.category
)
