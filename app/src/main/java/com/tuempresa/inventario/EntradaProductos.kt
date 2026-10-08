package com.tuempresa.inventario

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
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

        etCodigo = findViewById(R.id.etCodigo)
        etDescripcion = findViewById(R.id.etDescripcion)
        etLote = findViewById(R.id.etLote)
        etUsuario = findViewById(R.id.etUsuario)
        etCantidad = findViewById(R.id.etCantidad)
        etFechaIngreso = findViewById(R.id.etFechaIngreso)
        searchEditText = findViewById(R.id.searchEditText)

        // Establecer el foco inicial en searchEditText
        searchEditText.requestFocus()

        // Cargar los datos del stock
        stockList = cargarStockItems()
        allStockItems = stockList.toMutableList()

        // Variable para controlar si el foco en etCantidad debe establecerse
        var usuarioInteracted = false

        // Configurar el RecyclerView
        val rvProductos = findViewById<RecyclerView>(R.id.rvProductos)
        rvProductos.layoutManager = LinearLayoutManager(this)

        // Crear y configurar el adaptador
        stockAdapter = EntradaProductosAdapter(stockList) { selectedItem ->
            // Manejar la selección de un elemento
            etCodigo.setText(selectedItem.codigo)
            etDescripcion.setText(selectedItem.descripcion)
            etLote.setText(selectedItem.lote)

            // Deshabilitar campos
            etCodigo.isEnabled = false
            etDescripcion.isEnabled = false
            etLote.isEnabled = false

            // Mover el foco a etUsuario después de seleccionar un elemento
            etUsuario.requestFocus()

            // Limpiar el filtro
            searchEditText.text.clear()
        }
        rvProductos.adapter = stockAdapter

        // Configurar el botón "Ingresar"
        val btnIngresar = findViewById<Button>(R.id.btnIngresar)
        btnIngresar.setOnClickListener {
            val codigo = etCodigo.text.toString()
            val descripcion = etDescripcion.text.toString()
            val lote = etLote.text.toString()
            val usuario = etUsuario.text.toString()
            val cantidad = etCantidad.text.toString().toIntOrNull() ?: 0 // Convertir a Int
            val fechaIngreso = etFechaIngreso.text.toString()

            if (codigo.isEmpty() || descripcion.isEmpty() || lote.isEmpty() || usuario.isEmpty() || cantidad == 0 || fechaIngreso.isEmpty()) {
                Toast.makeText(this, "Es necesario llenar todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val nextId = getNextId()

            // Crear un nuevo objeto StockItem
            val nuevoStockItem = StockItem( // Cambiado a StockItem
                codigo = codigo,
                descripcion = descripcion,
                lote = lote,
                cantidad = cantidad, // Usar Int
                usuario = usuario,
                fechaVencimiento = "",
                ordenCompra = "",
                ubicacion = "",
                ubicacionDetallada = "",
                fechaIngreso = fechaIngreso
            )
            actualizarStock(nuevoStockItem)

            Toast.makeText(this, "Producto ingresado al stock", Toast.LENGTH_SHORT).show()

            etCodigo.text.clear()
            etDescripcion.text.clear()
            etLote.text.clear()
            etCantidad.text.clear()
            etUsuario.text.clear()

            saveNextId(nextId + 1)
        }

        val btnSalir = findViewById<Button>(R.id.btnSalir)
        btnSalir.setOnClickListener {
            finish()
        }
        // Establecer el foco en el campo de cantidad
        etUsuario.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                etCantidad.requestFocus()
            }
        }

        // Obtener y establecer la fecha actual
        val fechaActual = Calendar.getInstance().time
        val formatoFecha = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val fechaFormateada = formatoFecha.format(fechaActual)
        etFechaIngreso.setText(fechaFormateada)

        // Deshabilitar la edición del campo
        etFechaIngreso.isEnabled = false

        // Configurar el buscador
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterStockItems(s.toString())
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun actualizarStock(nuevoStockItem: StockItem) {
        Log.d("EntradaProductos", "Nuevo código: ${nuevoStockItem.codigo}")
        Log.d("EntradaProductos", "Nuevo item: $nuevoStockItem")

        // Asignar el valor de etUsuario al campo usuario
        nuevoStockItem.usuario = etUsuario.text.toString()

        val existingItemIndex = stockList.indexOfFirst { it.codigo == nuevoStockItem.codigo }
        if (existingItemIndex != -1) {
            Log.d("EntradaProductos", "Código existente encontrado en el índice: $existingItemIndex")
            val existingItem = stockList[existingItemIndex]
            val cantidadActualizada = existingItem.cantidad + nuevoStockItem.cantidad // Suma de Ints
            val stockItemActualizado = existingItem.copy(cantidad = cantidadActualizada, usuario = nuevoStockItem.usuario) // Asegúrate de actualizar también el usuario
            stockList[existingItemIndex] = stockItemActualizado
        } else {
            Log.d("EntradaProductos", "No se encontró el código existente")
            stockList.add(nuevoStockItem)
        }

        // Guardar la lista actualizada en SharedPreferences
        guardarStockItems(stockList)

        filterStockItems(searchEditText.text.toString())

        // Notificar al adaptador que los datos han cambiado
        stockAdapter.notifyDataSetChanged()
    }

    private fun cargarStockItems(): MutableList<StockItem> {
        val sharedPreferences = getSharedPreferences("StockData", Context.MODE_PRIVATE)
        val stockListJson = sharedPreferences.getString("stockList", null)

        if (stockListJson != null) {
            val gson = Gson()
            val type = object : TypeToken<MutableList<StockItem>>() {}.type
            val loadedList = try {
                gson.fromJson<MutableList<StockItem>>(stockListJson, type)
            } catch (e: Exception) {
                Log.e("CargarStockItems", "Error al convertir JSON: ${e.message}")
                return mutableListOf()
            }
            // Crear una copia de la lista cargada y retornarla
            return loadedList.toMutableList()
        }
        return mutableListOf()
    }
    // Guardar la lista actualizada en SharedPreferences
    private fun guardarStockItems(stockList: MutableList<StockItem>) {
        val sharedPreferences = getSharedPreferences("StockData", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        val gson = Gson()
        val stockListJson = gson.toJson(stockList)
        editor.putString("stockList", stockListJson)
        editor.apply()
    }

    private fun getNextId(): Int {
        val sharedPreferences = getSharedPreferences("StockData", Context.MODE_PRIVATE)
        return sharedPreferences.getInt("nextId", 1)
    }

    private fun saveNextId(nextId: Int) {
        val sharedPreferences = getSharedPreferences("StockData", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putInt("nextId", nextId)
        editor.apply()
    }
    private fun filterStockItems(query: String) {
        val filteredList = if (query.isNotEmpty()) {
            allStockItems.filter { item ->
                item.codigo.contains(query, ignoreCase = true) ||
                        item.descripcion.contains(query, ignoreCase = true) ||
                        item.lote.contains(query, ignoreCase = true)
            }.toMutableList()
        } else {
            allStockItems.toMutableList() // Asegurar que se use una copia
        }
        stockList.clear()
        stockList.addAll(filteredList)
        stockAdapter.notifyDataSetChanged()
    }

}