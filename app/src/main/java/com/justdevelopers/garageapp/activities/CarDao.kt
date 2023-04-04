package com.justdevelopers.garageapp.activities

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CarDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(carEntity: CarEntity)

    @Update
    suspend fun update(carEntity: CarEntity)

    @Delete
    suspend fun delete(carEntity: CarEntity)

    @Query("Select * from `car-table`")
    fun fetchAllCars():Flow<List<CarEntity>>

    @Query("Select * From `car-table` where id=:id")
    fun fetchCarById(id:Int):Flow<CarEntity>
}