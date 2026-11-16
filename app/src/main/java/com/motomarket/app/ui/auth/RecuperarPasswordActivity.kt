package com.motomarket.app.ui.auth

import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import com.motomarket.app.R
import com.motomarket.app.data.Repositorios
import com.motomarket.app.data.UsuarioRepository
import com.motomarket.app.databinding.ActivityRecuperarPasswordBinding
import com.motomarket.app.ui.common.BaseActivity
import com.motomarket.app.ui.common.conCarga
import com.motomarket.app.ui.common.esCorreoValido
import com.motomarket.app.ui.common.mostrarMensaje
import com.motomarket.app.ui.common.texto
import kotlinx.coroutines.launch

/** Envía el correo de restablecimiento de contraseña de Firebase Auth. */
class RecuperarPasswordActivity : BaseActivity() {

    private lateinit var binding: ActivityRecuperarPasswordBinding
    private val usuarioRepository: UsuarioRepository by lazy { Repositorios.usuarios }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRecuperarPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.btnEnviar.setOnClickListener { enviarEnlace() }
    }

    private fun enviarEnlace() {
        val correo = binding.etCorreo.texto()
        if (!esCorreoValido(correo)) {
            binding.etCorreo.error = getString(R.string.error_correo_invalido)
            return
        }
        if (!Repositorios.firebaseConfigurado(this)) {
            mostrarMensaje(R.string.error_firebase_no_configurado)
            return
        }
        lifecycleScope.launch {
            conCarga(binding.progreso, binding.btnEnviar) { usuarioRepository.recuperarPassword(correo) } ?: return@launch
            mostrarMensaje(getString(R.string.recuperar_confirmacion, correo))
            finish()
        }
    }
}
