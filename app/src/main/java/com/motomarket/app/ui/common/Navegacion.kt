package com.motomarket.app.ui.common

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.annotation.StringRes
import com.motomarket.app.data.Repositorios
import com.motomarket.app.models.Rol
import com.motomarket.app.ui.admin.AdminMainActivity
import com.motomarket.app.ui.auth.LoginActivity
import com.motomarket.app.ui.comprador.CompradorMainActivity
import com.motomarket.app.ui.vendedor.VendedorMainActivity

/** Muestra un Toast corto a partir de un recurso de texto. */
fun Context.mostrarMensaje(@StringRes mensaje: Int) =
    Toast.makeText(this, mensaje, Toast.LENGTH_SHORT).show()

/** Muestra un Toast corto con un texto ya construido. */
fun Context.mostrarMensaje(mensaje: CharSequence) =
    Toast.makeText(this, mensaje, Toast.LENGTH_SHORT).show()

/** Abre el módulo principal del rol y limpia la pila para que "atrás" no regrese al login. */
fun Activity.abrirInicioPorRol(rol: Rol) {
    val destino = when (rol) {
        Rol.COMPRADOR -> CompradorMainActivity::class.java
        Rol.VENDEDOR -> VendedorMainActivity::class.java
        Rol.ADMIN -> AdminMainActivity::class.java
    }
    startActivity(Intent(this, destino).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
}

/** Cierra la sesión de Firebase y vuelve al login borrando el historial de pantallas. */
fun Activity.cerrarSesion() {
    Repositorios.usuarios.cerrarSesion()
    startActivity(Intent(this, LoginActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
}
