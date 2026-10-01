package com.dariusepure.caractivitylog.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.dariusepure.caractivitylog.data.local.entities.CarEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CarDao {
    @Query("SELECT * FROM cars WHERE userId = :userId")
    suspend fun getCarsForUser(userId: String): List<CarEntity>

    @Query("SELECT * FROM cars")
    suspend fun getAllCars(): List<CarEntity>

    @Query("SELECT * FROM cars WHERE id = :id")
    suspend fun getCarById(id: String): CarEntity?

    @Query("SELECT * FROM cars WHERE userId = :userId")
    fun observeCarsForUser(userId: String): Flow<List<CarEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCars(cars: List<CarEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCar(car: CarEntity)

    @Query("DELETE FROM cars WHERE id = :id")
    suspend fun deleteCar(id: String)

    @Query("DELETE FROM cars WHERE userId = :userId")
    suspend fun deleteCarsForUser(userId: String)

    @Query("UPDATE cars SET userId = :targetUserId WHERE userId = :sourceUserId")
    suspend fun updateUserId(sourceUserId: String, targetUserId: String)
}
