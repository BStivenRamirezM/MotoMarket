package com.motomarket.app.ui.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.motomarket.app.R
import com.motomarket.app.data.ProductoRepository
import com.motomarket.app.data.Repositorios
import com.motomarket.app.databinding.DialogMotivoRechazoBinding
import com.motomarket.app.databinding.FragmentGestionProductosBinding
import com.motomarket.app.models.EstadoProducto
import com.motomarket.app.models.Producto
import com.motomarket.app.ui.common.intentar
import com.motomarket.app.ui.common.mostrarMensaje
import com.motomarket.app.ui.common.texto
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/** Moderación: aprueba o rechaza publicaciones pendientes y muestra el historial de decisiones. */
class GestionProductosFragment : Fragment() {

    private var _binding: FragmentGestionProductosBinding? = null
    private val binding get() = _binding!!

    private val productoRepository: ProductoRepository by lazy { Repositorios.productos }
    private val pendientesAdapter = ModeracionAdapter { producto, estado -> decidir(producto, estado) }
    private val historialAdapter = ModeracionAdapter()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentGestionProductosBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.rvPendientes.adapter = pendientesAdapter
        binding.rvHistorial.adapter = historialAdapter
        cargar()
    }

    /** Consulta pendientes e historial en paralelo. */
    private fun cargar() {
        viewLifecycleOwner.lifecycleScope.launch {
            val resultado = requireContext().intentar {
                coroutineScope {
                    val pendientes = async { productoRepository.obtenerPendientes() }
                    val historial = async { productoRepository.obtenerHistorialModeracion() }
                    pendientes.await() to historial.await()
                }
            } ?: return@launch
            pendientesAdapter.submitList(resultado.first)
            historialAdapter.submitList(resultado.second)
            binding.tvSinPendientes.isVisible = resultado.first.isEmpty()
        }
    }

    /** Aprobar es inmediato; rechazar pide antes el motivo que verá el vendedor. */
    private fun decidir(producto: Producto, estado: EstadoProducto) {
        if (estado == EstadoProducto.APROBADO) {
            guardarDecision(producto, estado, "")
            return
        }
        val dialogo = DialogMotivoRechazoBinding.inflate(layoutInflater)
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.aprobacion_rechazar_titulo, producto.nombre))
            .setView(dialogo.root)
            .setNegativeButton(R.string.cancelar, null)
            .setPositiveButton(R.string.aprobacion_rechazar) { _, _ ->
                val motivo = dialogo.etMotivo.texto().ifBlank { getString(R.string.aprobacion_motivo_defecto) }
                guardarDecision(producto, estado, motivo)
            }
            .show()
    }

    private fun guardarDecision(producto: Producto, estado: EstadoProducto, motivo: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            requireContext().intentar { productoRepository.moderar(producto.id, estado, motivo) } ?: return@launch
            val mensaje = if (estado == EstadoProducto.APROBADO) R.string.aprobacion_aprobado_msg else R.string.aprobacion_rechazado_msg
            requireContext().mostrarMensaje(getString(mensaje, producto.nombre))
            cargar()
        }
    }

    override fun onDestroyView() {
        binding.rvPendientes.adapter = null
        binding.rvHistorial.adapter = null
        _binding = null
        super.onDestroyView()
    }
}
