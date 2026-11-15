package com.motomarket.app.data.fake

import com.motomarket.app.data.ProductoRepository
import com.motomarket.app.models.Categoria
import com.motomarket.app.models.EstadoProducto
import com.motomarket.app.models.Moderacion
import com.motomarket.app.models.Producto
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FakeProductoRepository : ProductoRepository {

    private val productos = DatosEjemplo.productos.toMutableList()
    private val moderaciones = DatosEjemplo.moderaciones.toMutableList()
    private val estadosVisibles = setOf(EstadoProducto.ACTIVO, EstadoProducto.AGOTADO)

    override suspend fun obtenerCatalogo(): List<Producto> =
        productos.filter { it.estado in estadosVisibles }

    override suspend fun buscar(texto: String, categoria: Categoria?): List<Producto> =
        obtenerCatalogo().filter { producto ->
            (categoria == null || producto.categoria == categoria) &&
                (texto.isBlank() ||
                    producto.nombre.contains(texto, ignoreCase = true) ||
                    producto.marcaModeloCompatible.contains(texto, ignoreCase = true))
        }

    override suspend fun obtenerPorId(id: String): Producto? = productos.find { it.id == id }

    override suspend fun obtenerProductosVendedor(vendedor: String): List<Producto> =
        productos.filter { it.vendedor == vendedor }

    override suspend fun publicarProducto(producto: Producto) {
        productos.add(producto)
        moderaciones.add(0, Moderacion("m${moderaciones.size + 1}", producto.nombre, producto.vendedor, EstadoProducto.PENDIENTE))
    }

    override suspend fun obtenerModeraciones(): List<Moderacion> = moderaciones.toList()

    override suspend fun actualizarModeracion(id: String, estado: EstadoProducto) {
        val indice = moderaciones.indexOfFirst { it.id == id }
        if (indice < 0) return
        val hoy = SimpleDateFormat("dd/MM", Locale.getDefault()).format(Date())
        moderaciones[indice] = moderaciones[indice].copy(
            estado = estado,
            fecha = hoy,
            motivo = if (estado == EstadoProducto.RECHAZADO) "revisión del administrador" else ""
        )
    }
}
