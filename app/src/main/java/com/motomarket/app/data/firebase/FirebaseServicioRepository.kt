package com.motomarket.app.data.firebase

import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import com.motomarket.app.data.ServicioRepository
import com.motomarket.app.data.SesionRequeridaException
import com.motomarket.app.data.UsuarioRepository
import com.motomarket.app.models.EstadoServicio
import com.motomarket.app.models.LineaServicio
import com.motomarket.app.models.Mecanico
import com.motomarket.app.models.ServicioTecnico
import kotlinx.coroutines.tasks.await

/**
 * Servicios técnicos en la colección "servicios" y mecánicos públicos en "mecanicos".
 * Cada operación solo modifica los campos que las reglas de Firestore permiten a ese rol.
 */
class FirebaseServicioRepository(
    private val usuarioRepository: UsuarioRepository
) : ServicioRepository {

    private val db = Firebase.firestore
    private val servicios = db.collection(Colecciones.SERVICIOS)

    /** Lee los perfiles públicos activos y los ordena por nombre del taller. */
    override suspend fun obtenerMecanicos(): List<Mecanico> =
        db.collection(Colecciones.MECANICOS).whereEqualTo("activo", true).get().await().documents
            .map { it.aMecanico() }
            .sortedBy { it.nombreTaller.lowercase() }

    /** Crea el servicio en estado "solicitado", sin líneas ni total (los pone el mecánico). */
    override suspend fun solicitarServicio(mecanico: Mecanico, moto: String, descripcionFalla: String): ServicioTecnico {
        val comprador = usuarioRepository.obtenerUsuarioActual()
        val referencia = servicios.document()
        val ahora = System.currentTimeMillis()
        val servicio = ServicioTecnico(
            id = referencia.id,
            compradorId = comprador.id,
            compradorNombre = comprador.nombre,
            mecanicoId = mecanico.id,
            mecanicoNombre = mecanico.nombreTaller,
            moto = moto,
            descripcionFalla = descripcionFalla,
            estado = EstadoServicio.SOLICITADO,
            fechaSolicitud = ahora,
            fechaActualizacion = ahora
        )
        referencia.set(servicio.aMapa()).await()
        return servicio
    }

    /** Servicios con compradorId igual al usuario actual. */
    override suspend fun obtenerServiciosComprador(): List<ServicioTecnico> = consultarPor("compradorId")

    /** Servicios con mecanicoId igual al usuario actual. */
    override suspend fun obtenerServiciosMecanico(): List<ServicioTecnico> = consultarPor("mecanicoId")

    /** Lee un servicio; null si el documento no existe. */
    override suspend fun obtenerServicio(id: String): ServicioTecnico? {
        val documento = servicios.document(id).get().await()
        return if (documento.exists()) documento.aServicio() else null
    }

    /** Guarda líneas, total calculado y nota, y deja el servicio "cotizado". */
    override suspend fun enviarCotizacion(id: String, lineas: List<LineaServicio>, nota: String) {
        actualizar(
            id,
            "lineas" to lineas.aMapas(),
            "total" to lineas.sumOf { it.subtotal },
            "notaMecanico" to nota,
            "estado" to EstadoServicio.COTIZADO.aTexto()
        )
    }

    /** Pasa la solicitud a "rechazado" con el motivo del mecánico. */
    override suspend fun rechazarSolicitud(id: String, motivo: String) {
        actualizar(id, "notaMecanico" to motivo, "estado" to EstadoServicio.RECHAZADO.aTexto())
    }

    /** Pasa la solicitud a "cancelado". */
    override suspend fun cancelarSolicitud(id: String) {
        actualizar(id, "estado" to EstadoServicio.CANCELADO.aTexto())
    }

    /** Pasa la cotización a "rechazado". */
    override suspend fun rechazarCotizacion(id: String) {
        actualizar(id, "estado" to EstadoServicio.RECHAZADO.aTexto())
    }

    /** Registra la referencia y el método de pago, la fecha de pago y el estado "pagado". */
    override suspend fun registrarPago(id: String, refPago: String, metodoPago: String) {
        actualizar(
            id,
            "refPago" to refPago,
            "metodoPago" to metodoPago,
            "fechaPago" to System.currentTimeMillis(),
            "estado" to EstadoServicio.PAGADO.aTexto()
        )
    }

    /** Actualiza el estado de trabajo del servicio pagado. */
    override suspend fun avanzarEstado(id: String, estado: EstadoServicio) {
        actualizar(id, "estado" to estado.aTexto())
    }

    /** Consulta los servicios donde [campo] es el uid actual, del más reciente al más antiguo. */
    private suspend fun consultarPor(campo: String): List<ServicioTecnico> {
        val uid = usuarioRepository.uidActual() ?: throw SesionRequeridaException()
        return servicios.whereEqualTo(campo, uid).get().await().documents
            .map { it.aServicio() }
            .sortedByDescending { it.fechaSolicitud }
    }

    /** Aplica los [cambios] y registra siempre la fecha de la última actualización. */
    private suspend fun actualizar(id: String, vararg cambios: Pair<String, Any>) {
        servicios.document(id).update(mapOf(*cambios) + ("fechaActualizacion" to System.currentTimeMillis())).await()
    }
}
