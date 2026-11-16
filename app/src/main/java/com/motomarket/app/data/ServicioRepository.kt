package com.motomarket.app.data

import com.motomarket.app.models.EstadoServicio
import com.motomarket.app.models.LineaServicio
import com.motomarket.app.models.Mecanico
import com.motomarket.app.models.ServicioTecnico

/** Servicio técnico: solicitudes del comprador, cotizaciones del mecánico, pago y seguimiento. */
interface ServicioRepository {

    /** Mecánicos aprobados y activos que pueden recibir solicitudes. */
    suspend fun obtenerMecanicos(): List<Mecanico>

    /** El comprador actual solicita un servicio a [mecanico] describiendo su moto y la falla. */
    suspend fun solicitarServicio(mecanico: Mecanico, moto: String, descripcionFalla: String): ServicioTecnico

    /** Servicios solicitados por el comprador actual, del más reciente al más antiguo. */
    suspend fun obtenerServiciosComprador(): List<ServicioTecnico>

    /** Servicios asignados al mecánico actual, del más reciente al más antiguo. */
    suspend fun obtenerServiciosMecanico(): List<ServicioTecnico>

    /** Un servicio por id, o null si no existe o no es visible para el usuario. */
    suspend fun obtenerServicio(id: String): ServicioTecnico?

    /** El mecánico envía (o corrige) la cotización; el total se calcula a partir de las líneas. */
    suspend fun enviarCotizacion(id: String, lineas: List<LineaServicio>, nota: String)

    /** El mecánico rechaza una solicitud que aún no ha cotizado, explicando el motivo. */
    suspend fun rechazarSolicitud(id: String, motivo: String)

    /** El comprador cancela una solicitud que aún no tiene cotización. */
    suspend fun cancelarSolicitud(id: String)

    /** El comprador rechaza la cotización recibida. */
    suspend fun rechazarCotizacion(id: String)

    /** El comprador pagó la cotización: guarda la referencia de la pasarela y pasa a "pagado". */
    suspend fun registrarPago(id: String, refPago: String, metodoPago: String)

    /** El mecánico avanza el trabajo pagado a "en proceso" o "terminado". */
    suspend fun avanzarEstado(id: String, estado: EstadoServicio)
}
