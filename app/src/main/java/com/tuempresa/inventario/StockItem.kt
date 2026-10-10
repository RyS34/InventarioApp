package com.tuempresa.inventario.model

data class StockItem(
    val codigo: String,
    val descripcion: String,
    val lote: String,
    var cantidad: Int,
    val fechaIngreso: String,
    val fechaVencimiento: String,
    val ordenCompra: String,
    var ubicacion: String,
    var ubicacionDetallada: String,
    var usuario: String = "" // Cambiado a var
)