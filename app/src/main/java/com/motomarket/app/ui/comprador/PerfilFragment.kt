package com.motomarket.app.ui.comprador

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.motomarket.app.data.Repositorios
import com.motomarket.app.data.UsuarioRepository
import com.motomarket.app.databinding.FragmentPerfilBinding
import com.motomarket.app.databinding.ItemDireccionBinding
import com.motomarket.app.models.Rol
import com.motomarket.app.ui.common.TonoChip
import com.motomarket.app.ui.common.aplicarChip
import com.motomarket.app.ui.common.cerrarSesion
import com.motomarket.app.ui.common.etiqueta
import kotlinx.coroutines.launch

class PerfilFragment : Fragment() {

    private var _binding: FragmentPerfilBinding? = null
    private val binding get() = _binding!!

    private val usuarioRepository: UsuarioRepository = Repositorios.usuarios

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPerfilBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnCerrarSesion.setOnClickListener { requireActivity().cerrarSesion() }

        viewLifecycleOwner.lifecycleScope.launch {
            val usuario = usuarioRepository.obtenerUsuarioActual(Rol.COMPRADOR)
            binding.tvNombre.text = usuario.nombre
            binding.tvCorreo.text = usuario.correo
            binding.tvRol.aplicarChip(usuario.rol.etiqueta(), TonoChip.NARANJA)
            mostrarDirecciones(usuarioRepository.obtenerDirecciones(usuario.id))
        }
    }

    private fun mostrarDirecciones(direcciones: List<String>) {
        binding.contenedorDirecciones.removeAllViews()
        direcciones.forEach { direccion ->
            val item = ItemDireccionBinding.inflate(layoutInflater, binding.contenedorDirecciones, true)
            item.tvDireccion.text = direccion
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
