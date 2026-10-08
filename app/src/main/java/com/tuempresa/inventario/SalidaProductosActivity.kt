package com.tuempresa.inventario

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import android.content.Context
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
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
    private var numeroOperaciones = 1 // Contador para el número de operaciones

    private val openFileLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val uri = result.data?.data
                uri?.let {
                    try {
                        val inputStream = contentResolver.openInputStream(it)
                        inputStream?.let { stream ->
                            listaProductosBusqueda = obtenerProductosDesdeExcel(stream).toMutableList()
                            stream.close()
                            adaptadorProductosBusqueda.updateList(listaProductosBusqueda)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) { // Llamada al metodo onCreate de la clase padre
        super.onCreate(savedInstanceState)
        binding = ActivitySalidaProductosBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.tilBusqueda.editText?.let { editText ->
            etBusquedaProductos = editText as TextInputEditText
            initEtBusquedaProductos(binding.tilBusqueda)
        } ?: run {
            Toast.makeText(this, "Error: Campo de búsqueda no encontrado", Toast.LENGTH_SHORT).show()
            finish()
        }
        // Configurar el RecyclerView
        productosRecyclerView = binding.rvProductos
        productosRecyclerView.layoutManager = LinearLayoutManager(this)
        productosAdapter = ProductosAdapter(productosList)
        productosRecyclerView.adapter = productosAdapter

        listaProductosBusqueda = mutableListOf()
        Log.d("TuActividad", "Inicialización de listaProductosBusqueda: ${listaProductosBusqueda.size}")

        adaptadorProductosBusqueda = ProductosBusquedaAdapter(listaProductosBusqueda)
        binding.rvProductosBusqueda.layoutManager = LinearLayoutManager(this)
        binding.rvProductosBusqueda.adapter = adaptadorProductosBusqueda

        binding.rvProductosBusqueda.addOnItemTouchListener(
            RecyclerItemClickListener(this, binding.rvProductosBusqueda, this)
        )

        setupListeners()
        setupSearch()

        binding.tilBusqueda.setEndIconOnClickListener {
            val textoBusqueda = etBusquedaProductos.text.toString()
            if (textoBusqueda.isNotEmpty()) {
                filterList(textoBusqueda)
            } else {
                Toast.makeText(this, "Por favor, ingrese un término de búsqueda", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnImportarExcel.setOnClickListener {
            abrirSelectorDeArchivos()
        }
    }
    private fun obtenerProductosDesdeExcel(inputStream: InputStream): List<Producto> {
        val productos = mutableListOf<Producto>()
        try {
            Log.d("ExcelDebug", "Iniciando lectura de Excel")
            val workbook = WorkbookFactory.create(inputStream)//Leer el archivo Excel
            val sheet = workbook.getSheetAt(0)//Obtener la primera hoja del archivo excel
            Log.d("ExcelDebug", "Hoja obtenida: ${sheet.sheetName}")//Obtener el nombre de la hoja

            for (rowIndex in 1..sheet.lastRowNum) {//Iterar a traves de las filas
                val row = sheet.getRow(rowIndex) ?: continue
                Log.d("ExcelDebug", "Fila ${rowIndex} obtenida")

                // Leer las columnas B, C, D y E (E para cantidad)
                val codigo = row.getCell(1)?.stringCellValue ?: ""
                val descripcion = row.getCell(2)?.stringCellValue ?: ""
                val lote = row.getCell(3)?.stringCellValue ?: ""
                val cantidad = row.getCell(4)?.numericCellValue?.toInt() ?: 0 // Leer la cantidad desde la columna E
                Log.d("ExcelDebug", "Datos: Codigo=$codigo, Descripcion=$descripcion, Lote=$lote, Cantidad=$cantidad")

                val producto = Producto(codigo, descripcion, lote, "", "", "", cantidad)
                productos.add(producto)
            }
            // Cerrar el archivo
            workbook.close()
            Log.d("ExcelDebug", "Lectura de Excel completada")
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("ExcelError", "Error al leer el Excel: ${e.message}")
        }
        return productos
    }

    private fun abrirSelectorDeArchivos() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        openFileLauncher.launch(intent)
    }


    // Configurar el campo de búsqueda
    private fun initEtBusquedaProductos(editText: TextInputLayout) {}

    private fun setupSearch() {
        etBusquedaProductos.addTextChangedListener(object : TextWatcher {
            private var lastQuery: String? = null

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString()
                if (query != lastQuery) {
                    lastQuery = query
                    filterList(query)
                }
            }

            override fun afterTextChanged(s: Editable?) {} // No es necesario implementar este metodo
        })
    }

    // Filtrar la lista de productos
    private fun filterList(query: String?) {
        Log.d("TuActividad", "Tamaño de listaProductosBusqueda: ${listaProductosBusqueda.size}")
        if (!query.isNullOrEmpty()) {
            val filteredList = listaProductosBusqueda.filter { it.descripcion.contains(query, ignoreCase = true) }
            adaptadorProductosBusqueda.updateList(filteredList)
        } else {
            adaptadorProductosBusqueda.updateList(listaProductosBusqueda)
        }
    }

    // Configurar los listeners
    private fun setupListeners() {
        binding.btnEliminar.setOnClickListener {
            if (productosAdapter.selectedPosition != RecyclerView.NO_POSITION) {
                val producto = productosList[productosAdapter.selectedPosition]
                eliminarProducto(producto)
                productosList.removeAt(productosAdapter.selectedPosition)
                productosAdapter.notifyItemRemoved(productosAdapter.selectedPosition)
                productosAdapter.selectedPosition = RecyclerView.NO_POSITION
            } else {
                Toast.makeText(this, "Selecciona un producto para eliminar", Toast.LENGTH_SHORT).show()
            }
        }
        //
        binding.btnModificar.setOnClickListener {
            if (productosAdapter.selectedPosition != RecyclerView.NO_POSITION) {
                val producto = productosList[productosAdapter.selectedPosition]
                mostrarDialogoModificarProducto(producto)
            } else {
                Toast.makeText(this, "Selecciona un producto para modificar", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnAgregar.setOnClickListener { // Llamada al metodo onCreate de la clase padre
            agregarProductoDesdeBusqueda()
        }

        binding.btnSalir.setOnClickListener { // Llamada al metodo onCreate de la clase padre
            finish()
        }
    }

    override fun onItemClick(view: View, position: Int) {}
    override fun onLongItemClick(view: View, position: Int) {}

    override fun onItemDoubleClick(view: View, position: Int) {
        val productoSeleccionado = listaProductosBusqueda[position]

        binding.etDescripcion.setText(productoSeleccionado.descripcion)
        binding.etCodigo.setText(productoSeleccionado.codigo)
        binding.etLote.setText(productoSeleccionado.lote)

        if (!productoSeleccionado.lote.isNullOrEmpty()) {
            binding.etLote.setText(productoSeleccionado.lote)
        }

        binding.etFechaSalida.setText(obtenerFechaActual())
        binding.etOperacion.setText((numeroOperaciones++).toString())

        etBusquedaProductos.setText("")

        runOnUiThread {
            adaptadorProductosBusqueda.updateList(listaProductosBusqueda)
        }
    }

    private fun obtenerFechaActual(): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        return sdf.format(Date())
    }

    private fun agregarProductoDesdeBusqueda() {
        val descripcion = binding.etDescripcion.text.toString()
        val codigo = binding.etCodigo.text.toString()
        val lote = binding.etLote.text.toString()
        val fechaSalida = binding.etFechaSalida.text.toString()
        val operacion = binding.etOperacion.text.toString()
        val usuario = binding.etUsuario.text.toString()
        val cantidad = binding.etCantidad.text.toString()

        if (descripcion.isNotEmpty() && codigo.isNotEmpty() && lote.isNotEmpty() && usuario.isNotEmpty() && cantidad.isNotEmpty()) {
            val nuevoProducto = Producto(codigo, descripcion, lote, fechaSalida, operacion, usuario, cantidad.toInt())
            productosList.add(nuevoProducto)
            productosAdapter.notifyDataSetChanged()
            limpiarCampos()

            binding.etUsuario.requestFocus()
            binding.etUsuario.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) {
                    binding.etCantidad.requestFocus()
                }
            }
        } else {
            Toast.makeText(this, "Por favor, completa todos los campos", Toast.LENGTH_SHORT).show()
        }
    }

    private fun limpiarCampos() {
        val editTexts = listOf(
            binding.etDescripcion,
            binding.etCodigo,
            binding.etLote,
            binding.etUsuario,
            binding.etCantidad
        )
        // Limpiar los campos
        editTexts.forEach { editText ->
            editText.setText("")
        }
    }

    private fun eliminarProducto(producto: Producto) {
        Toast.makeText(this, "Producto eliminado", Toast.LENGTH_SHORT).show()
    }

    private fun mostrarDialogoModificarProducto(producto: Producto) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Modificar Producto")

        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL

        val codigoEditText = EditText(this)
        codigoEditText.hint = "Código"
        codigoEditText.setText(producto.codigo)
        layout.addView(codigoEditText)

        val descripcionEditText = EditText(this)
        descripcionEditText.hint = "Descripción"
        descripcionEditText.setText(producto.descripcion)
        layout.addView(descripcionEditText)

        val loteEditText = EditText(this)
        loteEditText.hint = "Lote"
        loteEditText.setText(producto.lote)
        layout.addView(loteEditText)

        val cantidadEditText = EditText(this)
        cantidadEditText.hint = "Cantidad"
        cantidadEditText.setText(producto.cantidad.toString())
        layout.addView(cantidadEditText)

        builder.setView(layout)

        // Agregar el botón de guardar
        builder.setPositiveButton("Guardar") { dialog, _ ->
            val nuevoCodigo = codigoEditText.text.toString()
            val nuevaDescripcion = descripcionEditText.text.toString()
            val nuevoLote = loteEditText.text.toString()
            val nuevaCantidad = cantidadEditText.text.toString().toIntOrNull() ?: 0

            if (nuevoCodigo.isNotEmpty() && nuevaDescripcion.isNotEmpty() && nuevoLote.isNotEmpty()) {
                // Actualizar el objeto original en la lista productosList
                val index = productosList.indexOf(producto)
                if (index != -1) {
                    productosList[index].codigo = nuevoCodigo
                    productosList[index].descripcion = nuevaDescripcion
                    productosList[index].lote = nuevoLote
                    productosList[index].cantidad = nuevaCantidad
                    productosAdapter.notifyItemChanged(index) // Notificar el cambio al adaptador
                    actualizarProducto(productosList[index]) // Actualizar en la base de datos o almacenamiento
                    Toast.makeText(this, "Producto modificado", Toast.LENGTH_SHORT).show()

                    // Actualizar el objeto correspondiente en listaProductosBusqueda
                    val productoEnBusqueda = listaProductosBusqueda.find { it.codigo == producto.codigo }
                    productoEnBusqueda?.let {
                        val cantidadActual = it.cantidad
                        val cantidadIngresada = cantidadEditText.text.toString().toIntOrNull() ?: 0
                        it.cantidad = cantidadActual - cantidadIngresada

                        // Actualizar la descripción, código y lote si es necesario
                        it.descripcion = nuevaDescripcion
                        it.codigo = nuevoCodigo
                        it.lote = nuevoLote

                        adaptadorProductosBusqueda.notifyDataSetChanged() // Notificar el cambio al adaptador de búsqueda
                    }
                }
            } else {
                Toast.makeText(this, "Por favor, completa todos los campos", Toast.LENGTH_SHORT).show()
            }
            dialog.dismiss()
        }
        // Agregar el botón de cancelar
        builder.setNegativeButton("Cancelar") { dialog, _ ->
            dialog.cancel()
        }
        // Mostrar el diálogo
        builder.show()
    }

    private fun actualizarProducto(producto: Producto) {
        // Implementa aquí la lógica para actualizar el producto en tu base de datos o almacenamiento.
        actualizarStockGeneral(producto, this)
    }
    private fun actualizarStockGeneral(producto: Producto, context: Context) { // Recibe Context como parámetro
        val sharedPreferences = context.getSharedPreferences("StockData", Context.MODE_PRIVATE)
        val stockListJson = sharedPreferences.getString("stockList", null)
        val gson = Gson()
        val type = object : TypeToken<MutableList<StockItem>>() {}.type
        val stockList = gson.fromJson<MutableList<StockItem>>(stockListJson, type) ?: mutableListOf()

        val stockItem = stockList.find { it.codigo == producto.codigo && it.lote == producto.lote }

        stockItem?.let {
            it.cantidad = it.cantidad - producto.cantidad // Ahora debería funcionar
            if (it.cantidad < 0) {// Si la cantidad es negativa, se establece en 0
                it.cantidad = 0 // Si la cantidad es negativa, se establece en 0
            }
            val editor = sharedPreferences.edit()
            val updatedStockListJson = gson.toJson(stockList)
            editor.putString("stockList", updatedStockListJson)
            editor.apply()
        }
    }
}