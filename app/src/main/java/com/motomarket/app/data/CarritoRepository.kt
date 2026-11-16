package com.motomarket.app.data

import com.motomarket.app.models.ItemCarrito
import com.motomarket.app.models.Producto
import com.motomarket.app.models.ResumenCarrito

/** Carrito persistente del comprador autenticado. */
interface CarritoRepository {

    /** Ítems con los datos actuales de cada producto y los totales calculados. */
    suspend fun obtenerResumen(): ResumenCarrito

    /** Suma una unidad del producto validando que no supere el stock disponible. */
    suspend fun agregar(producto: Producto)

    /** Quita el producto del carrito. */
    suspend fun eliminar(productoId: String)

    /** Vuelve a poner un ítem eliminado (acción "Deshacer"). */
    suspend fun restaurar(item: ItemCarrito)

    /** Elimina todos los ítems. */
    suspend fun vaciar()
}
