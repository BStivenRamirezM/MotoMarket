package com.motomarket.app.ui.auth

import android.os.Bundle
import android.util.Patterns
import androidx.lifecycle.lifecycleScope
import com.motomarket.app.R
import com.motomarket.app.data.Repositorios
import com.motomarket.app.data.UsuarioRepository
import com.motomarket.app.databinding.ActivityRecuperarPasswordBinding
import com.motomarket.app.ui.common.BaseActivity
import com.motomarket.app.ui.common.mostrarMensaje
import kotlinx.coroutines.launch

class RecuperarPasswordActivity : BaseActivity() {

    private lateinit var binding: ActivityRecuperarPasswordBinding
    private val usuarioRepository: UsuarioRepository = Repositorios.usuarios

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRecuperarPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.btnEnviar.setOnClickListener { enviarEnlace() }
    }

    private fun enviarEnlace() {
        val correo = binding.etCorreo.text.toString().trim()
        if (!Patterns.EMAIL_ADDRESS.matcher(correo).matches()) {
            binding.etCorreo.error = getString(R.string.error_correo_invalido)
            return
        }
        lifecycleScope.launch {
            if (usuarioRepository.recuperarPassword(correo)) {
                mostrarMensaje(getString(R.string.recuperar_confirmacion, correo))
                finish()
            }
        }
    }
}
