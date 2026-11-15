package com.motomarket.app.ui.admin

import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.motomarket.app.R
import com.motomarket.app.data.Repositorios
import com.motomarket.app.data.UsuarioRepository
import com.motomarket.app.databinding.DialogCrearUsuarioBinding
import com.motomarket.app.databinding.FragmentGestionUsuariosBinding
import com.motomarket.app.models.Rol
import com.motomarket.app.models.Usuario
import com.motomarket.app.ui.common.mostrarMensaje
import com.motomarket.app.ui.common.texto
import kotlinx.coroutines.launch

class GestionUsuariosFragment : Fragment() {

    private var _binding: FragmentGestionUsuariosBinding? = null
    private val binding get() = _binding!!

    private val usuarioRepository: UsuarioRepository = Repositorios.usuarios
    private val adapter = UsuarioAdapter { usuario, activo -> cambiarEstado(usuario, activo) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentGestionUsuariosBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.rvUsuarios.adapter = adapter
        binding.etBuscar.doAfterTextChanged { cargarUsuarios() }
        binding.btnCrearUsuario.setOnClickListener { mostrarDialogoCrear() }
        cargarUsuarios()
    }

    private fun cargarUsuarios() {
        val texto = binding.etBuscar.texto()
        viewLifecycleOwner.lifecycleScope.launch {
            adapter.submitList(usuarioRepository.buscarUsuarios(texto))
        }
    }

    private fun cambiarEstado(usuario: Usuario, activo: Boolean) {
        viewLifecycleOwner.lifecycleScope.launch {
            usuarioRepository.cambiarEstado(usuario.id, activo)
            val mensaje = if (activo) R.string.usuarios_cuenta_activada else R.string.usuarios_cuenta_desactivada
            requireContext().mostrarMensaje(getString(mensaje, usuario.nombre))
            cargarUsuarios()
        }
    }

    private fun mostrarDialogoCrear() {
        val dialogo = DialogCrearUsuarioBinding.inflate(layoutInflater)
        val alerta = MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.usuarios_crear)
            .setView(dialogo.root)
            .setNegativeButton(R.string.cancelar, null)
            .setPositiveButton(R.string.guardar, null)
            .show()

        alerta.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            if (formularioValido(dialogo)) {
                crearUsuario(dialogo.etNombre.texto(), dialogo.etCorreo.texto(), rolSeleccionado(dialogo))
                alerta.dismiss()
            }
        }
    }

    private fun formularioValido(dialogo: DialogCrearUsuarioBinding): Boolean {
        if (dialogo.etNombre.texto().isEmpty()) {
            dialogo.etNombre.error = getString(R.string.error_campo_obligatorio)
            return false
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(dialogo.etCorreo.texto()).matches()) {
            dialogo.etCorreo.error = getString(R.string.error_correo_invalido)
            return false
        }
        return true
    }

    private fun rolSeleccionado(dialogo: DialogCrearUsuarioBinding): Rol = when (dialogo.rgRol.checkedRadioButtonId) {
        R.id.rbVendedor -> Rol.VENDEDOR
        R.id.rbAdmin -> Rol.ADMIN
        else -> Rol.COMPRADOR
    }

    private fun crearUsuario(nombre: String, correo: String, rol: Rol) {
        viewLifecycleOwner.lifecycleScope.launch {
            val usuario = usuarioRepository.crearUsuario(nombre, correo, rol)
            requireContext().mostrarMensaje(getString(R.string.usuarios_creado, usuario.nombre))
            cargarUsuarios()
        }
    }

    override fun onDestroyView() {
        binding.rvUsuarios.adapter = null
        _binding = null
        super.onDestroyView()
    }
}
