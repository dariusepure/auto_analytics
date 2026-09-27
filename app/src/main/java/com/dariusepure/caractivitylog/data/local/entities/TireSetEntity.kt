package com.dariusepure.caractivitylog.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.dariusepure.caractivitylog.domain.TireSeason
import com.dariusepure.caractivitylog.domain.TireSet

@Entity(
    tableName = "tire_sets",
    foreignKeys = [
        ForeignKey(
            entity = CarEntity::class,
            parentColumns = ["id"],
            childColumns = ["carId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("carId")]
)
data class TireSetEntity(
    @PrimaryKey val id: String,
    val carId: String,
    val season: TireSeason,
    val brand: String,
    val width: Int,
    val ratio: Int,
    val diameter: Int,
    val dotWeek: Int?,
    val dotYear: Int?,
    val isActive: Boolean
)

fun TireSetEntity.toDomain(): TireSet = TireSet(
    id = id,
    season = season,
    brand = brand,
    width = width,
    ratio = ratio,
    diameter = diameter,
    dotWeek = dotWeek,
    dotYear = dotYear,
    isActive = isActive
)

fun TireSet.toEntity(carId: String): TireSetEntity = TireSetEntity(
    id = id,
    carId = carId,
    season = season,
    brand = brand,
    width = width,
    ratio = ratio,
    diameter = diameter,
    dotWeek = dotWeek,
    dotYear = dotYear,
    isActive = isActive
)
