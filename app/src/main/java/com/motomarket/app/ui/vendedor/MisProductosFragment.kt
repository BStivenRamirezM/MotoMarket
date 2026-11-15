package com.motomarket.app.ui.vendedor

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.motomarket.app.data.ProductoRepository
import com.motomarket.app.data.Repositorios
import com.motomarket.app.data.UsuarioRepository
import com.motomarket.app.databinding.FragmentMisProductosBinding
import kotlinx.coroutines.launch

class MisProductosFragment : Fragment() {

    private var _binding: FragmentMisProductosBinding? = null
    private val binding get() = _binding!!

    private val productoRepository: ProductoRepository = Repositorios.productos
    private val usuarioRepository: UsuarioRepository = Repositorios.usuarios
    private val adapter = MiProductoAdapter()

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

    override fun onResume() {
        super.onResume()
        viewLifecycleOwner.lifecycleScope.launch {
            val tienda = usuarioRepository.obtenerTienda()
            adapter.submitList(productoRepository.obtenerProductosVendedor(tienda.nombre))
        }
    }

    override fun onDestroyView() {
        binding.rvMisProductos.adapter = null
        _binding = null
        super.onDestroyView()
    }
}
