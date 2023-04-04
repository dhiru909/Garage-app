package com.justdevelopers.garageapp.activities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "car-table")
data class CarEntity(
    @PrimaryKey(autoGenerate = true)
    var id:Int = 0,
    var model:String="",
    var company:String="",
    var image:String="",
)
