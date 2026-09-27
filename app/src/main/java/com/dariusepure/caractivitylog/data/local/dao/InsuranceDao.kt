package com.dariusepure.caractivitylog.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.dariusepure.caractivitylog.data.local.entities.InsuranceEntity

@Dao
interface InsuranceDao {
    @Query("SELECT * FROM insurances WHERE carId = :carId ORDER BY date DESC")
    suspend fun getInsurancesForCar(carId: String): List<InsuranceEntity>

    @Query("SELECT * FROM insurances WHERE carId IN (:carIds) ORDER BY date DESC")
    suspend fun getInsurancesForCars(carIds: List<String>): List<InsuranceEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInsurances(items: List<InsuranceEntity>)

    @Query("DELETE FROM insurances WHERE id = :id")
    suspend fun deleteInsurance(id: String)

    @Query("DELETE FROM insurances WHERE carId = :carId")
    suspend fun deleteInsurancesForCar(carId: String)
}
