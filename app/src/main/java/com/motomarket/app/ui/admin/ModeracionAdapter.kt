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
import com.motomarket.app.models.Moderacion
import com.motomarket.app.ui.common.mostrarEstado

class ModeracionAdapter(
    private val onDecision: (Moderacion, EstadoProducto) -> Unit = { _, _ -> }
) : ListAdapter<Moderacion, ModeracionAdapter.ModeracionViewHolder>(ModeracionDiff) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ModeracionViewHolder {
        val binding = ItemModeracionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ModeracionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ModeracionViewHolder, position: Int) = holder.bind(getItem(position))

    inner class ModeracionViewHolder(private val binding: ItemModeracionBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(moderacion: Moderacion) {
            val context = binding.root.context
            binding.tvNombre.text = moderacion.nombreProducto
            binding.tvDetalle.text = when (moderacion.estado) {
                EstadoProducto.RECHAZADO -> context.getString(R.string.aprobacion_detalle_rechazado, moderacion.motivo)
                EstadoProducto.APROBADO -> context.getString(R.string.aprobacion_detalle_aprobado, moderacion.fecha)
                else -> context.getString(R.string.aprobacion_detalle_vendedor, moderacion.vendedor)
            }
            binding.tvEstado.mostrarEstado(moderacion.estado)
            binding.contenedorAcciones.isVisible = moderacion.estado == EstadoProducto.PENDIENTE
            binding.btnAprobar.setOnClickListener { onDecision(moderacion, EstadoProducto.APROBADO) }
            binding.btnRechazar.setOnClickListener { onDecision(moderacion, EstadoProducto.RECHAZADO) }
        }
    }
}

private object ModeracionDiff : DiffUtil.ItemCallback<Moderacion>() {
    override fun areItemsTheSame(oldItem: Moderacion, newItem: Moderacion) = oldItem.id == newItem.id
    override fun areContentsTheSame(oldItem: Moderacion, newItem: Moderacion) = oldItem == newItem
}
