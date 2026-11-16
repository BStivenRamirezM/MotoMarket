package com.motomarket.app.ui.common

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.motomarket.app.R

/**
 * Base de todas las Activities. Android 16+ fuerza edge-to-edge: aquí el contenido se aparta
 * de las barras del sistema (y del teclado) y estas muestran el gris oscuro de la ventana.
 */
abstract class BaseActivity : AppCompatActivity() {

    /** Aplica los insets una vez que la Activity ya fijó su contenido. */
    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }
        window.decorView.setBackgroundColor(ContextCompat.getColor(this, R.color.gris_oscuro))

        val contenido = findViewById<View>(android.R.id.content)
        ViewCompat.setOnApplyWindowInsetsListener(contenido) { vista, insets ->
            val barras = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime())
            vista.setPadding(barras.left, barras.top, barras.right, barras.bottom)
            WindowInsetsCompat.CONSUMED
        }
        ViewCompat.requestApplyInsets(contenido)
    }
}
