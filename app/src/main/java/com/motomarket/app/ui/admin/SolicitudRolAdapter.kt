package com.motomarket.app.ui.admin

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.motomarket.app.R
import com.motomarket.app.databinding.ItemSolicitudRolBinding
import com.motomarket.app.models.Usuario
import com.motomarket.app.ui.common.TonoChip
import com.motomarket.app.ui.common.aplicarChip
import com.motomarket.app.ui.common.etiqueta
import com.motomarket.app.ui.common.iniciales
import com.motomarket.app.util.formatearFecha

/** Solicitudes pendientes para ser vendedor o mecánico; el administrador las aprueba o rechaza. */
class SolicitudRolAdapter(
    private val onDecision: (Usuario, Boolean) -> Unit
) : ListAdapter<Usuario, SolicitudRolAdapter.SolicitudViewHolder>(SolicitudDiff) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SolicitudViewHolder {
        val binding = ItemSolicitudRolBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SolicitudViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SolicitudViewHolder, position: Int) = holder.bind(getItem(position))

    /** Muestra el negocio, el rol pedido (chip), quién lo pide y desde cuándo, con los botones de decisión. */
    inner class SolicitudViewHolder(private val binding: ItemSolicitudRolBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(usuario: Usuario) {
            val context = binding.root.context
            binding.tvAvatar.text = iniciales(usuario.nombrePublico)
            binding.tvTienda.text = usuario.nombrePublico
            binding.tvSolicitante.text = context.getString(R.string.solicitudes_solicitante, usuario.nombre, usuario.correo)
            binding.tvFecha.text = context.getString(R.string.solicitudes_fecha, formatearFecha(usuario.fechaSolicitud))
            binding.tvEstado.aplicarChip(usuario.rolSolicitado.etiqueta(), TonoChip.NARANJA)
            binding.btnAprobar.setOnClickListener { onDecision(usuario, true) }
            binding.btnRechazar.setOnClickListener { onDecision(usuario, false) }
        }
    }
}

/** Compara por id y por contenido para que la lista solo redibuje los elementos que cambian. */
private object SolicitudDiff : DiffUtil.ItemCallback<Usuario>() {
    override fun areItemsTheSame(oldItem: Usuario, newItem: Usuario) = oldItem.id == newItem.id
    override fun areContentsTheSame(oldItem: Usuario, newItem: Usuario) = oldItem == newItem
}
