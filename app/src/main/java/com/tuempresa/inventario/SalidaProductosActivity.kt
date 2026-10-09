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
import org.apache.poi.ss.usermodel.WorkbookFactory
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.tuempresa.inventario.model.StockItem

class SalidaProductosActivity : AppCompatActivity(), RecyclerItemClickListener.OnItemClickListener {

    private lateinit var binding: ActivitySalidaProductosBinding
    private lateinit var productosRecyclerView: RecyclerView
    private lateinit var etBusquedaProductos: TextInputEditText

    private lateinit var productosAdapter: ProductosAdapter
    private val productosList = mutableListOf<Producto>()
    private lateinit var listaProductosBusqueda: MutableList<Producto>
    private lateinit var adaptadorProductosBusqueda: ProductosBusquedaAdapter
    private var numeroOperaciones = 1

    private val openFileLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                try {
                    contentResolver.openInputStream(uri)?.use { stream ->
                        val nuevosProductos = obtenerProductosDesdeExcel(stream)
                        if (nuevosProductos.isNotEmpty()) {
                            listaProductosBusqueda = nuevosProductos.toMutableList()
                            adaptadorProductosBusqueda.updateList(listaProductosBusqueda)
                            // Guardar en el stock persistente para que no se pierda
                            guardarProductosEnStockPersistente(nuevosProductos)
                            Toast.makeText(this, "Excel importado y guardado en stock", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    Log.e("Salida", "Error Excel: ${e.message}")
                }
            }
        }
    }

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
            // Al seleccionar un producto de la búsqueda
            binding.etDescripcion.setText(selectedProduct.descripcion)
            binding.etCodigo.setText(selectedProduct.codigo)
            binding.etLote.setText(selectedProduct.lote)
            binding.etFechaSalida.setText(SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date()))
            binding.etOperacion.setText(numeroOperaciones.toString())
            
            // Si hay algo en el buscador, lo limpiamos para mostrar la lista completa,
            // pero nos aseguramos de que el item seleccionado sea visible.
            if (!etBusquedaProductos.text.isNullOrEmpty()) {
                etBusquedaProductos.text?.clear()
                // Al limpiar, se dispara filterList("") -> updateList(fullList)
                // Buscamos la nueva posición del producto seleccionado en la lista completa
                val newPos = adaptadorProductosBusqueda.selectedPosition
                if (newPos != RecyclerView.NO_POSITION) {
                    binding.rvProductosBusqueda.post {
                        binding.rvProductosBusqueda.scrollToPosition(newPos)
                    }
                }
            }

            binding.etUsuario.requestFocus()
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

        binding.tilBusqueda.setEndIconOnClickListener {
            scanner.startScan()
                .addOnSuccessListener { barcode: Barcode ->
                    val code = barcode.rawValue ?: ""
                    etBusquedaProductos.setText(code)
                    filterList(code)
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

        binding.btnImportarExcel.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            }
            openFileLauncher.launch(intent)
        }

        binding.etUsuario.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus && binding.etUsuario.text?.isNotEmpty() == true) {
                binding.etCantidad.requestFocus()
            }
        }
    }

    private fun obtenerProductosDesdeExcel(inputStream: InputStream): List<Producto> {
        val productos = mutableListOf<Producto>()
        try {
            val workbook = WorkbookFactory.create(inputStream)
            val sheet = workbook.getSheetAt(0)
            for (rowIndex in 1..sheet.lastRowNum) {
                val row = sheet.getRow(rowIndex) ?: continue
                val codigo = row.getCell(1)?.toString() ?: ""
                val descripcion = row.getCell(2)?.toString() ?: ""
                val lote = row.getCell(3)?.toString() ?: ""
                val cantidad = row.getCell(4)?.numericCellValue?.toInt() ?: 0
                productos.add(Producto(codigo, descripcion, lote, "", "", "", cantidad))
            }
            workbook.close()
        } catch (e: Exception) {
            Log.e("Excel", "Error parsing: ${e.message}")
        }
        return productos
    }

    private fun recargarDatosDesdeStock() {
        val sharedPrefs = getSharedPreferences("StockData", Context.MODE_PRIVATE)
        val json = sharedPrefs.getString("stockList", null)
        if (json != null) {
            val type = object : TypeToken<MutableList<StockItem>>() {}.type
            val stockItems: List<StockItem> = Gson().fromJson(json, type) ?: emptyList()
            listaProductosBusqueda = stockItems.map { 
                Producto(it.codigo, it.descripcion, it.lote, "", "", "", it.cantidad) 
            }.toMutableList()
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

    private fun filterList(query: String?) {
        if (!query.isNullOrEmpty()) {
            val filtered = listaProductosBusqueda.filter { 
                it.descripcion.contains(query, true) || it.codigo.contains(query, true) 
            }
            adaptadorProductosBusqueda.updateList(filtered)
        } else {
            adaptadorProductosBusqueda.updateList(listaProductosBusqueda)
        }
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
                        limpiarCampos()
                        etBusquedaProductos.text?.clear()
                        adaptadorProductosBusqueda.selectedPosition = RecyclerView.NO_POSITION
                        adaptadorProductosBusqueda.notifyDataSetChanged()
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

    private fun guardarProductosEnStockPersistente(productos: List<Producto>) {
        val sharedPrefs = getSharedPreferences("StockData", Context.MODE_PRIVATE)
        val json = sharedPrefs.getString("stockList", null)
        val type = object : TypeToken<MutableList<StockItem>>() {}.type
        val stockList: MutableList<StockItem> = Gson().fromJson(json, type) ?: mutableListOf()

        productos.forEach { p ->
            val existing = stockList.find { it.codigo == p.codigo && it.lote == p.lote }
            if (existing != null) {
                existing.cantidad = p.cantidad
            } else {
                stockList.add(StockItem(p.codigo, p.descripcion, p.lote, p.cantidad, "", "", "", "", ""))
            }
        }
        sharedPrefs.edit().putString("stockList", Gson().toJson(stockList)).apply()
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
