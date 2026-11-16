package com.motomarket.app.ui.admin

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.motomarket.app.R
import com.motomarket.app.databinding.ItemModeracionBinding
import com.motomarket.app.models.EstadoProducto
import com.motomarket.app.models.Producto
import com.motomarket.app.ui.common.cargarImagen
import com.motomarket.app.ui.common.mostrarEstado
import com.motomarket.app.util.formatearFechaCorta

/** Publicaciones a moderar; los botones Aprobar/Rechazar solo se muestran en las pendientes. */
class ModeracionAdapter(
    private val onDecision: (Producto, EstadoProducto) -> Unit = { _, _ -> }
) : ListAdapter<Producto, ModeracionAdapter.ModeracionViewHolder>(ModeracionDiff) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ModeracionViewHolder {
        val binding = ItemModeracionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ModeracionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ModeracionViewHolder, position: Int) = holder.bind(getItem(position))

    /** La línea de detalle muestra el vendedor, el motivo de rechazo o la fecha de aprobación. */
    inner class ModeracionViewHolder(private val binding: ItemModeracionBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(producto: Producto) {
            val context = binding.root.context
            binding.ivImagen.cargarImagen(producto.imagenUrl)
            binding.tvNombre.text = producto.nombre
            binding.tvDetalle.text = when (producto.estado) {
                EstadoProducto.RECHAZADO -> context.getString(R.string.aprobacion_detalle_rechazado, producto.motivoRechazo)
                EstadoProducto.APROBADO, EstadoProducto.AGOTADO ->
                    context.getString(R.string.aprobacion_detalle_aprobado, formatearFechaCorta(producto.fechaRevision))
                EstadoProducto.PENDIENTE -> context.getString(R.string.aprobacion_detalle_vendedor, producto.vendedorNombre)
            }
            binding.tvEstado.mostrarEstado(producto.estado)
            binding.contenedorAcciones.isVisible = producto.estado == EstadoProducto.PENDIENTE
            binding.btnAprobar.setOnClickListener { onDecision(producto, EstadoProducto.APROBADO) }
            binding.btnRechazar.setOnClickListener { onDecision(producto, EstadoProducto.RECHAZADO) }
        }
    }
}

/** Compara por id y por contenido para que la lista solo redibuje los elementos que cambian. */
private object ModeracionDiff : DiffUtil.ItemCallback<Producto>() {
    override fun areItemsTheSame(oldItem: Producto, newItem: Producto) = oldItem.id == newItem.id
    override fun areContentsTheSame(oldItem: Producto, newItem: Producto) = oldItem == newItem
}
