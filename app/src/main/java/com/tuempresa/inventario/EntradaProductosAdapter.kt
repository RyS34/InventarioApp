package com.tuempresa.inventario

import android.annotation.SuppressLint
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.tuempresa.inventario.model.StockItem

class EntradaProductosAdapter(
    private var stockList: MutableList<StockItem>,
    private val onItemClick: (StockItem) -> Unit
) : RecyclerView.Adapter<EntradaProductosAdapter.ViewHolder>() {

    private var selectedPosition = RecyclerView.NO_POSITION // Variable para rastrear la posición seleccionada

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvCodigo: TextView = itemView.findViewById(R.id.tvCodigo)
        val tvDescripcion: TextView = itemView.findViewById(R.id.tvDescripcion)
        val tvLote: TextView = itemView.findViewById(R.id.tvLote)
        val tvCantidad: TextView = itemView.findViewById(R.id.tvCantidad)
        val tvUsuario: TextView = itemView.findViewById(R.id.tvUsuario)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_stock_entrada, parent, false)
        return ViewHolder(view)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, @SuppressLint("RecyclerView") position: Int) {
        val stockItem = stockList[position]
        holder.tvCodigo.text = stockItem.codigo
        holder.tvDescripcion.text = stockItem.descripcion
        holder.tvLote.text = stockItem.lote
        holder.tvCantidad.text = stockItem.cantidad.toString() // Concatenar la cantidad
        holder.tvUsuario.text = stockItem.usuario // Concatenar el usuario

        // Cambiar el color de fondo si el elemento está seleccionado
        if (position == selectedPosition) {
            holder.itemView.setBackgroundColor(Color.LTGRAY) // Cambia el color a gris claro (puedes usar otro color)
        } else {
            holder.itemView.setBackgroundColor(Color.TRANSPARENT) // Restaura el color de fondo predeterminado
        }

        holder.itemView.setOnClickListener {
            selectedPosition = position // Actualiza la posición seleccionada
            notifyDataSetChanged() // Notifica al RecyclerView para que se actualice
            onItemClick(stockItem)
        }
    }

    override fun getItemCount(): Int {
        return stockList.size
    }

    fun updateStockList(newList: MutableList<StockItem>) {
        stockList = newList
        notifyDataSetChanged()
    }
}