package com.tuempresa.inventario

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ProductosBusquedaAdapter(val productos: MutableList<Producto>) : RecyclerView.Adapter<ProductosBusquedaAdapter.ProductoViewHolder>() {

    class ProductoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvCodigo: TextView = itemView.findViewById(R.id.tvCodigo)
        val tvDescripcion: TextView = itemView.findViewById(R.id.tvDescripcion)
        val tvLote: TextView = itemView.findViewById(R.id.tvLote)
        val tvCantidad: TextView = itemView.findViewById(R.id.tvCantidad) // Asegúrate que el ID es correcto
        // Agregar más TextViews según sea necesario
        fun bind(producto: Producto) {
            tvCodigo.text = producto.codigo
            tvDescripcion.text = producto.descripcion
            tvLote.text = producto.lote
            tvCantidad.text = producto.cantidad.toString()
        }
    }
    // Metodo para actualizar la lista de productos
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductoViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.item_producto_busqueda, parent, false)
        return ProductoViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ProductoViewHolder, position: Int) {
        val producto = productos[position]
        holder.bind(producto)// Agregar logs para verificar el tamaño de la lista y el contenido del producto
        // Agregar logs para verificar el tamaño de la lista y el contenido del producto
        Log.d("ProductosBusquedaAdapter", "onBindViewHolder: Tamaño de la lista: ${productos.size}")
        Log.d("ProductosBusquedaAdapter", "onBindViewHolder: Producto: ${producto.codigo}, ${producto.descripcion}, ${producto.lote}, ${producto.cantidad}")
    }
    // Metodo para actualizar la lista de productos
    override fun getItemCount(): Int {
        return productos.size
    }
    // Metodo para actualizar la lista de productos
    fun updateList(newList: List<Producto>) {
        Log.d("ProductosBusquedaAdapter", "updateList: Tamaño de newList: ${newList.size}")
        productos.clear() // Limpiar la lista actual
        productos.addAll(newList)
        notifyDataSetChanged()
    }
}