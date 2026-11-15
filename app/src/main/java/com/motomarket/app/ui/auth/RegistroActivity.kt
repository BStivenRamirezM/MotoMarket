package com.motomarket.app.ui.auth

import android.os.Bundle
import android.util.Patterns
import androidx.lifecycle.lifecycleScope
import com.motomarket.app.R
import com.motomarket.app.data.Repositorios
import com.motomarket.app.data.UsuarioRepository
import com.motomarket.app.databinding.ActivityRegistroBinding
import com.motomarket.app.models.Rol
import com.motomarket.app.ui.common.BaseActivity
import com.motomarket.app.ui.common.abrirInicioPorRol
import com.motomarket.app.ui.common.mostrarMensaje
import com.motomarket.app.ui.common.texto
import kotlinx.coroutines.launch

class RegistroActivity : BaseActivity() {

    private lateinit var binding: ActivityRegistroBinding
    private val usuarioRepository: UsuarioRepository = Repositorios.usuarios

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegistroBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.btnCrearCuenta.setOnClickListener { registrar() }
    }

    private fun registrar() {
        val campos = listOf(binding.etNombre, binding.etCorreo, binding.etTelefono, binding.etPassword)
        val vacio = campos.firstOrNull { it.texto().isEmpty() }
        if (vacio != null) {
            vacio.error = getString(R.string.error_campo_obligatorio)
            return
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(binding.etCorreo.texto()).matches()) {
            binding.etCorreo.error = getString(R.string.error_correo_invalido)
            return
        }
        val rol = if (binding.rgRol.checkedRadioButtonId == R.id.rbVendedor) Rol.VENDEDOR else Rol.COMPRADOR
        lifecycleScope.launch {
            val usuario = usuarioRepository.registrar(
                binding.etNombre.texto(),
                binding.etCorreo.texto(),
                binding.etTelefono.texto(),
                binding.etPassword.texto(),
                rol
            )
            mostrarMensaje(getString(R.string.registro_exito, usuario.nombre))
            abrirInicioPorRol(usuario.rol)
        }
    }
}
