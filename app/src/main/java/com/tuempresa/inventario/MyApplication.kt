package com.tuempresa.inventario

import android.app.Application
import androidx.room.Room
import com.tuempresa.inventario.data.AppDatabase

class MyApplication : Application() {
    val db by lazy {
        Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "inventario_db" // Nombre de tu base de datos
        ).build()
    }
}