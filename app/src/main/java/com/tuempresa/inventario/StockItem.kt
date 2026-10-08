package com.tuempresa.inventario.model

data class StockItem(
    val codigo: String,
    val descripcion: String,
    val lote: String,
    var cantidad: Int,
    val fechaIngreso: String,
    val fechaVencimiento: String,
    val ordenCompra: String,
    val ubicacion: String,
    val ubicacionDetallada: String,
    var usuario: String = "" // Cambiado a var
)