package com.motomarket.app.data.firebase

import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import com.google.firebase.storage.StorageMetadata
import com.google.firebase.storage.storage
import com.motomarket.app.data.DatosEjemplo
import com.motomarket.app.data.ProductoRepository
import com.motomarket.app.data.SesionRequeridaException
import com.motomarket.app.data.UsuarioRepository
import com.motomarket.app.models.Categoria
import com.motomarket.app.models.EstadoProducto
import com.motomarket.app.models.Producto
import kotlinx.coroutines.tasks.await

/**
 * Productos en Firestore ("productos") con sus imágenes en Storage (productos/{uid}/{timestamp}.jpg).
 * La búsqueda por texto se hace en el dispositivo porque Firestore no soporta "contiene".
 */
class FirebaseProductoRepository(
    private val usuarioRepository: UsuarioRepository
) : ProductoRepository {

    private val productos = Firebase.firestore.collection(Colecciones.PRODUCTOS)
    private val storage = Firebase.storage.reference

    /** Consulta aprobados (y categoría si aplica) y filtra por texto en el dispositivo. */
    override suspend fun buscarCatalogo(texto: String, categoria: Categoria?): List<Producto> {
        var consulta = productos.whereEqualTo("estado", EstadoProducto.APROBADO.aTexto())
        if (categoria != null) consulta = consulta.whereEqualTo("categoria", categoria.aTexto())
        return consulta.get().await().documents
            .map { it.aProducto() }
            .filter {
                texto.isBlank() ||
                    it.nombre.contains(texto.trim(), ignoreCase = true) ||
                    it.marcaModeloCompatible.contains(texto.trim(), ignoreCase = true)
            }
            .sortedByDescending { it.fechaCreacion }
    }

    /** Lee un producto por id; null si el documento no existe. */
    override suspend fun obtenerPorId(id: String): Producto? {
        val documento = productos.document(id).get().await()
        return if (documento.exists()) documento.aProducto() else null
    }

    /** Todas las publicaciones con vendedorId igual al indicado. */
    override suspend fun obtenerProductosVendedor(vendedorId: String): List<Producto> =
        productos.whereEqualTo("vendedorId", vendedorId).get().await().documents
            .map { it.aProducto() }
            .sortedByDescending { it.fechaCreacion }

    /** Sube la imagen si hay una nueva, fija los datos del vendedor y deja el producto pendiente. */
    override suspend fun guardarProducto(producto: Producto, imagen: ByteArray?): Producto {
        val vendedor = usuarioRepository.obtenerUsuarioActual()
        val referencia = if (producto.id.isBlank()) productos.document() else productos.document(producto.id)
        val imagenUrl = if (imagen != null) subirImagen(vendedor.id, imagen) else producto.imagenUrl
        val guardado = producto.copy(
            id = referencia.id,
            vendedorId = vendedor.id,
            vendedorNombre = vendedor.nombreVendedor,
            calificacionVendedor = vendedor.calificacion,
            estado = EstadoProducto.PENDIENTE,
            imagenUrl = imagenUrl,
            fechaCreacion = producto.fechaCreacion.takeIf { it > 0 } ?: System.currentTimeMillis(),
            fechaRevision = 0L,
            motivoRechazo = ""
        )
        referencia.set(guardado.aMapa()).await()
        if (imagen != null && producto.imagenUrl.isNotBlank()) borrarImagen(producto.imagenUrl)
        return guardado
    }

    /** Borra el documento y después su imagen en Storage. */
    override suspend fun eliminarProducto(producto: Producto) {
        productos.document(producto.id).delete().await()
        if (producto.imagenUrl.isNotBlank()) borrarImagen(producto.imagenUrl)
    }

    /** Publicaciones en estado pendiente, de la más antigua a la más reciente. */
    override suspend fun obtenerPendientes(): List<Producto> =
        productos.whereEqualTo("estado", EstadoProducto.PENDIENTE.aTexto()).get().await().documents
            .map { it.aProducto() }
            .sortedBy { it.fechaCreacion }

    /** Productos con fecha de revisión, del más reciente al más antiguo. */
    override suspend fun obtenerHistorialModeracion(): List<Producto> =
        productos.whereGreaterThan("fechaRevision", 0L).get().await().documents
            .map { it.aProducto() }
            .sortedByDescending { it.fechaRevision }
            .take(LIMITE_HISTORIAL)

    /** Guarda el nuevo estado, la fecha de revisión y el motivo de rechazo. */
    override suspend fun moderar(id: String, estado: EstadoProducto, motivo: String) {
        productos.document(id).update(
            mapOf(
                "estado" to estado.aTexto(),
                "fechaRevision" to System.currentTimeMillis(),
                "motivoRechazo" to motivo
            )
        ).await()
    }

    /** Escribe los productos de DatosEjemplo en un lote a nombre del usuario actual. */
    override suspend fun cargarDatosEjemplo(): Int {
        val uid = usuarioRepository.uidActual() ?: throw SesionRequeridaException()
        val ahora = System.currentTimeMillis()
        val lote = Firebase.firestore.batch()
        DatosEjemplo.productos.forEachIndexed { indice, ejemplo ->
            val referencia = productos.document()
            val producto = ejemplo.copy(
                id = referencia.id,
                vendedorId = uid,
                fechaCreacion = ahora + indice,
                fechaRevision = if (ejemplo.estado == EstadoProducto.PENDIENTE) 0L else ahora
            )
            lote.set(referencia, producto.aMapa())
        }
        lote.commit().await()
        return DatosEjemplo.productos.size
    }

    /** Sube la imagen ya comprimida y devuelve su URL pública de descarga. */
    private suspend fun subirImagen(uid: String, imagen: ByteArray): String {
        val referencia = storage.child("productos/$uid/${System.currentTimeMillis()}.jpg")
        val metadatos = StorageMetadata.Builder().setContentType("image/jpeg").build()
        referencia.putBytes(imagen, metadatos).await()
        return referencia.downloadUrl.await().toString()
    }

    /** Borra una imagen anterior; si falla no interrumpe la operación principal. */
    private suspend fun borrarImagen(url: String) {
        runCatching { Firebase.storage.getReferenceFromUrl(url).delete().await() }
    }

    private companion object {
        const val LIMITE_HISTORIAL = 30
    }
}
