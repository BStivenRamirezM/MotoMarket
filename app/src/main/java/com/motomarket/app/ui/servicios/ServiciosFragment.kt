package com.motomarket.app.ui.servicios

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.motomarket.app.R
import com.motomarket.app.data.Repositorios
import com.motomarket.app.data.ServicioRepository
import com.motomarket.app.databinding.FragmentServiciosBinding
import com.motomarket.app.ui.common.conCarga
import kotlinx.coroutines.launch

/**
 * Lista de servicios técnicos. Para el comprador ("Taller") muestra sus solicitudes y el botón para pedir
 * una nueva; para el mecánico ("Trabajos") muestra los servicios que le asignaron.
 */
class ServiciosFragment : Fragment() {

    private var _binding: FragmentServiciosBinding? = null
    private val binding get() = _binding!!

    private val servicioRepository: ServicioRepository by lazy { Repositorios.servicios }
    private val vistaMecanico by lazy { requireArguments().getBoolean(ARG_MECANICO) }
    private val adapter by lazy {
        ServicioAdapter(vistaMecanico) { servicio ->
            startActivity(DetalleServicioActivity.crearIntent(requireContext(), servicio.id))
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentServiciosBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.toolbar.setTitle(if (vistaMecanico) R.string.titulo_trabajos else R.string.titulo_servicio_tecnico)
        binding.bannerSolicitar.isVisible = !vistaMecanico
        binding.btnSolicitarServicio.isVisible = !vistaMecanico
        binding.tvSinServicios.setText(if (vistaMecanico) R.string.servicios_sin_trabajos else R.string.servicios_sin_solicitudes)
        binding.btnSolicitarServicio.setOnClickListener {
            startActivity(Intent(requireContext(), SolicitarServicioActivity::class.java))
        }
        binding.rvServicios.adapter = adapter
    }

    /** Se recarga al volver del detalle para reflejar cotizaciones, pagos y avances. */
    override fun onResume() {
        super.onResume()
        viewLifecycleOwner.lifecycleScope.launch {
            val servicios = requireContext().conCarga(binding.progreso) {
                if (vistaMecanico) servicioRepository.obtenerServiciosMecanico() else servicioRepository.obtenerServiciosComprador()
            } ?: return@launch
            adapter.submitList(servicios)
            binding.tvSinServicios.isVisible = servicios.isEmpty()
        }
    }

    override fun onDestroyView() {
        binding.rvServicios.adapter = null
        _binding = null
        super.onDestroyView()
    }

    companion object {
        private const val ARG_MECANICO = "vista_mecanico"

        /** Instancia para el comprador (false) o para el mecánico (true). */
        fun nuevo(vistaMecanico: Boolean): ServiciosFragment =
            ServiciosFragment().apply { arguments = Bundle().apply { putBoolean(ARG_MECANICO, vistaMecanico) } }
    }
}
