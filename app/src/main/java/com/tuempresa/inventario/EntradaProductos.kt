package com.tuempresa.inventario

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
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
    
    // Búsqueda
    private var allStockItems: MutableList<StockItem> = mutableListOf()
    private lateinit var adaptadorProductosBusqueda: ProductosBusquedaAdapter
    private var selectedStockItem: StockItem? = null

    // Lista de productos agregados (al estilo Salida)
    private val productosList = mutableListOf<Producto>()
    private lateinit var productosAdapter: ProductosAdapter
    private var numeroOperaciones = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEntradaProductosBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        setupRecyclerViews()
        setupScanner()
        setupListeners()
        recargarDatosDesdeStock()

        // Inicializar fecha y operación
        binding.etFechaIngreso.setText(SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date()))
        binding.etFechaIngreso.isEnabled = false
        binding.etOperacion.setText(numeroOperaciones.toString())
    }

    private fun setupUI() {
        binding.btnBack.setOnClickListener { finish() }
        
        binding.searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterStockItems(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.etUsuario.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus && binding.etUsuario.text?.isNotEmpty() == true) {
                binding.etCantidad.requestFocus()
            }
        }
        binding.searchEditText.requestFocus()
    }

    private fun setupRecyclerViews() {
        // RecyclerView de Búsqueda
        adaptadorProductosBusqueda = ProductosBusquedaAdapter(mutableListOf()) { item ->
            seleccionarProducto(item)
        }
        binding.rvProductosBusqueda.layoutManager = LinearLayoutManager(this)
        binding.rvProductosBusqueda.adapter = adaptadorProductosBusqueda

        // RecyclerView de Productos Agregados (Estilo Salida)
        productosAdapter = ProductosAdapter(productosList)
        binding.rvProductos.layoutManager = LinearLayoutManager(this)
        binding.rvProductos.adapter = productosAdapter
    }

    private fun setupScanner() {
        val options = GmsBarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
            .enableAutoZoom()
            .build()
        val scanner = GmsBarcodeScanning.getClient(this, options)

        binding.btnEscanear.setOnClickListener {
            scanner.startScan()
                .addOnSuccessListener { barcode ->
                    val rawValue = barcode.rawValue ?: ""
                    binding.searchEditText.setText(rawValue)
                    filterStockItems(rawValue, autoSelectIfExactMatch = true)
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Escaneo cancelado", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun setupListeners() {
        binding.btnActualizar.setOnClickListener {
            binding.btnActualizar.animate().rotationBy(360f).setDuration(500).start()
            recargarDatosDesdeStock()
            Toast.makeText(this, "Stock actualizado", Toast.LENGTH_SHORT).show()
        }

        binding.btnLimpiar.setOnClickListener {
            limpiarFormularioCompleto()
        }

        binding.btnIngresar.setOnClickListener {
            prepararIngreso()
        }

        binding.btnEliminar.setOnClickListener {
            if (productosAdapter.selectedPosition != RecyclerView.NO_POSITION) {
                val p = productosList[productosAdapter.selectedPosition]
                // Al eliminar de la lista de entrada, restamos la cantidad que habíamos sumado
                revertirStock(p)
                productosList.removeAt(productosAdapter.selectedPosition)
                productosAdapter.notifyItemRemoved(productosAdapter.selectedPosition)
                productosAdapter.selectedPosition = RecyclerView.NO_POSITION
            } else {
                Toast.makeText(this, "Seleccione un item para eliminar", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnModificar.setOnClickListener {
            if (productosAdapter.selectedPosition != RecyclerView.NO_POSITION) {
                mostrarDialogoModificar(productosList[productosAdapter.selectedPosition])
            } else {
                Toast.makeText(this, "Seleccione un item para modificar", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnSalir.setOnClickListener {
            if (productosList.isNotEmpty()) {
                AlertDialog.Builder(this)
                    .setTitle("¡Éxito!")
                    .setMessage("La operación de entrada se ha completado correctamente.")
                    .setPositiveButton("Aceptar") { dialog, _ ->
                        productosList.clear()
                        productosAdapter.notifyDataSetChanged()
                        limpiarFormularioCompleto()
                        numeroOperaciones++
                        binding.etOperacion.setText(numeroOperaciones.toString())
                        binding.searchEditText.requestFocus()
                        dialog.dismiss()
                    }
                    .setCancelable(false)
                    .show()
            } else {
                Toast.makeText(this, "No hay productos en la lista para finalizar", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun recargarDatosDesdeStock() {
        val prefs = getSharedPreferences("StockData", Context.MODE_PRIVATE)
        val json = prefs.getString("stockList", null)
        if (json != null) {
            val type = object : TypeToken<MutableList<StockItem>>() {}.type
            allStockItems = Gson().fromJson(json, type) ?: mutableListOf()
            adaptadorProductosBusqueda.updateList(allStockItems)
        }
    }

    private fun seleccionarProducto(item: StockItem) {
        selectedStockItem = item
        binding.etCodigo.setText(item.codigo)
        binding.etDescripcion.setText(item.descripcion)
        binding.etLote.setText(item.lote)

        // Usamos isFocusable = false en lugar de isEnabled = false para que
        // los bordes no se aclaren (mantengan el color normal) pero no sean editables.
        binding.etCodigo.isFocusable = false
        binding.etCodigo.isFocusableInTouchMode = false
        binding.etDescripcion.isFocusable = false
        binding.etDescripcion.isFocusableInTouchMode = false
        binding.etLote.isFocusable = false
        binding.etLote.isFocusableInTouchMode = false

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

    private fun prepararIngreso() {
        val codigo = binding.etCodigo.text.toString()
        val descripcion = binding.etDescripcion.text.toString()
        val lote = binding.etLote.text.toString()
        val usuario = binding.etUsuario.text.toString()
        val cantidadStr = binding.etCantidad.text.toString()
        val cantidad = cantidadStr.toIntOrNull() ?: 0

        if (codigo.isEmpty() || descripcion.isEmpty() || lote.isEmpty() || usuario.isEmpty() || cantidad <= 0) {
            Toast.makeText(this, "Complete todos los campos de identificación, usuario y cantidad", Toast.LENGTH_SHORT).show()
            return
        }

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
            fechaIngreso = binding.etFechaIngreso.text.toString()
        )

        // Agregar directamente a la lista visual y actualizar stock
        agregarAListaYActualizarStock(stockItem)
    }

    private fun agregarAListaYActualizarStock(item: StockItem) {
        // Agregar a la lista visual (usando el modelo Producto para el adapter)
        val p = Producto(
            item.codigo,
            item.descripcion,
            item.lote,
            item.fechaIngreso, // Usamos fechaIngreso para el campo fecha
            binding.etOperacion.text.toString(),
            item.usuario,
            item.cantidad
        )
        productosList.add(p)
        productosAdapter.notifyDataSetChanged()

        // Actualizar stock en SharedPreferences
        actualizarStockPersistente(item)
        
        limpiarCampos()
        Toast.makeText(this, "Producto agregado a la lista de entrada", Toast.LENGTH_SHORT).show()
        binding.searchEditText.requestFocus()
    }

    private fun actualizarStockPersistente(nuevoItem: StockItem, soloCantidad: Boolean = false) {
        val prefs = getSharedPreferences("StockData", Context.MODE_PRIVATE)
        val json = prefs.getString("stockList", null)
        val type = object : TypeToken<MutableList<StockItem>>() {}.type
        val currentStock: MutableList<StockItem> = Gson().fromJson(json, type) ?: mutableListOf()

        val index = currentStock.indexOfFirst { it.codigo == nuevoItem.codigo && it.lote == nuevoItem.lote }
        if (index != -1) {
            val itemExistente = currentStock[index]
            if (soloCantidad) {
                // Solo actualizamos la cantidad (para modificaciones en la lista o eliminación)
                itemExistente.cantidad = (itemExistente.cantidad + nuevoItem.cantidad).coerceAtLeast(0)
            } else {
                // Actualización completa (cuando se agrega desde el formulario/resumen)
                currentStock[index] = itemExistente.copy(
                    cantidad = itemExistente.cantidad + nuevoItem.cantidad,
                    usuario = nuevoItem.usuario,
                    ubicacion = nuevoItem.ubicacion,
                    ubicacionDetallada = nuevoItem.ubicacionDetallada
                )
            }
        } else if (!soloCantidad) {
            // Si no existe y no es solo cantidad, lo agregamos como nuevo
            currentStock.add(nuevoItem)
        }

        prefs.edit().putString("stockList", Gson().toJson(currentStock)).apply()
        recargarDatosDesdeStock()
    }

    private fun revertirStock(producto: Producto) {
        // Al eliminar de la lista de entrada, restamos la cantidad que habíamos sumado
        val itemReferencia = StockItem(producto.codigo, "", producto.lote, -producto.cantidad, "", "", "", "", "")
        actualizarStockPersistente(itemReferencia, soloCantidad = true)
    }

    private fun filterStockItems(query: String?, autoSelectIfExactMatch: Boolean = false) {
        val filtered = if (!query.isNullOrEmpty()) {
            allStockItems.filter { item ->
                item.codigo.contains(query, ignoreCase = true) ||
                        item.descripcion.contains(query, ignoreCase = true) ||
                        item.lote.contains(query, ignoreCase = true) ||
                        item.cantidad.toString().contains(query, ignoreCase = true) ||
                        item.fechaIngreso.contains(query, ignoreCase = true) ||
                        item.fechaVencimiento.contains(query, ignoreCase = true) ||
                        item.ordenCompra.contains(query, ignoreCase = true) ||
                        item.ubicacion.contains(query, ignoreCase = true) ||
                        item.ubicacionDetallada.contains(query, ignoreCase = true)
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

    private fun mostrarDialogoModificar(producto: Producto) {
        val builder = AlertDialog.Builder(this).setTitle("Modificar Cantidad de Entrada")
        val input = EditText(this).apply {
            hint = "Nueva Cantidad"
            setText(producto.cantidad.toString())
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
        }
        builder.setView(input)
        builder.setPositiveButton("Guardar") { _, _ ->
            val nuevaCant = input.text.toString().toIntOrNull() ?: producto.cantidad
            val diferencia = nuevaCant - producto.cantidad
            
            // Actualizar stock general con la diferencia (positiva o negativa) sin sobrescribir otros datos
            val stockItemReferencia = StockItem(producto.codigo, "", producto.lote, diferencia, "", "", "", "", "")
            actualizarStockPersistente(stockItemReferencia, soloCantidad = true)
            
            producto.cantidad = nuevaCant
            productosAdapter.notifyDataSetChanged()
        }
        builder.setNegativeButton("Cancelar", null)
        builder.show()
    }

    private fun limpiarCampos() {
        selectedStockItem = null
        binding.etCodigo.text?.clear()
        binding.etDescripcion.text?.clear()
        binding.etLote.text?.clear()
        binding.etCantidad.text?.clear()
        binding.etUsuario.text?.clear()

        binding.etCodigo.isFocusable = true
        binding.etCodigo.isFocusableInTouchMode = true
        binding.etDescripcion.isFocusable = true
        binding.etDescripcion.isFocusableInTouchMode = true
        binding.etLote.isFocusable = true
        binding.etLote.isFocusableInTouchMode = true
    }

    private fun limpiarFormularioCompleto() {
        limpiarCampos()
        binding.searchEditText.text?.clear()
        adaptadorProductosBusqueda.selectedPosition = RecyclerView.NO_POSITION
        adaptadorProductosBusqueda.notifyDataSetChanged()
    }

    override fun onResume() {
        super.onResume()
        recargarDatosDesdeStock()
    }
}
