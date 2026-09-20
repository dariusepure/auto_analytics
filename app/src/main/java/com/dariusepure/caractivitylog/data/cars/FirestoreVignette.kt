package com.dariusepure.caractivitylog.data.cars

import com.dariusepure.caractivitylog.domain.InspectionDurationUnit
import com.dariusepure.caractivitylog.domain.Vignette
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.Date

@Serializable
data class FirestoreVignette(
    @SerialName("id") val id: String = "",
    @SerialName("car_id") val carId: String = "",
    @SerialName("date") val date: Long = System.currentTimeMillis(),
    @SerialName("duration_value") val durationValue: Int = 1,
    @SerialName("duration_unit") val durationUnit: String = "MONTHS",
    @SerialName("country") val country: String = ""
)

fun Vignette.toFirebase() = FirestoreVignette(
    id = this.id,
    date = this.date.time,
    durationValue = this.durationValue,
    durationUnit = this.durationUnit.name,
    country = this.country
)

fun FirestoreVignette.fromFirebase() = Vignette(
    id = this.id,
    date = Date(this.date),
    durationValue = this.durationValue,
    durationUnit = try { InspectionDurationUnit.valueOf(this.durationUnit) } catch (e: Exception) { InspectionDurationUnit.MONTHS },
    country = this.country
)
