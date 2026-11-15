package com.motomarket.app.ui.comprador

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.motomarket.app.R
import com.motomarket.app.databinding.ItemCarritoBinding
import com.motomarket.app.models.ItemCarrito
import com.motomarket.app.util.formatearCOP

class CarritoAdapter : ListAdapter<ItemCarrito, CarritoAdapter.CarritoViewHolder>(CarritoDiff) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CarritoViewHolder {
        val binding = ItemCarritoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CarritoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CarritoViewHolder, position: Int) = holder.bind(getItem(position))

    class CarritoViewHolder(private val binding: ItemCarritoBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ItemCarrito) {
            binding.tvNombre.text = item.producto.nombre
            binding.tvCantidadPrecio.text = binding.root.context.getString(
                R.string.carrito_cantidad_precio, item.cantidad, formatearCOP(item.subtotal)
            )
        }
    }
}

private object CarritoDiff : DiffUtil.ItemCallback<ItemCarrito>() {
    override fun areItemsTheSame(oldItem: ItemCarrito, newItem: ItemCarrito) = oldItem.producto.id == newItem.producto.id
    override fun areContentsTheSame(oldItem: ItemCarrito, newItem: ItemCarrito) = oldItem == newItem
}
