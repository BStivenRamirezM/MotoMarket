package com.motomarket.app.ui.vendedor

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.EditText
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.motomarket.app.R
import com.motomarket.app.data.ProductoRepository
import com.motomarket.app.data.Repositorios
import com.motomarket.app.databinding.ActivityPublicarProductoBinding
import com.motomarket.app.models.Categoria
import com.motomarket.app.models.Producto
import com.motomarket.app.ui.common.BaseActivity
import com.motomarket.app.ui.common.cargarImagen
import com.motomarket.app.ui.common.conCarga
import com.motomarket.app.ui.common.habilitar
import com.motomarket.app.ui.common.mostrarMensaje
import com.motomarket.app.ui.common.texto
import com.motomarket.app.util.CompresorImagen
import kotlinx.coroutines.launch
import java.io.File

/**
 * Crea o edita una publicación del vendedor. La foto se toma con la cámara o se elige de la galería,
 * se comprime a menos de 1 MB y se sube a Storage; todo producto guardado queda pendiente de aprobación.
 */
class PublicarProductoActivity : BaseActivity() {

    private lateinit var binding: ActivityPublicarProductoBinding
    private val productoRepository: ProductoRepository by lazy { Repositorios.productos }

    private var productoEditado: Producto? = null
    private var imagenSeleccionada: Uri? = null
    private var uriFotoCamara: Uri? = null

    private val chipsPorCategoria by lazy {
        mapOf(
            Categoria.FRENOS to R.id.chipFrenos,
            Categoria.LLANTAS to R.id.chipLlantas,
            Categoria.CASCOS to R.id.chipCascos,
            Categoria.ACEITES to R.id.chipAceites,
            Categoria.TRANSMISION to R.id.chipTransmision,
            Categoria.ACCESORIOS to R.id.chipAccesorios
        )
    }

    private val tomarFoto = registerForActivityResult(ActivityResultContracts.TakePicture()) { guardada ->
        if (guardada) uriFotoCamara?.let { mostrarImagenLocal(it) }
    }

    private val elegirDeGaleria = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { mostrarImagenLocal(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPublicarProductoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.btnTomarFoto.setOnClickListener { abrirCamara() }
        binding.btnGaleria.setOnClickListener {
            elegirDeGaleria.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
        binding.btnPublicar.setOnClickListener { publicar() }
        binding.btnEliminar.setOnClickListener { confirmarEliminacion() }

        // La cámara puede destruir esta Activity; se recuperan las rutas de la foto pendiente.
        uriFotoCamara = savedInstanceState?.getString(ESTADO_FOTO_CAMARA)?.let(Uri::parse)
        savedInstanceState?.getString(ESTADO_IMAGEN)?.let { mostrarImagenLocal(Uri.parse(it)) }
        intent.getStringExtra(EXTRA_PRODUCTO_ID)?.let { cargarProducto(it) }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        uriFotoCamara?.let { outState.putString(ESTADO_FOTO_CAMARA, it.toString()) }
        imagenSeleccionada?.let { outState.putString(ESTADO_IMAGEN, it.toString()) }
    }

    /** Modo edición: llena el formulario con el producto y muestra la opción de eliminar. */
    private fun cargarProducto(id: String) {
        binding.toolbar.setTitle(R.string.titulo_editar_producto)
        binding.btnPublicar.setText(R.string.publicar_guardar_cambios)
        binding.btnEliminar.isVisible = true
        lifecycleScope.launch {
            val producto = conCarga(binding.progreso, binding.btnPublicar) { productoRepository.obtenerPorId(id) }
                ?: return@launch finish()
            productoEditado = producto
            binding.etNombre.setText(producto.nombre)
            binding.etCompatibilidad.setText(producto.marcaModeloCompatible)
            binding.etCilindraje.setText(producto.cilindraje)
            binding.etPrecio.setText(producto.precio.toString())
            binding.etStock.setText(producto.stock.toString())
            chipsPorCategoria[producto.categoria]?.let { binding.chipsCategoria.check(it) }
            if (imagenSeleccionada == null) binding.ivFoto.cargarImagen(producto.imagenUrl)
        }
    }

    /** Crea un archivo temporal compartido por FileProvider donde la cámara guarda la foto completa. */
    private fun abrirCamara() {
        val carpeta = File(cacheDir, CARPETA_FOTOS).apply { mkdirs() }
        val archivo = File(carpeta, "foto_${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", archivo)
        uriFotoCamara = uri
        try {
            tomarFoto.launch(uri)
        } catch (e: ActivityNotFoundException) {
            mostrarMensaje(R.string.publicar_sin_camara)
        }
    }

    /** Guarda la imagen elegida para subirla al publicar y la muestra como vista previa. */
    private fun mostrarImagenLocal(uri: Uri) {
        imagenSeleccionada = uri
        binding.ivFoto.cargarImagen(uri.toString())
    }

    /** Categoría del chip marcado (Accesorios por defecto). */
    private fun categoriaSeleccionada(): Categoria =
        chipsPorCategoria.entries.firstOrNull { it.value == binding.chipsCategoria.checkedChipId }?.key
            ?: Categoria.ACCESORIOS

    /** Valida el formulario, comprime la imagen nueva (si hay) y guarda en Firestore. */
    private fun publicar() {
        val precio = binding.etPrecio.texto().toIntOrNull()
        val stock = binding.etStock.texto().toIntOrNull()
        val vacio = listOf(binding.etNombre, binding.etCompatibilidad).firstOrNull { it.texto().isEmpty() }
        when {
            vacio != null -> return marcarError(vacio, R.string.error_campo_obligatorio)
            precio == null || precio <= 0 -> return marcarError(binding.etPrecio, R.string.error_precio)
            stock == null || stock < 0 -> return marcarError(binding.etStock, R.string.error_stock)
        }
        val base = productoEditado ?: Producto()
        val producto = base.copy(
            nombre = binding.etNombre.texto(),
            marcaModeloCompatible = binding.etCompatibilidad.texto(),
            cilindraje = binding.etCilindraje.texto(),
            precio = precio,
            stock = stock,
            categoria = categoriaSeleccionada()
        )
        binding.btnTomarFoto.habilitar(false)
        binding.btnGaleria.habilitar(false)
        lifecycleScope.launch {
            val guardado = conCarga(binding.progreso, binding.btnPublicar) {
                val imagen = imagenSeleccionada?.let { CompresorImagen.comprimir(this@PublicarProductoActivity, it) }
                productoRepository.guardarProducto(producto, imagen)
            }
            binding.btnTomarFoto.habilitar(true)
            binding.btnGaleria.habilitar(true)
            if (guardado != null) {
                mostrarMensaje(R.string.publicar_exito)
                finish()
            }
        }
    }

    /** Pide confirmación y elimina el producto con su imagen. */
    private fun confirmarEliminacion() {
        val producto = productoEditado ?: return
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.publicar_eliminar_titulo)
            .setMessage(getString(R.string.publicar_eliminar_mensaje, producto.nombre))
            .setNegativeButton(R.string.cancelar, null)
            .setPositiveButton(R.string.publicar_eliminar) { _, _ ->
                lifecycleScope.launch {
                    conCarga(binding.progreso, binding.btnEliminar) { productoRepository.eliminarProducto(producto) }
                        ?: return@launch
                    mostrarMensaje(R.string.publicar_eliminado)
                    finish()
                }
            }
            .show()
    }

    /** Muestra el error en el campo y le da el foco. */
    private fun marcarError(campo: EditText, mensaje: Int) {
        campo.error = getString(mensaje)
        campo.requestFocus()
    }

    companion object {
        private const val EXTRA_PRODUCTO_ID = "extra_producto_id"
        private const val ESTADO_FOTO_CAMARA = "foto_camara"
        private const val ESTADO_IMAGEN = "imagen_seleccionada"
        private const val CARPETA_FOTOS = "fotos"

        /** Intent para editar un producto existente. */
        fun crearIntent(context: Context, productoId: String): Intent =
            Intent(context, PublicarProductoActivity::class.java).putExtra(EXTRA_PRODUCTO_ID, productoId)
    }
}
