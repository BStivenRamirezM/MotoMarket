package com.motomarket.app.pagos

import androidx.annotation.StringRes
import com.motomarket.app.R
import java.io.Serializable

/**
 * Contrato común de cualquier proveedor de pagos (simulado hoy, Mercado Pago en el futuro).
 * La implementación muestra su propia pantalla de cobro y suspende hasta conocer el resultado;
 * quien la llama solo decide qué hacer con una transacción aprobada, rechazada o cancelada.
 */
interface PasarelaPagos {

    /** Cobra la [orden] y devuelve el resultado; nunca lanza por una cancelación del usuario. */
    suspend fun procesarPago(orden: OrdenPago): ResultadoPago
}

/** Medios de pago que se ofrecen en la pantalla "Pago seguro". */
enum class MetodoPago(val codigo: String, @param:StringRes val etiqueta: Int) {
    TARJETA("tarjeta", R.string.pago_tarjeta),
    PSE("pse", R.string.pago_pse)
}

/** Línea del resumen que se muestra al confirmar el pago. */
data class LineaPago(
    val descripcion: String,
    val cantidad: Int,
    val subtotal: Int
) : Serializable

/**
 * Lo que se va a cobrar. [referenciaInterna] identifica el intento de pago en la app
 * (factura) y es independiente de la referencia que asigne el proveedor.
 */
data class OrdenPago(
    val referenciaInterna: String,
    val lineas: List<LineaPago>,
    val subtotal: Int,
    val envio: Int,
    val total: Int,
    val metodo: MetodoPago,
    val compradorNombre: String,
    val compradorCorreo: String,
    val direccionEntrega: String
) : Serializable

/** Estado final de una transacción, común a todos los proveedores. */
enum class EstadoTransaccion { APROBADA, PENDIENTE, RECHAZADA, CANCELADA }

/** Resultado del cobro; [referencia] es la que asigna el proveedor y se guarda como refPago de la orden. */
data class ResultadoPago(
    val estado: EstadoTransaccion,
    val referencia: String = "",
    val mensaje: String = ""
) {
    val aprobado: Boolean get() = estado == EstadoTransaccion.APROBADA
}
