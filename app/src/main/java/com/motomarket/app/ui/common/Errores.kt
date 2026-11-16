package com.motomarket.app.ui.common

import android.content.Context
import android.util.Log
import android.view.View
import androidx.core.view.isVisible
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.firestore.FirebaseFirestoreException
import com.motomarket.app.R
import com.motomarket.app.data.CuentaDesactivadaException
import com.motomarket.app.data.ImagenMuyGrandeException
import com.motomarket.app.data.ImagenNoLegibleException
import com.motomarket.app.data.PerfilNoEncontradoException
import com.motomarket.app.data.SesionRequeridaException
import com.motomarket.app.data.StockInsuficienteException
import kotlinx.coroutines.CancellationException

private const val TAG = "MotoMarket"

/** Traduce cualquier error remoto o de validación a un mensaje claro en español. */
fun Context.mensajeDeError(error: Throwable): String = when (error) {
    is CuentaDesactivadaException -> getString(R.string.error_cuenta_desactivada)
    is PerfilNoEncontradoException -> getString(R.string.error_perfil_no_encontrado)
    is SesionRequeridaException -> getString(R.string.error_sesion_requerida)
    is StockInsuficienteException -> getString(R.string.error_stock_insuficiente, error.producto, error.disponible)
    is ImagenNoLegibleException -> getString(R.string.error_imagen)
    is ImagenMuyGrandeException -> getString(R.string.error_imagen_grande)
    is FirebaseAuthInvalidUserException, is FirebaseAuthInvalidCredentialsException ->
        getString(R.string.error_credenciales)
    is FirebaseAuthUserCollisionException -> getString(R.string.error_correo_en_uso)
    is FirebaseAuthWeakPasswordException -> getString(R.string.error_password_debil)
    is FirebaseNetworkException -> getString(R.string.error_sin_conexion)
    is FirebaseFirestoreException -> when (error.code) {
        FirebaseFirestoreException.Code.PERMISSION_DENIED -> getString(R.string.error_sin_permisos)
        FirebaseFirestoreException.Code.UNAVAILABLE -> getString(R.string.error_sin_conexion)
        else -> getString(R.string.error_generico)
    }
    else -> getString(R.string.error_generico)
}

/**
 * Ejecuta una llamada remota mostrando el error al usuario en vez de cerrar la app.
 * Devuelve null si falló; la cancelación de la corrutina se deja propagar.
 */
suspend fun <T> Context.intentar(bloque: suspend () -> T): T? = try {
    bloque()
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    Log.e(TAG, "Error en operación remota", e)
    mostrarMensaje(mensajeDeError(e))
    null
}

/** Igual que [intentar], pero muestra [progreso] y deshabilita [boton] mientras dura la operación. */
suspend fun <T> Context.conCarga(progreso: View?, boton: View? = null, bloque: suspend () -> T): T? {
    progreso?.isVisible = true
    boton?.habilitar(false)
    try {
        return intentar(bloque)
    } finally {
        progreso?.isVisible = false
        boton?.habilitar(true)
    }
}
