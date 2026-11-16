package com.motomarket.app.data

/** La cuenta existe pero el administrador la desactivó; se cierra la sesión. */
class CuentaDesactivadaException : Exception("Cuenta desactivada")

/** El usuario autenticado no tiene documento en la colección "usuarios". */
class PerfilNoEncontradoException : Exception("Perfil no encontrado")

/** Se intenta operar sin una sesión de Firebase activa. */
class SesionRequeridaException : Exception("Sesión requerida")

/** No hay unidades suficientes de [producto]; [disponible] es el stock actual. */
class StockInsuficienteException(val producto: String, val disponible: Int) :
    Exception("Stock insuficiente de $producto")

/** La imagen elegida no se pudo leer o comprimir. */
class ImagenNoLegibleException : Exception("Imagen no legible")

/** Ni con la compresión más agresiva la imagen cabe en el documento de Firestore (límite de 1 MB). */
class ImagenMuyGrandeException : Exception("Imagen demasiado grande")
