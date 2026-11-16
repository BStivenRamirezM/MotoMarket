package com.motomarket.app.ui.comprador

import android.os.Bundle
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.motomarket.app.data.PedidoRepository
import com.motomarket.app.data.Repositorios
import com.motomarket.app.databinding.ActivityHistorialPedidosBinding
import com.motomarket.app.ui.common.BaseActivity
import com.motomarket.app.ui.common.conCarga
import com.motomarket.app.ui.vendedor.PedidoAdapter
import kotlinx.coroutines.launch

/** Historial de órdenes del comprador con su estado de envío. */
class HistorialPedidosActivity : BaseActivity() {

    private lateinit var binding: ActivityHistorialPedidosBinding
    private val pedidoRepository: PedidoRepository by lazy { Repositorios.pedidos }
    private val adapter = PedidoAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHistorialPedidosBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.rvPedidos.adapter = adapter

        lifecycleScope.launch {
            val pedidos = conCarga(binding.progreso) { pedidoRepository.obtenerHistorialComprador() } ?: return@launch
            adapter.submitList(pedidos)
            binding.tvSinPedidos.isVisible = pedidos.isEmpty()
        }
    }
}
