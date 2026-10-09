package com.tuempresa.inventario

import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView

class ProductosBusquedaAdapter(
    val productos: MutableList<Producto>,
    private val onItemClick: (Producto) -> Unit
) : RecyclerView.Adapter<ProductosBusquedaAdapter.ProductoViewHolder>() {

    var selectedPosition = RecyclerView.NO_POSITION

    class ProductoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardView: MaterialCardView = itemView as MaterialCardView
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

        val context = holder.itemView.context
        // Efecto de selección visual consistente usando setCardBackgroundColor
        if (selectedPosition == position) {
            holder.cardView.setCardBackgroundColor(ContextCompat.getColor(context, R.color.primary_variant))
            holder.tvDescripcion.setTextColor(Color.WHITE)
            holder.tvCodigo.setTextColor(Color.WHITE)
            holder.tvCantidad.setTextColor(Color.WHITE)
            holder.tvLote.setTextColor(Color.WHITE)
            holder.cardView.strokeWidth = 0
        } else {
            holder.cardView.setCardBackgroundColor(Color.WHITE)
            holder.tvDescripcion.setTextColor(ContextCompat.getColor(context, R.color.black))
            holder.tvCodigo.setTextColor(ContextCompat.getColor(context, R.color.warm_gray))
            holder.tvCantidad.setTextColor(ContextCompat.getColor(context, R.color.primary))
            holder.tvLote.setTextColor(ContextCompat.getColor(context, R.color.warm_gray))
            holder.cardView.strokeWidth = 1 // mantén el borde sutil si no está seleccionado
        }

        holder.itemView.setOnClickListener {
            val oldPosition = selectedPosition
            selectedPosition = holder.bindingAdapterPosition
            if (oldPosition != RecyclerView.NO_POSITION) {
                notifyItemChanged(oldPosition)
            }
            notifyItemChanged(selectedPosition)
            onItemClick(producto)
        }
    }

    override fun getItemCount(): Int {
        return productos.size
    }

    fun updateList(newList: List<Producto>) {
        // Intentar mantener la selección si el producto sigue en la nueva lista
        val selectedProduct = if (selectedPosition != RecyclerView.NO_POSITION && selectedPosition < productos.size) {
            productos[selectedPosition]
        } else {
            null
        }

        productos.clear()
        productos.addAll(newList)

        if (selectedProduct != null) {
            selectedPosition = productos.indexOfFirst { 
                it.codigo == selectedProduct.codigo && it.lote == selectedProduct.lote 
            }
        } else {
            selectedPosition = RecyclerView.NO_POSITION
        }

        notifyDataSetChanged()
    }
}