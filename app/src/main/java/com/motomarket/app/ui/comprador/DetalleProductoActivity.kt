package com.motomarket.app.ui.comprador

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import com.motomarket.app.R
import com.motomarket.app.data.CarritoRepository
import com.motomarket.app.data.ProductoRepository
import com.motomarket.app.data.Repositorios
import com.motomarket.app.databinding.ActivityDetalleProductoBinding
import com.motomarket.app.models.Producto
import com.motomarket.app.ui.common.BaseActivity
import com.motomarket.app.ui.common.habilitar
import com.motomarket.app.ui.common.mostrarMensaje
import com.motomarket.app.util.formatearCOP
import com.motomarket.app.util.formatearCalificacion
import kotlinx.coroutines.launch

class DetalleProductoActivity : BaseActivity() {

    private lateinit var binding: ActivityDetalleProductoBinding
    private val productoRepository: ProductoRepository = Repositorios.productos
    private val carritoRepository: CarritoRepository = Repositorios.carrito

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetalleProductoBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.toolbar.setNavigationOnClickListener { finish() }

        val productoId = intent.getStringExtra(EXTRA_PRODUCTO_ID).orEmpty()
        lifecycleScope.launch {
            val producto = productoRepository.obtenerPorId(productoId)
            if (producto == null) {
                mostrarMensaje(R.string.detalle_no_encontrado)
                finish()
            } else {
                mostrarProducto(producto)
            }
        }
    }

    private fun mostrarProducto(producto: Producto) {
        binding.tvNombre.text = producto.nombre
        binding.tvPrecio.text = formatearCOP(producto.precio)
        binding.tvCompatibilidad.text = producto.marcaModeloCompatible
        binding.tvCilindraje.text = getString(R.string.detalle_cilindraje, producto.cilindraje)
        binding.tvVendedor.text = producto.vendedor
        binding.tvCalificacion.text = getString(R.string.calificacion_estrella, formatearCalificacion(producto.calificacionVendedor))
        binding.tvStock.text = if (producto.stock > 0) {
            resources.getQuantityString(R.plurals.stock_unidades, producto.stock, producto.stock)
        } else {
            getString(R.string.estado_agotado)
        }

        binding.btnAgregarCarrito.habilitar(producto.stock > 0)
        binding.btnAgregarCarrito.setOnClickListener {
            lifecycleScope.launch {
                carritoRepository.agregar(producto)
                mostrarMensaje(R.string.detalle_agregado)
            }
        }
    }

    companion object {
        private const val EXTRA_PRODUCTO_ID = "extra_producto_id"

        fun crearIntent(context: Context, productoId: String): Intent =
            Intent(context, DetalleProductoActivity::class.java).putExtra(EXTRA_PRODUCTO_ID, productoId)
    }
}
