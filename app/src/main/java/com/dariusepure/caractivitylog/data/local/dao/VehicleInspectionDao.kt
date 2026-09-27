package com.dariusepure.caractivitylog.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.dariusepure.caractivitylog.data.local.entities.VehicleInspectionEntity

@Dao
interface VehicleInspectionDao {
    @Query("SELECT * FROM inspections WHERE carId = :carId ORDER BY date DESC")
    suspend fun getInspectionsForCar(carId: String): List<VehicleInspectionEntity>

    @Query("SELECT * FROM inspections WHERE carId IN (:carIds) ORDER BY date DESC")
    suspend fun getInspectionsForCars(carIds: List<String>): List<VehicleInspectionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInspections(items: List<VehicleInspectionEntity>)

    @Query("DELETE FROM inspections WHERE id = :id")
    suspend fun deleteInspection(id: String)

    @Query("DELETE FROM inspections WHERE carId = :carId")
    suspend fun deleteInspectionsForCar(carId: String)
}
