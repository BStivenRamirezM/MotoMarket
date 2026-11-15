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
import kotlinx.coroutines.launch

class InicioFragment : Fragment() {

    private var _binding: FragmentInicioBinding? = null
    private val binding get() = _binding!!

    private val productoRepository: ProductoRepository = Repositorios.productos
    private val adapter = ProductoAdapter { producto ->
        startActivity(DetalleProductoActivity.crearIntent(requireContext(), producto.id))
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentInicioBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.rvProductos.adapter = adapter
        binding.chipsCategorias.setOnCheckedStateChangeListener { _, _ -> cargarProductos() }
        binding.etBuscar.doAfterTextChanged { cargarProductos() }
        cargarProductos()
    }

    private fun categoriaSeleccionada(): Categoria? = when (binding.chipsCategorias.checkedChipId) {
        R.id.chipFrenos -> Categoria.FRENOS
        R.id.chipLlantas -> Categoria.LLANTAS
        R.id.chipCascos -> Categoria.CASCOS
        R.id.chipAceites -> Categoria.ACEITES
        else -> null
    }

    private fun cargarProductos() {
        val texto = binding.etBuscar.text.toString()
        val categoria = categoriaSeleccionada()
        viewLifecycleOwner.lifecycleScope.launch {
            val productos = productoRepository.buscar(texto, categoria)
            adapter.submitList(productos)
            binding.tvSinResultados.isVisible = productos.isEmpty()
        }
    }

    override fun onDestroyView() {
        binding.rvProductos.adapter = null
        _binding = null
        super.onDestroyView()
    }
}
