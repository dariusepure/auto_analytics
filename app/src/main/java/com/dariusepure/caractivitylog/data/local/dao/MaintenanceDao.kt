package com.dariusepure.caractivitylog.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.dariusepure.caractivitylog.data.local.entities.MaintenanceEntity

@Dao
interface MaintenanceDao {
    @Query("SELECT * FROM maintenance_logs WHERE carId = :carId ORDER BY date DESC")
    suspend fun getMaintenanceLogsForCar(carId: String): List<MaintenanceEntity>

    @Query("SELECT * FROM maintenance_logs WHERE carId IN (:carIds) ORDER BY date DESC")
    suspend fun getMaintenanceLogsForCars(carIds: List<String>): List<MaintenanceEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMaintenanceLogs(items: List<MaintenanceEntity>)

    @Query("DELETE FROM maintenance_logs WHERE id = :id")
    suspend fun deleteMaintenanceLog(id: String)

    @Query("DELETE FROM maintenance_logs WHERE carId = :carId")
    suspend fun deleteMaintenanceLogsForCar(carId: String)
}
