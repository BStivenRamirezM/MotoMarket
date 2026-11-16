package com.motomarket.app.ui.auth

import android.os.Bundle
import android.widget.EditText
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.motomarket.app.R
import com.motomarket.app.data.Repositorios
import com.motomarket.app.data.UsuarioRepository
import com.motomarket.app.data.local.SesionLocal
import com.motomarket.app.databinding.ActivityRegistroBinding
import com.motomarket.app.ui.common.BaseActivity
import com.motomarket.app.ui.common.LONGITUD_MINIMA_PASSWORD
import com.motomarket.app.ui.common.abrirInicioPorRol
import com.motomarket.app.ui.common.conCarga
import com.motomarket.app.ui.common.esCorreoValido
import com.motomarket.app.ui.common.mostrarMensaje
import com.motomarket.app.ui.common.texto
import kotlinx.coroutines.launch

/**
 * Registro: toda cuenta nace como comprador. Quien quiera vender marca la casilla y deja el nombre
 * de su tienda; la solicitud queda pendiente hasta que un administrador la apruebe.
 */
class RegistroActivity : BaseActivity() {

    private lateinit var binding: ActivityRegistroBinding
    private val usuarioRepository: UsuarioRepository by lazy { Repositorios.usuarios }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegistroBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.cbQuieroVender.setOnCheckedChangeListener { _, marcado -> binding.contenedorTienda.isVisible = marcado }
        binding.btnCrearCuenta.setOnClickListener { registrar() }
    }

    /** Crea la cuenta de comprador (con solicitud de vendedor si se pidió) y abre el catálogo. */
    private fun registrar() {
        if (!formularioValido()) return
        val tienda = binding.etNombreTienda.texto().takeIf { binding.cbQuieroVender.isChecked }
        lifecycleScope.launch {
            val usuario = conCarga(binding.progreso, binding.btnCrearCuenta) {
                usuarioRepository.registrar(
                    binding.etNombre.texto(),
                    binding.etCorreo.texto(),
                    binding.etTelefono.texto(),
                    binding.etPassword.text.toString(),
                    tienda
                )
            } ?: return@launch
            SesionLocal(this@RegistroActivity).guardar(usuario.id, usuario.correo)
            val mensaje = if (tienda != null) R.string.registro_exito_con_solicitud else R.string.registro_exito
            mostrarMensaje(getString(mensaje, usuario.nombre))
            abrirInicioPorRol(usuario.rol)
        }
    }

    /** Campos obligatorios, correo válido, contraseña de al menos 8 caracteres y tienda si quiere vender. */
    private fun formularioValido(): Boolean {
        val vacio = listOf(binding.etNombre, binding.etCorreo, binding.etTelefono, binding.etPassword)
            .firstOrNull { it.texto().isEmpty() }
        return when {
            vacio != null -> marcarError(vacio, getString(R.string.error_campo_obligatorio))
            !esCorreoValido(binding.etCorreo.texto()) -> marcarError(binding.etCorreo, getString(R.string.error_correo_invalido))
            binding.etPassword.text.length < LONGITUD_MINIMA_PASSWORD ->
                marcarError(binding.etPassword, getString(R.string.error_password_corta, LONGITUD_MINIMA_PASSWORD))
            binding.cbQuieroVender.isChecked && binding.etNombreTienda.texto().isEmpty() ->
                marcarError(binding.etNombreTienda, getString(R.string.error_campo_obligatorio))
            else -> true
        }
    }

    /** Muestra el error en el campo, le da el foco y devuelve false. */
    private fun marcarError(campo: EditText, mensaje: String): Boolean {
        campo.error = mensaje
        campo.requestFocus()
        return false
    }
}
