package com.dariusepure.caractivitylog.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.dariusepure.caractivitylog.data.local.dao.CarDao
import com.dariusepure.caractivitylog.data.local.dao.FuelLogDao
import com.dariusepure.caractivitylog.data.local.dao.InsuranceDao
import com.dariusepure.caractivitylog.data.local.dao.MaintenanceDao
import com.dariusepure.caractivitylog.data.local.dao.MileageLogDao
import com.dariusepure.caractivitylog.data.local.dao.TireSetDao
import com.dariusepure.caractivitylog.data.local.dao.VehicleInspectionDao
import com.dariusepure.caractivitylog.data.local.dao.VignetteDao
import com.dariusepure.caractivitylog.data.local.entities.CarEntity
import com.dariusepure.caractivitylog.data.local.entities.FuelLogEntity
import com.dariusepure.caractivitylog.data.local.entities.InsuranceEntity
import com.dariusepure.caractivitylog.data.local.entities.MaintenanceEntity
import com.dariusepure.caractivitylog.data.local.entities.MileageLogEntity
import com.dariusepure.caractivitylog.data.local.entities.TireSetEntity
import com.dariusepure.caractivitylog.data.local.entities.VehicleInspectionEntity
import com.dariusepure.caractivitylog.data.local.entities.VignetteEntity

@Database(
    entities = [
        CarEntity::class,
        VehicleInspectionEntity::class,
        FuelLogEntity::class,
        MaintenanceEntity::class,
        MileageLogEntity::class,
        InsuranceEntity::class,
        VignetteEntity::class,
        TireSetEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun carDao(): CarDao
    abstract fun vehicleInspectionDao(): VehicleInspectionDao
    abstract fun fuelLogDao(): FuelLogDao
    abstract fun maintenanceDao(): MaintenanceDao
    abstract fun mileageLogDao(): MileageLogDao
    abstract fun insuranceDao(): InsuranceDao
    abstract fun vignetteDao(): VignetteDao
    abstract fun tireSetDao(): TireSetDao
}
