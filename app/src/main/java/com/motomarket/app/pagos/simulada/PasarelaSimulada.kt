package com.motomarket.app.pagos.simulada

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContract
import com.motomarket.app.pagos.EstadoTransaccion
import com.motomarket.app.pagos.OrdenPago
import com.motomarket.app.pagos.PasarelaPagos
import com.motomarket.app.pagos.ResultadoPago
import com.motomarket.app.pagos.abrirPantallaDePago

/**
 * Pasarela de pruebas sin proveedor real: muestra [PagoSimuladoActivity] con el resumen,
 * simula el procesamiento y aprueba la transacción con una referencia "SIM-<timestamp>".
 */
class PasarelaSimulada(private val activity: ComponentActivity) : PasarelaPagos {

    override suspend fun procesarPago(orden: OrdenPago): ResultadoPago =
        activity.abrirPantallaDePago(ContratoPagoSimulado(), orden)

    /** Envía la orden a la pantalla simulada y traduce su respuesta a [ResultadoPago]. */
    private class ContratoPagoSimulado : ActivityResultContract<OrdenPago, ResultadoPago>() {

        override fun createIntent(context: Context, input: OrdenPago): Intent =
            PagoSimuladoActivity.crearIntent(context, input)

        override fun parseResult(resultCode: Int, intent: Intent?): ResultadoPago {
            if (resultCode != Activity.RESULT_OK || intent == null) return ResultadoPago(EstadoTransaccion.CANCELADA)
            return ResultadoPago(
                estado = EstadoTransaccion.APROBADA,
                referencia = intent.getStringExtra(PagoSimuladoActivity.EXTRA_REFERENCIA).orEmpty()
            )
        }
    }
}
