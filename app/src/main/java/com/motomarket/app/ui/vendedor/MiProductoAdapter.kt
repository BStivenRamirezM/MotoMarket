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
import com.motomarket.app.ui.common.cargarImagenBase64
import com.motomarket.app.ui.common.mostrarEstado
import com.motomarket.app.util.formatearCOP

/** Publicaciones del vendedor con su estado de moderación; al tocar una se abre para editarla. */
class MiProductoAdapter(
    private val onProductoClick: (Producto) -> Unit
) : ListAdapter<Producto, MiProductoAdapter.MiProductoViewHolder>(MiProductoDiff) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MiProductoViewHolder {
        val binding = ItemMiProductoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MiProductoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MiProductoViewHolder, position: Int) = holder.bind(getItem(position))

    /** La línea de detalle explica el estado: en revisión, motivo de rechazo o stock disponible. */
    inner class MiProductoViewHolder(private val binding: ItemMiProductoBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(producto: Producto) {
            val context = binding.root.context
            binding.ivImagen.cargarImagenBase64(producto.imagenBase64)
            binding.tvNombre.text = producto.nombre
            binding.tvPrecio.text = formatearCOP(producto.precio)
            binding.tvDetalle.text = when (producto.estado) {
                EstadoProducto.PENDIENTE -> context.getString(R.string.mis_productos_pendiente_aprobacion)
                EstadoProducto.RECHAZADO -> context.getString(R.string.aprobacion_detalle_rechazado, producto.motivoRechazo)
                else -> context.getString(R.string.mis_productos_stock, producto.stock)
            }
            binding.tvEstado.mostrarEstado(producto.estado)
            binding.root.setOnClickListener { onProductoClick(producto) }
        }
    }
}

/** Compara por id y por contenido para que la lista solo redibuje los elementos que cambian. */
private object MiProductoDiff : DiffUtil.ItemCallback<Producto>() {
    override fun areItemsTheSame(oldItem: Producto, newItem: Producto) = oldItem.id == newItem.id
    override fun areContentsTheSame(oldItem: Producto, newItem: Producto) = oldItem == newItem
}
