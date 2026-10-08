package com.tuempresa.inventario

import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.tuempresa.inventario.model.StockItem
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class IngresoStockActivity : AppCompatActivity() {

    private lateinit var etFechaIngreso: EditText
    private lateinit var etFechaVencimiento: EditText
    private val calendar: Calendar = Calendar.getInstance()

    private lateinit var etCodigo: EditText
    private lateinit var etDescripcion: EditText
    private lateinit var etLote: EditText
    private lateinit var etCantidad: EditText
    private lateinit var etOrdenCompra: EditText
    private lateinit var spinnerUbicacion: Spinner
    private lateinit var spinnerUbicacionDetallada: Spinner

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ingreso_stock)

        // Inicializar las vistas
        etCodigo = findViewById(R.id.etCodigo3)
        etDescripcion = findViewById(R.id.etDescripcion3)
        etLote = findViewById(R.id.etLote3)
        etCantidad = findViewById(R.id.etCantidad3)
        etFechaIngreso = findViewById(R.id.etFechaIngreso)
        etFechaVencimiento = findViewById(R.id.etFechaVencimiento)
        etOrdenCompra = findViewById(R.id.etOrdenCompra)
        spinnerUbicacion = findViewById(R.id.spinnerUbicacion)
        spinnerUbicacionDetallada = findViewById(R.id.spinnerUbicacionDetallada)
        val btnIngresar = findViewById<Button>(R.id.buttonIngresar)
        val btnSalir = findViewById<Button>(R.id.buttonSalir1)

        val ubicaciones = resources.getStringArray(R.array.Ubicacion)
        val ubicacionesDetalladas = resources.getStringArray(R.array.UbicacionDetallada)

        val adaptadorUbicaciones = ArrayAdapter(this, android.R.layout.simple_spinner_item, ubicaciones)
        val adaptadorUbicacionesDetalladas = ArrayAdapter(this, android.R.layout.simple_spinner_item, ubicacionesDetalladas)

        adaptadorUbicaciones.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        adaptadorUbicacionesDetalladas.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        spinnerUbicacion.adapter = adaptadorUbicaciones
        spinnerUbicacionDetallada.adapter = adaptadorUbicacionesDetalladas

        etFechaIngreso.setOnClickListener {
            mostrarDatePicker(etFechaIngreso)
        }

        etFechaVencimiento.setOnClickListener {
            mostrarDatePicker(etFechaVencimiento)
        }

        btnIngresar.setOnClickListener {
            if (validarCampos()) {
                val codigo = etCodigo.text.toString()
                val descripcion = etDescripcion.text.toString()
                val lote = etLote.text.toString()
                val cantidadString = etCantidad.text.toString()
                val fechaVencimiento = etFechaVencimiento.text.toString()
                val ubicacion = spinnerUbicacion.selectedItem.toString()
                val ubicacionDetallada = spinnerUbicacionDetallada.selectedItem.toString()
                val fechaIngreso = etFechaIngreso.text.toString()
                val ordenCompra = etOrdenCompra.text.toString()

                val cantidad = cantidadString.toIntOrNull() ?: 0

                // Crear un objeto StockItem
                val stockItem = StockItem(
                    codigo = codigo,
                    descripcion = descripcion,
                    lote = lote,
                    cantidad = cantidad, // Asignar directamente el Int
                    fechaIngreso = fechaIngreso,
                    fechaVencimiento = fechaVencimiento,
                    ordenCompra = ordenCompra,
                    ubicacion = ubicacion,
                    ubicacionDetallada = ubicacionDetallada
                )

                guardarStockItem(stockItem)

                Toast.makeText(this, "Artículo ingresado con éxito.", Toast.LENGTH_SHORT).show()
                limpiarCampos()
            } else {
                Toast.makeText(this, "Por favor, complete todos los campos.", Toast.LENGTH_SHORT).show()
            }
        }

        btnSalir.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        limpiarCampos()
    }

    private fun mostrarDatePicker(editText: EditText) {
        val dateSetListener = DatePickerDialog.OnDateSetListener { _, year, monthOfYear, dayOfMonth ->
            calendar.set(Calendar.YEAR, year)
            calendar.set(Calendar.MONTH, monthOfYear)
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)

            val formatoFecha = "dd/MM/yyyy"
            val sdf = SimpleDateFormat(formatoFecha, Locale.getDefault())
            editText.setText(sdf.format(calendar.time))

            if (editText == etFechaIngreso) {
                etFechaVencimiento.requestFocus()
            } else if (editText == etFechaVencimiento) {
                etOrdenCompra.requestFocus()
            }
        }

        DatePickerDialog(
            this,
            dateSetListener,
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun limpiarCampos() {
        etCodigo.text.clear()
        etDescripcion.text.clear()
        etLote.text.clear()
        etCantidad.text.clear()
        etFechaIngreso.text.clear()
        etFechaVencimiento.text.clear()
        etOrdenCompra.text.clear()
        spinnerUbicacion.setSelection(0)
        spinnerUbicacionDetallada.setSelection(0)
    }

    private fun validarCampos(): Boolean {
        return  etCodigo.text.isNotEmpty() &&
                etDescripcion.text.isNotEmpty() &&
                etLote.text.isNotEmpty() &&
                etCantidad.text.isNotEmpty() &&
                etFechaIngreso.text.isNotEmpty() &&
                etFechaVencimiento.text.isNotEmpty() &&
                etOrdenCompra.text.isNotEmpty() &&
                spinnerUbicacion.selectedItemPosition != 0 &&
                spinnerUbicacionDetallada.selectedItemPosition != 0
    }

    private fun guardarStockItem(stockItem: StockItem) {
        val sharedPreferences = getSharedPreferences("StockData", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        val gson = Gson()

        val stockListJson = sharedPreferences.getString("stockList", null)
        val type = object : TypeToken<MutableList<StockItem>>() {}.type
        val stockList: MutableList<StockItem> = gson.fromJson(stockListJson, type) ?: mutableListOf()

        stockList.add(stockItem)
        val updatedStockListJson = gson.toJson(stockList)
        editor.putString("stockList", updatedStockListJson)
        editor.apply()
    }
}