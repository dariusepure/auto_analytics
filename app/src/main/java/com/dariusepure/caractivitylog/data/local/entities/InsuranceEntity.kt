package com.dariusepure.caractivitylog.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.dariusepure.caractivitylog.domain.InspectionDurationUnit
import com.dariusepure.caractivitylog.domain.Insurance
import java.util.Date

@Entity(
    tableName = "insurances",
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
data class InsuranceEntity(
    @PrimaryKey val id: String,
    val carId: String,
    val date: Date,
    val durationValue: Int,
    val durationUnit: InspectionDurationUnit,
    val provider: String
)

fun InsuranceEntity.toDomain(): Insurance = Insurance(
    id = id,
    date = date,
    durationValue = durationValue,
    durationUnit = durationUnit,
    provider = provider
)

fun Insurance.toEntity(carId: String): InsuranceEntity = InsuranceEntity(
    id = id,
    carId = carId,
    date = date,
    durationValue = durationValue,
    durationUnit = durationUnit,
    provider = provider
)
