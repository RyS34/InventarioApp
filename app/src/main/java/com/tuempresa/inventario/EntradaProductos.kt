package com.tuempresa.inventario

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.activity.result.contract.ActivityResultContracts
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import com.tuempresa.inventario.model.StockItem
import java.text.SimpleDateFormat
import java.util.*
import com.tuempresa.inventario.databinding.ActivityEntradaProductosBinding

class EntradaProductos : AppCompatActivity() {
    private lateinit var binding: ActivityEntradaProductosBinding
    private var allStockItems: MutableList<StockItem> = mutableListOf()
    private lateinit var adaptadorProductosBusqueda: ProductosBusquedaAdapter
    private var tempStockItem: StockItem? = null

    private val mostrarDatosLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val data = result.data
            tempStockItem?.let { item ->
                // Actualizar ubicación si el usuario la cambió en la pantalla de resumen
                val nuevaUbicacion = data?.getStringExtra("ubicacion") ?: item.ubicacion
                val nuevaUbiDetallada = data?.getStringExtra("ubiDetallada") ?: item.ubicacionDetallada
                
                val itemFinal = item.copy(
                    ubicacion = nuevaUbicacion,
                    ubicacionDetallada = nuevaUbiDetallada
                )
                
                actualizarStock(itemFinal)
                limpiarFormularioCompleto()
                
                AlertDialog.Builder(this)
                    .setTitle("¡Éxito!")
                    .setMessage("Entrada de producto registrada correctamente.")
                    .setPositiveButton("Aceptar", null)
                    .show()
            }
        }
        tempStockItem = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEntradaProductosBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inicialización de UI
        binding.btnBack.setOnClickListener {
            finish()
        }

        // Configurar el RecyclerView de búsqueda
        adaptadorProductosBusqueda = ProductosBusquedaAdapter(mutableListOf()) { selectedItem ->
            seleccionarProducto(selectedItem)
        }
        binding.rvProductosBusqueda.layoutManager = LinearLayoutManager(this)
        binding.rvProductosBusqueda.adapter = adaptadorProductosBusqueda

        // Configurar el escáner inteligente (GMS Code Scanner)
        val options = GmsBarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
            .enableAutoZoom()
            .build()
        val scanner = GmsBarcodeScanning.getClient(this, options)

        binding.btnEscanear.setOnClickListener {
            scanner.startScan()
                .addOnSuccessListener { barcode: Barcode ->
                    val rawValue = barcode.rawValue ?: ""
                    binding.searchEditText.setText(rawValue)
                    filterStockItems(rawValue, autoSelectIfExactMatch = true)
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Escaneo cancelado", Toast.LENGTH_SHORT).show()
                }
        }

        // Cargar los datos del stock inicial
        recargarDatosDesdeStock()

        // Botón Ingresar
        binding.btnIngresar.setOnClickListener {
            ejecutarIngreso()
        }

        // Botón Salir
        binding.btnSalir.setOnClickListener {
            finish()
        }

        binding.btnLimpiar.setOnClickListener {
            limpiarFormularioCompleto()
        }

        // Gestión de foco automática
        binding.etUsuario.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus && binding.etUsuario.text?.isNotEmpty() == true) {
                binding.etCantidad.requestFocus()
            }
        }

        // Inicializar fecha de proceso
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        binding.etFechaIngreso.setText(sdf.format(Date()))
        binding.etFechaIngreso.isEnabled = false

        // Buscador reactivo
        binding.searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterStockItems(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.searchEditText.requestFocus()
    }

    private fun recargarDatosDesdeStock() {
        val prefs = getSharedPreferences("StockData", Context.MODE_PRIVATE)
        val json = prefs.getString("stockList", null)
        if (json != null) {
            val type = object : TypeToken<MutableList<StockItem>>() {}.type
            allStockItems = Gson().fromJson(json, type) ?: mutableListOf()
            adaptadorProductosBusqueda.updateList(allStockItems)
        } else {
            allStockItems = mutableListOf()
            adaptadorProductosBusqueda.updateList(allStockItems)
        }
    }
    // Variables para almacenar el producto seleccionado y la cantidad ingresada
    private var selectedStockItem: StockItem? = null

    // Seleccionar un producto de la lista de búsqueda y actualizar los campos
    private fun seleccionarProducto(item: StockItem) {
        selectedStockItem = item
        binding.etCodigo.setText(item.codigo)
        binding.etDescripcion.setText(item.descripcion)
        binding.etLote.setText(item.lote)

        // Limpiar búsqueda para resaltar el item
        if (!binding.searchEditText.text.isNullOrEmpty()) {
            binding.searchEditText.text?.clear()
            val newPos = adaptadorProductosBusqueda.items.indexOfFirst {
                it.codigo == item.codigo && it.lote == item.lote
            }
            if (newPos != RecyclerView.NO_POSITION) {
                adaptadorProductosBusqueda.selectedPosition = newPos
                adaptadorProductosBusqueda.notifyDataSetChanged()
                binding.rvProductosBusqueda.post {
                    binding.rvProductosBusqueda.scrollToPosition(newPos)
                }
            }
        }

        binding.etUsuario.requestFocus()
    }
    // Ejecutar la entrada de producto y lanzar la pantalla de resumen con los datos ingresados
    private fun ejecutarIngreso() {
        val codigo = binding.etCodigo.text.toString()
        val descripcion = binding.etDescripcion.text.toString()
        val lote = binding.etLote.text.toString()
        val usuario = binding.etUsuario.text.toString()
        val cantidadStr = binding.etCantidad.text.toString()
        val cantidad = cantidadStr.toIntOrNull() ?: 0
        val fechaIngreso = binding.etFechaIngreso.text.toString()

        if (codigo.isEmpty() || usuario.isEmpty() || cantidad <= 0) {
            Toast.makeText(this, "Complete código, usuario y cantidad válida", Toast.LENGTH_SHORT).show()
            return
        }

        // Si el producto fue seleccionado de la búsqueda, mantenemos sus datos originales (vencimiento, etc)
        // Si es un código nuevo, se usan valores por defecto
        val stockItem = StockItem(
            codigo = codigo,
            descripcion = descripcion,
            lote = lote,
            cantidad = cantidad,
            usuario = usuario,
            fechaVencimiento = selectedStockItem?.fechaVencimiento ?: "",
            ordenCompra = selectedStockItem?.ordenCompra ?: "",
            ubicacion = selectedStockItem?.ubicacion ?: "",
            ubicacionDetallada = selectedStockItem?.ubicacionDetallada ?: "",
            fechaIngreso = fechaIngreso
        )

        // Lanzar la pantalla de resumen
        val intent = Intent(this, activity_mostrarDatos::class.java).apply {
            putExtra("codigo", stockItem.codigo)
            putExtra("descripcion", stockItem.descripcion)
            putExtra("lote", stockItem.lote)
            putExtra("cantidad", stockItem.cantidad.toString())
            putExtra("fechaIngreso", stockItem.fechaIngreso)
            putExtra("fechaVencimiento", stockItem.fechaVencimiento)
            putExtra("ordenCompra", stockItem.ordenCompra)
            putExtra("ubicacion", stockItem.ubicacion)
            putExtra("ubiDetallada", stockItem.ubicacionDetallada)
        }
        
        this.tempStockItem = stockItem
        mostrarDatosLauncher.launch(intent)
    }
    // Actualizar el stock con la nueva entrada de producto
    private fun actualizarStock(nuevoItem: StockItem) {
        val prefs = getSharedPreferences("StockData", Context.MODE_PRIVATE)
        val json = prefs.getString("stockList", null)
        val type = object : TypeToken<MutableList<StockItem>>() {}.type
        val currentStock: MutableList<StockItem> = Gson().fromJson(json, type) ?: mutableListOf()

        val index = currentStock.indexOfFirst { it.codigo == nuevoItem.codigo && it.lote == nuevoItem.lote }
        if (index != -1) {
            val itemExistente = currentStock[index]
            currentStock[index] = itemExistente.copy(
                cantidad = itemExistente.cantidad + nuevoItem.cantidad,
                usuario = nuevoItem.usuario,
                ubicacion = nuevoItem.ubicacion,
                ubicacionDetallada = nuevoItem.ubicacionDetallada
            )
        } else {
            currentStock.add(nuevoItem)
        }

        prefs.edit().putString("stockList", Gson().toJson(currentStock)).apply()
        recargarDatosDesdeStock()
    }
    // Filtrado reactivo de productos en la búsqueda de código
    private fun filterStockItems(query: String?, autoSelectIfExactMatch: Boolean = false) {
        val filtered = if (!query.isNullOrEmpty()) {
            allStockItems.filter { item ->
                item.codigo.contains(query, ignoreCase = true) ||
                        item.descripcion.contains(query, ignoreCase = true) ||
                        item.lote.contains(query, ignoreCase = true)
            }
        } else {
            allStockItems
        }
        adaptadorProductosBusqueda.updateList(filtered)

        if (autoSelectIfExactMatch && !query.isNullOrEmpty()) {
            val exactMatch = filtered.find { it.codigo.equals(query, ignoreCase = true) }
            if (exactMatch != null) {
                seleccionarProducto(exactMatch)
            } else if (filtered.size == 1) {
                seleccionarProducto(filtered[0])
            }
        }
    }
    // Limpiar formulario completo (campos y selección) y volver a la búsqueda
    private fun limpiarFormularioCompleto() {
        selectedStockItem = null
        binding.etCodigo.text?.clear()
        binding.etDescripcion.text?.clear()
        binding.etLote.text?.clear()
        binding.etCantidad.text?.clear()
        binding.etUsuario.text?.clear()
        binding.searchEditText.text?.clear()
        adaptadorProductosBusqueda.selectedPosition = RecyclerView.NO_POSITION
        adaptadorProductosBusqueda.notifyDataSetChanged()
        binding.searchEditText.requestFocus()
    }
}
