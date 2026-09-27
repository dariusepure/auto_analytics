package com.dariusepure.caractivitylog.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.dariusepure.caractivitylog.data.local.entities.VignetteEntity

@Dao
interface VignetteDao {
    @Query("SELECT * FROM vignettes WHERE carId = :carId ORDER BY date DESC")
    suspend fun getVignettesForCar(carId: String): List<VignetteEntity>

    @Query("SELECT * FROM vignettes WHERE carId IN (:carIds) ORDER BY date DESC")
    suspend fun getVignettesForCars(carIds: List<String>): List<VignetteEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVignettes(items: List<VignetteEntity>)

    @Query("DELETE FROM vignettes WHERE id = :id")
    suspend fun deleteVignette(id: String)

    @Query("DELETE FROM vignettes WHERE carId = :carId")
    suspend fun deleteVignettesForCar(carId: String)
}
