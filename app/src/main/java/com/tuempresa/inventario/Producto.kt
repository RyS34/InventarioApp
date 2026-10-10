package com.tuempresa.inventario

data class Producto(
    var codigo: String,
    var descripcion: String,
    var lote: String,
    var fecha: String, // Cambiado de fechaSalida a fecha para uso unificado
    var operacion: String,
    var usuario: String,
    var cantidad: Int
)