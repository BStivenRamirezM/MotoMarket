package com.motomarket.app.pagos

import androidx.activity.ComponentActivity
import com.motomarket.app.pagos.simulada.PasarelaSimulada

/**
 * Único punto donde se elige el proveedor de pagos. Para integrar Mercado Pago basta con
 * devolver `MercadoPagoPasarela(activity)` aquí (ver README, sección "Pasarela de pagos").
 */
object Pasarelas {

    /** Pasarela ligada a [activity], que aloja las pantallas del proveedor. */
    fun para(activity: ComponentActivity): PasarelaPagos = PasarelaSimulada(activity)
}
