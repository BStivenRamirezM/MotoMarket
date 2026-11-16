package com.motomarket.app.ui.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
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
import com.motomarket.app.ui.common.LONGITUD_MINIMA_PASSWORD
import com.motomarket.app.ui.common.conCarga
import com.motomarket.app.ui.common.esCorreoValido
import com.motomarket.app.ui.common.etiqueta
import com.motomarket.app.ui.common.intentar
import com.motomarket.app.ui.common.mostrarMensaje
import com.motomarket.app.ui.common.texto
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Gestión de cuentas: buscar, activar/desactivar, cambiar rol y crear usuarios. */
class GestionUsuariosFragment : Fragment() {

    private var _binding: FragmentGestionUsuariosBinding? = null
    private val binding get() = _binding!!

    private val usuarioRepository: UsuarioRepository by lazy { Repositorios.usuarios }
    private val adapter = UsuarioAdapter(
        onCambiarEstado = { usuario, activo -> cambiarEstado(usuario, activo) },
        onUsuarioClick = { usuario -> elegirRol(usuario) }
    )
    private var busqueda: Job? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentGestionUsuariosBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.rvUsuarios.adapter = adapter
        binding.etBuscar.doAfterTextChanged { cargarUsuarios(ESPERA_ESCRITURA_MS) }
        binding.btnCrearUsuario.setOnClickListener { mostrarDialogoCrear() }
        cargarUsuarios()
    }

    /** Busca usuarios; [espera] agrupa las teclas para no consultar en cada letra. */
    private fun cargarUsuarios(espera: Long = 0L) {
        busqueda?.cancel()
        busqueda = viewLifecycleOwner.lifecycleScope.launch {
            delay(espera)
            val texto = binding.etBuscar.texto()
            requireContext().conCarga(binding.progreso) { usuarioRepository.buscarUsuarios(texto) }
                ?.let { adapter.submitList(it) }
        }
    }

    /** El administrador no puede desactivarse a sí mismo para no quedar sin acceso. */
    private fun cambiarEstado(usuario: Usuario, activo: Boolean) {
        if (usuario.id == usuarioRepository.uidActual()) {
            requireContext().mostrarMensaje(R.string.usuarios_no_desactivar_propio)
            cargarUsuarios()
            return
        }
        viewLifecycleOwner.lifecycleScope.launch {
            val resultado = requireContext().intentar { usuarioRepository.cambiarEstado(usuario.id, activo) }
            if (resultado != null) {
                val mensaje = if (activo) R.string.usuarios_cuenta_activada else R.string.usuarios_cuenta_desactivada
                requireContext().mostrarMensaje(getString(mensaje, usuario.nombre))
            }
            cargarUsuarios()
        }
    }

    /** Diálogo de selección única para asignar un nuevo rol. */
    private fun elegirRol(usuario: Usuario) {
        if (usuario.id == usuarioRepository.uidActual()) {
            requireContext().mostrarMensaje(R.string.usuarios_no_cambiar_rol_propio)
            return
        }
        val roles = Rol.entries
        var elegido = roles.indexOf(usuario.rol)
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.usuarios_cambiar_rol, usuario.nombre))
            .setSingleChoiceItems(roles.map { getString(it.etiqueta()) }.toTypedArray(), elegido) { _, indice -> elegido = indice }
            .setNegativeButton(R.string.cancelar, null)
            .setPositiveButton(R.string.guardar) { _, _ ->
                val rol = roles[elegido]
                if (rol == usuario.rol) return@setPositiveButton
                viewLifecycleOwner.lifecycleScope.launch {
                    requireContext().intentar { usuarioRepository.cambiarRol(usuario.id, rol) } ?: return@launch
                    requireContext().mostrarMensaje(getString(R.string.usuarios_rol_actualizado, usuario.nombre))
                    cargarUsuarios()
                }
            }
            .show()
    }

    /** Abre el formulario de creación; el diálogo solo se cierra si la creación tiene éxito. */
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
                crearUsuario(dialogo, alerta)
            }
        }
    }

    /** Nombre obligatorio, correo válido y contraseña de al menos 8 caracteres. */
    private fun formularioValido(dialogo: DialogCrearUsuarioBinding): Boolean = when {
        dialogo.etNombre.texto().isEmpty() -> marcarError(dialogo.etNombre, getString(R.string.error_campo_obligatorio))
        !esCorreoValido(dialogo.etCorreo.texto()) -> marcarError(dialogo.etCorreo, getString(R.string.error_correo_invalido))
        dialogo.etPassword.text.length < LONGITUD_MINIMA_PASSWORD ->
            marcarError(dialogo.etPassword, getString(R.string.error_password_corta, LONGITUD_MINIMA_PASSWORD))
        else -> true
    }

    /** Muestra el error en el campo y devuelve false para cortar la validación. */
    private fun marcarError(campo: EditText, mensaje: String): Boolean {
        campo.error = mensaje
        return false
    }

    /** Rol marcado en el formulario de creación. */
    private fun rolSeleccionado(dialogo: DialogCrearUsuarioBinding): Rol = when (dialogo.rgRol.checkedRadioButtonId) {
        R.id.rbVendedor -> Rol.VENDEDOR
        R.id.rbAdmin -> Rol.ADMIN
        else -> Rol.COMPRADOR
    }

    /** Crea la cuenta en Auth y su perfil; el diálogo se cierra solo si la operación tuvo éxito. */
    private fun crearUsuario(dialogo: DialogCrearUsuarioBinding, alerta: AlertDialog) {
        val boton = alerta.getButton(AlertDialog.BUTTON_POSITIVE)
        viewLifecycleOwner.lifecycleScope.launch {
            val usuario = requireContext().conCarga(binding.progreso, boton) {
                usuarioRepository.crearUsuario(
                    dialogo.etNombre.texto(), dialogo.etCorreo.texto(), dialogo.etPassword.text.toString(), rolSeleccionado(dialogo)
                )
            } ?: return@launch
            alerta.dismiss()
            requireContext().mostrarMensaje(getString(R.string.usuarios_creado, usuario.nombre))
            cargarUsuarios()
        }
    }

    override fun onDestroyView() {
        binding.rvUsuarios.adapter = null
        _binding = null
        super.onDestroyView()
    }

    private companion object {
        const val ESPERA_ESCRITURA_MS = 350L
    }
}
