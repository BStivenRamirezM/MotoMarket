package com.motomarket.app.ui.admin

import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.annotation.IdRes
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.motomarket.app.R
import com.motomarket.app.data.Repositorios
import com.motomarket.app.data.UsuarioRepository
import com.motomarket.app.databinding.ActivityAdminMainBinding
import com.motomarket.app.databinding.NavHeaderAdminBinding
import com.motomarket.app.models.Rol
import com.motomarket.app.ui.common.BaseActivity
import com.motomarket.app.ui.common.cerrarSesion
import kotlinx.coroutines.launch

class AdminMainActivity : BaseActivity() {

    private lateinit var binding: ActivityAdminMainBinding
    private val usuarioRepository: UsuarioRepository = Repositorios.usuarios

    private val cerrarDrawerAlVolver = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() = binding.drawerLayout.closeDrawer(GravityCompat.START)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        configurarDrawer()
        mostrarCorreoAdmin()
        if (savedInstanceState == null) {
            abrirSeccion(R.id.nav_panel)
        } else {
            binding.navView.checkedItem?.let { binding.toolbar.title = it.title }
        }
    }

    private fun configurarDrawer() {
        val toggle = ActionBarDrawerToggle(
            this, binding.drawerLayout, binding.toolbar, R.string.cd_abrir_menu, R.string.cd_cerrar_menu
        )
        toggle.drawerArrowDrawable.color = ContextCompat.getColor(this, R.color.blanco)
        binding.drawerLayout.addDrawerListener(toggle)
        binding.drawerLayout.addDrawerListener(object : DrawerLayout.SimpleDrawerListener() {
            override fun onDrawerOpened(drawerView: View) {
                cerrarDrawerAlVolver.isEnabled = true
            }

            override fun onDrawerClosed(drawerView: View) {
                cerrarDrawerAlVolver.isEnabled = false
            }
        })
        toggle.syncState()
        onBackPressedDispatcher.addCallback(this, cerrarDrawerAlVolver)

        binding.navView.setNavigationItemSelectedListener { item ->
            if (item.itemId == R.id.nav_cerrar_sesion) {
                cerrarSesion()
            } else {
                abrirSeccion(item.itemId)
            }
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            true
        }
    }

    private fun mostrarCorreoAdmin() {
        val header = NavHeaderAdminBinding.bind(binding.navView.getHeaderView(0))
        lifecycleScope.launch {
            header.tvCorreoAdmin.text = usuarioRepository.obtenerUsuarioActual(Rol.ADMIN).correo
        }
    }

    fun abrirSeccion(@IdRes itemId: Int) {
        val (fragment, titulo) = when (itemId) {
            R.id.nav_usuarios -> GestionUsuariosFragment() to R.string.titulo_gestion_usuarios
            R.id.nav_aprobacion -> GestionProductosFragment() to R.string.titulo_aprobacion_productos
            R.id.nav_reportes -> ReporteVentasFragment() to R.string.titulo_reportes
            else -> DashboardFragment() to R.string.titulo_panel_admin
        }
        binding.navView.setCheckedItem(itemId)
        binding.toolbar.setTitle(titulo)
        mostrar(fragment)
    }

    private fun mostrar(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.contenedor, fragment)
            .commit()
    }
}
