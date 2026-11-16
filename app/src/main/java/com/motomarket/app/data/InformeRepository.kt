package com.motomarket.app.data

import com.motomarket.app.models.Pedido
import com.motomarket.app.models.ServicioTecnico
import com.motomarket.app.models.Usuario

/**
 * Datos crudos para los informes en un rango de fechas. El alcance depende del rol del usuario actual:
 * el administrador ve todo; el vendedor solo las órdenes que contienen sus productos.
 */
interface InformeRepository {

    /** Órdenes creadas entre [desde] y [hasta] (milisegundos, ambos incluidos) visibles para el usuario. */
    suspend fun obtenerPedidos(desde: Long, hasta: Long): List<Pedido>

    /** Servicios técnicos solicitados en el rango (solo administrador). */
    suspend fun obtenerServicios(desde: Long, hasta: Long): List<ServicioTecnico>

    /** Usuarios registrados en el rango (solo administrador). */
    suspend fun obtenerUsuariosRegistrados(desde: Long, hasta: Long): List<Usuario>
}
