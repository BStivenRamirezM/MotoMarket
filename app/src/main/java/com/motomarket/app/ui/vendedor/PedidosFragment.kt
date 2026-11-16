package com.motomarket.app.ui.vendedor

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.motomarket.app.R
import com.motomarket.app.data.PedidoRepository
import com.motomarket.app.data.Repositorios
import com.motomarket.app.databinding.FragmentPedidosBinding
import com.motomarket.app.models.Pedido
import com.motomarket.app.ui.common.conCarga
import com.motomarket.app.ui.common.etiqueta
import com.motomarket.app.ui.common.intentar
import com.motomarket.app.ui.common.mostrarMensaje
import com.motomarket.app.util.formatearCOP
import kotlinx.coroutines.launch

/** Pedidos recibidos por el vendedor; al tocar uno avanza su estado (Por enviar → Enviado → Entregado). */
class PedidosFragment : Fragment() {

    private var _binding: FragmentPedidosBinding? = null
    private val binding get() = _binding!!

    private val pedidoRepository: PedidoRepository by lazy { Repositorios.pedidos }
    private lateinit var adapter: PedidoAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPedidosBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adapter = PedidoAdapter(Repositorios.usuarios.uidActual()) { pedido -> confirmarAvance(pedido) }
        binding.rvPedidos.adapter = adapter
        cargar()
    }

    /** Carga los pedidos y el resumen de ventas del mes. */
    private fun cargar() {
        viewLifecycleOwner.lifecycleScope.launch {
            val pedidos = requireContext().conCarga(binding.progreso) { pedidoRepository.obtenerPedidosVendedor() }
                ?: return@launch
            adapter.submitList(pedidos)
            binding.tvSinPedidos.isVisible = pedidos.isEmpty()
            val resumen = requireContext().intentar { pedidoRepository.obtenerResumenVentasMes() } ?: return@launch
            binding.tvTotalVentas.text = formatearCOP(resumen.totalMes)
            binding.tvCantidadPedidos.text = resources.getQuantityString(
                R.plurals.pedidos_cantidad, resumen.cantidadPedidos, resumen.cantidadPedidos
            )
        }
    }

    /** Pide confirmación antes de pasar el pedido al siguiente estado de envío. */
    private fun confirmarAvance(pedido: Pedido) {
        val siguiente = pedido.estado.siguiente() ?: return requireContext().mostrarMensaje(R.string.pedidos_sin_avance)
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.pedidos_cambiar_estado_titulo, pedido.numero))
            .setMessage(getString(R.string.pedidos_cambiar_estado_mensaje, getString(siguiente.etiqueta())))
            .setNegativeButton(R.string.cancelar, null)
            .setPositiveButton(R.string.aceptar) { _, _ ->
                viewLifecycleOwner.lifecycleScope.launch {
                    requireContext().intentar { pedidoRepository.cambiarEstado(pedido.id, siguiente) } ?: return@launch
                    cargar()
                }
            }
            .show()
    }

    override fun onDestroyView() {
        binding.rvPedidos.adapter = null
        _binding = null
        super.onDestroyView()
    }
}
