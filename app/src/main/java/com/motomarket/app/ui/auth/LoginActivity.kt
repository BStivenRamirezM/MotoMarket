package com.motomarket.app.ui.auth

import android.content.Intent
import android.os.Bundle
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.motomarket.app.R
import com.motomarket.app.data.Repositorios
import com.motomarket.app.data.UsuarioRepository
import com.motomarket.app.databinding.ActivityLoginBinding
import com.motomarket.app.models.Rol
import com.motomarket.app.ui.common.BaseActivity
import com.motomarket.app.ui.common.abrirInicioPorRol
import com.motomarket.app.ui.common.mostrarMensaje
import com.motomarket.app.ui.common.texto
import kotlinx.coroutines.launch

class LoginActivity : BaseActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val usuarioRepository: UsuarioRepository = Repositorios.usuarios

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnHuella.setOnClickListener { autenticarConHuella() }
        binding.btnIniciarSesion.setOnClickListener { iniciarSesion() }
        binding.tvOlvidePassword.setOnClickListener {
            startActivity(Intent(this, RecuperarPasswordActivity::class.java))
        }
        binding.tvRegistrarse.setOnClickListener {
            startActivity(Intent(this, RegistroActivity::class.java))
        }
        binding.btnDemoComprador.setOnClickListener { abrirInicioPorRol(Rol.COMPRADOR) }
        binding.btnDemoVendedor.setOnClickListener { abrirInicioPorRol(Rol.VENDEDOR) }
        binding.btnDemoAdmin.setOnClickListener { abrirInicioPorRol(Rol.ADMIN) }
    }

    private fun iniciarSesion() {
        val correo = binding.etCorreo.texto()
        val password = binding.etPassword.text.toString()
        if (correo.isEmpty()) {
            binding.etCorreo.error = getString(R.string.error_campo_obligatorio)
            return
        }
        if (password.isEmpty()) {
            binding.etPassword.error = getString(R.string.error_campo_obligatorio)
            return
        }
        lifecycleScope.launch {
            val usuario = usuarioRepository.iniciarSesion(correo, password)
            if (usuario == null) {
                mostrarMensaje(R.string.login_error_credenciales)
            } else {
                abrirInicioPorRol(usuario.rol)
            }
        }
    }

    private fun autenticarConHuella() {
        if (BiometricManager.from(this).canAuthenticate(BIOMETRIC_WEAK) != BiometricManager.BIOMETRIC_SUCCESS) {
            mostrarMensaje(R.string.huella_no_disponible)
            return
        }
        val prompt = BiometricPrompt(this, ContextCompat.getMainExecutor(this), object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                mostrarMensaje(R.string.huella_exito)
                abrirInicioPorRol(Rol.COMPRADOR)
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                if (errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON && errorCode != BiometricPrompt.ERROR_USER_CANCELED) {
                    mostrarMensaje(errString)
                }
            }
        })
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle(getString(R.string.huella_titulo))
            .setSubtitle(getString(R.string.huella_subtitulo))
            .setNegativeButtonText(getString(R.string.cancelar))
            .setAllowedAuthenticators(BIOMETRIC_WEAK)
            .build()
        prompt.authenticate(info)
    }
}
