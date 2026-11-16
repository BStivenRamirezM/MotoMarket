package com.motomarket.app.ui.comprador

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.motomarket.app.R
import com.motomarket.app.data.ProductoRepository
import com.motomarket.app.data.Repositorios
import com.motomarket.app.databinding.FragmentInicioBinding
import com.motomarket.app.models.Categoria
import com.motomarket.app.ui.common.conCarga
import com.motomarket.app.ui.common.texto
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Catálogo del comprador: solo productos aprobados, con buscador y filtro por categoría. */
class InicioFragment : Fragment() {

    private var _binding: FragmentInicioBinding? = null
    private val binding get() = _binding!!

    private val productoRepository: ProductoRepository by lazy { Repositorios.productos }
    private val adapter = ProductoAdapter { producto ->
        startActivity(DetalleProductoActivity.crearIntent(requireContext(), producto.id))
    }
    private var busqueda: Job? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentInicioBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.rvProductos.adapter = adapter
        binding.chipsCategorias.setOnCheckedStateChangeListener { _, _ -> cargarProductos() }
        binding.etBuscar.doAfterTextChanged { cargarProductos(ESPERA_ESCRITURA_MS) }
    }

    /** Se recarga al volver a la pestaña para reflejar aprobaciones y cambios de stock. */
    override fun onResume() {
        super.onResume()
        cargarProductos()
    }

    /** Categoría del chip marcado, o null para "Todo". */
    private fun categoriaSeleccionada(): Categoria? = when (binding.chipsCategorias.checkedChipId) {
        R.id.chipFrenos -> Categoria.FRENOS
        R.id.chipLlantas -> Categoria.LLANTAS
        R.id.chipCascos -> Categoria.CASCOS
        R.id.chipAceites -> Categoria.ACEITES
        R.id.chipTransmision -> Categoria.TRANSMISION
        R.id.chipAccesorios -> Categoria.ACCESORIOS
        else -> null
    }

    /** Consulta el catálogo; [espera] agrupa las teclas para no consultar Firestore en cada letra. */
    private fun cargarProductos(espera: Long = 0L) {
        busqueda?.cancel()
        busqueda = viewLifecycleOwner.lifecycleScope.launch {
            delay(espera)
            val texto = binding.etBuscar.texto()
            val categoria = categoriaSeleccionada()
            val productos = requireContext().conCarga(binding.progreso) {
                productoRepository.buscarCatalogo(texto, categoria)
            } ?: return@launch
            adapter.submitList(productos)
            binding.tvSinResultados.isVisible = productos.isEmpty()
        }
    }

    override fun onDestroyView() {
        binding.rvProductos.adapter = null
        _binding = null
        super.onDestroyView()
    }

    private companion object {
        const val ESPERA_ESCRITURA_MS = 350L
    }
}
