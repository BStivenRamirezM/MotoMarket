@file:Suppress("DEPRECATION")

package com.motomarket.app.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Guarda cifrados el uid y el correo del último inicio de sesión para habilitar el ingreso con huella.
 * EncryptedSharedPreferences está marcada como obsoleta por AndroidX, pero sigue siendo funcional
 * y es lo que exige el proyecto; si el almacén cifrado falla, la huella simplemente no se ofrece.
 */
class SesionLocal(context: Context) {

    private val preferencias: SharedPreferences? = runCatching {
        val clave = MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
        EncryptedSharedPreferences.create(
            context,
            ARCHIVO,
            clave,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }.getOrNull()

    val uid: String? get() = preferencias?.getString(CLAVE_UID, null)
    val correo: String? get() = preferencias?.getString(CLAVE_CORREO, null)

    /** Registra la sesión exitosa para futuros ingresos con huella. */
    fun guardar(uid: String, correo: String) {
        preferencias?.edit {
            putString(CLAVE_UID, uid)
            putString(CLAVE_CORREO, correo)
        }
    }

    private companion object {
        const val ARCHIVO = "sesion_segura"
        const val CLAVE_UID = "uid"
        const val CLAVE_CORREO = "correo"
    }
}
