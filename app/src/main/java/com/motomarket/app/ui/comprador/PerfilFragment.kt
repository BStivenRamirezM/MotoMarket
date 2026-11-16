package com.motomarket.app.ui.comprador

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.motomarket.app.data.Repositorios
import com.motomarket.app.data.UsuarioRepository
import com.motomarket.app.databinding.FragmentPerfilBinding
import com.motomarket.app.databinding.ItemDireccionBinding
import com.motomarket.app.models.Usuario
import com.motomarket.app.ui.common.TonoChip
import com.motomarket.app.ui.common.aplicarChip
import com.motomarket.app.ui.common.cerrarSesion
import com.motomarket.app.ui.common.conCarga
import com.motomarket.app.ui.common.etiqueta
import kotlinx.coroutines.launch

/** Perfil del comprador: datos de la cuenta, direcciones usadas, historial de pedidos y cierre de sesión. */
class PerfilFragment : Fragment() {

    private var _binding: FragmentPerfilBinding? = null
    private val binding get() = _binding!!

    private val usuarioRepository: UsuarioRepository by lazy { Repositorios.usuarios }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPerfilBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnCerrarSesion.setOnClickListener { requireActivity().cerrarSesion() }
        binding.btnMisPedidos.setOnClickListener {
            startActivity(Intent(requireContext(), HistorialPedidosActivity::class.java))
        }
    }

    /** Se recarga al volver para mostrar las direcciones de compras recientes. */
    override fun onResume() {
        super.onResume()
        viewLifecycleOwner.lifecycleScope.launch {
            requireContext().conCarga(binding.progreso) { usuarioRepository.obtenerUsuarioActual() }?.let { mostrarUsuario(it) }
        }
    }

    private fun mostrarUsuario(usuario: Usuario) {
        binding.tvNombre.text = usuario.nombre
        binding.tvCorreo.text = usuario.correo
        binding.tvRol.aplicarChip(usuario.rol.etiqueta(), TonoChip.NARANJA)
        binding.tvSinDirecciones.isVisible = usuario.direcciones.isEmpty()
        binding.contenedorDirecciones.removeAllViews()
        usuario.direcciones.forEach { direccion ->
            ItemDireccionBinding.inflate(layoutInflater, binding.contenedorDirecciones, true).tvDireccion.text = direccion
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
