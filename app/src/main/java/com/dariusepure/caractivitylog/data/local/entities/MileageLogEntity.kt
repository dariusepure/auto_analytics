package com.dariusepure.caractivitylog.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.dariusepure.caractivitylog.domain.MileageLog
import java.util.Date

@Entity(
    tableName = "mileage_logs",
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
data class MileageLogEntity(
    @PrimaryKey val id: String,
    val carId: String,
    val km: Double,
    val date: Date
)

fun MileageLogEntity.toDomain(): MileageLog = MileageLog(
    id = id,
    km = km,
    date = date
)

fun MileageLog.toEntity(carId: String): MileageLogEntity = MileageLogEntity(
    id = id,
    carId = carId,
    km = km,
    date = date
)
