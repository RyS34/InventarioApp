package com.tuempresa.inventario

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.tuempresa.inventario.model.StockItem

class StockAdapter(
    var stockList: MutableList<StockItem>, // Cambiado a MutableList
    private val itemClickListener: (StockItem) -> Unit
) : RecyclerView.Adapter<StockAdapter.StockViewHolder>() {

    private var selectedPosition = RecyclerView.NO_POSITION

    class StockViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvCodigo: TextView = itemView.findViewById(R.id.tvCodigo)
        val tvDescripcion: TextView = itemView.findViewById(R.id.tvDescripcion)
        val tvLote: TextView = itemView.findViewById(R.id.tvLote)
        val tvCantidad: TextView = itemView.findViewById(R.id.tvCantidad)
        val tvFechaVencimiento: TextView = itemView.findViewById(R.id.tvFechaVencimiento)
        val tvUbicacion: TextView = itemView.findViewById(R.id.tvUbicacion)
        val tvUbicacionDetallada: TextView = itemView.findViewById(R.id.tvUbicacionDetallada)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StockViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_stock, parent, false)
        return StockViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: StockViewHolder, position: Int) {
        val currentItem = stockList[position]

        holder.tvCodigo.text = currentItem.codigo
        holder.tvDescripcion.text = currentItem.descripcion
        holder.tvLote.text = currentItem.lote ?: ""
        holder.tvCantidad.text = currentItem.cantidad.toString()
        holder.tvFechaVencimiento.text = currentItem.fechaVencimiento
        holder.tvUbicacion.text = currentItem.ubicacion
        holder.tvUbicacionDetallada.text = currentItem.ubicacionDetallada

        if (position == selectedPosition) {
            holder.itemView.setBackgroundResource(R.color.primary_variant)
            holder.tvCodigo.setTextColor(Color.WHITE)
            holder.tvDescripcion.setTextColor(Color.WHITE)
            holder.tvLote.setTextColor(Color.WHITE)
            holder.tvCantidad.setTextColor(Color.WHITE)
            holder.tvFechaVencimiento.setTextColor(Color.WHITE)
            holder.tvUbicacion.setTextColor(Color.WHITE)
            holder.tvUbicacionDetallada.setTextColor(Color.WHITE)
        } else {
            holder.itemView.setBackgroundColor(Color.TRANSPARENT)
            holder.tvCodigo.setTextColor(holder.itemView.context.getColor(R.color.black))
            holder.tvDescripcion.setTextColor(holder.itemView.context.getColor(R.color.black))
            holder.tvLote.setTextColor(holder.itemView.context.getColor(R.color.warm_gray))
            holder.tvCantidad.setTextColor(holder.itemView.context.getColor(R.color.primary))
            holder.tvFechaVencimiento.setTextColor(holder.itemView.context.getColor(R.color.warm_gray))
            holder.tvUbicacion.setTextColor(holder.itemView.context.getColor(R.color.warm_gray))
            holder.tvUbicacionDetallada.setTextColor(holder.itemView.context.getColor(R.color.warm_gray))
        }

        holder.itemView.setOnClickListener {
            val previousSelectedPosition = selectedPosition
            selectedPosition = if (position == selectedPosition) {
                RecyclerView.NO_POSITION
            } else {
                position
            }
            if (previousSelectedPosition != RecyclerView.NO_POSITION) {
                notifyItemChanged(previousSelectedPosition)
            }
            if (selectedPosition != RecyclerView.NO_POSITION) {
                notifyItemChanged(selectedPosition)
            }
            itemClickListener(currentItem)
        }
    }

    override fun getItemCount() = stockList.size
}