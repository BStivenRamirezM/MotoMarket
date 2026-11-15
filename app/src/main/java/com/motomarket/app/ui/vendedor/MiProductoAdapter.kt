package com.motomarket.app.ui.vendedor

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.motomarket.app.R
import com.motomarket.app.databinding.ItemMiProductoBinding
import com.motomarket.app.models.EstadoProducto
import com.motomarket.app.models.Producto
import com.motomarket.app.ui.common.mostrarEstado
import com.motomarket.app.util.formatearCOP

class MiProductoAdapter : ListAdapter<Producto, MiProductoAdapter.MiProductoViewHolder>(MiProductoDiff) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MiProductoViewHolder {
        val binding = ItemMiProductoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MiProductoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MiProductoViewHolder, position: Int) = holder.bind(getItem(position))

    class MiProductoViewHolder(private val binding: ItemMiProductoBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(producto: Producto) {
            val context = binding.root.context
            binding.tvNombre.text = producto.nombre
            binding.tvPrecio.text = formatearCOP(producto.precio)
            binding.tvDetalle.text = if (producto.estado == EstadoProducto.EN_REVISION) {
                context.getString(R.string.mis_productos_pendiente_aprobacion)
            } else {
                context.getString(R.string.mis_productos_stock, producto.stock)
            }
            binding.tvEstado.mostrarEstado(producto.estado)
        }
    }
}

private object MiProductoDiff : DiffUtil.ItemCallback<Producto>() {
    override fun areItemsTheSame(oldItem: Producto, newItem: Producto) = oldItem.id == newItem.id
    override fun areContentsTheSame(oldItem: Producto, newItem: Producto) = oldItem == newItem
}
