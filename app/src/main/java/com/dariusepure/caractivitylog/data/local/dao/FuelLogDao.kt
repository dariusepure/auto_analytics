package com.dariusepure.caractivitylog.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.dariusepure.caractivitylog.data.local.entities.FuelLogEntity

@Dao
interface FuelLogDao {
    @Query("SELECT * FROM fuel_logs WHERE carId = :carId ORDER BY date DESC")
    suspend fun getFuelLogsForCar(carId: String): List<FuelLogEntity>

    @Query("SELECT * FROM fuel_logs WHERE carId IN (:carIds) ORDER BY date DESC")
    suspend fun getFuelLogsForCars(carIds: List<String>): List<FuelLogEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFuelLogs(items: List<FuelLogEntity>)

    @Query("DELETE FROM fuel_logs WHERE id = :id")
    suspend fun deleteFuelLog(id: String)

    @Query("DELETE FROM fuel_logs WHERE carId = :carId")
    suspend fun deleteFuelLogsForCar(carId: String)
}
