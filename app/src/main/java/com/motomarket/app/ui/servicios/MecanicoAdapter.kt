package com.motomarket.app.ui.servicios

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.motomarket.app.R
import com.motomarket.app.databinding.ItemMecanicoBinding
import com.motomarket.app.models.Mecanico
import com.motomarket.app.ui.common.iniciales
import com.motomarket.app.util.formatearCalificacion

/** Mecánicos disponibles como opciones de selección única, resaltando el elegido. */
class MecanicoAdapter(
    private val onSeleccion: (Mecanico) -> Unit
) : ListAdapter<Mecanico, MecanicoAdapter.MecanicoViewHolder>(MecanicoDiff) {

    private var seleccionadoId: String? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MecanicoViewHolder {
        val binding = ItemMecanicoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MecanicoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MecanicoViewHolder, position: Int) = holder.bind(getItem(position))

    /** Marca [mecanico] como elegido y redibuja la lista para mover el resaltado. */
    private fun seleccionar(mecanico: Mecanico) {
        seleccionadoId = mecanico.id
        notifyItemRangeChanged(0, itemCount)
        onSeleccion(mecanico)
    }

    /** Taller, mecánico con teléfono y calificación, y la etiqueta "Elegir" / "Elegido". */
    inner class MecanicoViewHolder(private val binding: ItemMecanicoBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(mecanico: Mecanico) {
            val context = binding.root.context
            val elegido = mecanico.id == seleccionadoId
            binding.tvAvatar.text = iniciales(mecanico.nombreTaller)
            binding.tvTaller.text = mecanico.nombreTaller
            val calificacion = if (mecanico.calificacion > 0) {
                context.getString(R.string.calificacion_estrella, formatearCalificacion(mecanico.calificacion))
            } else {
                context.getString(R.string.sin_calificaciones)
            }
            binding.tvDetalle.text = context.getString(R.string.mecanico_detalle_item, mecanico.nombre, mecanico.telefono, calificacion)
            binding.tvElegir.setText(if (elegido) R.string.pago_elegido else R.string.pago_elegir)
            binding.root.isSelected = elegido
            binding.root.setOnClickListener { seleccionar(mecanico) }
        }
    }
}

/** Compara por id y por contenido para que la lista solo redibuje los elementos que cambian. */
private object MecanicoDiff : DiffUtil.ItemCallback<Mecanico>() {
    override fun areItemsTheSame(oldItem: Mecanico, newItem: Mecanico) = oldItem.id == newItem.id
    override fun areContentsTheSame(oldItem: Mecanico, newItem: Mecanico) = oldItem == newItem
}
