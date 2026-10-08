package com.tuempresa.inventario

data class Producto(
    var codigo: String,
    var descripcion: String,
    var lote: String,
    var fechaSalida: String,
    var operacion: String,
    var usuario: String,
    var cantidad: Int
)