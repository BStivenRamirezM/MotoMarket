package com.motomarket.app.ui.auth

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import com.motomarket.app.databinding.ActivitySplashBinding
import com.motomarket.app.ui.common.BaseActivity

/** Pantalla de bienvenida con el logo; tras 2 segundos abre el login. */
@SuppressLint("CustomSplashScreen")
class SplashActivity : BaseActivity() {

    private val handler = Handler(Looper.getMainLooper())
    private val irAlLogin = Runnable {
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(ActivitySplashBinding.inflate(layoutInflater).root)
        handler.postDelayed(irAlLogin, DURACION_MS)
    }

    override fun onDestroy() {
        handler.removeCallbacks(irAlLogin)
        super.onDestroy()
    }

    private companion object {
        const val DURACION_MS = 2000L
    }
}
