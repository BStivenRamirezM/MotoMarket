package com.motomarket.app.ui.comprador

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.motomarket.app.databinding.ItemProductoBinding
import com.motomarket.app.models.EstadoProducto
import com.motomarket.app.models.Producto
import com.motomarket.app.ui.common.cargarImagenBase64
import com.motomarket.app.ui.common.mostrarEstado
import com.motomarket.app.util.formatearCOP

/** Tarjetas de la grilla del catálogo; notifica el producto tocado para abrir su detalle. */
class ProductoAdapter(
    private val onProductoClick: (Producto) -> Unit
) : ListAdapter<Producto, ProductoAdapter.ProductoViewHolder>(ProductoDiff) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductoViewHolder {
        val binding = ItemProductoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ProductoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ProductoViewHolder, position: Int) = holder.bind(getItem(position))

    /** Muestra foto, nombre, precio y el chip "Agotado" cuando aplica. */
    inner class ProductoViewHolder(private val binding: ItemProductoBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(producto: Producto) {
            binding.ivImagen.cargarImagenBase64(producto.imagenBase64)
            binding.tvNombre.text = producto.nombre
            binding.tvPrecio.text = formatearCOP(producto.precio)
            binding.tvEstado.isVisible = producto.estado == EstadoProducto.AGOTADO || producto.stock == 0
            binding.tvEstado.mostrarEstado(EstadoProducto.AGOTADO)
            binding.root.setOnClickListener { onProductoClick(producto) }
        }
    }
}

/** Compara por id y por contenido para que la lista solo redibuje los elementos que cambian. */
private object ProductoDiff : DiffUtil.ItemCallback<Producto>() {
    override fun areItemsTheSame(oldItem: Producto, newItem: Producto) = oldItem.id == newItem.id
    override fun areContentsTheSame(oldItem: Producto, newItem: Producto) = oldItem == newItem
}
