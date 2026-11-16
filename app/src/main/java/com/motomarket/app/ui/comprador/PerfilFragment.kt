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
import com.motomarket.app.databinding.DialogSolicitarVenderBinding
import com.motomarket.app.databinding.FragmentPerfilBinding
import com.motomarket.app.databinding.ItemDireccionBinding
import com.motomarket.app.models.EstadoSolicitud
import com.motomarket.app.models.Rol
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
 * Perfil del comprador: datos de la cuenta, solicitud para vender, direcciones usadas,
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

    /** Se recarga al volver para mostrar direcciones nuevas y cambios en la solicitud de vendedor. */
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
     * La tarjeta "Vender en MotoMarket" cambia según la solicitud: permite pedirla, muestra que está en
     * revisión o el motivo del rechazo, y si ya fue aprobada lleva al módulo de vendedor.
     */
    private fun mostrarSolicitud(usuario: Usuario) {
        binding.cardVender.isVisible = usuario.rol != Rol.ADMIN
        val boton = binding.btnSolicitarVender
        when {
            usuario.rol == Rol.VENDEDOR -> {
                binding.tvEstadoSolicitud.isVisible = true
                binding.tvEstadoSolicitud.aplicarChip(R.string.estado_aprobado, TonoChip.VERDE)
                binding.tvDetalleSolicitud.text = getString(R.string.vender_aprobada, usuario.nombreVendedor)
                boton.isVisible = true
                boton.setText(R.string.vender_ir_a_tienda)
                boton.setOnClickListener { requireActivity().abrirInicioPorRol(Rol.VENDEDOR) }
            }
            usuario.solicitudVendedor == EstadoSolicitud.PENDIENTE -> {
                binding.tvEstadoSolicitud.isVisible = true
                binding.tvEstadoSolicitud.aplicarChip(R.string.estado_pendiente, TonoChip.AMARILLO)
                binding.tvDetalleSolicitud.text = getString(R.string.vender_pendiente, usuario.nombreTienda)
                boton.isVisible = false
            }
            usuario.solicitudVendedor == EstadoSolicitud.RECHAZADA -> {
                binding.tvEstadoSolicitud.isVisible = true
                binding.tvEstadoSolicitud.aplicarChip(R.string.estado_rechazado, TonoChip.ROJO)
                binding.tvDetalleSolicitud.text = getString(R.string.vender_rechazada, usuario.motivoSolicitud)
                boton.isVisible = true
                boton.setText(R.string.vender_volver_a_solicitar)
                boton.setOnClickListener { pedirNombreTienda(usuario.nombreTienda) }
            }
            else -> {
                binding.tvEstadoSolicitud.isVisible = false
                binding.tvDetalleSolicitud.setText(R.string.vender_descripcion)
                boton.isVisible = true
                boton.setText(R.string.vender_solicitar)
                boton.setOnClickListener { pedirNombreTienda("") }
            }
        }
    }

    /** Diálogo para escribir el nombre de la tienda; solo se cierra si la solicitud se envía. */
    private fun pedirNombreTienda(nombreActual: String) {
        val dialogo = DialogSolicitarVenderBinding.inflate(layoutInflater)
        dialogo.etNombreTienda.setText(nombreActual)
        val alerta = MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.vender_solicitar)
            .setView(dialogo.root)
            .setNegativeButton(R.string.cancelar, null)
            .setPositiveButton(R.string.vender_enviar, null)
            .show()
        alerta.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val tienda = dialogo.etNombreTienda.texto()
            if (tienda.isEmpty()) {
                dialogo.etNombreTienda.error = getString(R.string.error_campo_obligatorio)
            } else {
                enviarSolicitud(tienda, alerta)
            }
        }
    }

    /** Guarda la solicitud pendiente y refresca la tarjeta. */
    private fun enviarSolicitud(tienda: String, alerta: AlertDialog) {
        viewLifecycleOwner.lifecycleScope.launch {
            requireContext().conCarga(binding.progreso, alerta.getButton(AlertDialog.BUTTON_POSITIVE)) {
                usuarioRepository.solicitarSerVendedor(tienda)
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
