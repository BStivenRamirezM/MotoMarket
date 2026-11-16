package com.motomarket.app.data.firebase

import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import android.util.Base64
import com.motomarket.app.data.DatosEjemplo
import com.motomarket.app.data.ImagenMuyGrandeException
import com.motomarket.app.data.ProductoRepository
import com.motomarket.app.data.SesionRequeridaException
import com.motomarket.app.data.UsuarioRepository
import com.motomarket.app.models.Categoria
import com.motomarket.app.models.EstadoProducto
import com.motomarket.app.models.Producto
import kotlinx.coroutines.tasks.await

/**
 * Productos en Firestore ("productos"). La foto se guarda en Base64 dentro del propio documento
 * (campo "imagenBase64"), ya comprimida a ~200 KB para respetar el límite de 1 MB por documento.
 * Migrar a Firebase Storage solo requeriría cambiar [guardarProducto] y [eliminarProducto] aquí.
 * La búsqueda por texto se hace en el dispositivo porque Firestore no soporta "contiene".
 */
class FirebaseProductoRepository(
    private val usuarioRepository: UsuarioRepository
) : ProductoRepository {

    private val productos = Firebase.firestore.collection(Colecciones.PRODUCTOS)

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

    /** Codifica la imagen nueva en Base64 (si hay), fija los datos del vendedor y deja el producto pendiente. */
    override suspend fun guardarProducto(producto: Producto, imagen: ByteArray?): Producto {
        val vendedor = usuarioRepository.obtenerUsuarioActual()
        val referencia = if (producto.id.isBlank()) productos.document() else productos.document(producto.id)
        val imagenBase64 = imagen?.let { aBase64(it) } ?: producto.imagenBase64
        val guardado = producto.copy(
            id = referencia.id,
            vendedorId = vendedor.id,
            vendedorNombre = vendedor.nombrePublico,
            calificacionVendedor = vendedor.calificacion,
            estado = EstadoProducto.PENDIENTE,
            imagenBase64 = imagenBase64,
            fechaCreacion = producto.fechaCreacion.takeIf { it > 0 } ?: System.currentTimeMillis(),
            fechaRevision = 0L,
            motivoRechazo = ""
        )
        referencia.set(guardado.aMapa()).await()
        return guardado
    }

    /** Borra el documento; la imagen va dentro de él, así que no queda nada más que limpiar. */
    override suspend fun eliminarProducto(producto: Producto) {
        productos.document(producto.id).delete().await()
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

    /**
     * Codifica la imagen comprimida en Base64 y comprueba que, junto al resto de campos, el documento
     * no se acerque al límite de 1 MB (1.048.576 bytes) de Firestore.
     */
    private fun aBase64(imagen: ByteArray): String {
        val base64 = Base64.encodeToString(imagen, Base64.NO_WRAP)
        if (base64.length > MAXIMO_BASE64) throw ImagenMuyGrandeException()
        return base64
    }

    private companion object {
        const val LIMITE_HISTORIAL = 30

        /** Deja ~150 KB libres para el resto de campos del documento. */
        const val MAXIMO_BASE64 = 900_000
    }
}
