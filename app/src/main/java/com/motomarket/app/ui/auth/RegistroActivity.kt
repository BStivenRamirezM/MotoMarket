package com.motomarket.app.ui.auth

import android.os.Bundle
import android.widget.EditText
import androidx.lifecycle.lifecycleScope
import com.motomarket.app.R
import com.motomarket.app.data.Repositorios
import com.motomarket.app.data.UsuarioRepository
import com.motomarket.app.data.local.SesionLocal
import com.motomarket.app.databinding.ActivityRegistroBinding
import com.motomarket.app.models.Rol
import com.motomarket.app.ui.common.BaseActivity
import com.motomarket.app.ui.common.LONGITUD_MINIMA_PASSWORD
import com.motomarket.app.ui.common.abrirInicioPorRol
import com.motomarket.app.ui.common.conCarga
import com.motomarket.app.ui.common.esCorreoValido
import com.motomarket.app.ui.common.mostrarMensaje
import com.motomarket.app.ui.common.texto
import kotlinx.coroutines.launch

/**
 * Registro de compradores y vendedores: crea la cuenta en Firebase Auth y su perfil en "usuarios".
 * El rol administrador no se ofrece aquí; se asigna manualmente en Firestore.
 */
class RegistroActivity : BaseActivity() {

    private lateinit var binding: ActivityRegistroBinding
    private val usuarioRepository: UsuarioRepository by lazy { Repositorios.usuarios }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegistroBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.btnCrearCuenta.setOnClickListener { registrar() }
    }

    private fun registrar() {
        if (!formularioValido()) return
        val rol = if (binding.rgRol.checkedRadioButtonId == R.id.rbVendedor) Rol.VENDEDOR else Rol.COMPRADOR
        lifecycleScope.launch {
            val usuario = conCarga(binding.progreso, binding.btnCrearCuenta) {
                usuarioRepository.registrar(
                    binding.etNombre.texto(),
                    binding.etCorreo.texto(),
                    binding.etTelefono.texto(),
                    binding.etPassword.text.toString(),
                    rol
                )
            } ?: return@launch
            SesionLocal(this@RegistroActivity).guardar(usuario.id, usuario.correo)
            mostrarMensaje(getString(R.string.registro_exito, usuario.nombre))
            abrirInicioPorRol(usuario.rol)
        }
    }

    /** Campos obligatorios, correo con formato válido y contraseña de al menos 8 caracteres. */
    private fun formularioValido(): Boolean {
        val vacio = listOf(binding.etNombre, binding.etCorreo, binding.etTelefono, binding.etPassword)
            .firstOrNull { it.texto().isEmpty() }
        return when {
            vacio != null -> marcarError(vacio, getString(R.string.error_campo_obligatorio))
            !esCorreoValido(binding.etCorreo.texto()) -> marcarError(binding.etCorreo, getString(R.string.error_correo_invalido))
            binding.etPassword.text.length < LONGITUD_MINIMA_PASSWORD ->
                marcarError(binding.etPassword, getString(R.string.error_password_corta, LONGITUD_MINIMA_PASSWORD))
            else -> true
        }
    }

    private fun marcarError(campo: EditText, mensaje: String): Boolean {
        campo.error = mensaje
        campo.requestFocus()
        return false
    }
}
