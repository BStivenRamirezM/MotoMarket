package com.motomarket.app.data.firebase

import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.app
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.firestore
import com.motomarket.app.data.CuentaDesactivadaException
import com.motomarket.app.data.PerfilNoEncontradoException
import com.motomarket.app.data.SesionRequeridaException
import com.motomarket.app.data.UsuarioRepository
import com.motomarket.app.models.Rol
import com.motomarket.app.models.Usuario
import kotlinx.coroutines.tasks.await

/** Implementación con Firebase Auth (correo/contraseña) y la colección "usuarios" de Firestore. */
class FirebaseUsuarioRepository : UsuarioRepository {

    private val auth = Firebase.auth
    private val usuarios = Firebase.firestore.collection(Colecciones.USUARIOS)

    override suspend fun iniciarSesion(correo: String, password: String): Usuario {
        auth.signInWithEmailAndPassword(correo, password).await()
        return validarPerfil(auth.currentUser?.uid ?: throw SesionRequeridaException())
    }

    override suspend fun registrar(nombre: String, correo: String, telefono: String, password: String, rol: Rol): Usuario {
        val cuenta = auth.createUserWithEmailAndPassword(correo, password).await().user
            ?: throw SesionRequeridaException()
        val usuario = Usuario(
            id = cuenta.uid, nombre = nombre, correo = correo, telefono = telefono,
            rol = rol, activo = true, fechaRegistro = System.currentTimeMillis()
        )
        try {
            usuarios.document(cuenta.uid).set(usuario.aMapa()).await()
        } catch (e: Exception) {
            // Sin perfil la cuenta quedaría inutilizable, así que se revierte la creación en Auth.
            cuenta.delete().await()
            throw e
        }
        return usuario
    }

    override suspend fun recuperarPassword(correo: String) {
        auth.sendPasswordResetEmail(correo).await()
    }

    override suspend fun sesionActiva(): Usuario? {
        val uid = auth.currentUser?.uid ?: return null
        return validarPerfil(uid)
    }

    override fun uidActual(): String? = auth.currentUser?.uid

    override fun cerrarSesion() = auth.signOut()

    override suspend fun obtenerUsuarioActual(): Usuario {
        val uid = uidActual() ?: throw SesionRequeridaException()
        val documento = usuarios.document(uid).get().await()
        if (!documento.exists()) throw PerfilNoEncontradoException()
        return documento.aUsuario()
    }

    override suspend fun agregarDireccion(direccion: String) {
        val uid = uidActual() ?: throw SesionRequeridaException()
        usuarios.document(uid).update("direcciones", FieldValue.arrayUnion(direccion)).await()
    }

    override suspend fun buscarUsuarios(texto: String): List<Usuario> =
        usuarios.get().await().documents
            .map { it.aUsuario() }
            .filter {
                texto.isBlank() || it.nombre.contains(texto, ignoreCase = true) || it.correo.contains(texto, ignoreCase = true)
            }
            .sortedBy { it.nombre.lowercase() }

    override suspend fun cambiarEstado(id: String, activo: Boolean) {
        usuarios.document(id).update("activo", activo).await()
    }

    override suspend fun cambiarRol(id: String, rol: Rol) {
        usuarios.document(id).update("rol", rol.aTexto()).await()
    }

    /**
     * Crea la cuenta con una instancia secundaria de FirebaseApp: crear usuarios con la instancia
     * principal cambiaría la sesión del administrador por la del usuario nuevo.
     */
    override suspend fun crearUsuario(nombre: String, correo: String, password: String, rol: Rol): Usuario {
        val authSecundaria = authSecundaria()
        try {
            val cuenta = authSecundaria.createUserWithEmailAndPassword(correo, password).await().user
                ?: throw SesionRequeridaException()
            val usuario = Usuario(
                id = cuenta.uid, nombre = nombre, correo = correo,
                rol = rol, activo = true, fechaRegistro = System.currentTimeMillis()
            )
            usuarios.document(cuenta.uid).set(usuario.aMapa()).await()
            return usuario
        } finally {
            authSecundaria.signOut()
        }
    }

    /** Lee el perfil y cierra la sesión si no existe o si la cuenta está desactivada. */
    private suspend fun validarPerfil(uid: String): Usuario {
        val documento = usuarios.document(uid).get().await()
        if (!documento.exists()) {
            auth.signOut()
            throw PerfilNoEncontradoException()
        }
        val usuario = documento.aUsuario()
        if (!usuario.activo) {
            auth.signOut()
            throw CuentaDesactivadaException()
        }
        return usuario
    }

    private fun authSecundaria(): FirebaseAuth {
        val principal = Firebase.app
        val contexto = principal.applicationContext
        val app = FirebaseApp.getApps(contexto).firstOrNull { it.name == APP_SECUNDARIA }
            ?: FirebaseApp.initializeApp(contexto, principal.options, APP_SECUNDARIA)
        return FirebaseAuth.getInstance(app)
    }

    private companion object {
        const val APP_SECUNDARIA = "registro_admin"
    }
}
