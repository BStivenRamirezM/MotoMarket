package com.motomarket.app.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.os.Build
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.coroutines.resume

/** Obtiene la ubicación del dispositivo con Fused Location y la convierte en dirección legible. */
class ServicioUbicacion(private val context: Context) {

    /** True si el usuario concedió al menos la ubicación aproximada. */
    fun tienePermiso(): Boolean = listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
        .any { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }

    /** Ubicación actual de alta precisión; si no llega a tiempo usa la última conocida (puede ser null). */
    @RequiresPermission(anyOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    suspend fun ubicacionActual(): Location? {
        val cliente = LocationServices.getFusedLocationProviderClient(context)
        val cancelacion = CancellationTokenSource()
        return try {
            cliente.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cancelacion.token).await()
                ?: cliente.lastLocation.await()
        } finally {
            cancelacion.cancel()
        }
    }

    /** Dirección legible de unas coordenadas con Geocoder, o null si el servicio no está disponible. */
    suspend fun direccionDe(lat: Double, lng: Double): String? {
        if (!Geocoder.isPresent()) return null
        val geocoder = Geocoder(context, Locale.forLanguageTag("es-CO"))
        val resultados = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            suspendCancellableCoroutine { continuacion ->
                geocoder.getFromLocation(lat, lng, 1, object : Geocoder.GeocodeListener {
                    override fun onGeocode(direcciones: MutableList<android.location.Address>) =
                        continuacion.resume(direcciones.toList())

                    override fun onError(mensaje: String?) = continuacion.resume(emptyList())
                })
            }
        } else {
            withContext(Dispatchers.IO) {
                @Suppress("DEPRECATION")
                runCatching { geocoder.getFromLocation(lat, lng, 1).orEmpty() }.getOrDefault(emptyList())
            }
        }
        return resultados.firstOrNull()?.getAddressLine(0)
    }
}
