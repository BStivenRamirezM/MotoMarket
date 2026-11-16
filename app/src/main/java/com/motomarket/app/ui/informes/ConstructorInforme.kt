package com.motomarket.app.ui.informes

import android.content.Context
import com.motomarket.app.R
import com.motomarket.app.data.InformeRepository
import com.motomarket.app.models.EstadoPedido
import com.motomarket.app.models.EstadoServicio
import com.motomarket.app.models.Pedido
import com.motomarket.app.models.Rol
import com.motomarket.app.ui.common.etiqueta
import com.motomarket.app.util.formatearCOP
import com.motomarket.app.util.formatearFecha
import com.motomarket.app.util.formatearMiles

/**
 * Convierte los datos del [InformeRepository] en un [Informe] según el tipo elegido. Si [vendedorId]
 * no es null, los valores de cada orden se limitan a los productos de ese vendedor.
 */
class ConstructorInforme(
    private val context: Context,
    private val repositorio: InformeRepository,
    private val vendedorId: String?
) {

    /** Consulta los datos del rango y arma el informe del [tipo] indicado. */
    suspend fun construir(tipo: TipoInforme, desde: Long, hasta: Long): Informe {
        val rango = context.getString(R.string.informe_rango, formatearFecha(desde), formatearFecha(hasta))
        val titulo = context.getString(tipo.titulo)
        return when (tipo) {
            TipoInforme.VENTAS -> ventas(titulo, rango, pagados(repositorio.obtenerPedidos(desde, hasta)))
            TipoInforme.MAS_VENDIDOS -> masVendidos(titulo, rango, pagados(repositorio.obtenerPedidos(desde, hasta)))
            TipoInforme.ESTADO_PEDIDOS -> estadoPedidos(titulo, rango, repositorio.obtenerPedidos(desde, hasta))
            TipoInforme.SERVICIOS -> servicios(titulo, rango, desde, hasta)
            TipoInforme.USUARIOS -> usuarios(titulo, rango, desde, hasta)
        }
    }

    /** Solo cuentan como venta las órdenes que ya se pagaron. */
    private fun pagados(pedidos: List<Pedido>) = pedidos.filter { it.estado != EstadoPedido.PENDIENTE }

    /** Valor de la orden para quien consulta: total para el admin, sus ítems para el vendedor. */
    private fun valor(pedido: Pedido): Int =
        vendedorId?.let { id -> pedido.itemsDeVendedor(id).sumOf { it.subtotal } } ?: pedido.total

    /** Ítems de la orden que puede ver quien consulta (todos para el admin). */
    private fun itemsVisibles(pedido: Pedido) = vendedorId?.let { pedido.itemsDeVendedor(it) } ?: pedido.items

    /** Ventas pagadas: total, número de órdenes, ticket promedio y una fila por orden. */
    private fun ventas(titulo: String, rango: String, pedidos: List<Pedido>): Informe {
        val total = pedidos.sumOf { valor(it) }
        val promedio = if (pedidos.isEmpty()) 0 else total / pedidos.size
        return Informe(
            titulo, rango,
            listOf(
                Indicador(formatearCOP(total), texto(R.string.informe_ind_total_vendido)),
                Indicador(formatearMiles(pedidos.size.toLong()), texto(R.string.informe_ind_ordenes)),
                Indicador(formatearCOP(promedio), texto(R.string.informe_ind_ticket))
            ),
            columnas(R.string.informe_col_fecha, R.string.informe_col_orden, R.string.informe_col_comprador, R.string.informe_col_productos, R.string.informe_col_valor, R.string.informe_col_estado),
            pedidos.map { pedido ->
                listOf(
                    formatearFecha(pedido.fecha),
                    pedido.numero,
                    pedido.compradorNombre,
                    itemsVisibles(pedido).joinToString { context.getString(R.string.pedidos_item, it.nombre, it.cantidad) },
                    formatearCOP(valor(pedido)),
                    texto(pedido.estado.etiqueta())
                )
            }
        )
    }

    /** Ranking de productos por unidades vendidas en órdenes pagadas, con sus ingresos. */
    private fun masVendidos(titulo: String, rango: String, pedidos: List<Pedido>): Informe {
        val ranking = pedidos.flatMap { itemsVisibles(it) }
            .groupBy { it.nombre }
            .map { (nombre, items) -> Triple(nombre, items.sumOf { it.cantidad }, items.sumOf { it.subtotal }) }
            .sortedByDescending { it.second }
        return Informe(
            titulo, rango,
            listOf(
                Indicador(formatearMiles(ranking.size.toLong()), texto(R.string.informe_ind_productos_distintos)),
                Indicador(formatearMiles(ranking.sumOf { it.second }.toLong()), texto(R.string.informe_ind_unidades)),
                Indicador(ranking.firstOrNull()?.first ?: texto(R.string.sin_dato), texto(R.string.informe_ind_top))
            ),
            columnas(R.string.informe_col_posicion, R.string.informe_col_producto, R.string.informe_col_unidades, R.string.informe_col_ingresos),
            ranking.mapIndexed { indice, (nombre, unidades, ingresos) ->
                listOf((indice + 1).toString(), nombre, formatearMiles(unidades.toLong()), formatearCOP(ingresos))
            }
        )
    }

    /** Cantidad y valor de órdenes por estado (sin pagar, por enviar, enviado, entregado). */
    private fun estadoPedidos(titulo: String, rango: String, pedidos: List<Pedido>): Informe {
        val porEstado = EstadoPedido.entries.map { estado ->
            val grupo = pedidos.filter { it.estado == estado }
            Triple(estado, grupo.size, grupo.sumOf { valor(it) })
        }
        val entregados = porEstado.first { it.first == EstadoPedido.ENTREGADO }.second
        val pendientesEnvio = porEstado.first { it.first == EstadoPedido.PAGADO }.second
        return Informe(
            titulo, rango,
            listOf(
                Indicador(formatearMiles(pedidos.size.toLong()), texto(R.string.informe_ind_ordenes)),
                Indicador(formatearMiles(pendientesEnvio.toLong()), texto(R.string.informe_ind_por_enviar)),
                Indicador(formatearMiles(entregados.toLong()), texto(R.string.informe_ind_entregados))
            ),
            columnas(R.string.informe_col_estado, R.string.informe_col_ordenes, R.string.informe_col_valor),
            porEstado.map { (estado, cantidad, total) ->
                listOf(texto(estado.etiqueta()), formatearMiles(cantidad.toLong()), formatearCOP(total))
            }
        )
    }

    /** Servicios técnicos del rango: ingresos pagados, solicitudes y terminados, con una fila por servicio. */
    private suspend fun servicios(titulo: String, rango: String, desde: Long, hasta: Long): Informe {
        val servicios = repositorio.obtenerServicios(desde, hasta)
        val pagados = servicios.filter { it.estado in setOf(EstadoServicio.PAGADO, EstadoServicio.EN_PROCESO, EstadoServicio.TERMINADO) }
        return Informe(
            titulo, rango,
            listOf(
                Indicador(formatearCOP(pagados.sumOf { it.total }), texto(R.string.informe_ind_ingresos_servicios)),
                Indicador(formatearMiles(servicios.size.toLong()), texto(R.string.informe_ind_solicitudes)),
                Indicador(formatearMiles(servicios.count { it.estado == EstadoServicio.TERMINADO }.toLong()), texto(R.string.informe_ind_terminados))
            ),
            columnas(R.string.informe_col_fecha, R.string.informe_col_servicio, R.string.informe_col_comprador, R.string.informe_col_taller, R.string.informe_col_valor, R.string.informe_col_estado),
            servicios.map {
                listOf(
                    formatearFecha(it.fechaSolicitud),
                    it.numero,
                    it.compradorNombre,
                    it.mecanicoNombre,
                    if (it.lineas.isEmpty()) texto(R.string.servicio_por_cotizar) else formatearCOP(it.total),
                    texto(it.estado.etiqueta())
                )
            }
        )
    }

    /** Usuarios registrados en el rango con su rol y estado; resumen por compradores y vendedores/mecánicos. */
    private suspend fun usuarios(titulo: String, rango: String, desde: Long, hasta: Long): Informe {
        val usuarios = repositorio.obtenerUsuariosRegistrados(desde, hasta)
        val comerciantes = usuarios.count { it.rol == Rol.VENDEDOR || it.rol == Rol.MECANICO }
        return Informe(
            titulo, rango,
            listOf(
                Indicador(formatearMiles(usuarios.size.toLong()), texto(R.string.informe_ind_registrados)),
                Indicador(formatearMiles(usuarios.count { it.activo }.toLong()), texto(R.string.informe_ind_activos)),
                Indicador(formatearMiles(comerciantes.toLong()), texto(R.string.informe_ind_comerciantes))
            ),
            columnas(R.string.informe_col_fecha, R.string.informe_col_nombre, R.string.informe_col_correo, R.string.informe_col_rol, R.string.informe_col_estado),
            usuarios.map {
                listOf(
                    formatearFecha(it.fechaRegistro),
                    it.nombre,
                    it.correo,
                    texto(it.rol.etiqueta()),
                    texto(if (it.activo) R.string.usuarios_activo else R.string.usuarios_inactivo)
                )
            }
        )
    }

    /** Texto del recurso indicado. */
    private fun texto(id: Int) = context.getString(id)

    /** Encabezados de la tabla a partir de recursos de texto. */
    private fun columnas(vararg ids: Int) = ids.map { texto(it) }
}
