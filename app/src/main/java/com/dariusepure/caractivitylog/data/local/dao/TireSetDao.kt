package com.dariusepure.caractivitylog.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.dariusepure.caractivitylog.data.local.entities.TireSetEntity

@Dao
interface TireSetDao {
    @Query("SELECT * FROM tire_sets WHERE carId = :carId")
    suspend fun getTireSetsForCar(carId: String): List<TireSetEntity>

    @Query("SELECT * FROM tire_sets WHERE carId IN (:carIds)")
    suspend fun getTireSetsForCars(carIds: List<String>): List<TireSetEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTireSets(items: List<TireSetEntity>)

    @Query("DELETE FROM tire_sets WHERE id = :id")
    suspend fun deleteTireSet(id: String)

    @Query("DELETE FROM tire_sets WHERE carId = :carId")
    suspend fun deleteTireSetsForCar(carId: String)
}
