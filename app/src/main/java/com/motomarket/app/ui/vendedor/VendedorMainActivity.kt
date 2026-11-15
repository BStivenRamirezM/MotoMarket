package com.motomarket.app.ui.vendedor

import android.os.Bundle
import androidx.fragment.app.Fragment
import com.motomarket.app.R
import com.motomarket.app.databinding.ActivityVendedorMainBinding
import com.motomarket.app.ui.common.BaseActivity

class VendedorMainActivity : BaseActivity() {

    private lateinit var binding: ActivityVendedorMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVendedorMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.bottomNav.setOnItemSelectedListener { item ->
            mostrar(fragmentoPara(item.itemId))
            true
        }
        if (savedInstanceState == null) {
            binding.bottomNav.selectedItemId = R.id.nav_mis_productos
        }
    }

    private fun fragmentoPara(itemId: Int): Fragment = when (itemId) {
        R.id.nav_pedidos -> PedidosFragment()
        R.id.nav_perfil_vendedor -> PerfilVendedorFragment()
        else -> MisProductosFragment()
    }

    private fun mostrar(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.contenedor, fragment)
            .commit()
    }
}
