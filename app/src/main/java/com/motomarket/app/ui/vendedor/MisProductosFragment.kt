package com.motomarket.app.ui.vendedor

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.motomarket.app.data.ProductoRepository
import com.motomarket.app.data.Repositorios
import com.motomarket.app.data.SesionRequeridaException
import com.motomarket.app.databinding.FragmentMisProductosBinding
import com.motomarket.app.ui.common.conCarga
import kotlinx.coroutines.launch

/** Publicaciones del vendedor actual; el FAB crea una nueva y tocar una la abre para editar o eliminar. */
class MisProductosFragment : Fragment() {

    private var _binding: FragmentMisProductosBinding? = null
    private val binding get() = _binding!!

    private val productoRepository: ProductoRepository by lazy { Repositorios.productos }
    private val adapter = MiProductoAdapter { producto ->
        startActivity(PublicarProductoActivity.crearIntent(requireContext(), producto.id))
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMisProductosBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.rvMisProductos.adapter = adapter
        binding.fabPublicar.setOnClickListener {
            startActivity(Intent(requireContext(), PublicarProductoActivity::class.java))
        }
    }

    /** Se recarga al volver de publicar o editar. */
    override fun onResume() {
        super.onResume()
        viewLifecycleOwner.lifecycleScope.launch {
            val productos = requireContext().conCarga(binding.progreso) {
                val uid = Repositorios.usuarios.uidActual() ?: throw SesionRequeridaException()
                productoRepository.obtenerProductosVendedor(uid)
            } ?: return@launch
            adapter.submitList(productos)
            binding.tvSinProductos.isVisible = productos.isEmpty()
        }
    }

    override fun onDestroyView() {
        binding.rvMisProductos.adapter = null
        _binding = null
        super.onDestroyView()
    }
}
