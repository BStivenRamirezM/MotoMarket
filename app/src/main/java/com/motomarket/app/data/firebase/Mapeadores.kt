package com.motomarket.app.data.firebase

import com.google.firebase.firestore.DocumentSnapshot
import com.motomarket.app.models.Categoria
import com.motomarket.app.models.EstadoPedido
import com.motomarket.app.models.EstadoProducto
import com.motomarket.app.models.ItemPedido
import com.motomarket.app.models.Pedido
import com.motomarket.app.models.Producto
import com.motomarket.app.models.Rol
import com.motomarket.app.models.Usuario

/** Nombres de las colecciones de Firestore usadas por la app. */
internal object Colecciones {
    const val USUARIOS = "usuarios"
    const val PRODUCTOS = "productos"
    const val PEDIDOS = "pedidos"
    const val CARRITOS = "carritos"
    const val ITEMS = "items"
}

/** Los enums se guardan en minúscula; si llega un valor desconocido se usa [defecto]. */
internal inline fun <reified T : Enum<T>> enumDe(valor: String?, defecto: T): T =
    enumValues<T>().firstOrNull { it.name.equals(valor, ignoreCase = true) } ?: defecto

internal fun Enum<*>.aTexto(): String = name.lowercase()

/** Convierte un producto en el mapa que se guarda en "productos/{id}". */
internal fun Producto.aMapa(): Map<String, Any> = mapOf(
    "id" to id,
    "nombre" to nombre,
    "nombreBusqueda" to nombre.lowercase(),
    "precio" to precio,
    "categoria" to categoria.aTexto(),
    "marcaModeloCompatible" to marcaModeloCompatible,
    "cilindraje" to cilindraje,
    "stock" to stock,
    "vendedorId" to vendedorId,
    "vendedorNombre" to vendedorNombre,
    "calificacionVendedor" to calificacionVendedor,
    "estado" to estado.aTexto(),
    "imagenUrl" to imagenUrl,
    "fechaCreacion" to fechaCreacion,
    "fechaRevision" to fechaRevision,
    "motivoRechazo" to motivoRechazo
)

/** Reconstruye un producto desde su documento. */
internal fun DocumentSnapshot.aProducto(): Producto = Producto(
    id = id,
    nombre = getString("nombre").orEmpty(),
    precio = getLong("precio")?.toInt() ?: 0,
    categoria = enumDe(getString("categoria"), Categoria.ACCESORIOS),
    marcaModeloCompatible = getString("marcaModeloCompatible").orEmpty(),
    cilindraje = getString("cilindraje").orEmpty(),
    stock = getLong("stock")?.toInt() ?: 0,
    vendedorId = getString("vendedorId").orEmpty(),
    vendedorNombre = getString("vendedorNombre").orEmpty(),
    calificacionVendedor = getDouble("calificacionVendedor") ?: 0.0,
    estado = enumDe(getString("estado"), EstadoProducto.PENDIENTE),
    imagenUrl = getString("imagenUrl").orEmpty(),
    fechaCreacion = getLong("fechaCreacion") ?: 0L,
    fechaRevision = getLong("fechaRevision") ?: 0L,
    motivoRechazo = getString("motivoRechazo").orEmpty()
)

/** Convierte un perfil en el mapa de "usuarios/{uid}". */
internal fun Usuario.aMapa(): Map<String, Any> = mapOf(
    "uid" to id,
    "nombre" to nombre,
    "correo" to correo,
    "telefono" to telefono,
    "rol" to rol.aTexto(),
    "activo" to activo,
    "fechaRegistro" to fechaRegistro,
    "calificacion" to calificacion,
    "direcciones" to direcciones
)

/** Reconstruye un perfil desde su documento. */
internal fun DocumentSnapshot.aUsuario(): Usuario = Usuario(
    id = id,
    nombre = getString("nombre").orEmpty(),
    correo = getString("correo").orEmpty(),
    telefono = getString("telefono").orEmpty(),
    rol = enumDe(getString("rol"), Rol.COMPRADOR),
    activo = getBoolean("activo") ?: false,
    fechaRegistro = getLong("fechaRegistro") ?: 0L,
    calificacion = getDouble("calificacion") ?: 0.0,
    direcciones = (get("direcciones") as? List<*>)?.filterIsInstance<String>().orEmpty()
)

/** Convierte una orden en el mapa de "pedidos/{id}". */
internal fun Pedido.aMapa(): Map<String, Any?> = mapOf(
    "id" to id,
    "compradorId" to compradorId,
    "compradorNombre" to compradorNombre,
    "items" to items.map {
        mapOf(
            "productoId" to it.productoId,
            "nombre" to it.nombre,
            "precio" to it.precio,
            "cantidad" to it.cantidad,
            "vendedorId" to it.vendedorId
        )
    },
    "vendedorIds" to vendedorIds,
    "subtotal" to subtotal,
    "envio" to envio,
    "total" to total,
    "direccionEntrega" to direccionEntrega,
    "lat" to lat,
    "lng" to lng,
    "metodoPago" to metodoPago,
    "estado" to estado.aTexto(),
    "refPago" to refPago,
    "fecha" to fecha
)

/** Reconstruye una orden desde su documento. */
internal fun DocumentSnapshot.aPedido(): Pedido = Pedido(
    id = id,
    compradorId = getString("compradorId").orEmpty(),
    compradorNombre = getString("compradorNombre").orEmpty(),
    items = (get("items") as? List<*>)?.mapNotNull { elemento ->
        (elemento as? Map<*, *>)?.let { mapa ->
            ItemPedido(
                productoId = mapa["productoId"] as? String ?: "",
                nombre = mapa["nombre"] as? String ?: "",
                precio = (mapa["precio"] as? Number)?.toInt() ?: 0,
                cantidad = (mapa["cantidad"] as? Number)?.toInt() ?: 0,
                vendedorId = mapa["vendedorId"] as? String ?: ""
            )
        }
    }.orEmpty(),
    vendedorIds = (get("vendedorIds") as? List<*>)?.filterIsInstance<String>().orEmpty(),
    subtotal = getLong("subtotal")?.toInt() ?: 0,
    envio = getLong("envio")?.toInt() ?: 0,
    total = getLong("total")?.toInt() ?: 0,
    direccionEntrega = getString("direccionEntrega").orEmpty(),
    lat = getDouble("lat"),
    lng = getDouble("lng"),
    metodoPago = getString("metodoPago").orEmpty(),
    estado = enumDe(getString("estado"), EstadoPedido.PENDIENTE),
    refPago = getString("refPago").orEmpty(),
    fecha = getLong("fecha") ?: 0L
)
