package com.tuempresa.inventario

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputLayout
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import com.tuempresa.inventario.model.StockItem
import java.text.SimpleDateFormat
import java.util.*

class EntradaProductos : AppCompatActivity() {
    private lateinit var stockAdapter: EntradaProductosAdapter
    private lateinit var stockList: MutableList<StockItem>
    private lateinit var etCodigo: EditText
    private lateinit var etDescripcion: EditText
    private lateinit var etLote: EditText
    private lateinit var etUsuario: EditText
    private lateinit var etCantidad: EditText
    private lateinit var etFechaIngreso: EditText
    private lateinit var searchEditText: EditText
    private var allStockItems: MutableList<StockItem> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_entrada_productos)

        // Inicialización de vistas
        etCodigo = findViewById(R.id.etCodigo)
        etDescripcion = findViewById(R.id.etDescripcion)
        etLote = findViewById(R.id.etLote)
        etUsuario = findViewById(R.id.etUsuario)
        etCantidad = findViewById(R.id.etCantidad)
        etFechaIngreso = findViewById(R.id.etFechaIngreso)
        searchEditText = findViewById(R.id.searchEditText)
        val tilCodigoInput = findViewById<TextInputLayout>(R.id.tilCodigo)
        val btnBack = findViewById<ImageButton>(R.id.btnBack)

        btnBack.setOnClickListener {
            finish()
        }

        // Configurar el escáner inteligente (GMS Code Scanner)
        val options = GmsBarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
            .enableAutoZoom()
            .build()
        val scanner = GmsBarcodeScanning.getClient(this, options)

        tilCodigoInput.setEndIconOnClickListener {
            scanner.startScan()
                .addOnSuccessListener { barcode: Barcode ->
                    val rawValue = barcode.rawValue
                    etCodigo.setText(rawValue)
                    // Flujo de foco: tras escanear, ir al usuario
                    etUsuario.requestFocus()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Escaneo cancelado", Toast.LENGTH_SHORT).show()
                }
        }

        // Cargar los datos del stock inicial
        stockList = cargarStockItems()
        allStockItems = stockList.toMutableList()

        // Configurar el RecyclerView
        val rvProductos = findViewById<RecyclerView>(R.id.rvProductos)
        rvProductos.layoutManager = LinearLayoutManager(this)

        stockAdapter = EntradaProductosAdapter(stockList) { selectedItem ->
            // Al seleccionar un item de la lista de búsqueda
            etCodigo.setText(selectedItem.codigo)
            etDescripcion.setText(selectedItem.descripcion)
            etLote.setText(selectedItem.lote)

            // Bloquear edición de campos maestros para integridad
            etCodigo.isEnabled = false
            etDescripcion.isEnabled = false
            etLote.isEnabled = false

            // Mover foco a Usuario para completar la transacción
            etUsuario.requestFocus()
            searchEditText.text.clear()
        }
        rvProductos.adapter = stockAdapter

        // Botón Ingresar
        findViewById<Button>(R.id.btnIngresar).setOnClickListener {
            ejecutarIngreso()
        }

        // Botón Salir
        findViewById<Button>(R.id.btnSalir).setOnClickListener {
            finish()
        }

        // Gestión de foco automática
        etUsuario.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus && etUsuario.text.isNotEmpty()) {
                etCantidad.requestFocus()
            }
        }

        // Inicializar fecha de proceso
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        etFechaIngreso.setText(sdf.format(Date()))
        etFechaIngreso.isEnabled = false

        // Buscador reactivo
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterStockItems(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        searchEditText.requestFocus()
    }

    private fun ejecutarIngreso() {
        val codigo = etCodigo.text.toString()
        val descripcion = etDescripcion.text.toString()
        val lote = etLote.text.toString()
        val usuario = etUsuario.text.toString()
        val cantidad = etCantidad.text.toString().toIntOrNull() ?: 0
        val fechaIngreso = etFechaIngreso.text.toString()

        if (codigo.isEmpty() || usuario.isEmpty() || cantidad <= 0) {
            Toast.makeText(this, "Complete código, usuario y cantidad válida", Toast.LENGTH_SHORT).show()
            return
        }

        val nuevoStockItem = StockItem(
            codigo = codigo,
            descripcion = descripcion,
            lote = lote,
            cantidad = cantidad,
            usuario = usuario,
            fechaVencimiento = "",
            ordenCompra = "",
            ubicacion = "",
            ubicacionDetallada = "",
            fechaIngreso = fechaIngreso
        )

        actualizarStock(nuevoStockItem)
        limpiarFormulario()
    }

    private fun actualizarStock(nuevoItem: StockItem) {
        val index = stockList.indexOfFirst { it.codigo == nuevoItem.codigo && it.lote == nuevoItem.lote }
        if (index != -1) {
            val itemExistente = stockList[index]
            val total = itemExistente.cantidad + nuevoItem.cantidad
            stockList[index] = itemExistente.copy(cantidad = total, usuario = nuevoItem.usuario)
        } else {
            stockList.add(nuevoItem)
        }

        guardarStockItems(stockList)
        allStockItems = stockList.toMutableList()
        filterStockItems(searchEditText.text.toString())
        Toast.makeText(this, "Producto actualizado en stock", Toast.LENGTH_SHORT).show()
    }

    private fun cargarStockItems(): MutableList<StockItem> {
        val prefs = getSharedPreferences("StockData", Context.MODE_PRIVATE)
        val json = prefs.getString("stockList", null) ?: return mutableListOf()
        return try {
            val type = object : TypeToken<MutableList<StockItem>>() {}.type
            Gson().fromJson(json, type)
        } catch (e: Exception) {
            mutableListOf()
        }
    }

    private fun guardarStockItems(lista: MutableList<StockItem>) {
        val prefs = getSharedPreferences("StockData", Context.MODE_PRIVATE)
        prefs.edit().putString("stockList", Gson().toJson(lista)).apply()
    }

    private fun filterStockItems(query: String) {
        val filtered = if (query.isEmpty()) {
            allStockItems
        } else {
            allStockItems.filter { 
                it.codigo.contains(query, true) || it.descripcion.contains(query, true) 
            }
        }
        stockList.clear()
        stockList.addAll(filtered)
        stockAdapter.notifyDataSetChanged()
    }

    private fun limpiarFormulario() {
        etCodigo.text.clear()
        etDescripcion.text.clear()
        etLote.text.clear()
        etCantidad.text.clear()
        etUsuario.text.clear()
        etCodigo.isEnabled = true
        etDescripcion.isEnabled = true
        etLote.isEnabled = true
        searchEditText.requestFocus()
    }
}
