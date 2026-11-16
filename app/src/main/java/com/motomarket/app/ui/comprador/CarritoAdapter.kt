package com.motomarket.app.ui.comprador

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.motomarket.app.R
import com.motomarket.app.databinding.ItemCarritoBinding
import com.motomarket.app.models.ItemCarrito
import com.motomarket.app.ui.common.cargarImagenBase64
import com.motomarket.app.util.formatearCOP

/** Ítems del carrito con su cantidad, subtotal y botón para eliminarlos. */
class CarritoAdapter(
    private val onEliminar: (ItemCarrito) -> Unit
) : ListAdapter<ItemCarrito, CarritoAdapter.CarritoViewHolder>(CarritoDiff) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CarritoViewHolder {
        val binding = ItemCarritoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CarritoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CarritoViewHolder, position: Int) = holder.bind(getItem(position))

    /** Muestra la miniatura, "Cant: X · $subtotal" y la papelera del ítem. */
    inner class CarritoViewHolder(private val binding: ItemCarritoBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ItemCarrito) {
            binding.ivImagen.cargarImagenBase64(item.producto.imagenBase64)
            binding.tvNombre.text = item.producto.nombre
            binding.tvCantidadPrecio.text = binding.root.context.getString(
                R.string.carrito_cantidad_precio, item.cantidad, formatearCOP(item.subtotal)
            )
            binding.btnEliminar.setOnClickListener { onEliminar(item) }
        }
    }
}

/** Compara por id y por contenido para que la lista solo redibuje los elementos que cambian. */
private object CarritoDiff : DiffUtil.ItemCallback<ItemCarrito>() {
    override fun areItemsTheSame(oldItem: ItemCarrito, newItem: ItemCarrito) = oldItem.producto.id == newItem.producto.id
    override fun areContentsTheSame(oldItem: ItemCarrito, newItem: ItemCarrito) = oldItem == newItem
}
