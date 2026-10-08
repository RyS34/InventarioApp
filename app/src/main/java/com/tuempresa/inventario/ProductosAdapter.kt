package com.tuempresa.inventario

import android.annotation.SuppressLint
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView

class ProductosAdapter(private var productosList: MutableList<Producto> = mutableListOf()) :
    RecyclerView.Adapter<ProductosAdapter.ProductoViewHolder>() {

    var selectedPosition = RecyclerView.NO_POSITION

    fun updateList(newList: List<Producto>) {
        val diffResult = DiffUtil.calculateDiff(ProductoDiffCallback(productosList, newList))
        productosList.clear()
        productosList.addAll(newList)
        diffResult.dispatchUpdatesTo(this)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductoViewHolder {
        val itemView =
            LayoutInflater.from(parent.context).inflate(R.layout.item_producto_salida, parent, false)
        return ProductoViewHolder(itemView)
    }

    @SuppressLint("SetTextI18n", "NotifyDataSetChanged")
    override fun onBindViewHolder(holder: ProductoViewHolder, position: Int) {
        val producto = productosList[position]
        holder.tvCodigo.text = producto.codigo
        holder.tvDescripcion.text = producto.descripcion
        holder.tvLote.text = producto.lote
        holder.tvCantidad.text = producto.cantidad.toString()
        holder.tvUsuario.text = producto.usuario

        // Establecer el color de fondo del elemento seleccionado
        holder.itemView.setOnClickListener {
            val newPosition = holder.bindingAdapterPosition
            if (newPosition != RecyclerView.NO_POSITION) {
                selectedPosition = newPosition
                notifyDataSetChanged()
            }
        }

        // Establecer el color de fondo y textos del elemento seleccionado
        if (selectedPosition == position) {
            holder.itemView.setBackgroundResource(R.color.primary_variant)
            holder.tvDescripcion.setTextColor(Color.WHITE)
            holder.tvCodigo.setTextColor(Color.WHITE)
            holder.tvCantidad.setTextColor(Color.WHITE)
            holder.tvLote.setTextColor(Color.WHITE)
        } else {
            holder.itemView.setBackgroundColor(Color.TRANSPARENT)
            holder.tvDescripcion.setTextColor(holder.itemView.context.getColor(R.color.text_primary))
            holder.tvCodigo.setTextColor(holder.itemView.context.getColor(R.color.text_secondary))
            holder.tvCantidad.setTextColor(holder.itemView.context.getColor(R.color.primary))
            holder.tvLote.setTextColor(holder.itemView.context.getColor(R.color.text_hint))
        }
    }

    override fun getItemCount() = productosList.size

    class ProductoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvCodigo: TextView = itemView.findViewById(R.id.tvCodigo)
        val tvDescripcion: TextView = itemView.findViewById(R.id.tvDescripcion)
        val tvLote: TextView = itemView.findViewById(R.id.tvLote)
        val tvCantidad: TextView = itemView.findViewById(R.id.tvCantidad)
        val tvUsuario: TextView = itemView.findViewById(R.id.tvUsuario)
    }
}