package com.tuempresa.inventario.data

import androidx.room.Database
import androidx.room.RoomDatabase
import com.tuempresa.inventario.data.dao.ProductoDao
import com.tuempresa.inventario.data.entities.Producto

@Database(entities = [Producto::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun productoDao(): ProductoDao
}