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
import com.motomarket.app.models.EstadoSolicitud
import com.motomarket.app.models.Rol
import com.motomarket.app.models.Usuario
import kotlinx.coroutines.tasks.await

/** Implementación con Firebase Auth (correo/contraseña) y la colección "usuarios" de Firestore. */
class FirebaseUsuarioRepository : UsuarioRepository {

    private val auth = Firebase.auth
    private val usuarios = Firebase.firestore.collection(Colecciones.USUARIOS)

    /** Autentica con correo y contraseña y valida el perfil (existente y activo). */
    override suspend fun iniciarSesion(correo: String, password: String): Usuario {
        auth.signInWithEmailAndPassword(correo, password).await()
        return validarPerfil(auth.currentUser?.uid ?: throw SesionRequeridaException())
    }

    /**
     * Crea la cuenta en Auth y su perfil siempre como comprador; si pidió vender, la solicitud
     * queda pendiente. Si falla el perfil, borra la cuenta creada.
     */
    override suspend fun registrar(nombre: String, correo: String, telefono: String, password: String, tiendaSolicitada: String?): Usuario {
        val cuenta = auth.createUserWithEmailAndPassword(correo, password).await().user
            ?: throw SesionRequeridaException()
        val ahora = System.currentTimeMillis()
        val usuario = Usuario(
            id = cuenta.uid, nombre = nombre, correo = correo, telefono = telefono,
            rol = Rol.COMPRADOR, activo = true, fechaRegistro = ahora,
            nombreTienda = tiendaSolicitada.orEmpty(),
            solicitudVendedor = if (tiendaSolicitada != null) EstadoSolicitud.PENDIENTE else EstadoSolicitud.NINGUNA,
            fechaSolicitud = if (tiendaSolicitada != null) ahora else 0L
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

    /** Marca la solicitud como pendiente con el nombre de tienda; las reglas impiden cambiar el rol aquí. */
    override suspend fun solicitarSerVendedor(nombreTienda: String) {
        val uid = uidActual() ?: throw SesionRequeridaException()
        usuarios.document(uid).update(
            mapOf(
                "nombreTienda" to nombreTienda,
                "solicitudVendedor" to EstadoSolicitud.PENDIENTE.aTexto(),
                "motivoSolicitud" to "",
                "fechaSolicitud" to System.currentTimeMillis()
            )
        ).await()
    }

    /** Perfiles con solicitud pendiente, ordenados por fecha de solicitud. */
    override suspend fun obtenerSolicitudesVendedor(): List<Usuario> =
        usuarios.whereEqualTo("solicitudVendedor", EstadoSolicitud.PENDIENTE.aTexto()).get().await().documents
            .map { it.aUsuario() }
            .sortedBy { it.fechaSolicitud }

    /** Aprobar asigna el rol vendedor; rechazar conserva el rol comprador y guarda el motivo. */
    override suspend fun resolverSolicitudVendedor(id: String, aprobar: Boolean, motivo: String) {
        val cambios = if (aprobar) {
            mapOf(
                "rol" to Rol.VENDEDOR.aTexto(),
                "solicitudVendedor" to EstadoSolicitud.APROBADA.aTexto(),
                "motivoSolicitud" to ""
            )
        } else {
            mapOf(
                "solicitudVendedor" to EstadoSolicitud.RECHAZADA.aTexto(),
                "motivoSolicitud" to motivo
            )
        }
        usuarios.document(id).update(cambios).await()
    }

    /** Pide a Firebase Auth que envíe el correo de restablecimiento. */
    override suspend fun recuperarPassword(correo: String) {
        auth.sendPasswordResetEmail(correo).await()
    }

    /** Perfil del usuario con sesión guardada en el dispositivo, validando que siga activo. */
    override suspend fun sesionActiva(): Usuario? {
        val uid = auth.currentUser?.uid ?: return null
        return validarPerfil(uid)
    }

    /** Uid del usuario autenticado en Firebase Auth. */
    override fun uidActual(): String? = auth.currentUser?.uid

    /** Cierra la sesión de Firebase Auth. */
    override fun cerrarSesion() = auth.signOut()

    /** Lee el documento de perfil del usuario autenticado. */
    override suspend fun obtenerUsuarioActual(): Usuario {
        val uid = uidActual() ?: throw SesionRequeridaException()
        val documento = usuarios.document(uid).get().await()
        if (!documento.exists()) throw PerfilNoEncontradoException()
        return documento.aUsuario()
    }

    /** Agrega la dirección al arreglo del perfil con arrayUnion (sin duplicados). */
    override suspend fun agregarDireccion(direccion: String) {
        val uid = uidActual() ?: throw SesionRequeridaException()
        usuarios.document(uid).update("direcciones", FieldValue.arrayUnion(direccion)).await()
    }

    /** Lista todos los perfiles y filtra por nombre o correo en el dispositivo. */
    override suspend fun buscarUsuarios(texto: String): List<Usuario> =
        usuarios.get().await().documents
            .map { it.aUsuario() }
            .filter {
                texto.isBlank() || it.nombre.contains(texto, ignoreCase = true) || it.correo.contains(texto, ignoreCase = true)
            }
            .sortedBy { it.nombre.lowercase() }

    /** Actualiza el campo activo del perfil. */
    override suspend fun cambiarEstado(id: String, activo: Boolean) {
        usuarios.document(id).update("activo", activo).await()
    }

    /** Actualiza el campo rol del perfil. */
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

    /** FirebaseAuth de una instancia secundaria de FirebaseApp, creada una sola vez. */
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
