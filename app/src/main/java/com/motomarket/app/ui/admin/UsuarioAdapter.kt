package com.motomarket.app.ui.admin

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.motomarket.app.R
import com.motomarket.app.databinding.ItemUsuarioBinding
import com.motomarket.app.models.Usuario
import com.motomarket.app.ui.common.etiqueta
import com.motomarket.app.ui.common.iniciales

class UsuarioAdapter(
    private val onCambiarEstado: (Usuario, Boolean) -> Unit
) : ListAdapter<Usuario, UsuarioAdapter.UsuarioViewHolder>(UsuarioDiff) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UsuarioViewHolder {
        val binding = ItemUsuarioBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return UsuarioViewHolder(binding)
    }

    override fun onBindViewHolder(holder: UsuarioViewHolder, position: Int) = holder.bind(getItem(position))

    inner class UsuarioViewHolder(private val binding: ItemUsuarioBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(usuario: Usuario) {
            val context = binding.root.context
            binding.tvAvatar.text = iniciales(usuario.nombre)
            binding.tvNombre.text = usuario.nombre
            binding.tvRol.text = context.getString(
                R.string.usuarios_rol_estado,
                context.getString(usuario.rol.etiqueta()),
                context.getString(if (usuario.activo) R.string.usuarios_activo else R.string.usuarios_inactivo)
            )
            binding.swActivo.setOnCheckedChangeListener(null)
            binding.swActivo.isChecked = usuario.activo
            binding.swActivo.setOnCheckedChangeListener { _, activo -> onCambiarEstado(usuario, activo) }
        }
    }
}

private object UsuarioDiff : DiffUtil.ItemCallback<Usuario>() {
    override fun areItemsTheSame(oldItem: Usuario, newItem: Usuario) = oldItem.id == newItem.id
    override fun areContentsTheSame(oldItem: Usuario, newItem: Usuario) = oldItem == newItem
}
