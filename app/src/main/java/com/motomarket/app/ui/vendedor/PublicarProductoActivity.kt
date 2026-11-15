package com.motomarket.app.ui.vendedor

import android.content.ActivityNotFoundException
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.motomarket.app.R
import com.motomarket.app.data.ProductoRepository
import com.motomarket.app.data.Repositorios
import com.motomarket.app.data.UsuarioRepository
import com.motomarket.app.databinding.ActivityPublicarProductoBinding
import com.motomarket.app.models.Categoria
import com.motomarket.app.models.EstadoProducto
import com.motomarket.app.models.Producto
import com.motomarket.app.ui.common.BaseActivity
import com.motomarket.app.ui.common.mostrarMensaje
import com.motomarket.app.ui.common.texto
import java.util.UUID
import kotlinx.coroutines.launch

class PublicarProductoActivity : BaseActivity() {

    private lateinit var binding: ActivityPublicarProductoBinding
    private val productoRepository: ProductoRepository = Repositorios.productos
    private val usuarioRepository: UsuarioRepository = Repositorios.usuarios

    private val tomarFoto = registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { foto ->
        if (foto != null) {
            binding.ivFoto.setImageBitmap(foto)
            binding.ivFoto.isVisible = true
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPublicarProductoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.btnTomarFoto.setOnClickListener { abrirCamara() }
        binding.btnPublicar.setOnClickListener { publicar() }
    }

    private fun abrirCamara() {
        try {
            tomarFoto.launch(null)
        } catch (e: ActivityNotFoundException) {
            mostrarMensaje(R.string.publicar_sin_camara)
        }
    }

    private fun categoriaSeleccionada(): Categoria = when (binding.chipsCategoria.checkedChipId) {
        R.id.chipFrenos -> Categoria.FRENOS
        R.id.chipLlantas -> Categoria.LLANTAS
        R.id.chipCascos -> Categoria.CASCOS
        R.id.chipAceites -> Categoria.ACEITES
        R.id.chipTransmision -> Categoria.TRANSMISION
        else -> Categoria.ACCESORIOS
    }

    private fun publicar() {
        val campos = listOf(binding.etNombre, binding.etCompatibilidad, binding.etPrecio, binding.etStock)
        val vacio = campos.firstOrNull { it.texto().isEmpty() }
        if (vacio != null) {
            vacio.error = getString(R.string.error_campo_obligatorio)
            return
        }
        lifecycleScope.launch {
            val tienda = usuarioRepository.obtenerTienda()
            val producto = Producto(
                id = UUID.randomUUID().toString(),
                nombre = binding.etNombre.texto(),
                precio = binding.etPrecio.texto().toIntOrNull() ?: 0,
                categoria = categoriaSeleccionada(),
                marcaModeloCompatible = binding.etCompatibilidad.texto(),
                cilindraje = "",
                stock = binding.etStock.texto().toIntOrNull() ?: 0,
                vendedor = tienda.nombre,
                calificacionVendedor = tienda.calificacion,
                estado = EstadoProducto.EN_REVISION
            )
            productoRepository.publicarProducto(producto)
            mostrarMensaje(R.string.publicar_exito)
            finish()
        }
    }
}
