package com.motomarket.app.pagos

import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Abre la pantalla de un proveedor de pagos y suspende hasta que devuelva su resultado.
 * Registra un launcher de un solo uso en el registro de la Activity y lo libera al terminar,
 * así cualquier pasarela puede exponer una API `suspend` sin manejar callbacks.
 */
internal suspend fun <E, S> ComponentActivity.abrirPantallaDePago(contrato: ActivityResultContract<E, S>, entrada: E): S =
    suspendCancellableCoroutine { continuacion ->
        lateinit var lanzador: ActivityResultLauncher<E>
        lanzador = activityResultRegistry.register("pasarela_${System.nanoTime()}", contrato) { salida ->
            lanzador.unregister()
            if (continuacion.isActive) continuacion.resume(salida)
        }
        continuacion.invokeOnCancellation { lanzador.unregister() }
        lanzador.launch(entrada)
    }
