package com.dariusepure.caractivitylog.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.dariusepure.caractivitylog.data.local.entities.MileageLogEntity

@Dao
interface MileageLogDao {
    @Query("SELECT * FROM mileage_logs WHERE carId = :carId ORDER BY date DESC")
    suspend fun getMileageLogsForCar(carId: String): List<MileageLogEntity>

    @Query("SELECT * FROM mileage_logs WHERE carId IN (:carIds) ORDER BY date DESC")
    suspend fun getMileageLogsForCars(carIds: List<String>): List<MileageLogEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMileageLogs(items: List<MileageLogEntity>)

    @Query("DELETE FROM mileage_logs WHERE id = :id")
    suspend fun deleteMileageLog(id: String)

    @Query("DELETE FROM mileage_logs WHERE carId = :carId")
    suspend fun deleteMileageLogsForCar(carId: String)
}
