package com.tuempresa.inventario

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.tuempresa.inventario.databinding.ActivitySalidaProductosBinding
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.tuempresa.inventario.model.StockItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SalidaProductosActivity : AppCompatActivity(), RecyclerItemClickListener.OnItemClickListener {

    private lateinit var binding: ActivitySalidaProductosBinding
    private lateinit var productosRecyclerView: RecyclerView
    private lateinit var etBusquedaProductos: TextInputEditText

    private lateinit var productosAdapter: ProductosAdapter
    private val productosList = mutableListOf<Producto>()
    private lateinit var listaProductosBusqueda: MutableList<StockItem>
    private lateinit var adaptadorProductosBusqueda: ProductosBusquedaAdapter
    private var numeroOperaciones = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySalidaProductosBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inicialización de UI
        etBusquedaProductos = binding.etBusquedaProductos
        
        productosRecyclerView = binding.rvProductos
        productosRecyclerView.layoutManager = LinearLayoutManager(this)
        productosAdapter = ProductosAdapter(productosList)
        productosRecyclerView.adapter = productosAdapter

        listaProductosBusqueda = mutableListOf()
        adaptadorProductosBusqueda = ProductosBusquedaAdapter(mutableListOf()) { selectedProduct ->
            seleccionarProducto(selectedProduct)
        }
        binding.rvProductosBusqueda.layoutManager = LinearLayoutManager(this)
        binding.rvProductosBusqueda.adapter = adaptadorProductosBusqueda

        binding.btnBack.setOnClickListener {
            finish()
        }

        setupListeners()
        setupSearch()
        recargarDatosDesdeStock()

        // Escáner inteligente para búsqueda rápida
        val options = GmsBarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
            .enableAutoZoom()
            .build()
        val scanner = GmsBarcodeScanning.getClient(this, options)

        binding.btnEscanear.setOnClickListener {
            scanner.startScan()
                .addOnSuccessListener { barcode: Barcode ->
                    val code = barcode.rawValue ?: ""
                    etBusquedaProductos.setText(code)
                    // Forzamos el filtrado con auto-selección si hay coincidencia exacta
                    filterList(code, autoSelectIfExactMatch = true)
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Escaneo cancelado", Toast.LENGTH_SHORT).show()
                }
        }

        binding.btnActualizar.setOnClickListener {
            binding.btnActualizar.animate().rotationBy(360f).setDuration(500).start()
            recargarDatosDesdeStock()
            Toast.makeText(this, "Lista de productos actualizada", Toast.LENGTH_SHORT).show()
        }

        binding.btnLimpiar.setOnClickListener {
            limpiarFormularioCompleto()
        }

        binding.etUsuario.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus && binding.etUsuario.text?.isNotEmpty() == true) {
                binding.etCantidad.requestFocus()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        recargarDatosDesdeStock()
    }

    private fun recargarDatosDesdeStock() {
        val sharedPrefs = getSharedPreferences("StockData", Context.MODE_PRIVATE)
        val json = sharedPrefs.getString("stockList", null)
        if (json != null) {
            val type = object : TypeToken<MutableList<StockItem>>() {}.type
            listaProductosBusqueda = Gson().fromJson(json, type) ?: mutableListOf()
            adaptadorProductosBusqueda.updateList(listaProductosBusqueda)
        }
    }

    private fun setupSearch() {
        etBusquedaProductos.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterList(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun filterList(query: String?, autoSelectIfExactMatch: Boolean = false) {
        val filtered = if (!query.isNullOrEmpty()) {
            listaProductosBusqueda.filter { item ->
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
            listaProductosBusqueda
        }
        adaptadorProductosBusqueda.updateList(filtered)

        // Si se escaneó y hay una coincidencia exacta de código, o solo quedó un resultado, lo seleccionamos
        if (autoSelectIfExactMatch && !query.isNullOrEmpty()) {
            val exactMatch = filtered.find { it.codigo.equals(query, ignoreCase = true) }
            if (exactMatch != null) {
                seleccionarProducto(exactMatch)
            } else if (filtered.size == 1) {
                seleccionarProducto(filtered[0])
            }
        }
    }

    private fun seleccionarProducto(item: StockItem) {
        binding.etDescripcion.setText(item.descripcion)
        binding.etCodigo.setText(item.codigo)
        binding.etLote.setText(item.lote)
        binding.etFechaSalida.setText(SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date()))
        binding.etOperacion.setText(numeroOperaciones.toString())

        // Limpiar búsqueda para ver la lista completa pero mantener el foco en la acción
        if (!etBusquedaProductos.text.isNullOrEmpty()) {
            etBusquedaProductos.text?.clear()
            // Buscamos la nueva posición del producto seleccionado en la lista completa para resaltar
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

    private fun setupListeners() {
        binding.btnAgregar.setOnClickListener { agregarProductoSalida() }
        
        binding.btnSalir.setOnClickListener {
            if (productosList.isNotEmpty()) {
                AlertDialog.Builder(this)
                    .setTitle("¡Éxito!")
                    .setMessage("La operación de salida se ha completado correctamente.")
                    .setPositiveButton("Aceptar") { dialog, _ ->
                        productosList.clear()
                        productosAdapter.notifyDataSetChanged()
                        limpiarFormularioCompleto()
                        numeroOperaciones++
                        binding.etOperacion.setText(numeroOperaciones.toString())
                        etBusquedaProductos.requestFocus()
                        dialog.dismiss()
                    }
                    .setCancelable(false)
                    .show()
            } else {
                Toast.makeText(this, "No hay productos en la lista para finalizar", Toast.LENGTH_SHORT).show()
            }
        }
        
        binding.btnEliminar.setOnClickListener {
            if (productosAdapter.selectedPosition != RecyclerView.NO_POSITION) {
                val p = productosList[productosAdapter.selectedPosition]
                actualizarStockGeneral(p.copy(cantidad = -p.cantidad))
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
    }

    override fun onItemClick(view: View, position: Int) {}
    override fun onLongItemClick(view: View, position: Int) {}
    override fun onItemDoubleClick(view: View, position: Int) {}

    private fun agregarProductoSalida() {
        val desc = binding.etDescripcion.text.toString()
        val cod = binding.etCodigo.text.toString()
        val lote = binding.etLote.text.toString()
        val cant = binding.etCantidad.text.toString().toIntOrNull() ?: 0
        val usu = binding.etUsuario.text.toString()

        val stockDisponible = listaProductosBusqueda.find { it.codigo == cod && it.lote == lote }?.cantidad ?: 0

        if (cod.isNotEmpty() && cant > 0 && usu.isNotEmpty()) {
            if (cant > stockDisponible) {
                Toast.makeText(this, "Stock insuficiente ($stockDisponible disponible)", Toast.LENGTH_SHORT).show()
                return
            }

            val p = Producto(cod, desc, lote, binding.etFechaSalida.text.toString(), 
                             binding.etOperacion.text.toString(), usu, cant)
            productosList.add(p)
            productosAdapter.notifyDataSetChanged()
            actualizarStockGeneral(p)
            limpiarCampos()
            Toast.makeText(this, "Producto agregado a la lista de salida", Toast.LENGTH_SHORT).show()
            etBusquedaProductos.requestFocus()
        } else {
            Toast.makeText(this, "Complete todos los campos operativos", Toast.LENGTH_SHORT).show()
        }
    }

    private fun limpiarCampos() {
        binding.etDescripcion.text?.clear()
        binding.etCodigo.text?.clear()
        binding.etLote.text?.clear()
        binding.etUsuario.text?.clear()
        binding.etCantidad.text?.clear()
    }

    private fun limpiarFormularioCompleto() {
        limpiarCampos()
        etBusquedaProductos.text?.clear()
        adaptadorProductosBusqueda.selectedPosition = RecyclerView.NO_POSITION
        adaptadorProductosBusqueda.notifyDataSetChanged()
        Toast.makeText(this, "Formulario limpiado", Toast.LENGTH_SHORT).show()
    }

    private fun actualizarStockGeneral(producto: Producto) {
        val sharedPrefs = getSharedPreferences("StockData", Context.MODE_PRIVATE)
        val json = sharedPrefs.getString("stockList", null)
        val type = object : TypeToken<MutableList<StockItem>>() {}.type
        val stockList: MutableList<StockItem> = Gson().fromJson(json, type) ?: mutableListOf()

        val item = stockList.find { it.codigo == producto.codigo && it.lote == producto.lote }
        item?.let {
            it.cantidad = (it.cantidad - producto.cantidad).coerceAtLeast(0)
            sharedPrefs.edit().putString("stockList", Gson().toJson(stockList)).apply()
            
            listaProductosBusqueda.find { p -> p.codigo == it.codigo && p.lote == it.lote }?.let { localP ->
                localP.cantidad = it.cantidad
            }
            filterList(etBusquedaProductos.text.toString())
        }
    }

    private fun mostrarDialogoModificar(producto: Producto) {
        val builder = AlertDialog.Builder(this).setTitle("Modificar Cantidad")
        val input = EditText(this).apply {
            hint = "Nueva Cantidad"
            setText(producto.cantidad.toString())
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
        }
        builder.setView(input)
        builder.setPositiveButton("Guardar") { _, _ ->
            val nuevaCant = input.text.toString().toIntOrNull() ?: producto.cantidad
            val diferencia = nuevaCant - producto.cantidad
            val stockDisponible = listaProductosBusqueda.find { it.codigo == producto.codigo && it.lote == producto.lote }?.cantidad ?: 0
            
            if (diferencia > stockDisponible) {
                Toast.makeText(this, "Stock insuficiente", Toast.LENGTH_SHORT).show()
            } else {
                actualizarStockGeneral(producto.copy(cantidad = diferencia))
                producto.cantidad = nuevaCant
                productosAdapter.notifyDataSetChanged()
            }
        }
        builder.setNegativeButton("Cancelar", null)
        builder.show()
    }
}
