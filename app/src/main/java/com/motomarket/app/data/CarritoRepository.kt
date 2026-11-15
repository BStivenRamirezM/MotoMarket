package com.motomarket.app.data

import com.motomarket.app.models.ItemCarrito
import com.motomarket.app.models.Producto
import com.motomarket.app.models.ResumenCarrito

interface CarritoRepository {
    suspend fun obtenerResumen(): ResumenCarrito
    suspend fun agregar(producto: Producto)
    suspend fun eliminar(productoId: String)
    suspend fun restaurar(item: ItemCarrito, posicion: Int)
    suspend fun vaciar()
}
