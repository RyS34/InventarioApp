package com.tuempresa.inventario

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class ProductoTest {

    @Test
    fun testEquals() {
        val producto1 = Producto("123", "Producto A", "Lote 1", "", "", "", 10)
        val producto2 = Producto("123", "Producto A", "Lote 1", "", "", "", 10)
        val producto3 = Producto("456", "Producto B", "Lote 2", "", "", "", 20)

        assertEquals(producto1, producto2)
        assertNotEquals(producto1, producto3)
    }

    @Test
    fun testHashCode() {
        val producto1 = Producto("123", "Producto A", "Lote 1", "", "", "", 10)
        val producto2 = Producto("123", "Producto A", "Lote 1", "", "", "", 10)
        val producto3 = Producto("456", "Producto B", "Lote 2", "", "", "", 20)

        assertEquals(producto1.hashCode(), producto2.hashCode())
        assertNotEquals(producto1.hashCode(), producto3.hashCode())
    }
}