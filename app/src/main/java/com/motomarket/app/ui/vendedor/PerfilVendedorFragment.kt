package com.motomarket.app.ui.vendedor

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.motomarket.app.R
import com.motomarket.app.data.Repositorios
import com.motomarket.app.data.UsuarioRepository
import com.motomarket.app.databinding.FragmentPerfilVendedorBinding
import com.motomarket.app.models.Usuario
import com.motomarket.app.ui.common.cerrarSesion
import com.motomarket.app.ui.common.intentar
import com.motomarket.app.util.formatearCalificacion
import com.motomarket.app.util.formatearFecha
import kotlinx.coroutines.launch

/** Datos de la tienda del vendedor (nombre, calificación, contacto) y cierre de sesión. */
class PerfilVendedorFragment : Fragment() {

    private var _binding: FragmentPerfilVendedorBinding? = null
    private val binding get() = _binding!!

    private val usuarioRepository: UsuarioRepository by lazy { Repositorios.usuarios }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPerfilVendedorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnCerrarSesion.setOnClickListener { requireActivity().cerrarSesion() }
        viewLifecycleOwner.lifecycleScope.launch {
            requireContext().intentar { usuarioRepository.obtenerUsuarioActual() }?.let { mostrarTienda(it) }
        }
    }

    private fun mostrarTienda(vendedor: Usuario) {
        binding.tvNombreTienda.text = vendedor.nombre
        binding.tvCalificacion.text = if (vendedor.calificacion > 0) {
            getString(R.string.perfil_vendedor_calificacion, formatearCalificacion(vendedor.calificacion))
        } else {
            getString(R.string.sin_calificaciones)
        }
        binding.tvTelefono.text = vendedor.telefono.ifBlank { getString(R.string.sin_dato) }
        binding.tvCorreo.text = vendedor.correo
        binding.tvMiembroDesde.text = getString(R.string.perfil_vendedor_desde, formatearFecha(vendedor.fechaRegistro))
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
