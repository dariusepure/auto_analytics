package com.dariusepure.caractivitylog.data.cars

import com.dariusepure.caractivitylog.domain.InspectionDurationUnit
import com.dariusepure.caractivitylog.domain.VehicleInspection
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.Date

@Serializable
data class FirestoreVehicleInspection(
    @SerialName("id") val id: String = "",
    @SerialName("car_id") val carId: String = "",
    @SerialName("date") val date: Long = System.currentTimeMillis(),
    @SerialName("mileage") val mileage: Double = 0.0,
    @SerialName("duration_value") val durationValue: Int = 1,
    @SerialName("duration_unit") val durationUnit: String = "YEARS",
    @SerialName("mileage_log_id") val mileageLogId: String = ""
)

fun VehicleInspection.toFirebase() = FirestoreVehicleInspection(
    id = this.id,
    date = this.date.time,
    mileage = this.mileage,
    durationValue = this.durationValue,
    durationUnit = this.durationUnit.name,
    mileageLogId = this.mileageLogId
)

fun FirestoreVehicleInspection.fromFirebase() = VehicleInspection(
    id = this.id,
    date = Date(this.date),
    mileage = this.mileage,
    durationValue = this.durationValue,
    durationUnit = try { InspectionDurationUnit.valueOf(this.durationUnit) } catch (e: Exception) { InspectionDurationUnit.YEARS },
    mileageLogId = this.mileageLogId
)
