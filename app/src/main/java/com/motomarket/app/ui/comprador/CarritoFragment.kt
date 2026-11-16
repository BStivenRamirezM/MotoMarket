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
import com.motomarket.app.ui.common.conCarga
import com.motomarket.app.ui.common.habilitar
import com.motomarket.app.ui.common.intentar
import com.motomarket.app.util.formatearCOP
import kotlinx.coroutines.launch

/** Carrito persistente del comprador: totales, eliminar con "Deshacer", vaciar e ir a pagar. */
class CarritoFragment : Fragment() {

    private var _binding: FragmentCarritoBinding? = null
    private val binding get() = _binding!!

    private val carritoRepository: CarritoRepository by lazy { Repositorios.carrito }
    private val adapter = CarritoAdapter { item -> eliminarItem(item) }

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

    /** Se recarga al volver del detalle o de la pasarela para reflejar cambios. */
    override fun onResume() {
        super.onResume()
        recargar()
    }

    /** Vuelve a leer el carrito desde Firestore y refresca la lista y los totales. */
    private fun recargar() {
        viewLifecycleOwner.lifecycleScope.launch {
            requireContext().conCarga(binding.progreso) { carritoRepository.obtenerResumen() }?.let { mostrarResumen(it) }
        }
    }

    /** Pinta ítems, subtotal, envío y total; deshabilita el pago si el carrito está vacío. */
    private fun mostrarResumen(resumen: ResumenCarrito) {
        adapter.submitList(resumen.items)
        val vacio = resumen.items.isEmpty()
        binding.tvCarritoVacio.isVisible = vacio
        binding.tvSubtotal.text = formatearCOP(resumen.subtotal)
        binding.tvEtiquetaEnvio.setText(R.string.carrito_envio)
        binding.tvEnvio.text = formatearCOP(resumen.envio)
        binding.tvTotal.text = formatearCOP(resumen.total)
        binding.btnIrAPagar.habilitar(!vacio)
        binding.toolbar.menu.findItem(R.id.accion_vaciar_carrito)?.isVisible = !vacio
    }

    /** Elimina el ítem y ofrece deshacerlo durante unos segundos. */
    private fun eliminarItem(item: ItemCarrito) {
        viewLifecycleOwner.lifecycleScope.launch {
            requireContext().intentar { carritoRepository.eliminar(item.producto.id) } ?: return@launch
            recargar()
            Snackbar.make(binding.root, getString(R.string.carrito_eliminado, item.producto.nombre), Snackbar.LENGTH_LONG)
                .setAction(R.string.deshacer) {
                    viewLifecycleOwner.lifecycleScope.launch {
                        requireContext().intentar { carritoRepository.restaurar(item) }
                        recargar()
                    }
                }
                .setAnchorView(binding.btnIrAPagar)
                .show()
        }
    }

    /** Pide confirmación antes de vaciar todo el carrito. */
    private fun confirmarVaciado() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.carrito_vaciar_titulo)
            .setMessage(R.string.carrito_vaciar_mensaje)
            .setNegativeButton(R.string.cancelar, null)
            .setPositiveButton(R.string.carrito_vaciar) { _, _ ->
                viewLifecycleOwner.lifecycleScope.launch {
                    requireContext().intentar { carritoRepository.vaciar() }
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
