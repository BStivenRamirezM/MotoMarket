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
import com.motomarket.app.data.local.SesionLocal
import com.motomarket.app.databinding.ActivityLoginBinding
import com.motomarket.app.models.Usuario
import com.motomarket.app.ui.common.BaseActivity
import com.motomarket.app.ui.common.abrirInicioPorRol
import com.motomarket.app.ui.common.conCarga
import com.motomarket.app.ui.common.esCorreoValido
import com.motomarket.app.ui.common.mostrarMensaje
import com.motomarket.app.ui.common.texto
import kotlinx.coroutines.launch

/**
 * Inicio de sesión con correo/contraseña de Firebase Auth y reingreso con huella.
 * Después de autenticar valida que la cuenta esté activa y abre el módulo según el rol.
 */
class LoginActivity : BaseActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val usuarioRepository: UsuarioRepository by lazy { Repositorios.usuarios }
    private val sesionLocal by lazy { SesionLocal(this) }
    private var firebaseListo = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseListo = Repositorios.firebaseConfigurado(this)
        if (!firebaseListo) mostrarMensaje(R.string.error_firebase_no_configurado)
        sesionLocal.correo?.let { binding.etCorreo.setText(it) }

        binding.btnHuella.setOnClickListener { if (verificarFirebase()) autenticarConHuella() }
        binding.btnIniciarSesion.setOnClickListener { if (verificarFirebase()) iniciarSesion() }
        binding.tvOlvidePassword.setOnClickListener {
            startActivity(Intent(this, RecuperarPasswordActivity::class.java))
        }
        binding.tvRegistrarse.setOnClickListener {
            startActivity(Intent(this, RegistroActivity::class.java))
        }
    }

    /** Evita cualquier llamada a Firebase si la app se compiló sin google-services.json. */
    private fun verificarFirebase(): Boolean {
        if (!firebaseListo) mostrarMensaje(R.string.error_firebase_no_configurado)
        return firebaseListo
    }

    /** Valida el formulario y autentica contra Firebase. */
    private fun iniciarSesion() {
        val correo = binding.etCorreo.texto()
        val password = binding.etPassword.text.toString()
        if (!esCorreoValido(correo)) {
            binding.etCorreo.error = getString(R.string.error_correo_invalido)
            return
        }
        if (password.isEmpty()) {
            binding.etPassword.error = getString(R.string.error_campo_obligatorio)
            return
        }
        lifecycleScope.launch {
            conCarga(binding.progreso, binding.btnIniciarSesion) {
                usuarioRepository.iniciarSesion(correo, password)
            }?.let { entrar(it) }
        }
    }

    /**
     * La huella solo reabre una sesión de Firebase que sigue vigente en este dispositivo y que
     * coincide con el último usuario que ingresó con contraseña.
     */
    private fun autenticarConHuella() {
        val uidGuardado = sesionLocal.uid
        if (uidGuardado == null || uidGuardado != usuarioRepository.uidActual()) {
            mostrarMensaje(R.string.huella_sin_sesion)
            return
        }
        if (BiometricManager.from(this).canAuthenticate(BIOMETRIC_WEAK) != BiometricManager.BIOMETRIC_SUCCESS) {
            mostrarMensaje(R.string.huella_no_disponible)
            return
        }
        val prompt = BiometricPrompt(this, ContextCompat.getMainExecutor(this), object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                lifecycleScope.launch {
                    conCarga(binding.progreso, binding.btnHuella) { usuarioRepository.sesionActiva() }
                        ?.let { entrar(it) }
                }
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

    /** Recuerda la sesión para la huella y navega al módulo del rol. */
    private fun entrar(usuario: Usuario) {
        sesionLocal.guardar(usuario.id, usuario.correo)
        mostrarMensaje(getString(R.string.login_bienvenido_usuario, usuario.nombre))
        abrirInicioPorRol(usuario.rol)
    }
}
