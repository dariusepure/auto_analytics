package com.dariusepure.caractivitylog.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.dariusepure.caractivitylog.domain.InspectionDurationUnit
import com.dariusepure.caractivitylog.domain.VehicleInspection
import java.util.Date

@Entity(
    tableName = "inspections",
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
data class VehicleInspectionEntity(
    @PrimaryKey val id: String,
    val carId: String,
    val date: Date,
    val mileage: Double,
    val durationValue: Int,
    val durationUnit: InspectionDurationUnit,
    val mileageLogId: String
)

fun VehicleInspectionEntity.toDomain(): VehicleInspection = VehicleInspection(
    id = id,
    date = date,
    mileage = mileage,
    durationValue = durationValue,
    durationUnit = durationUnit,
    mileageLogId = mileageLogId
)

fun VehicleInspection.toEntity(carId: String): VehicleInspectionEntity = VehicleInspectionEntity(
    id = id,
    carId = carId,
    date = date,
    mileage = mileage,
    durationValue = durationValue,
    durationUnit = durationUnit,
    mileageLogId = mileageLogId
)
