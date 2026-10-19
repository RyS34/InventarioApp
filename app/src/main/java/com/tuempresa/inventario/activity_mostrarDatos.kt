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

    private lateinit var tvCodigoValue: TextView
    private lateinit var tvDescripcionValue: TextView
    private lateinit var tvLoteValue: MaterialAutoCompleteTextView
    private lateinit var tvCantidadValue: TextView
    private lateinit var tvFechaIngresoValue: TextView
    private lateinit var tvFechaVencimientoValue: TextView
    private lateinit var tvOrdenCompraValue: TextView
    private lateinit var tvUbicacionValue: MaterialAutoCompleteTextView
    private lateinit var tvUbiDetalladaValue: MaterialAutoCompleteTextView
    private lateinit var tvStockTotalValue: TextView

    private var fullStockList: List<StockItem> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mostrar_datos)

        // Obtener referencias a los TextViews de valores
        tvCodigoValue = findViewById(R.id.tvCodigoValue)
        tvDescripcionValue = findViewById(R.id.tvDescripcionValue)
        tvLoteValue = findViewById(R.id.tvLoteValue)
        tvCantidadValue = findViewById(R.id.tvCantidadValue)
        tvFechaIngresoValue = findViewById(R.id.tvFechaIngresoValue)
        tvFechaVencimientoValue = findViewById(R.id.tvFechaVencimientoValue)
        tvOrdenCompraValue = findViewById(R.id.tvOrdenCompraValue)
        tvUbicacionValue = findViewById(R.id.tvUbicacionValue)
        tvUbiDetalladaValue = findViewById(R.id.tvUbiDetalladaValue)
        tvStockTotalValue = findViewById(R.id.tvStockTotalValue)

        // Obtener los botones
        val btnOk = findViewById<Button>(R.id.buttonOk)
        val btnEditar = findViewById<Button>(R.id.buttonEditar)
        val btnBack = findViewById<ImageButton>(R.id.btnBack)

        // Obtener los datos del Intent
        val dataIntent = intent
        val readOnly = dataIntent.getBooleanExtra("readOnly", false)
        val codigoRecibido = dataIntent.getStringExtra("codigo") ?: ""
        val loteRecibido = dataIntent.getStringExtra("lote") ?: ""

        // Cargar lista completa para permitir cambio de lote
        cargarStockCompleto()

        // Configurar adaptadores para las ubicaciones
        val ubicaciones = resources.getStringArray(R.array.Ubicacion)
        val ubicacionesDetalladas = resources.getStringArray(R.array.UbicacionDetallada)
        val adapterUbi = ArrayAdapter(this, R.layout.spinner_item, ubicaciones)
        val adapterDet = ArrayAdapter(this, R.layout.spinner_item, ubicacionesDetalladas)
        tvUbicacionValue.setAdapter(adapterUbi)
        tvUbiDetalladaValue.setAdapter(adapterDet)

        // Configurar Dropdown de Lotes
        val lotesDisponibles = fullStockList.filter { it.codigo == codigoRecibido }.map { it.lote }
        val adapterLote = ArrayAdapter(this, R.layout.spinner_item, lotesDisponibles)
        tvLoteValue.setAdapter(adapterLote)

        // Listener para cambio de lote
        tvLoteValue.setOnItemClickListener { parent, _, position, _ ->
            val loteSeleccionado = parent.getItemAtPosition(position).toString()
            val item = fullStockList.find { it.codigo == codigoRecibido && it.lote == loteSeleccionado }
            item?.let { actualizarInterfazConItem(it) }
        }

        // Mostrar datos iniciales
        val itemInicial = fullStockList.find { it.codigo == codigoRecibido && it.lote == loteRecibido }
        if (itemInicial != null) {
            actualizarInterfazConItem(itemInicial)
        } else {
            // Fallback si no se encuentra (usar datos del intent)
            tvCodigoValue.text = codigoRecibido
            tvLoteValue.setText(loteRecibido, false)
            tvDescripcionValue.text = dataIntent.getStringExtra("descripcion")
            tvCantidadValue.text = dataIntent.getStringExtra("cantidad")
            tvFechaIngresoValue.text = dataIntent.getStringExtra("fechaIngreso")
            tvFechaVencimientoValue.text = dataIntent.getStringExtra("fechaVencimiento")
            tvOrdenCompraValue.text = dataIntent.getStringExtra("ordenCompra")
            tvUbicacionValue.setText(dataIntent.getStringExtra("ubicacion") ?: "", false)
            tvUbiDetalladaValue.setText(dataIntent.getStringExtra("ubiDetallada") ?: "", false)
            tvStockTotalValue.text = calcularStockTotal(codigoRecibido).toString()
        }

        tvLoteValue.setOnClickListener { tvLoteValue.showDropDown() }
        tvUbicacionValue.setOnClickListener { tvUbicacionValue.showDropDown() }
        tvUbiDetalladaValue.setOnClickListener { tvUbiDetalladaValue.showDropDown() }

        if (readOnly) {
            btnEditar.visibility = View.GONE
        }

        btnBack.setOnClickListener { finish() }

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

        btnEditar.setOnClickListener {
            setResult(RESULT_CANCELED)
            finish()
        }
    }

    private fun cargarStockCompleto() {
        val sharedPreferences = getSharedPreferences("StockData", MODE_PRIVATE)
        val stockListJson = sharedPreferences.getString("stockList", null)
        if (stockListJson != null) {
            val gson = Gson()
            val type = object : TypeToken<MutableList<StockItem>>() {}.type
            fullStockList = gson.fromJson(stockListJson, type) ?: emptyList()
        }
    }

    private fun actualizarInterfazConItem(item: StockItem) {
        tvCodigoValue.text = item.codigo
        tvDescripcionValue.text = item.descripcion
        tvLoteValue.setText(item.lote, false)
        tvCantidadValue.text = item.cantidad.toString()
        tvFechaIngresoValue.text = item.fechaIngreso
        tvFechaVencimientoValue.text = item.fechaVencimiento
        tvOrdenCompraValue.text = item.ordenCompra
        tvUbicacionValue.setText(item.ubicacion, false)
        tvUbiDetalladaValue.setText(item.ubicacionDetallada, false)
        tvStockTotalValue.text = calcularStockTotal(item.codigo).toString()
    }

    private fun calcularStockTotal(codigo: String): Int {
        return fullStockList.filter { it.codigo == codigo }.sumOf { it.cantidad }
    }
}