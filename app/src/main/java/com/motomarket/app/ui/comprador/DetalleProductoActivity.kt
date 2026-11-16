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
import com.motomarket.app.models.EstadoProducto
import com.motomarket.app.models.Producto
import com.motomarket.app.ui.common.BaseActivity
import com.motomarket.app.ui.common.cargarImagenBase64
import com.motomarket.app.ui.common.conCarga
import com.motomarket.app.ui.common.habilitar
import com.motomarket.app.ui.common.mostrarMensaje
import com.motomarket.app.util.formatearCOP
import com.motomarket.app.util.formatearCalificacion
import kotlinx.coroutines.launch

/** Detalle de un producto con foto, compatibilidad, vendedor y stock; permite agregarlo al carrito. */
class DetalleProductoActivity : BaseActivity() {

    private lateinit var binding: ActivityDetalleProductoBinding
    private val productoRepository: ProductoRepository by lazy { Repositorios.productos }
    private val carritoRepository: CarritoRepository by lazy { Repositorios.carrito }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetalleProductoBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.btnAgregarCarrito.habilitar(false)

        val productoId = intent.getStringExtra(EXTRA_PRODUCTO_ID).orEmpty()
        lifecycleScope.launch {
            val producto = conCarga(binding.progreso) { productoRepository.obtenerPorId(productoId) }
            if (producto == null) {
                mostrarMensaje(R.string.detalle_no_encontrado)
                finish()
            } else {
                mostrarProducto(producto)
            }
        }
    }

    /** Pinta los datos del producto y habilita el carrito solo si está aprobado y con stock. */
    private fun mostrarProducto(producto: Producto) {
        binding.ivImagen.cargarImagenBase64(producto.imagenBase64)
        binding.tvNombre.text = producto.nombre
        binding.tvPrecio.text = formatearCOP(producto.precio)
        binding.tvCompatibilidad.text = producto.marcaModeloCompatible
        binding.tvCilindraje.text = getString(R.string.detalle_cilindraje, producto.cilindraje.ifBlank { getString(R.string.sin_dato) })
        binding.tvVendedor.text = producto.vendedorNombre
        binding.tvCalificacion.text = if (producto.calificacionVendedor > 0) {
            getString(R.string.calificacion_estrella, formatearCalificacion(producto.calificacionVendedor))
        } else {
            getString(R.string.sin_calificaciones)
        }
        val disponible = producto.stock > 0 && producto.estado == EstadoProducto.APROBADO
        binding.tvStock.text = if (producto.stock > 0) {
            resources.getQuantityString(R.plurals.stock_unidades, producto.stock, producto.stock)
        } else {
            getString(R.string.estado_agotado)
        }
        binding.btnAgregarCarrito.habilitar(disponible)
        binding.btnAgregarCarrito.setOnClickListener { agregarAlCarrito(producto) }
    }

    /** El repositorio rechaza la operación si la cantidad en el carrito superaría el stock. */
    private fun agregarAlCarrito(producto: Producto) {
        lifecycleScope.launch {
            conCarga(binding.progreso, binding.btnAgregarCarrito) { carritoRepository.agregar(producto) } ?: return@launch
            mostrarMensaje(R.string.detalle_agregado)
        }
    }

    companion object {
        private const val EXTRA_PRODUCTO_ID = "extra_producto_id"

        /** Intent para abrir el detalle de [productoId]. */
        fun crearIntent(context: Context, productoId: String): Intent =
            Intent(context, DetalleProductoActivity::class.java).putExtra(EXTRA_PRODUCTO_ID, productoId)
    }
}
