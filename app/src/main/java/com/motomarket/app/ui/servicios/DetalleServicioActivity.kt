package com.motomarket.app.ui.servicios

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.motomarket.app.R
import com.motomarket.app.data.Repositorios
import com.motomarket.app.data.ServicioRepository
import com.motomarket.app.databinding.ActivityDetalleServicioBinding
import com.motomarket.app.databinding.DialogLineaServicioBinding
import com.motomarket.app.databinding.DialogMotivoRechazoBinding
import com.motomarket.app.databinding.ItemLineaServicioBinding
import com.motomarket.app.models.EstadoServicio
import com.motomarket.app.models.LineaServicio
import com.motomarket.app.models.ServicioTecnico
import com.motomarket.app.pagos.EstadoTransaccion
import com.motomarket.app.pagos.LineaPago
import com.motomarket.app.pagos.MetodoPago
import com.motomarket.app.pagos.OrdenPago
import com.motomarket.app.pagos.Pasarelas
import com.motomarket.app.pagos.ResultadoPago
import com.motomarket.app.ui.common.BaseActivity
import com.motomarket.app.ui.common.conCarga
import com.motomarket.app.ui.common.etiqueta
import com.motomarket.app.ui.common.mostrarEstado
import com.motomarket.app.ui.common.mostrarMensaje
import com.motomarket.app.ui.common.texto
import com.motomarket.app.util.formatearCOP
import com.motomarket.app.util.formatearFecha
import kotlinx.coroutines.launch

/**
 * Detalle de un servicio técnico que se adapta a quien lo abre. El mecánico arma la cotización con
 * líneas libres (cantidad × precio), la envía o rechaza la solicitud y avanza el trabajo pagado.
 * El comprador acepta y paga la cotización con la pasarela, la rechaza o cancela su solicitud.
 */
class DetalleServicioActivity : BaseActivity() {

    private lateinit var binding: ActivityDetalleServicioBinding
    private val servicioRepository: ServicioRepository by lazy { Repositorios.servicios }
    private val pasarela by lazy { Pasarelas.para(this) }

    private var servicio: ServicioTecnico? = null
    private val lineasEditadas = mutableListOf<LineaServicio>()
    private var editando = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetalleServicioBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.btnAgregarLinea.setOnClickListener { editarLinea(null) }
        cargar()
    }

    /** Lee el servicio y decide qué puede hacer el usuario actual según su papel y el estado. */
    private fun cargar() {
        lifecycleScope.launch {
            val id = intent.getStringExtra(EXTRA_SERVICIO_ID).orEmpty()
            val actual = conCarga(binding.progreso) { servicioRepository.obtenerServicio(id) }
            if (actual == null) {
                mostrarMensaje(R.string.servicio_no_encontrado)
                finish()
                return@launch
            }
            servicio = actual
            val esMecanico = actual.mecanicoId == Repositorios.usuarios.uidActual()
            editando = esMecanico && actual.estado.cotizable
            lineasEditadas.clear()
            lineasEditadas.addAll(actual.lineas)
            mostrarServicio(actual, esMecanico)
        }
    }

    /** Pinta los datos generales del servicio y configura cotización y acciones. */
    private fun mostrarServicio(servicio: ServicioTecnico, esMecanico: Boolean) {
        binding.tvNumero.text = getString(R.string.servicio_numero, servicio.numero)
        binding.tvEstado.mostrarEstado(servicio.estado)
        binding.tvContraparte.text = if (esMecanico) {
            getString(R.string.servicio_cliente, servicio.compradorNombre)
        } else {
            getString(R.string.servicio_taller, servicio.mecanicoNombre)
        }
        binding.tvMoto.text = servicio.moto
        binding.tvFalla.text = servicio.descripcionFalla
        binding.tvFecha.text = getString(R.string.servicio_solicitado_el, formatearFecha(servicio.fechaSolicitud))
        binding.tvNota.isVisible = servicio.notaMecanico.isNotBlank()
        binding.tvNota.text = getString(R.string.servicio_nota_mecanico, servicio.notaMecanico)
        binding.tvRefPago.isVisible = servicio.refPago.isNotBlank()
        binding.tvRefPago.text = getString(R.string.servicio_ref_pago, servicio.refPago, formatearFecha(servicio.fechaPago))
        binding.tvAyudaCotizacion.text = getString(ayudaCotizacion(servicio.estado, esMecanico))
        binding.btnAgregarLinea.isVisible = editando
        pintarLineas()
        if (esMecanico) configurarAccionesMecanico(servicio) else configurarAccionesComprador(servicio)
    }

    @StringRes
    private fun ayudaCotizacion(estado: EstadoServicio, esMecanico: Boolean): Int = when {
        editando -> R.string.servicio_ayuda_editar
        estado == EstadoServicio.SOLICITADO && !esMecanico -> R.string.servicio_ayuda_esperando
        estado == EstadoServicio.COTIZADO && !esMecanico -> R.string.servicio_ayuda_revisar
        else -> R.string.servicio_ayuda_cerrada
    }

    /** Dibuja las líneas actuales y el total; en edición cada línea se puede tocar o eliminar. */
    private fun pintarLineas() {
        binding.contenedorLineas.removeAllViews()
        lineasEditadas.forEachIndexed { indice, linea ->
            ItemLineaServicioBinding.inflate(layoutInflater, binding.contenedorLineas, true).apply {
                tvDescripcion.text = linea.descripcion
                tvCantidadPrecio.text = getString(R.string.servicio_linea_detalle, linea.cantidad, formatearCOP(linea.precioUnitario))
                tvSubtotal.text = formatearCOP(linea.subtotal)
                btnEliminar.isVisible = editando
                btnEliminar.setOnClickListener {
                    lineasEditadas.removeAt(indice)
                    pintarLineas()
                }
                if (editando) root.setOnClickListener { editarLinea(indice) }
            }
        }
        binding.tvTotal.text = formatearCOP(lineasEditadas.sumOf { it.subtotal })
        binding.tvTotal.isVisible = lineasEditadas.isNotEmpty() || !editando
    }

    /** Diálogo para crear ([indice] null) o editar una línea con descripción, cantidad y precio unitario. */
    private fun editarLinea(indice: Int?) {
        val dialogo = DialogLineaServicioBinding.inflate(layoutInflater)
        indice?.let { lineasEditadas[it] }?.let { linea ->
            dialogo.etDescripcion.setText(linea.descripcion)
            dialogo.etCantidad.setText(linea.cantidad.toString())
            dialogo.etPrecio.setText(linea.precioUnitario.toString())
        } ?: dialogo.etCantidad.setText("1")
        val alerta = MaterialAlertDialogBuilder(this)
            .setTitle(if (indice == null) R.string.servicio_agregar_linea else R.string.servicio_editar_linea)
            .setView(dialogo.root)
            .setNegativeButton(R.string.cancelar, null)
            .setPositiveButton(R.string.guardar, null)
            .show()
        alerta.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val cantidad = dialogo.etCantidad.texto().toIntOrNull()
            val precio = dialogo.etPrecio.texto().toIntOrNull()
            when {
                dialogo.etDescripcion.texto().isEmpty() -> {
                    dialogo.etDescripcion.error = getString(R.string.error_campo_obligatorio)
                }
                cantidad == null || cantidad < 1 -> {
                    dialogo.etCantidad.error = getString(R.string.error_cantidad)
                }
                precio == null || precio <= 0 -> {
                    dialogo.etPrecio.error = getString(R.string.error_precio)
                }
                else -> {
                    val linea = LineaServicio(dialogo.etDescripcion.texto(), cantidad, precio)
                    if (indice == null) lineasEditadas.add(linea) else lineasEditadas[indice] = linea
                    pintarLineas()
                    alerta.dismiss()
                }
            }
        }
    }

    /** Mecánico: cotizar o rechazar mientras no esté pagado; luego, avanzar el trabajo. */
    private fun configurarAccionesMecanico(servicio: ServicioTecnico) {
        val siguiente = servicio.estado.siguiente()
        when {
            editando -> {
                mostrarBoton(binding.btnPrincipal, R.string.servicio_enviar_cotizacion) { enviarCotizacion(servicio) }
                if (servicio.estado == EstadoServicio.SOLICITADO) {
                    mostrarBoton(binding.btnSecundario, R.string.servicio_rechazar_solicitud) { rechazarSolicitud(servicio) }
                } else {
                    binding.btnSecundario.isVisible = false
                }
            }
            siguiente != null -> {
                binding.btnPrincipal.isVisible = true
                binding.btnPrincipal.text = getString(R.string.servicio_marcar_como, getString(siguiente.etiqueta()))
                binding.btnPrincipal.setOnClickListener { avanzar(servicio, siguiente) }
                binding.btnSecundario.isVisible = false
            }
            else -> ocultarAcciones()
        }
    }

    /** Comprador: cancelar mientras espera, o aceptar y pagar / rechazar la cotización recibida. */
    private fun configurarAccionesComprador(servicio: ServicioTecnico) {
        when (servicio.estado) {
            EstadoServicio.SOLICITADO -> {
                binding.btnPrincipal.isVisible = false
                mostrarBoton(binding.btnSecundario, R.string.servicio_cancelar_solicitud) {
                    confirmar(R.string.servicio_cancelar_solicitud, R.string.servicio_cancelar_mensaje) {
                        ejecutar(R.string.servicio_cancelado_msg) { servicioRepository.cancelarSolicitud(servicio.id) }
                    }
                }
            }
            EstadoServicio.COTIZADO -> {
                binding.btnPrincipal.isVisible = true
                binding.btnPrincipal.text = getString(R.string.servicio_aceptar_pagar, formatearCOP(servicio.total))
                binding.btnPrincipal.setOnClickListener { elegirMetodoYPagar(servicio) }
                mostrarBoton(binding.btnSecundario, R.string.servicio_rechazar_cotizacion) {
                    confirmar(R.string.servicio_rechazar_cotizacion, R.string.servicio_rechazar_cotizacion_mensaje) {
                        ejecutar(R.string.servicio_cotizacion_rechazada_msg) { servicioRepository.rechazarCotizacion(servicio.id) }
                    }
                }
            }
            else -> ocultarAcciones()
        }
    }

    /** Muestra un botón de acción con su texto y su comportamiento. */
    private fun mostrarBoton(boton: Button, @StringRes texto: Int, accion: () -> Unit) {
        boton.isVisible = true
        boton.setText(texto)
        boton.setOnClickListener { accion() }
    }

    /** Oculta ambos botones cuando el usuario ya no tiene acciones disponibles. */
    private fun ocultarAcciones() {
        binding.btnPrincipal.isVisible = false
        binding.btnSecundario.isVisible = false
    }

    /** Envía la cotización con las líneas editadas; exige al menos una línea. */
    private fun enviarCotizacion(servicio: ServicioTecnico) {
        if (lineasEditadas.isEmpty()) return mostrarMensaje(R.string.servicio_sin_lineas)
        val lineas = lineasEditadas.toList()
        ejecutar(R.string.servicio_cotizacion_enviada_msg) { servicioRepository.enviarCotizacion(servicio.id, lineas, "") }
    }

    /** El mecánico rechaza la solicitud indicando un motivo que verá el comprador. */
    private fun rechazarSolicitud(servicio: ServicioTecnico) {
        val dialogo = DialogMotivoRechazoBinding.inflate(layoutInflater)
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.servicio_rechazar_solicitud)
            .setView(dialogo.root)
            .setNegativeButton(R.string.cancelar, null)
            .setPositiveButton(R.string.aprobacion_rechazar) { _, _ ->
                val motivo = dialogo.etMotivo.texto().ifBlank { getString(R.string.servicio_motivo_defecto) }
                ejecutar(R.string.servicio_solicitud_rechazada_msg) { servicioRepository.rechazarSolicitud(servicio.id, motivo) }
            }
            .show()
    }

    /** Confirma y pasa el trabajo pagado al siguiente estado. */
    private fun avanzar(servicio: ServicioTecnico, siguiente: EstadoServicio) {
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.servicio_numero, servicio.numero))
            .setMessage(getString(R.string.pedidos_cambiar_estado_mensaje, getString(siguiente.etiqueta())))
            .setNegativeButton(R.string.cancelar, null)
            .setPositiveButton(R.string.aceptar) { _, _ ->
                ejecutar(R.string.servicio_estado_actualizado_msg) { servicioRepository.avanzarEstado(servicio.id, siguiente) }
            }
            .show()
    }

    /** Pide el medio de pago y cobra la cotización con la pasarela configurada en [Pasarelas]. */
    private fun elegirMetodoYPagar(servicio: ServicioTecnico) {
        val metodos = MetodoPago.entries
        var elegido = 0
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.pago_metodo)
            .setSingleChoiceItems(metodos.map { getString(it.etiqueta) }.toTypedArray(), elegido) { _, indice -> elegido = indice }
            .setNegativeButton(R.string.cancelar, null)
            .setPositiveButton(R.string.servicio_continuar_pago) { _, _ -> pagar(servicio, metodos[elegido]) }
            .show()
    }

    /** Arma la orden de pago con las líneas de la cotización y la cobra con la pasarela. */
    private fun pagar(servicio: ServicioTecnico, metodo: MetodoPago) {
        lifecycleScope.launch {
            val comprador = conCarga(binding.progreso, binding.btnPrincipal) { Repositorios.usuarios.obtenerUsuarioActual() }
                ?: return@launch
            val orden = OrdenPago(
                referenciaInterna = "SRV-${servicio.numero}",
                lineas = servicio.lineas.map { LineaPago(it.descripcion, it.cantidad, it.subtotal) },
                subtotal = servicio.total,
                envio = 0,
                total = servicio.total,
                metodo = metodo,
                compradorNombre = comprador.nombre,
                compradorCorreo = comprador.correo,
                direccionEntrega = getString(R.string.servicio_en_taller, servicio.mecanicoNombre)
            )
            procesarResultado(servicio, metodo, pasarela.procesarPago(orden))
        }
    }

    /** Si el pago se aprueba lo registra en el servicio; si no, informa al comprador. */
    private fun procesarResultado(servicio: ServicioTecnico, metodo: MetodoPago, resultado: ResultadoPago) {
        when (resultado.estado) {
            EstadoTransaccion.APROBADA -> lifecycleScope.launch {
                conCarga(binding.progreso, binding.btnPrincipal) {
                    servicioRepository.registrarPago(servicio.id, resultado.referencia, metodo.codigo)
                } ?: return@launch mostrarAviso(getString(R.string.servicio_pago_sin_registro, resultado.referencia))
                MaterialAlertDialogBuilder(this@DetalleServicioActivity)
                    .setTitle(R.string.pago_exitoso_titulo)
                    .setMessage(getString(R.string.servicio_pago_exitoso, servicio.numero, formatearCOP(servicio.total), resultado.referencia))
                    .setPositiveButton(R.string.aceptar) { _, _ -> cargar() }
                    .setCancelable(false)
                    .show()
            }
            EstadoTransaccion.CANCELADA -> mostrarMensaje(R.string.servicio_pago_cancelado)
            EstadoTransaccion.PENDIENTE -> mostrarAviso(getString(R.string.pago_pendiente, resultado.referencia))
            EstadoTransaccion.RECHAZADA -> mostrarAviso(getString(R.string.pago_rechazado, resultado.mensaje))
        }
    }

    /** Ejecuta una acción remota con carga y, si sale bien, avisa y recarga el detalle. */
    private fun ejecutar(@StringRes exito: Int, accion: suspend () -> Unit) {
        lifecycleScope.launch {
            conCarga(binding.progreso, binding.btnPrincipal) { accion() } ?: return@launch
            mostrarMensaje(exito)
            cargar()
        }
    }

    /** Diálogo de confirmación antes de una acción que no se puede deshacer. */
    private fun confirmar(@StringRes titulo: Int, @StringRes mensaje: Int, accion: () -> Unit) {
        MaterialAlertDialogBuilder(this)
            .setTitle(titulo)
            .setMessage(mensaje)
            .setNegativeButton(R.string.cancelar, null)
            .setPositiveButton(R.string.aceptar) { _, _ -> accion() }
            .show()
    }

    /** Diálogo informativo para pagos pendientes, rechazados o errores. */
    private fun mostrarAviso(mensaje: String) {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.titulo_servicio_tecnico)
            .setMessage(mensaje)
            .setPositiveButton(R.string.aceptar, null)
            .show()
    }

    companion object {
        private const val EXTRA_SERVICIO_ID = "extra_servicio_id"

        /** Intent para abrir el detalle de [servicioId]. */
        fun crearIntent(context: Context, servicioId: String): Intent =
            Intent(context, DetalleServicioActivity::class.java).putExtra(EXTRA_SERVICIO_ID, servicioId)
    }
}
