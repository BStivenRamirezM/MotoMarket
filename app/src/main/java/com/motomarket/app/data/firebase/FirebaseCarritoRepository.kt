package com.motomarket.app.data.firebase

import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import com.motomarket.app.data.CarritoRepository
import com.motomarket.app.data.SesionRequeridaException
import com.motomarket.app.data.StockInsuficienteException
import com.motomarket.app.data.UsuarioRepository
import com.motomarket.app.models.ItemCarrito
import com.motomarket.app.models.Producto
import com.motomarket.app.models.ResumenCarrito
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.tasks.await

/**
 * Carrito en "carritos/{uid}/items/{productoId}" con {cantidad, agregado}. Los datos del producto
 * se leen de nuevo en cada consulta para mostrar siempre precio y stock actuales.
 */
class FirebaseCarritoRepository(
    private val usuarioRepository: UsuarioRepository
) : CarritoRepository {

    private val db = Firebase.firestore

    private fun items() = db.collection(Colecciones.CARRITOS)
        .document(usuarioRepository.uidActual() ?: throw SesionRequeridaException())
        .collection(Colecciones.ITEMS)

    override suspend fun obtenerResumen(): ResumenCarrito = coroutineScope {
        val documentos = items().get().await().documents
        val itemsCarrito = documentos.map { documento ->
            async {
                // Un producto borrado o retirado de la venta deja de ser legible y se omite.
                val producto = runCatching {
                    db.collection(Colecciones.PRODUCTOS).document(documento.id).get().await()
                }.getOrNull()?.takeIf { it.exists() }?.aProducto()
                producto?.let {
                    ItemCarrito(it, documento.getLong("cantidad")?.toInt() ?: 1, documento.getLong("agregado") ?: 0L)
                }
            }
        }.awaitAll().filterNotNull().sortedBy { it.agregado }
        ResumenCarrito(itemsCarrito, COSTO_ENVIO)
    }

    override suspend fun agregar(producto: Producto) {
        val referencia = items().document(producto.id)
        val actual = referencia.get().await()
        val cantidad = (actual.getLong("cantidad")?.toInt() ?: 0) + 1
        val stock = db.collection(Colecciones.PRODUCTOS).document(producto.id).get().await()
            .getLong("stock")?.toInt() ?: 0
        if (cantidad > stock) throw StockInsuficienteException(producto.nombre, stock)
        referencia.set(
            mapOf(
                "productoId" to producto.id,
                "cantidad" to cantidad,
                "agregado" to (actual.getLong("agregado") ?: System.currentTimeMillis())
            )
        ).await()
    }

    override suspend fun eliminar(productoId: String) {
        items().document(productoId).delete().await()
    }

    override suspend fun restaurar(item: ItemCarrito) {
        items().document(item.producto.id).set(
            mapOf("productoId" to item.producto.id, "cantidad" to item.cantidad, "agregado" to item.agregado)
        ).await()
    }

    override suspend fun vaciar() {
        val documentos = items().get().await().documents
        if (documentos.isEmpty()) return
        val lote = db.batch()
        documentos.forEach { lote.delete(it.reference) }
        lote.commit().await()
    }

    private companion object {
        const val COSTO_ENVIO = 9_000
    }
}
