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
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.motomarket.app.R
import com.motomarket.app.data.Repositorios
import com.motomarket.app.databinding.ActivityAdminMainBinding
import com.motomarket.app.databinding.NavHeaderAdminBinding
import com.motomarket.app.ui.common.BaseActivity
import com.motomarket.app.ui.common.cerrarSesion
import com.motomarket.app.ui.common.intentar
import com.motomarket.app.ui.common.mostrarMensaje
import com.motomarket.app.ui.informes.InformesFragment
import kotlinx.coroutines.launch

/**
 * Contenedor del módulo administrador con menú lateral: panel, usuarios, aprobación de productos,
 * reportes, informes, carga de datos de ejemplo y cierre de sesión.
 */
class AdminMainActivity : BaseActivity() {

    private lateinit var binding: ActivityAdminMainBinding

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

    /** Configura el botón del menú, el cierre con "atrás" y la navegación entre secciones. */
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
            when (item.itemId) {
                R.id.nav_cerrar_sesion -> cerrarSesion()
                R.id.nav_cargar_semilla -> confirmarCargaDeEjemplo()
                else -> abrirSeccion(item.itemId)
            }
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            item.itemId != R.id.nav_cargar_semilla
        }
    }

    /** El correo del encabezado se lee del perfil del administrador autenticado. */
    private fun mostrarCorreoAdmin() {
        val header = NavHeaderAdminBinding.bind(binding.navView.getHeaderView(0))
        lifecycleScope.launch {
            intentar { Repositorios.usuarios.obtenerUsuarioActual() }?.let { header.tvCorreoAdmin.text = it.correo }
        }
    }

    /** Publica en Firestore los productos de ejemplo de los mockups a nombre del administrador. */
    private fun confirmarCargaDeEjemplo() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.drawer_cargar_semilla)
            .setMessage(R.string.semilla_mensaje)
            .setNegativeButton(R.string.cancelar, null)
            .setPositiveButton(R.string.aceptar) { _, _ ->
                lifecycleScope.launch {
                    val creados = intentar { Repositorios.productos.cargarDatosEjemplo() } ?: return@launch
                    mostrarMensaje(getString(R.string.semilla_exito, creados))
                    abrirSeccion(R.id.nav_panel)
                }
            }
            .show()
    }

    /** Muestra la sección del menú y actualiza el título de la barra. */
    fun abrirSeccion(@IdRes itemId: Int) {
        val (fragment, titulo) = when (itemId) {
            R.id.nav_usuarios -> GestionUsuariosFragment() to R.string.titulo_gestion_usuarios
            R.id.nav_aprobacion -> GestionProductosFragment() to R.string.titulo_aprobacion_productos
            R.id.nav_reportes -> ReporteVentasFragment() to R.string.titulo_reportes
            R.id.nav_informes -> InformesFragment.nuevo(esAdmin = true, mostrarBarra = false) to R.string.titulo_informes
            else -> DashboardFragment() to R.string.titulo_panel_admin
        }
        binding.navView.setCheckedItem(itemId)
        binding.toolbar.setTitle(titulo)
        mostrar(fragment)
    }

    /** Reemplaza el contenido por el fragment de la sección elegida. */
    private fun mostrar(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.contenedor, fragment)
            .commit()
    }
}
