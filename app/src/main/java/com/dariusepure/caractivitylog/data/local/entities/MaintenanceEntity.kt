package com.dariusepure.caractivitylog.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.dariusepure.caractivitylog.domain.Maintenance
import java.util.Date

@Entity(
    tableName = "maintenance_logs",
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
data class MaintenanceEntity(
    @PrimaryKey val id: String,
    val carId: String,
    val date: Date,
    val km: Double,
    val description: String,
    val mileageLogId: String,
    val category: String
)

fun MaintenanceEntity.toDomain(): Maintenance = Maintenance(
    id = id,
    date = date,
    km = km,
    description = description,
    mileageLogId = mileageLogId,
    category = category
)

fun Maintenance.toEntity(carId: String): MaintenanceEntity = MaintenanceEntity(
    id = id,
    carId = carId,
    date = date,
    km = km,
    description = description,
    mileageLogId = mileageLogId,
    category = category
)
