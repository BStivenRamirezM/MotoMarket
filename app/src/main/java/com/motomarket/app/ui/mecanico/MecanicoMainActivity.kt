package com.motomarket.app.ui.mecanico

import android.os.Bundle
import androidx.fragment.app.Fragment
import com.motomarket.app.R
import com.motomarket.app.databinding.ActivityMecanicoMainBinding
import com.motomarket.app.ui.common.BaseActivity
import com.motomarket.app.ui.servicios.ServiciosFragment
import com.motomarket.app.ui.vendedor.PerfilVendedorFragment

/** Contenedor del módulo mecánico con navegación inferior entre Trabajos (servicios asignados) y Perfil. */
class MecanicoMainActivity : BaseActivity() {

    private lateinit var binding: ActivityMecanicoMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMecanicoMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.bottomNav.setOnItemSelectedListener { item ->
            mostrar(fragmentoPara(item.itemId))
            true
        }
        if (savedInstanceState == null) {
            binding.bottomNav.selectedItemId = R.id.nav_trabajos
        }
    }

    /** Fragment que corresponde a cada pestaña; el perfil reutiliza el de vendedor con datos del taller. */
    private fun fragmentoPara(itemId: Int): Fragment = when (itemId) {
        R.id.nav_perfil_mecanico -> PerfilVendedorFragment()
        else -> ServiciosFragment.nuevo(vistaMecanico = true)
    }

    /** Reemplaza el contenido por el fragment de la pestaña elegida. */
    private fun mostrar(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.contenedor, fragment)
            .commit()
    }
}
