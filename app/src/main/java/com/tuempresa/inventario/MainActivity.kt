package com.tuempresa.inventario

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.tuempresa.inventario.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.buttonIngresoStock.setOnClickListener {
            val intent = Intent(this, IngresoStockActivity::class.java)
            startActivity(intent)
        }

        binding.buttonEntradaProductos.setOnClickListener {
            val intent = Intent(this, EntradaProductos::class.java)
            startActivity(intent)
        }

        binding.buttonSalidaProductos.setOnClickListener {
            val intent = Intent(this, SalidaProductosActivity::class.java)
            startActivity(intent)
        }

        binding.buttonStockGeneral.setOnClickListener {
            val intent = Intent(this, StockGeneralActivity::class.java)
            startActivity(intent)
        }
    }
}