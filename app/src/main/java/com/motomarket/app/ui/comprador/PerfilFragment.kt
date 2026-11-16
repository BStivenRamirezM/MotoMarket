package com.motomarket.app.ui.comprador

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.motomarket.app.R
import com.motomarket.app.data.Repositorios
import com.motomarket.app.data.UsuarioRepository
import com.motomarket.app.databinding.DialogSolicitarRolBinding
import com.motomarket.app.databinding.FragmentPerfilBinding
import com.motomarket.app.databinding.ItemDireccionBinding
import com.motomarket.app.models.EstadoSolicitud
import com.motomarket.app.models.Rol
import com.motomarket.app.models.SolicitudRol
import com.motomarket.app.models.Usuario
import com.motomarket.app.ui.common.TonoChip
import com.motomarket.app.ui.common.abrirInicioPorRol
import com.motomarket.app.ui.common.aplicarChip
import com.motomarket.app.ui.common.cerrarSesion
import com.motomarket.app.ui.common.conCarga
import com.motomarket.app.ui.common.etiqueta
import com.motomarket.app.ui.common.mostrarMensaje
import com.motomarket.app.ui.common.texto
import kotlinx.coroutines.launch

/**
 * Perfil del comprador: datos de la cuenta, solicitud para vender o ser mecánico, direcciones usadas,
 * historial de pedidos y cierre de sesión.
 */
class PerfilFragment : Fragment() {

    private var _binding: FragmentPerfilBinding? = null
    private val binding get() = _binding!!

    private val usuarioRepository: UsuarioRepository by lazy { Repositorios.usuarios }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPerfilBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnCerrarSesion.setOnClickListener { requireActivity().cerrarSesion() }
        binding.btnMisPedidos.setOnClickListener {
            startActivity(Intent(requireContext(), HistorialPedidosActivity::class.java))
        }
    }

    /** Se recarga al volver para mostrar direcciones nuevas y cambios en la solicitud de rol. */
    override fun onResume() {
        super.onResume()
        cargarUsuario()
    }

    /** Lee el perfil del usuario autenticado y lo pinta. */
    private fun cargarUsuario() {
        viewLifecycleOwner.lifecycleScope.launch {
            requireContext().conCarga(binding.progreso) { usuarioRepository.obtenerUsuarioActual() }?.let { mostrarUsuario(it) }
        }
    }

    /** Pinta nombre, correo, rol, el estado de la solicitud y la lista de direcciones guardadas. */
    private fun mostrarUsuario(usuario: Usuario) {
        binding.tvNombre.text = usuario.nombre
        binding.tvCorreo.text = usuario.correo
        binding.tvRol.aplicarChip(usuario.rol.etiqueta(), TonoChip.NARANJA)
        mostrarSolicitud(usuario)
        binding.tvSinDirecciones.isVisible = usuario.direcciones.isEmpty()
        binding.contenedorDirecciones.removeAllViews()
        usuario.direcciones.forEach { direccion ->
            ItemDireccionBinding.inflate(layoutInflater, binding.contenedorDirecciones, true).tvDireccion.text = direccion
        }
    }

    /**
     * La tarjeta "Vende o repara en MotoMarket" cambia según la solicitud: permite pedir el rol de
     * vendedor o de mecánico, muestra que está en revisión o el motivo del rechazo y, si ya fue aprobada,
     * lleva al módulo correspondiente.
     */
    private fun mostrarSolicitud(usuario: Usuario) {
        binding.cardVender.isVisible = usuario.rol != Rol.ADMIN
        val principal = binding.btnSolicitarVender
        val secundario = binding.btnSolicitarMecanico
        val rolPedido = getString(usuario.rolSolicitado.etiqueta()).lowercase()
        when {
            usuario.rol == Rol.VENDEDOR || usuario.rol == Rol.MECANICO -> {
                binding.tvEstadoSolicitud.isVisible = true
                binding.tvEstadoSolicitud.aplicarChip(R.string.estado_aprobado, TonoChip.VERDE)
                binding.tvDetalleSolicitud.text = getString(R.string.solicitud_aprobada, usuario.nombrePublico)
                principal.isVisible = true
                principal.setText(if (usuario.rol == Rol.MECANICO) R.string.mecanico_ir_a_taller else R.string.vender_ir_a_tienda)
                principal.setOnClickListener { requireActivity().abrirInicioPorRol(usuario.rol) }
                secundario.isVisible = false
            }
            usuario.solicitudRol == EstadoSolicitud.PENDIENTE -> {
                binding.tvEstadoSolicitud.isVisible = true
                binding.tvEstadoSolicitud.aplicarChip(R.string.estado_pendiente, TonoChip.AMARILLO)
                binding.tvDetalleSolicitud.text = getString(R.string.solicitud_pendiente, rolPedido, usuario.nombreNegocio)
                principal.isVisible = false
                secundario.isVisible = false
            }
            usuario.solicitudRol == EstadoSolicitud.RECHAZADA -> {
                binding.tvEstadoSolicitud.isVisible = true
                binding.tvEstadoSolicitud.aplicarChip(R.string.estado_rechazado, TonoChip.ROJO)
                binding.tvDetalleSolicitud.text = getString(R.string.solicitud_rechazada, rolPedido, usuario.motivoSolicitud)
                mostrarBotonesDeSolicitud(usuario.nombreNegocio)
            }
            else -> {
                binding.tvEstadoSolicitud.isVisible = false
                binding.tvDetalleSolicitud.setText(R.string.vender_descripcion)
                mostrarBotonesDeSolicitud("")
            }
        }
    }

    /** Muestra las dos opciones para pedir un rol: vendedor (principal) o mecánico (secundaria). */
    private fun mostrarBotonesDeSolicitud(nombreActual: String) {
        binding.btnSolicitarVender.isVisible = true
        binding.btnSolicitarVender.setText(R.string.vender_solicitar)
        binding.btnSolicitarVender.setOnClickListener { pedirNombreNegocio(Rol.VENDEDOR, nombreActual) }
        binding.btnSolicitarMecanico.isVisible = true
        binding.btnSolicitarMecanico.setOnClickListener { pedirNombreNegocio(Rol.MECANICO, nombreActual) }
    }

    /** Diálogo para escribir el nombre de la tienda o taller; solo se cierra si la solicitud se envía. */
    private fun pedirNombreNegocio(rol: Rol, nombreActual: String) {
        val dialogo = DialogSolicitarRolBinding.inflate(layoutInflater)
        dialogo.etNombreTienda.setText(nombreActual)
        val alerta = MaterialAlertDialogBuilder(requireContext())
            .setTitle(if (rol == Rol.MECANICO) R.string.mecanico_solicitar else R.string.vender_solicitar)
            .setView(dialogo.root)
            .setNegativeButton(R.string.cancelar, null)
            .setPositiveButton(R.string.vender_enviar, null)
            .show()
        alerta.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val negocio = dialogo.etNombreTienda.texto()
            if (negocio.isEmpty()) {
                dialogo.etNombreTienda.error = getString(R.string.error_campo_obligatorio)
            } else {
                enviarSolicitud(SolicitudRol(rol, negocio), alerta)
            }
        }
    }

    /** Guarda la solicitud pendiente y refresca la tarjeta. */
    private fun enviarSolicitud(solicitud: SolicitudRol, alerta: AlertDialog) {
        viewLifecycleOwner.lifecycleScope.launch {
            requireContext().conCarga(binding.progreso, alerta.getButton(AlertDialog.BUTTON_POSITIVE)) {
                usuarioRepository.solicitarRol(solicitud)
            } ?: return@launch
            alerta.dismiss()
            requireContext().mostrarMensaje(R.string.vender_solicitud_enviada)
            cargarUsuario()
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
