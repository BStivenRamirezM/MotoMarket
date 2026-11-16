package com.motomarket.app.ui.comprador

import android.Manifest
import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.motomarket.app.R
import com.motomarket.app.data.CarritoRepository
import com.motomarket.app.data.PedidoRepository
import com.motomarket.app.data.Repositorios
import com.motomarket.app.data.UsuarioRepository
import com.motomarket.app.databinding.ActivityPasarelaPagoBinding
import com.motomarket.app.models.Pedido
import com.motomarket.app.models.ResumenCarrito
import com.motomarket.app.pagos.EstadoTransaccion
import com.motomarket.app.pagos.LineaPago
import com.motomarket.app.pagos.MetodoPago
import com.motomarket.app.pagos.OrdenPago
import com.motomarket.app.pagos.PasarelaPagos
import com.motomarket.app.pagos.Pasarelas
import com.motomarket.app.pagos.ResultadoPago
import com.motomarket.app.ui.common.BaseActivity
import com.motomarket.app.ui.common.conCarga
import com.motomarket.app.ui.common.habilitar
import com.motomarket.app.ui.common.intentar
import com.motomarket.app.ui.common.mensajeDeError
import com.motomarket.app.ui.common.mostrarMensaje
import com.motomarket.app.ui.common.texto
import com.motomarket.app.util.ServicioUbicacion
import com.motomarket.app.util.formatearCOP
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/**
 * Pago seguro: dirección por GPS (o escrita a mano), método de pago y cobro con la [PasarelaPagos]
 * configurada en [Pasarelas]. Si el pago se aprueba crea la orden, descuenta stock, vacía el carrito
 * y muestra el número de orden.
 */
class PasarelaPagoActivity : BaseActivity() {

    private lateinit var binding: ActivityPasarelaPagoBinding
    private val carritoRepository: CarritoRepository by lazy { Repositorios.carrito }
    private val pedidoRepository: PedidoRepository by lazy { Repositorios.pedidos }
    private val usuarioRepository: UsuarioRepository by lazy { Repositorios.usuarios }
    private val pasarela: PasarelaPagos by lazy { Pasarelas.para(this) }
    private val servicioUbicacion by lazy { ServicioUbicacion(this) }

    private var metodoSeleccionado: MetodoPago? = null
    private var direccionGps: String? = null
    private var lat: Double? = null
    private var lng: Double? = null

    private val permisoUbicacion = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permisos ->
        if (permisos.values.any { it }) {
            obtenerUbicacion()
        } else {
            mostrarMensaje(R.string.ubicacion_permiso_denegado)
            binding.etDireccion.requestFocus()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPasarelaPagoBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.toolbar.setNavigationOnClickListener { finish() }

        val opciones: Map<View, Pair<TextView, MetodoPago>> = mapOf(
            binding.opcionTarjeta to (binding.tvElegirTarjeta to MetodoPago.TARJETA),
            binding.opcionPse to (binding.tvElegirPse to MetodoPago.PSE)
        )
        opciones.keys.forEach { opcion -> opcion.setOnClickListener { seleccionarMetodo(opcion, opciones) } }
        binding.btnUsarUbicacion.setOnClickListener { solicitarUbicacion() }
        binding.btnConfirmarPago.setOnClickListener { confirmarPago() }
        binding.btnConfirmarPago.habilitar(false)
        cargarResumen()
    }

    /** Lee el carrito para mostrar el total en el botón de confirmar. */
    private fun cargarResumen() {
        lifecycleScope.launch {
            val actual = conCarga(binding.progreso) { carritoRepository.obtenerResumen() } ?: return@launch
            binding.btnConfirmarPago.text = getString(R.string.pago_confirmar, formatearCOP(actual.total))
            binding.btnConfirmarPago.habilitar(actual.items.isNotEmpty())
        }
    }

    /** Resalta la opción elegida, cambia su etiqueta a "Elegido" y guarda el método. */
    private fun seleccionarMetodo(seleccionada: View, opciones: Map<View, Pair<TextView, MetodoPago>>) {
        opciones.forEach { (opcion, datos) ->
            val activa = opcion == seleccionada
            opcion.isSelected = activa
            datos.first.setText(if (activa) R.string.pago_elegido else R.string.pago_elegir)
            if (activa) metodoSeleccionado = datos.second
        }
    }

    /** Pide el permiso de ubicación solo si aún no se ha concedido. */
    private fun solicitarUbicacion() {
        if (servicioUbicacion.tienePermiso()) {
            obtenerUbicacion()
        } else {
            permisoUbicacion.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        }
    }

    /** Obtiene coordenadas con Fused Location y las convierte en dirección con Geocoder. */
    @SuppressLint("MissingPermission")
    private fun obtenerUbicacion() {
        if (!servicioUbicacion.tienePermiso()) return
        lifecycleScope.launch {
            val ubicacion = conCarga(binding.progresoUbicacion, binding.btnUsarUbicacion) { servicioUbicacion.ubicacionActual() }
            if (ubicacion == null) {
                mostrarMensaje(R.string.ubicacion_no_disponible)
                binding.etDireccion.requestFocus()
                return@launch
            }
            lat = ubicacion.latitude
            lng = ubicacion.longitude
            val direccion = servicioUbicacion.direccionDe(ubicacion.latitude, ubicacion.longitude)
                ?: getString(R.string.ubicacion_coordenadas, ubicacion.latitude, ubicacion.longitude)
            direccionGps = direccion
            binding.etDireccion.setText(direccion)
        }
    }

    /** Valida dirección, método y stock; luego cobra con la pasarela y, si aprueba, registra la orden. */
    private fun confirmarPago() {
        val metodo = metodoSeleccionado ?: return mostrarMensaje(R.string.pago_elige_metodo)
        val direccion = binding.etDireccion.texto()
        if (direccion.isEmpty()) {
            binding.etDireccion.error = getString(R.string.error_direccion_obligatoria)
            binding.etDireccion.requestFocus()
            return
        }

        lifecycleScope.launch {
            val compra = conCarga(binding.progreso, binding.btnConfirmarPago) { carritoRepository.obtenerResumen() }
                ?: return@launch
            if (compra.items.isEmpty()) return@launch mostrarMensaje(R.string.carrito_vacio)
            compra.items.firstOrNull { it.cantidad > it.producto.stock }?.let {
                return@launch mostrarMensaje(getString(R.string.error_stock_insuficiente, it.producto.nombre, it.producto.stock))
            }
            val comprador = intentar { usuarioRepository.obtenerUsuarioActual() } ?: return@launch

            val orden = OrdenPago(
                referenciaInterna = "MM-${System.currentTimeMillis()}",
                lineas = compra.items.map { LineaPago(it.producto.nombre, it.cantidad, it.subtotal) },
                subtotal = compra.subtotal,
                envio = compra.envio,
                total = compra.total,
                metodo = metodo,
                compradorNombre = comprador.nombre,
                compradorCorreo = comprador.correo,
                direccionEntrega = direccion
            )
            val resultado = pasarela.procesarPago(orden)
            procesarResultado(resultado, compra, metodo, direccion)
        }
    }

    /** Decide según el estado de la transacción: registrar la orden o informar al comprador. */
    private fun procesarResultado(resultado: ResultadoPago, compra: ResumenCarrito, metodo: MetodoPago, direccion: String) {
        when (resultado.estado) {
            EstadoTransaccion.APROBADA -> registrarOrden(resultado, compra, metodo, direccion)
            EstadoTransaccion.CANCELADA -> mostrarMensaje(R.string.pago_cancelado)
            EstadoTransaccion.PENDIENTE -> mostrarAviso(getString(R.string.pago_pendiente, resultado.referencia))
            EstadoTransaccion.RECHAZADA -> mostrarAviso(getString(R.string.pago_rechazado, resultado.mensaje))
        }
    }

    /**
     * Con el pago aprobado: crea la orden con la referencia de la pasarela y descuenta stock en una
     * transacción, vacía el carrito y guarda la dirección. Si algo falla después del cobro, informa la referencia.
     */
    private fun registrarOrden(resultado: ResultadoPago, compra: ResumenCarrito, metodo: MetodoPago, direccion: String) {
        val conGps = direccion == direccionGps
        lifecycleScope.launch {
            binding.progreso.isVisible = true
            binding.btnConfirmarPago.habilitar(false)
            try {
                val pedido = pedidoRepository.confirmarCompra(
                    compra, direccion, lat.takeIf { conGps }, lng.takeIf { conGps }, metodo.codigo, resultado.referencia
                )
                carritoRepository.vaciar()
                runCatching { usuarioRepository.agregarDireccion(direccion) }
                mostrarExito(pedido)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                mostrarAviso(getString(R.string.pago_aprobado_sin_orden, resultado.referencia, mensajeDeError(e)))
            } finally {
                binding.progreso.isVisible = false
                binding.btnConfirmarPago.habilitar(true)
            }
        }
    }

    /** Diálogo final con el número de orden y la referencia de pago; al aceptar cierra la pasarela. */
    private fun mostrarExito(pedido: Pedido) {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.pago_exitoso_titulo)
            .setMessage(getString(R.string.pago_exitoso_mensaje, pedido.numero, formatearCOP(pedido.total), pedido.refPago))
            .setCancelable(false)
            .setPositiveButton(R.string.aceptar) { _, _ -> finish() }
            .show()
    }

    /** Diálogo informativo para pagos cancelados, pendientes, rechazados o errores. */
    private fun mostrarAviso(mensaje: String) {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.titulo_pago)
            .setMessage(mensaje)
            .setPositiveButton(R.string.aceptar, null)
            .show()
    }
}
