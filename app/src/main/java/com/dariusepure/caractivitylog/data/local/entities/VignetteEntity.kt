package com.dariusepure.caractivitylog.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.dariusepure.caractivitylog.domain.InspectionDurationUnit
import com.dariusepure.caractivitylog.domain.Vignette
import java.util.Date

@Entity(
    tableName = "vignettes",
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
data class VignetteEntity(
    @PrimaryKey val id: String,
    val carId: String,
    val date: Date,
    val durationValue: Int,
    val durationUnit: InspectionDurationUnit,
    val country: String
)

fun VignetteEntity.toDomain(): Vignette = Vignette(
    id = id,
    date = date,
    durationValue = durationValue,
    durationUnit = durationUnit,
    country = country
)

fun Vignette.toEntity(carId: String): VignetteEntity = VignetteEntity(
    id = id,
    carId = carId,
    date = date,
    durationValue = durationValue,
    durationUnit = durationUnit,
    country = country
)
