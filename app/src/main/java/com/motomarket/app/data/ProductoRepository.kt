package com.motomarket.app.data

import com.motomarket.app.models.Categoria
import com.motomarket.app.models.EstadoProducto
import com.motomarket.app.models.Producto

/** Catálogo, publicaciones de los vendedores y moderación del administrador. */
interface ProductoRepository {

    /** Productos aprobados, opcionalmente filtrados por categoría y por texto en nombre o compatibilidad. */
    suspend fun buscarCatalogo(texto: String, categoria: Categoria?): List<Producto>

    /** Producto por id, o null si no existe o no es visible para el usuario. */
    suspend fun obtenerPorId(id: String): Producto?

    /** Todas las publicaciones de un vendedor, sin importar su estado. */
    suspend fun obtenerProductosVendedor(vendedorId: String): List<Producto>

    /**
     * Crea (id vacío) o actualiza un producto del vendedor actual. Si llega [imagen] la sube a Storage.
     * Toda publicación nueva o editada queda pendiente de aprobación.
     */
    suspend fun guardarProducto(producto: Producto, imagen: ByteArray?): Producto

    /** Elimina el producto y su imagen. */
    suspend fun eliminarProducto(producto: Producto)

    /** Publicaciones esperando moderación. */
    suspend fun obtenerPendientes(): List<Producto>

    /** Últimas publicaciones moderadas (aprobadas, rechazadas o agotadas). */
    suspend fun obtenerHistorialModeracion(): List<Producto>

    /** Aprueba o rechaza una publicación registrando la fecha y el motivo. */
    suspend fun moderar(id: String, estado: EstadoProducto, motivo: String)

    /** Publica los productos de [DatosEjemplo] a nombre del usuario actual; devuelve cuántos creó. */
    suspend fun cargarDatosEjemplo(): Int
}
