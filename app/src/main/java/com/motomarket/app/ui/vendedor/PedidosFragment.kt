package com.motomarket.app.ui.vendedor

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.motomarket.app.R
import com.motomarket.app.data.PedidoRepository
import com.motomarket.app.data.Repositorios
import com.motomarket.app.databinding.FragmentPedidosBinding
import com.motomarket.app.util.formatearCOP
import kotlinx.coroutines.launch

class PedidosFragment : Fragment() {

    private var _binding: FragmentPedidosBinding? = null
    private val binding get() = _binding!!

    private val pedidoRepository: PedidoRepository = Repositorios.pedidos
    private val adapter = PedidoAdapter()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPedidosBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.rvPedidos.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            adapter.submitList(pedidoRepository.obtenerPedidosVendedor())
            val resumen = pedidoRepository.obtenerResumenVentasMes()
            binding.tvTotalVentas.text = formatearCOP(resumen.totalMes)
            binding.tvCantidadPedidos.text = resources.getQuantityString(
                R.plurals.pedidos_cantidad, resumen.cantidadPedidos, resumen.cantidadPedidos
            )
        }
    }

    override fun onDestroyView() {
        binding.rvPedidos.adapter = null
        _binding = null
        super.onDestroyView()
    }
}
