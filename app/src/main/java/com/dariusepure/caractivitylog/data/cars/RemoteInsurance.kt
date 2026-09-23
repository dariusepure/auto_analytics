package com.dariusepure.caractivitylog.data.cars

import com.dariusepure.caractivitylog.domain.InspectionDurationUnit
import com.dariusepure.caractivitylog.domain.Insurance
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.Date
import java.util.UUID

@Serializable
data class RemoteInsurance(
    @SerialName("id") val id: String = "",
    @SerialName("car_id") val carId: String = "",
    @SerialName("date") val date: Long = System.currentTimeMillis(),
    @SerialName("duration_value") val durationValue: Int = 1,
    @SerialName("duration_unit") val durationUnit: String = "MONTHS",
    @SerialName("provider") val provider: String = ""
)

fun Insurance.toRemote() = RemoteInsurance(
    id = if (this.id.isBlank()) UUID.randomUUID().toString() else this.id,
    date = this.date.time,
    durationValue = this.durationValue,
    durationUnit = this.durationUnit.name,
    provider = this.provider
)

fun RemoteInsurance.fromRemote() = Insurance(
    id = this.id,
    date = Date(this.date),
    durationValue = this.durationValue,
    durationUnit = try { InspectionDurationUnit.valueOf(this.durationUnit) } catch (e: Exception) { InspectionDurationUnit.MONTHS },
    provider = this.provider
)
