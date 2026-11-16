package com.motomarket.app.pagos.simulada

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.core.content.IntentCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.motomarket.app.R
import com.motomarket.app.databinding.ActivityPagoSimuladoBinding
import com.motomarket.app.databinding.ItemLineaPagoBinding
import com.motomarket.app.pagos.OrdenPago
import com.motomarket.app.ui.common.BaseActivity
import com.motomarket.app.util.formatearCOP
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Pantalla de confirmación de la pasarela simulada: muestra el resumen del cobro y, al pagar,
 * simula 2 segundos de procesamiento antes de devolver una transacción aprobada.
 */
class PagoSimuladoActivity : BaseActivity() {

    private lateinit var binding: ActivityPagoSimuladoBinding

    /** Mientras se "procesa" el pago no se permite volver atrás. */
    private val bloquearAtras = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() = Unit
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPagoSimuladoBinding.inflate(layoutInflater)
        setContentView(binding.root)
        onBackPressedDispatcher.addCallback(this, bloquearAtras)

        val orden = IntentCompat.getSerializableExtra(intent, EXTRA_ORDEN, OrdenPago::class.java)
        if (orden == null) {
            cancelar()
            return
        }
        mostrarResumen(orden)
        binding.toolbar.setNavigationOnClickListener { cancelar() }
        binding.btnCancelar.setOnClickListener { cancelar() }
        binding.btnPagar.setOnClickListener { procesar() }
    }

    /** Llena el resumen con las líneas, los totales, el método, el pagador y la dirección. */
    private fun mostrarResumen(orden: OrdenPago) {
        binding.contenedorLineas.removeAllViews()
        orden.lineas.forEach { linea ->
            ItemLineaPagoBinding.inflate(layoutInflater, binding.contenedorLineas, true).apply {
                tvDescripcion.text = getString(R.string.pago_simulado_linea, linea.descripcion, linea.cantidad)
                tvSubtotal.text = formatearCOP(linea.subtotal)
            }
        }
        binding.tvSubtotal.text = formatearCOP(orden.subtotal)
        binding.tvEnvio.text = formatearCOP(orden.envio)
        binding.tvTotal.text = formatearCOP(orden.total)
        binding.tvMetodo.setText(orden.metodo.etiqueta)
        binding.tvComprador.text = getString(R.string.pago_simulado_comprador, orden.compradorNombre, orden.compradorCorreo)
        binding.tvDireccion.text = orden.direccionEntrega
        binding.tvFactura.text = getString(R.string.pago_simulado_factura, orden.referenciaInterna)
        binding.btnPagar.text = getString(R.string.pago_simulado_pagar, formatearCOP(orden.total))
    }

    /** Muestra el progreso, espera el tiempo de procesamiento y aprueba con referencia SIM-<timestamp>. */
    private fun procesar() {
        binding.contenedorBotones.isVisible = false
        binding.contenedorProcesando.isVisible = true
        binding.toolbar.navigationIcon = null
        bloquearAtras.isEnabled = true
        lifecycleScope.launch {
            delay(TIEMPO_PROCESAMIENTO_MS)
            setResult(Activity.RESULT_OK, Intent().putExtra(EXTRA_REFERENCIA, "SIM-${System.currentTimeMillis()}"))
            finish()
        }
    }

    /** Devuelve RESULT_CANCELED, que la pasarela traduce a una transacción cancelada. */
    private fun cancelar() {
        setResult(Activity.RESULT_CANCELED)
        finish()
    }

    companion object {
        const val EXTRA_REFERENCIA = "referencia"
        private const val EXTRA_ORDEN = "orden"
        private const val TIEMPO_PROCESAMIENTO_MS = 2_000L

        /** Intent con la orden a cobrar. */
        fun crearIntent(context: Context, orden: OrdenPago): Intent =
            Intent(context, PagoSimuladoActivity::class.java).putExtra(EXTRA_ORDEN, orden)
    }
}
