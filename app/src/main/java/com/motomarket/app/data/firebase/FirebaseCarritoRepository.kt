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

    /** Subcolección de ítems del carrito del usuario autenticado. */
    private fun items() = db.collection(Colecciones.CARRITOS)
        .document(usuarioRepository.uidActual() ?: throw SesionRequeridaException())
        .collection(Colecciones.ITEMS)

    /** Lee los ítems y, en paralelo, cada producto para tener precio y stock actuales. */
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

    /** Suma una unidad y rechaza la operación si superaría el stock del producto. */
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

    /** Borra el documento del producto en el carrito. */
    override suspend fun eliminar(productoId: String) {
        items().document(productoId).delete().await()
    }

    /** Reescribe el ítem con su cantidad y fecha originales. */
    override suspend fun restaurar(item: ItemCarrito) {
        items().document(item.producto.id).set(
            mapOf("productoId" to item.producto.id, "cantidad" to item.cantidad, "agregado" to item.agregado)
        ).await()
    }

    /** Borra todos los ítems en un solo lote de escritura. */
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
