package com.tuempresa.inventario

import androidx.recyclerview.widget.DiffUtil

class ProductoDiffCallback(
    private val oldList: List<Producto>,
    private val newList: List<Producto>
) : DiffUtil.Callback() {

    override fun getOldListSize() = oldList.size

    override fun getNewListSize() = newList.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition].codigo == newList[newItemPosition].codigo
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition] == newList[newItemPosition]
    }
}