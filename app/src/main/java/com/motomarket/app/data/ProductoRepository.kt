package com.motomarket.app.data

import com.motomarket.app.models.Categoria
import com.motomarket.app.models.EstadoProducto
import com.motomarket.app.models.Moderacion
import com.motomarket.app.models.Producto

interface ProductoRepository {
    suspend fun obtenerCatalogo(): List<Producto>
    suspend fun buscar(texto: String, categoria: Categoria?): List<Producto>
    suspend fun obtenerPorId(id: String): Producto?
    suspend fun obtenerProductosVendedor(vendedor: String): List<Producto>
    suspend fun publicarProducto(producto: Producto)
    suspend fun obtenerModeraciones(): List<Moderacion>
    suspend fun actualizarModeracion(id: String, estado: EstadoProducto)
}
