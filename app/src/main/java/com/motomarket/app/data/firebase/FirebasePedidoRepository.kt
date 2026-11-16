package com.motomarket.app.data.firebase

import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import com.motomarket.app.data.PedidoRepository
import com.motomarket.app.data.SesionRequeridaException
import com.motomarket.app.data.StockInsuficienteException
import com.motomarket.app.data.UsuarioRepository
import com.motomarket.app.models.EstadoPedido
import com.motomarket.app.models.EstadoProducto
import com.motomarket.app.models.ItemPedido
import com.motomarket.app.models.Pedido
import com.motomarket.app.models.ResumenCarrito
import com.motomarket.app.models.ResumenVentas
import kotlinx.coroutines.tasks.await
import java.util.Calendar

/** Órdenes en la colección "pedidos"; la compra descuenta stock dentro de una transacción. */
class FirebasePedidoRepository(
    private val usuarioRepository: UsuarioRepository
) : PedidoRepository {

    private val db = Firebase.firestore
    private val pedidos = db.collection(Colecciones.PEDIDOS)

    override suspend fun confirmarCompra(
        resumen: ResumenCarrito,
        direccion: String,
        lat: Double?,
        lng: Double?,
        metodoPago: String,
        refPago: String
    ): Pedido {
        val comprador = usuarioRepository.obtenerUsuarioActual()
        val referencia = pedidos.document()
        val items = resumen.items.map {
            ItemPedido(it.producto.id, it.producto.nombre, it.producto.precio, it.cantidad, it.producto.vendedorId)
        }
        val pedido = Pedido(
            id = referencia.id,
            compradorId = comprador.id,
            compradorNombre = comprador.nombre,
            items = items,
            vendedorIds = items.map { it.vendedorId }.distinct(),
            subtotal = resumen.subtotal,
            envio = resumen.envio,
            total = resumen.total,
            direccionEntrega = direccion,
            lat = lat,
            lng = lng,
            metodoPago = metodoPago,
            estado = EstadoPedido.PAGADO,
            refPago = refPago,
            fecha = System.currentTimeMillis()
        )

        db.runTransaction { transaccion ->
            // Firestore exige hacer todas las lecturas antes de la primera escritura.
            val stocks = items.map { item ->
                val documento = transaccion.get(db.collection(Colecciones.PRODUCTOS).document(item.productoId))
                item to (documento.getLong("stock")?.toInt() ?: 0)
            }
            stocks.forEach { (item, stock) ->
                if (stock < item.cantidad) throw StockInsuficienteException(item.nombre, stock)
            }
            stocks.forEach { (item, stock) ->
                val restante = stock - item.cantidad
                val cambios = mutableMapOf<String, Any>("stock" to restante)
                if (restante == 0) cambios["estado"] = EstadoProducto.AGOTADO.aTexto()
                transaccion.update(db.collection(Colecciones.PRODUCTOS).document(item.productoId), cambios)
            }
            transaccion.set(referencia, pedido.aMapa())
        }.await()
        return pedido
    }

    override suspend fun obtenerHistorialComprador(): List<Pedido> {
        val uid = usuarioRepository.uidActual() ?: throw SesionRequeridaException()
        return pedidos.whereEqualTo("compradorId", uid).get().await().documents
            .map { it.aPedido() }
            .sortedByDescending { it.fecha }
    }

    override suspend fun obtenerPedidosVendedor(): List<Pedido> {
        val uid = usuarioRepository.uidActual() ?: throw SesionRequeridaException()
        return pedidos.whereArrayContains("vendedorIds", uid).get().await().documents
            .map { it.aPedido() }
            .sortedByDescending { it.fecha }
    }

    override suspend fun obtenerResumenVentasMes(): ResumenVentas {
        val uid = usuarioRepository.uidActual() ?: throw SesionRequeridaException()
        val inicioMes = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val pagadosDelMes = obtenerPedidosVendedor().filter { it.fecha >= inicioMes && it.estado != EstadoPedido.PENDIENTE }
        return ResumenVentas(
            totalMes = pagadosDelMes.sumOf { pedido -> pedido.itemsDeVendedor(uid).sumOf { it.subtotal } },
            cantidadPedidos = pagadosDelMes.size
        )
    }

    override suspend fun cambiarEstado(id: String, estado: EstadoPedido) {
        pedidos.document(id).update("estado", estado.aTexto()).await()
    }
}
