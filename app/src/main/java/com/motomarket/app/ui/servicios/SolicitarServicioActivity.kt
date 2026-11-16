package com.motomarket.app.ui.servicios

import android.os.Bundle
import android.widget.EditText
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.motomarket.app.R
import com.motomarket.app.data.Repositorios
import com.motomarket.app.data.ServicioRepository
import com.motomarket.app.databinding.ActivitySolicitarServicioBinding
import com.motomarket.app.models.Mecanico
import com.motomarket.app.ui.common.BaseActivity
import com.motomarket.app.ui.common.conCarga
import com.motomarket.app.ui.common.mostrarMensaje
import com.motomarket.app.ui.common.texto
import kotlinx.coroutines.launch

/**
 * El comprador elige un mecánico aprobado, describe su moto y la falla, y envía la solicitud.
 * No hay precio aquí: el mecánico responde con una cotización detallada.
 */
class SolicitarServicioActivity : BaseActivity() {

    private lateinit var binding: ActivitySolicitarServicioBinding
    private val servicioRepository: ServicioRepository by lazy { Repositorios.servicios }
    private var mecanicoElegido: Mecanico? = null
    private val adapter = MecanicoAdapter { mecanico -> mecanicoElegido = mecanico }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySolicitarServicioBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.rvMecanicos.adapter = adapter
        binding.btnEnviar.setOnClickListener { enviar() }
        cargarMecanicos()
    }

    /** Lista los mecánicos activos; si no hay ninguno lo indica y no deja enviar. */
    private fun cargarMecanicos() {
        lifecycleScope.launch {
            val mecanicos = conCarga(binding.progresoMecanicos) { servicioRepository.obtenerMecanicos() } ?: return@launch
            adapter.submitList(mecanicos)
            binding.tvSinMecanicos.isVisible = mecanicos.isEmpty()
            binding.btnEnviar.isEnabled = mecanicos.isNotEmpty()
        }
    }

    /** Valida mecánico, moto y descripción, y crea la solicitud en Firestore. */
    private fun enviar() {
        val mecanico = mecanicoElegido ?: return mostrarMensaje(R.string.solicitar_falta_mecanico)
        val vacio = listOf(binding.etMoto, binding.etFalla).firstOrNull { it.texto().isEmpty() }
        if (vacio != null) return marcarError(vacio)
        lifecycleScope.launch {
            conCarga(binding.progreso, binding.btnEnviar) {
                servicioRepository.solicitarServicio(mecanico, binding.etMoto.texto(), binding.etFalla.texto())
            } ?: return@launch
            mostrarMensaje(getString(R.string.solicitar_enviada, mecanico.nombreTaller))
            finish()
        }
    }

    /** Marca el campo como obligatorio y le da el foco. */
    private fun marcarError(campo: EditText) {
        campo.error = getString(R.string.error_campo_obligatorio)
        campo.requestFocus()
    }
}
