package com.tuempresa.inventario.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.tuempresa.inventario.data.entities.Producto

@Dao
interface ProductoDao {
    @Insert
    suspend fun insertar(producto: Producto) // No necesitas un tipo de retorno específico

    @Update
    suspend fun actualizar(producto: Producto) // No necesitas un tipo de retorno específico

    @Delete
    suspend fun eliminar(producto: Producto) // No necesitas un tipo de retorno específico

    @Query("SELECT * FROM productos")
    suspend fun obtenerTodos(): List<Producto> // Retorna una lista de Productos

    @Query("SELECT * FROM productos WHERE id = :id")
    suspend fun obtenerPorId(id: Int): Producto? // Retorna un Producto o null

    @Query("SELECT * FROM productos WHERE codigo_producto = :codigo")
    suspend fun obtenerPorCodigo(codigo: String): Producto? // Retorna un Producto o null
}