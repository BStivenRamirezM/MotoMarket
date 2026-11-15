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
import com.motomarket.app.ui.common.cerrarSesion
import com.motomarket.app.util.formatearCalificacion
import kotlinx.coroutines.launch

class PerfilVendedorFragment : Fragment() {

    private var _binding: FragmentPerfilVendedorBinding? = null
    private val binding get() = _binding!!

    private val usuarioRepository: UsuarioRepository = Repositorios.usuarios

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPerfilVendedorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnCerrarSesion.setOnClickListener { requireActivity().cerrarSesion() }

        viewLifecycleOwner.lifecycleScope.launch {
            val tienda = usuarioRepository.obtenerTienda()
            binding.tvNombreTienda.text = tienda.nombre
            binding.tvCalificacion.text = getString(R.string.perfil_vendedor_calificacion, formatearCalificacion(tienda.calificacion))
            binding.tvPropietario.text = tienda.propietario
            binding.tvCorreo.text = tienda.correo
            binding.tvCiudad.text = tienda.ciudad
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
