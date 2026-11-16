package com.motomarket.app.ui.servicios

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.motomarket.app.R
import com.motomarket.app.databinding.ItemServicioBinding
import com.motomarket.app.models.ServicioTecnico
import com.motomarket.app.ui.common.mostrarEstado
import com.motomarket.app.util.formatearCOP
import com.motomarket.app.util.formatearFecha

/**
 * Lista de servicios técnicos, compartida por comprador y mecánico. Con [vistaMecanico] la línea de
 * detalle muestra al cliente; si no, muestra el taller.
 */
class ServicioAdapter(
    private val vistaMecanico: Boolean,
    private val onServicioClick: (ServicioTecnico) -> Unit
) : ListAdapter<ServicioTecnico, ServicioAdapter.ServicioViewHolder>(ServicioDiff) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ServicioViewHolder {
        val binding = ItemServicioBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ServicioViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ServicioViewHolder, position: Int) = holder.bind(getItem(position))

    /** Número y moto, contraparte con fecha, total (o "por cotizar") y chip de estado. */
    inner class ServicioViewHolder(private val binding: ItemServicioBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(servicio: ServicioTecnico) {
            val context = binding.root.context
            binding.tvTitulo.text = context.getString(R.string.servicio_titulo_item, servicio.numero, servicio.moto)
            val contraparte = if (vistaMecanico) servicio.compradorNombre else servicio.mecanicoNombre
            binding.tvDetalle.text = context.getString(R.string.servicio_detalle_item, contraparte, formatearFecha(servicio.fechaSolicitud))
            binding.tvTotal.text = if (servicio.lineas.isEmpty()) {
                context.getString(R.string.servicio_por_cotizar)
            } else {
                formatearCOP(servicio.total)
            }
            binding.tvEstado.mostrarEstado(servicio.estado)
            binding.root.setOnClickListener { onServicioClick(servicio) }
        }
    }
}

/** Compara por id y por contenido para que la lista solo redibuje los elementos que cambian. */
private object ServicioDiff : DiffUtil.ItemCallback<ServicioTecnico>() {
    override fun areItemsTheSame(oldItem: ServicioTecnico, newItem: ServicioTecnico) = oldItem.id == newItem.id
    override fun areContentsTheSame(oldItem: ServicioTecnico, newItem: ServicioTecnico) = oldItem == newItem
}
