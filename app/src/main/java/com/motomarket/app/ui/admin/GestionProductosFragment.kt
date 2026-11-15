package com.motomarket.app.ui.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.motomarket.app.R
import com.motomarket.app.data.ProductoRepository
import com.motomarket.app.data.Repositorios
import com.motomarket.app.databinding.FragmentGestionProductosBinding
import com.motomarket.app.models.EstadoProducto
import com.motomarket.app.models.Moderacion
import com.motomarket.app.ui.common.mostrarMensaje
import kotlinx.coroutines.launch

class GestionProductosFragment : Fragment() {

    private var _binding: FragmentGestionProductosBinding? = null
    private val binding get() = _binding!!

    private val productoRepository: ProductoRepository = Repositorios.productos
    private val pendientesAdapter = ModeracionAdapter { moderacion, estado -> decidir(moderacion, estado) }
    private val historialAdapter = ModeracionAdapter()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentGestionProductosBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.rvPendientes.adapter = pendientesAdapter
        binding.rvHistorial.adapter = historialAdapter
        cargarModeraciones()
    }

    private fun cargarModeraciones() {
        viewLifecycleOwner.lifecycleScope.launch {
            val (pendientes, historial) = productoRepository.obtenerModeraciones()
                .partition { it.estado == EstadoProducto.PENDIENTE }
            pendientesAdapter.submitList(pendientes)
            historialAdapter.submitList(historial)
            binding.tvSinPendientes.isVisible = pendientes.isEmpty()
        }
    }

    private fun decidir(moderacion: Moderacion, estado: EstadoProducto) {
        viewLifecycleOwner.lifecycleScope.launch {
            productoRepository.actualizarModeracion(moderacion.id, estado)
            val mensaje = if (estado == EstadoProducto.APROBADO) R.string.aprobacion_aprobado_msg else R.string.aprobacion_rechazado_msg
            requireContext().mostrarMensaje(getString(mensaje, moderacion.nombreProducto))
            cargarModeraciones()
        }
    }

    override fun onDestroyView() {
        binding.rvPendientes.adapter = null
        binding.rvHistorial.adapter = null
        _binding = null
        super.onDestroyView()
    }
}
