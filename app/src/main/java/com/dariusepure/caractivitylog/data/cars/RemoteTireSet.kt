package com.dariusepure.caractivitylog.data.cars

import com.dariusepure.caractivitylog.domain.TireSeason
import com.dariusepure.caractivitylog.domain.TireSet
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class RemoteTireSet(
    @SerialName("id") val id: String = "",
    @SerialName("car_id") val carId: String = "",
    @SerialName("season") val season: String = "SUMMER",
    @SerialName("brand") val brand: String = "",
    @SerialName("width") val width: Int = 0,
    @SerialName("ratio") val ratio: Int = 0,
    @SerialName("diameter") val diameter: Int = 0,
    @SerialName("dot_week") val dotWeek: Int? = null,
    @SerialName("dot_year") val dotYear: Int? = null,
    @SerialName("is_active") val isActive: Boolean = false
)

fun TireSet.toRemote() = RemoteTireSet(
    id = if (this.id.isBlank()) UUID.randomUUID().toString() else this.id,
    season = this.season.name,
    brand = this.brand,
    width = this.width,
    ratio = this.ratio,
    diameter = this.diameter,
    dotWeek = this.dotWeek,
    dotYear = this.dotYear,
    isActive = this.isActive
)

fun RemoteTireSet.fromRemote() = TireSet(
    id = this.id,
    season = try { TireSeason.valueOf(this.season) } catch (e: Exception) { TireSeason.SUMMER },
    brand = this.brand,
    width = this.width,
    ratio = this.ratio,
    diameter = this.diameter,
    dotWeek = this.dotWeek,
    dotYear = this.dotYear,
    isActive = this.isActive
)
