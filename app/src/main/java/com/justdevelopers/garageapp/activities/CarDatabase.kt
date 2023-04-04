package com.justdevelopers.garageapp.activities

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
@Database(entities = [CarEntity::class], version = 1, exportSchema = false)
abstract class CarDatabase: RoomDatabase() {
    abstract fun carDao():CarDao

    companion object{

        private var INSTANCE:CarDatabase? = null

        fun getInstance(context: Context):CarDatabase{

            synchronized(this){
                var instance = INSTANCE
                if(instance == null){
                    instance = Room.databaseBuilder(
                        context.applicationContext,
                        CarDatabase::class.java,
                        "car_database"
                    ).fallbackToDestructiveMigration()
                        .build()
                    INSTANCE = instance
                }
                return instance
            }
        }
    }
}