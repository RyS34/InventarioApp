package com.tuempresa.inventario

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.tuempresa.inventario.model.StockItem

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
        val tvUbicacionValue = findViewById<MaterialAutoCompleteTextView>(R.id.tvUbicacionValue)
        val tvUbiDetalladaValue = findViewById<MaterialAutoCompleteTextView>(R.id.tvUbiDetalladaValue)
        val tvStockTotalValue = findViewById<TextView>(R.id.tvStockTotalValue)

        // Obtener los botones
        val btnOk = findViewById<Button>(R.id.buttonOk)
        val btnEditar = findViewById<Button>(R.id.buttonEditar)
        val btnBack = findViewById<ImageButton>(R.id.btnBack)

        // Obtener los datos del Intent
        val dataIntent = intent
        val readOnly = dataIntent.getBooleanExtra("readOnly", false)

        val codigo = dataIntent.getStringExtra("codigo")
        val descripcion = dataIntent.getStringExtra("descripcion")
        val lote = dataIntent.getStringExtra("lote")
        val cantidad = dataIntent.getStringExtra("cantidad")
        val fechaIngreso = dataIntent.getStringExtra("fechaIngreso")
        val fechaVencimiento = dataIntent.getStringExtra("fechaVencimiento")
        val ordenCompra = dataIntent.getStringExtra("ordenCompra")
        val ubicacion = dataIntent.getStringExtra("ubicacion")
        val ubiDetallada = dataIntent.getStringExtra("ubiDetallada")

        // Mostrar los datos en los TextViews de valores
        tvCodigoValue.text = codigo
        tvDescripcionValue.text = descripcion
        tvLoteValue.text = lote
        tvCantidadValue.text = cantidad
        tvFechaIngresoValue.text = fechaIngreso
        tvFechaVencimientoValue.text = fechaVencimiento
        tvOrdenCompraValue.text = ordenCompra
        
        // Configurar adaptadores para las ubicaciones SIEMPRE
        val ubicaciones = resources.getStringArray(R.array.Ubicacion)
        val ubicacionesDetalladas = resources.getStringArray(R.array.UbicacionDetallada)
        
        val adapterUbi = ArrayAdapter(this, R.layout.spinner_item, ubicaciones)
        val adapterDet = ArrayAdapter(this, R.layout.spinner_item, ubicacionesDetalladas)
        
        tvUbicacionValue.setAdapter(adapterUbi)
        tvUbiDetalladaValue.setAdapter(adapterDet)

        // Forzar el despliegue al hacer clic
        tvUbicacionValue.setOnClickListener { tvUbicacionValue.showDropDown() }
        tvUbiDetalladaValue.setOnClickListener { tvUbiDetalladaValue.showDropDown() }

        // Cargar los valores actuales sin filtrar la lista
        tvUbicacionValue.setText(ubicacion ?: "", false)
        tvUbiDetalladaValue.setText(ubiDetallada ?: "", false)

        if (readOnly) {
            // En modo lectura ocultamos el botón de modificar pero dejamos que se vea la ubicación
            btnEditar.visibility = View.GONE
        }

        btnBack.setOnClickListener {
            finish()
        }

        // Calcular y mostrar Stock Total
        if (codigo != null) {
            val stockTotal = calcularStockTotal(codigo)
            tvStockTotalValue.text = stockTotal.toString()
        }

        // Configurar el listener del botón OK
        btnOk.setOnClickListener {
            val resultIntent = Intent().apply {
                putExtra("ubicacion", tvUbicacionValue.text.toString())
                putExtra("ubiDetallada", tvUbiDetalladaValue.text.toString())
                putExtra("codigo", tvCodigoValue.text.toString())
                putExtra("lote", tvLoteValue.text.toString())
            }
            setResult(RESULT_OK, resultIntent)
            finish() 
        }

        // Configurar el listener del botón Modificar
        btnEditar.setOnClickListener {
            setResult(RESULT_CANCELED)
            finish()
        }
    }

    private fun calcularStockTotal(codigo: String): Int {
        val sharedPreferences = getSharedPreferences("StockData", MODE_PRIVATE)
        val stockListJson = sharedPreferences.getString("stockList", null)
        if (stockListJson != null) {
            val gson = Gson()
            val type = object : TypeToken<MutableList<StockItem>>() {}.type
            val stockList: List<StockItem> = gson.fromJson(stockListJson, type)

            // Sumar cantidades de todos los items que tengan el mismo código
            return stockList.filter { it.codigo == codigo }.sumOf { it.cantidad }
        }
        return 0
    }
}