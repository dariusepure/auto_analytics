package com.dariusepure.caractivitylog.core.data.local.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CarDao {
    @Query("SELECT * FROM cars WHERE userId = :userId ORDER BY updatedAtTimestamp DESC")
    fun getCarsFlow(userId: String): Flow<List<CarEntity>>

    @Query("SELECT * FROM cars WHERE userId = :userId ORDER BY updatedAtTimestamp DESC")
    suspend fun getCars(userId: String): List<CarEntity>

    @Query("SELECT * FROM cars WHERE id = :id LIMIT 1")
    suspend fun getCarById(id: String): CarEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(car: CarEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(cars: List<CarEntity>)

    @Delete
    suspend fun delete(car: CarEntity)

    @Query("DELETE FROM cars WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM cars WHERE userId = :userId")
    suspend fun deleteAllForUser(userId: String)
}
