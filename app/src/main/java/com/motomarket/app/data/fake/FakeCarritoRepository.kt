package com.motomarket.app.data.fake

import com.motomarket.app.data.CarritoRepository
import com.motomarket.app.models.ItemCarrito
import com.motomarket.app.models.Producto
import com.motomarket.app.models.ResumenCarrito

class FakeCarritoRepository : CarritoRepository {

    private val items = DatosEjemplo.carrito.toMutableList()

    override suspend fun obtenerResumen(): ResumenCarrito = ResumenCarrito(items.toList(), DatosEjemplo.envio)

    override suspend fun agregar(producto: Producto) {
        val indice = items.indexOfFirst { it.producto.id == producto.id }
        if (indice >= 0) {
            items[indice] = items[indice].copy(cantidad = items[indice].cantidad + 1)
        } else {
            items.add(ItemCarrito(producto, 1))
        }
    }

    override suspend fun eliminar(productoId: String) {
        items.removeAll { it.producto.id == productoId }
    }

    override suspend fun restaurar(item: ItemCarrito, posicion: Int) {
        items.add(posicion.coerceIn(0, items.size), item)
    }

    override suspend fun vaciar() = items.clear()
}
