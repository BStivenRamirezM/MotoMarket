package com.motomarket.app.ui.comprador

import android.os.Bundle
import androidx.fragment.app.Fragment
import com.motomarket.app.R
import com.motomarket.app.databinding.ActivityCompradorMainBinding
import com.motomarket.app.ui.common.BaseActivity

class CompradorMainActivity : BaseActivity() {

    private lateinit var binding: ActivityCompradorMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCompradorMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.bottomNav.setOnItemSelectedListener { item ->
            mostrar(fragmentoPara(item.itemId))
            true
        }
        if (savedInstanceState == null) {
            binding.bottomNav.selectedItemId = R.id.nav_inicio
        }
    }

    private fun fragmentoPara(itemId: Int): Fragment = when (itemId) {
        R.id.nav_carrito -> CarritoFragment()
        R.id.nav_perfil -> PerfilFragment()
        else -> InicioFragment()
    }

    private fun mostrar(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.contenedor, fragment)
            .commit()
    }
}
