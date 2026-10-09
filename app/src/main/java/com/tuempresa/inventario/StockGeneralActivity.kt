package com.tuempresa.inventario

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import com.tuempresa.inventario.model.StockItem
import org.apache.poi.ss.usermodel.WorkbookFactory


class StockGeneralActivity : AppCompatActivity() {

    private lateinit var recyclerViewStock: RecyclerView
    private lateinit var stockAdapter: StockAdapter
    private lateinit var buttonSalir: Button
    private lateinit var buttonImportar: Button
    private lateinit var btnActualizar: ImageButton
    private lateinit var openFileLauncher: ActivityResultLauncher<Intent>
    private lateinit var searchEditText: TextInputEditText
    private lateinit var tilSearch: TextInputLayout
    private lateinit var btnEscanear: com.google.android.material.button.MaterialButton
    private var allStockItems: MutableList<StockItem> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stock_general)

        recyclerViewStock = findViewById(R.id.recyclerViewStock)
        recyclerViewStock.layoutManager = LinearLayoutManager(this)

        stockAdapter = StockAdapter(mutableListOf()) { stockItem ->
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
                putExtra("readOnly", true)
            }
            startActivity(intent)
        }
        recyclerViewStock.adapter = stockAdapter

        buttonSalir = findViewById(R.id.buttonSalir)
        buttonSalir.setOnClickListener {
            finish()
        }

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            finish()
        }

        buttonImportar = findViewById(R.id.buttonImportar)
        buttonImportar.setOnClickListener {
            openFileLauncher.launch(createOpenFileIntent())
        }

        btnActualizar = findViewById(R.id.btnActualizar)
        btnActualizar.setOnClickListener {
            // Animación de giro
            btnActualizar.animate().rotationBy(360f).setDuration(500).start()

            allStockItems = cargarStockItems()
            filterStockItems(searchEditText.text.toString())
            Toast.makeText(this, "Stock actualizado", Toast.LENGTH_SHORT).show()
        }

        openFileLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val uri: Uri? = result.data?.data
                if (uri != null) {
                    leerArchivoExcel(uri)
                }
            }
        }

        searchEditText = findViewById(R.id.searchEditText)
        tilSearch = findViewById(R.id.tilSearch)
        btnEscanear = findViewById(R.id.btnEscanear)

        val options = GmsBarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
            .enableAutoZoom()
            .build()
        val scanner = GmsBarcodeScanning.getClient(this, options)

        btnEscanear.setOnClickListener {
            scanner.startScan()
                .addOnSuccessListener { barcode: Barcode ->
                    val code = barcode.rawValue ?: ""
                    searchEditText.setText(code)
                    filterStockItems(code)
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Escaneo cancelado", Toast.LENGTH_SHORT).show()
                }
        }

        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterStockItems(s.toString())
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    override fun onResume() {
        super.onResume()
        allStockItems = cargarStockItems()
        Log.d("StockGeneralActivity", "Tamaño de allStockItems en onResume: ${allStockItems.size}")
        filterStockItems(searchEditText.text.toString())
    }

    private fun cargarStockItems(): MutableList<StockItem> {
        val sharedPreferences = getSharedPreferences("StockData", Context.MODE_PRIVATE)
        val stockListJson = sharedPreferences.getString("stockList", null)

        if (stockListJson != null) {
            val gson = Gson()
            val type = object : TypeToken<MutableList<StockItem>>() {}.type
            return try {
                gson.fromJson<MutableList<StockItem>>(stockListJson, type)
            } catch (e: Exception) {
                Log.e("CargarStockItems", "Error al convertir JSON: ${e.message}")
                mutableListOf()
            }
        }
        return mutableListOf()
    }

    private fun createOpenFileIntent(): Intent {
        return Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        }
    }

    fun leerArchivoExcel(uri: Uri) = try {
        contentResolver.openInputStream(uri)?.use { inputStream ->
            val workbook = WorkbookFactory.create(inputStream)
            val sheet = workbook.getSheetAt(0) // Suponiendo que los datos están en la primera hoja

            val stockList = mutableListOf<StockItem>() // Lista para almacenar los datos

            for (rowIndex in 1..sheet.lastRowNum) { // Comenzar desde la segunda fila
                val row = sheet.getRow(rowIndex) ?: continue // Evitar filas nulas

                val codigo = row.getCell(1)?.toString() ?: ""
                val descripcion = row.getCell(2)?.toString() ?: ""
                val lote = row.getCell(3)?.toString() ?: ""
                val cantidadString = row.getCell(4)?.toString()?.trim() ?: "0"
                val fechaIngreso = row.getCell(5)?.toString() ?: ""
                val fechaVencimiento = row.getCell(6)?.toString() ?: ""
                val ordenCompra = row.getCell(7)?.toString() ?: ""
                val ubicacion = row.getCell(8)?.toString() ?: ""
                val ubicacionDetallada = row.getCell(9)?.toString() ?: ""

                val cantidad = try {
                    Log.d("StockGeneralActivity", "CantidadString antes: $cantidadString")
                    val cantidadDouble = cantidadString.toDouble()
                    val cantidadInt = cantidadDouble.toInt()
                    Log.d("StockGeneralActivity", "CantidadInt después: $cantidadInt")
                    cantidadInt
                } catch (e: NumberFormatException) {
                    Log.e("StockGeneralActivity", "Error al convertir cantidad: $cantidadString", e)
                    0
                }
                // Crear un objeto StockItem y agregarlo a la lista
                val nuevoStockItem = StockItem(
                    codigo,
                    descripcion,
                    lote,
                    cantidad,
                    fechaIngreso,
                    fechaVencimiento,
                    ordenCompra,
                    ubicacion,
                    ubicacionDetallada
                )
                stockList.add(nuevoStockItem)
                Log.d("StockGeneralActivity", "Leído: $nuevoStockItem")
            }

            Log.d("StockGeneralActivity", "Tamaño de stockList leído: ${stockList.size}")
            mostrarDatosEnRecyclerView(stockList.toMutableList())

            guardarStockItems(stockList)

            workbook.close()

            runOnUiThread {
                Toast.makeText(this, "Datos importados correctamente", Toast.LENGTH_SHORT).show()
            }
        } ?: run {
            runOnUiThread {
                Toast.makeText(this, "No se pudo abrir el archivo.", Toast.LENGTH_LONG).show()
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
        runOnUiThread {
            Toast.makeText(this, "Error al leer el archivo Excel: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun mostrarDatosEnRecyclerView(stockList: MutableList<StockItem>) {
        runOnUiThread {
            Log.d("StockGeneralActivity", "Tamaño de stockList: ${stockList.size}")
            if (stockList.isNotEmpty()) {
                Log.d("StockGeneralActivity", "Primer elemento: ${stockList[0]}")
            }
            stockAdapter.stockList.clear()
            stockAdapter.stockList.addAll(stockList)
            stockAdapter.notifyDataSetChanged()
        }
    }

    private fun guardarStockItems(stockList: MutableList<StockItem>) {
        val sharedPreferences = getSharedPreferences("StockData", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        val gson = Gson()
        val stockListJson = gson.toJson(stockList)
        editor.putString("stockList", stockListJson)
        editor.apply()
    }

    private fun filterStockItems(query: String) {
        val filteredList = if (query.isNotEmpty()) {
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
            }.toMutableList()
        } else {
            allStockItems
        }
        stockAdapter.stockList.clear()
        stockAdapter.stockList.addAll(filteredList)
        stockAdapter.notifyDataSetChanged()
    }
}