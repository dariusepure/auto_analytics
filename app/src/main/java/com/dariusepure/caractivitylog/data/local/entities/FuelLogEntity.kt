package com.dariusepure.caractivitylog.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.dariusepure.caractivitylog.domain.FuelLog
import java.util.Date

@Entity(
    tableName = "fuel_logs",
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
data class FuelLogEntity(
    @PrimaryKey val id: String,
    val carId: String,
    val date: Date,
    val km: Double,
    val liters: Double,
    val isFullTank: Boolean,
    val mileageLogId: String
)

fun FuelLogEntity.toDomain(): FuelLog = FuelLog(
    id = id,
    date = date,
    km = km,
    liters = liters,
    isFullTank = isFullTank,
    mileageLogId = mileageLogId
)

fun FuelLog.toEntity(carId: String): FuelLogEntity = FuelLogEntity(
    id = id,
    carId = carId,
    date = date,
    km = km,
    liters = liters,
    isFullTank = isFullTank,
    mileageLogId = mileageLogId
)
