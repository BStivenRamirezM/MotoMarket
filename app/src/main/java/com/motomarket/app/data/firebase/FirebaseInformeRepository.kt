package com.motomarket.app.data.firebase

import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import com.motomarket.app.data.InformeRepository
import com.motomarket.app.data.UsuarioRepository
import com.motomarket.app.models.Pedido
import com.motomarket.app.models.Rol
import com.motomarket.app.models.ServicioTecnico
import com.motomarket.app.models.Usuario
import kotlinx.coroutines.tasks.await

/**
 * Consultas de rango para los informes. El administrador filtra por fecha en el servidor; el vendedor
 * consulta sus órdenes con array-contains (lo que permiten las reglas) y filtra la fecha en el dispositivo.
 */
class FirebaseInformeRepository(
    private val usuarioRepository: UsuarioRepository
) : InformeRepository {

    private val db = Firebase.firestore

    /** Órdenes del rango: todas para el admin, solo las que incluyen productos del vendedor para él. */
    override suspend fun obtenerPedidos(desde: Long, hasta: Long): List<Pedido> {
        val usuario = usuarioRepository.obtenerUsuarioActual()
        val pedidos = db.collection(Colecciones.PEDIDOS)
        val documentos = if (usuario.rol == Rol.ADMIN) {
            pedidos.whereGreaterThanOrEqualTo("fecha", desde).whereLessThanOrEqualTo("fecha", hasta).get().await().documents
        } else {
            pedidos.whereArrayContains("vendedorIds", usuario.id).get().await().documents
        }
        return documentos.map { it.aPedido() }.filter { it.fecha in desde..hasta }.sortedByDescending { it.fecha }
    }

    /** Servicios solicitados en el rango, ordenados del más reciente al más antiguo. */
    override suspend fun obtenerServicios(desde: Long, hasta: Long): List<ServicioTecnico> =
        db.collection(Colecciones.SERVICIOS)
            .whereGreaterThanOrEqualTo("fechaSolicitud", desde).whereLessThanOrEqualTo("fechaSolicitud", hasta)
            .get().await().documents
            .map { it.aServicio() }
            .sortedByDescending { it.fechaSolicitud }

    /** Perfiles con fecha de registro en el rango, del más reciente al más antiguo. */
    override suspend fun obtenerUsuariosRegistrados(desde: Long, hasta: Long): List<Usuario> =
        db.collection(Colecciones.USUARIOS)
            .whereGreaterThanOrEqualTo("fechaRegistro", desde).whereLessThanOrEqualTo("fechaRegistro", hasta)
            .get().await().documents
            .map { it.aUsuario() }
            .sortedByDescending { it.fechaRegistro }
}
