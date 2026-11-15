package com.motomarket.app.ui.comprador

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.motomarket.app.R
import com.motomarket.app.data.CarritoRepository
import com.motomarket.app.data.Repositorios
import com.motomarket.app.databinding.ActivityPasarelaPagoBinding
import com.motomarket.app.ui.common.BaseActivity
import com.motomarket.app.ui.common.mostrarMensaje
import com.motomarket.app.util.formatearCOP
import kotlinx.coroutines.launch

class PasarelaPagoActivity : BaseActivity() {

    private lateinit var binding: ActivityPasarelaPagoBinding
    private val carritoRepository: CarritoRepository = Repositorios.carrito
    private var metodoSeleccionado: View? = null
    private var total = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPasarelaPagoBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.toolbar.setNavigationOnClickListener { finish() }

        val opciones: Map<View, TextView> = mapOf(
            binding.opcionTarjeta to binding.tvElegirTarjeta,
            binding.opcionPse to binding.tvElegirPse
        )
        opciones.keys.forEach { opcion ->
            opcion.setOnClickListener { seleccionarMetodo(opcion, opciones) }
        }
        binding.btnConfirmarPago.setOnClickListener { confirmarPago() }

        lifecycleScope.launch {
            total = carritoRepository.obtenerResumen().total
            binding.btnConfirmarPago.text = getString(R.string.pago_confirmar, formatearCOP(total))
        }
    }

    private fun seleccionarMetodo(seleccionada: View, opciones: Map<View, TextView>) {
        metodoSeleccionado = seleccionada
        opciones.forEach { (opcion, etiqueta) ->
            val activa = opcion == seleccionada
            opcion.isSelected = activa
            etiqueta.setText(if (activa) R.string.pago_elegido else R.string.pago_elegir)
        }
    }

    private fun confirmarPago() {
        if (metodoSeleccionado == null) {
            mostrarMensaje(R.string.pago_elige_metodo)
            return
        }
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.pago_exitoso_titulo)
            .setMessage(getString(R.string.pago_exitoso_mensaje, formatearCOP(total)))
            .setCancelable(false)
            .setPositiveButton(R.string.aceptar) { _, _ ->
                lifecycleScope.launch {
                    carritoRepository.vaciar()
                    finish()
                }
            }
            .show()
    }
}
