package com.tuempresa.inventario.data.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "productos")
data class Producto(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "codigo_producto") val codigo: String,
    val descripcion: String,
    val precio: Double,
    val cantidad: Int,
    @ColumnInfo(name = "fecha_ingreso") val fechaingreso: String,
    @ColumnInfo(name = "fecha_vencimiento") val fechavencimiento: String,
    @ColumnInfo(name = "orden_compra") val ordencompra: String,
    val ubicacion: String,
    @ColumnInfo(name = "ubicacion_detallada") val ubicaciondetallada: String,
    val usuario: String
)