package com.motomarket.app.ui.comprador

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.motomarket.app.R
import com.motomarket.app.data.CarritoRepository
import com.motomarket.app.data.Repositorios
import com.motomarket.app.databinding.FragmentCarritoBinding
import com.motomarket.app.models.ItemCarrito
import com.motomarket.app.models.ResumenCarrito
import com.motomarket.app.ui.common.habilitar
import com.motomarket.app.util.formatearCOP
import kotlinx.coroutines.launch

class CarritoFragment : Fragment() {

    private var _binding: FragmentCarritoBinding? = null
    private val binding get() = _binding!!

    private val carritoRepository: CarritoRepository = Repositorios.carrito
    private val adapter = CarritoAdapter { item, posicion -> eliminarItem(item, posicion) }

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
        binding.toolbar.setOnMenuItemClickListener { menuItem ->
            if (menuItem.itemId == R.id.accion_vaciar_carrito) confirmarVaciado()
            true
        }
    }

    override fun onResume() {
        super.onResume()
        recargar()
    }

    private fun recargar() {
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
        binding.toolbar.menu.findItem(R.id.accion_vaciar_carrito)?.isVisible = !vacio
    }

    private fun eliminarItem(item: ItemCarrito, posicion: Int) {
        viewLifecycleOwner.lifecycleScope.launch {
            carritoRepository.eliminar(item.producto.id)
            recargar()
            Snackbar.make(binding.root, getString(R.string.carrito_eliminado, item.producto.nombre), Snackbar.LENGTH_LONG)
                .setAction(R.string.deshacer) {
                    viewLifecycleOwner.lifecycleScope.launch {
                        carritoRepository.restaurar(item, posicion)
                        recargar()
                    }
                }
                .setAnchorView(binding.btnIrAPagar)
                .show()
        }
    }

    private fun confirmarVaciado() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.carrito_vaciar_titulo)
            .setMessage(R.string.carrito_vaciar_mensaje)
            .setNegativeButton(R.string.cancelar, null)
            .setPositiveButton(R.string.carrito_vaciar) { _, _ ->
                viewLifecycleOwner.lifecycleScope.launch {
                    carritoRepository.vaciar()
                    recargar()
                }
            }
            .show()
    }

    override fun onDestroyView() {
        binding.rvCarrito.adapter = null
        _binding = null
        super.onDestroyView()
    }
}
