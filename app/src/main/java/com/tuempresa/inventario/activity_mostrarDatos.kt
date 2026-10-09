package com.tuempresa.inventario

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class activity_mostrarDatos : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mostrar_datos)

        // Obtener referencias a los TextViews de valores
        val tvCodigoValue = findViewById<TextView>(R.id.tvCodigoValue)
        val tvDescripcionValue = findViewById<TextView>(R.id.tvDescripcionValue)
        val tvLoteValue = findViewById<TextView>(R.id.tvLoteValue)
        val tvCantidadValue = findViewById<TextView>(R.id.tvCantidadValue)
        val tvFechaIngresoValue = findViewById<TextView>(R.id.tvFechaIngresoValue)
        val tvFechaVencimientoValue = findViewById<TextView>(R.id.tvFechaVencimientoValue)
        val tvOrdenCompraValue = findViewById<TextView>(R.id.tvOrdenCompraValue)

        // Obtener los botones
        val btnOk = findViewById<Button>(R.id.buttonOk)
        val btnEditar = findViewById<Button>(R.id.buttonEditar)
        val btnBack = findViewById<ImageButton>(R.id.btnBack)

        // Obtener los datos del Intent
        val dataIntent = intent
        val readOnly = dataIntent.getBooleanExtra("readOnly", false)

        // Si es solo lectura, ocultamos el botón Modificar
        if (readOnly) {
            btnEditar.visibility = View.GONE
        }

        btnBack.setOnClickListener {
            finish()
        }

        val codigo = dataIntent.getStringExtra("codigo")
        val descripcion = dataIntent.getStringExtra("descripcion")
        val lote = dataIntent.getStringExtra("lote")
        val cantidad = dataIntent.getStringExtra("cantidad")
        val fechaIngreso = dataIntent.getStringExtra("fechaIngreso")
        val fechaVencimiento = dataIntent.getStringExtra("fechaVencimiento")
        val ordenCompra = dataIntent.getStringExtra("ordenCompra")

        // Mostrar los datos en los TextViews de valores
        tvCodigoValue.text = codigo
        tvDescripcionValue.text = descripcion
        tvLoteValue.text = lote
        tvCantidadValue.text = cantidad
        tvFechaIngresoValue.text = fechaIngreso
        tvFechaVencimientoValue.text = fechaVencimiento
        tvOrdenCompraValue.text = ordenCompra

        // Configurar el listener del botón OK
        btnOk.setOnClickListener {
            setResult(RESULT_OK)
            finish() 
        }

        // Configurar el listener del botón Modificar
        btnEditar.setOnClickListener {
            setResult(RESULT_CANCELED)
            finish()
        }
    }
}