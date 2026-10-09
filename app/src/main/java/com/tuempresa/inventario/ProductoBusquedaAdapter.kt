package com.tuempresa.inventario

import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ProductosBusquedaAdapter(
    val productos: MutableList<Producto>,
    private val onItemClick: (Producto) -> Unit
) : RecyclerView.Adapter<ProductosBusquedaAdapter.ProductoViewHolder>() {

    var selectedPosition = RecyclerView.NO_POSITION

    class ProductoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvCodigo: TextView = itemView.findViewById(R.id.tvCodigo)
        val tvDescripcion: TextView = itemView.findViewById(R.id.tvDescripcion)
        val tvLote: TextView = itemView.findViewById(R.id.tvLote)
        val tvCantidad: TextView = itemView.findViewById(R.id.tvCantidad)
        
        fun bind(producto: Producto) {
            tvCodigo.text = producto.codigo
            tvDescripcion.text = producto.descripcion
            tvLote.text = producto.lote
            tvCantidad.text = producto.cantidad.toString()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductoViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.item_producto_busqueda, parent, false)
        return ProductoViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ProductoViewHolder, position: Int) {
        val producto = productos[position]
        holder.bind(producto)

        // Efecto de selección
        if (selectedPosition == position) {
            holder.itemView.setBackgroundResource(R.color.primary_variant)
            holder.tvDescripcion.setTextColor(Color.WHITE)
            holder.tvCodigo.setTextColor(Color.WHITE)
            holder.tvCantidad.setTextColor(Color.WHITE)
            holder.tvLote.setTextColor(Color.WHITE)
        } else {
            holder.itemView.setBackgroundColor(Color.TRANSPARENT)
            holder.tvDescripcion.setTextColor(holder.itemView.context.getColor(R.color.black))
            holder.tvCodigo.setTextColor(holder.itemView.context.getColor(R.color.warm_gray))
            holder.tvCantidad.setTextColor(holder.itemView.context.getColor(R.color.primary))
            holder.tvLote.setTextColor(holder.itemView.context.getColor(R.color.warm_gray))
        }

        holder.itemView.setOnClickListener {
            val oldPosition = selectedPosition
            selectedPosition = holder.bindingAdapterPosition
            notifyItemChanged(oldPosition)
            notifyItemChanged(selectedPosition)
            onItemClick(producto)
        }
    }

    override fun getItemCount(): Int {
        return productos.size
    }

    fun updateList(newList: List<Producto>) {
        Log.d("ProductosBusquedaAdapter", "updateList: Tamaño de newList: ${newList.size}")
        productos.clear()
        productos.addAll(newList)
        notifyDataSetChanged()
    }
}