package com.motomarket.app.ui.comprador

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.motomarket.app.R
import com.motomarket.app.data.CarritoRepository
import com.motomarket.app.data.Repositorios
import com.motomarket.app.databinding.FragmentCarritoBinding
import com.motomarket.app.models.ResumenCarrito
import com.motomarket.app.ui.common.habilitar
import com.motomarket.app.util.formatearCOP
import kotlinx.coroutines.launch

class CarritoFragment : Fragment() {

    private var _binding: FragmentCarritoBinding? = null
    private val binding get() = _binding!!

    private val carritoRepository: CarritoRepository = Repositorios.carrito
    private val adapter = CarritoAdapter()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCarritoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.rvCarrito.adapter = adapter
        binding.btnIrAPagar.setOnClickListener {
            startActivity(Intent(requireContext(), PasarelaPagoActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        viewLifecycleOwner.lifecycleScope.launch { mostrarResumen(carritoRepository.obtenerResumen()) }
    }

    private fun mostrarResumen(resumen: ResumenCarrito) {
        adapter.submitList(resumen.items)
        val vacio = resumen.items.isEmpty()
        binding.tvCarritoVacio.isVisible = vacio
        binding.tvSubtotal.text = formatearCOP(resumen.subtotal)
        binding.tvEtiquetaEnvio.text = getString(R.string.carrito_envio, resumen.envio.ciudad)
        binding.tvEnvio.text = formatearCOP(if (vacio) 0 else resumen.envio.costo)
        binding.tvTotal.text = formatearCOP(resumen.total)
        binding.btnIrAPagar.habilitar(!vacio)
    }

    override fun onDestroyView() {
        binding.rvCarrito.adapter = null
        _binding = null
        super.onDestroyView()
    }
}
