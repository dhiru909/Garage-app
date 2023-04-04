package com.justdevelopers.garageapp.activities

import android.app.Application

class CarApp:Application() {
    val db by lazy {
        CarDatabase.getInstance(this)
    }
}