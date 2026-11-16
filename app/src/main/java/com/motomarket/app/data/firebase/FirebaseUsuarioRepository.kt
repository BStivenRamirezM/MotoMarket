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
import com.motomarket.app.models.SolicitudRol
import com.motomarket.app.models.Usuario
import kotlinx.coroutines.tasks.await

/** Implementación con Firebase Auth (correo/contraseña) y la colección "usuarios" de Firestore. */
class FirebaseUsuarioRepository : UsuarioRepository {

    private val auth = Firebase.auth
    private val usuarios = Firebase.firestore.collection(Colecciones.USUARIOS)
    private val mecanicos = Firebase.firestore.collection(Colecciones.MECANICOS)

    /** Autentica con correo y contraseña y valida el perfil (existente y activo). */
    override suspend fun iniciarSesion(correo: String, password: String): Usuario {
        auth.signInWithEmailAndPassword(correo, password).await()
        return validarPerfil(auth.currentUser?.uid ?: throw SesionRequeridaException())
    }

    /**
     * Crea la cuenta en Auth y su perfil siempre como comprador; si pidió ser vendedor o mecánico,
     * la solicitud queda pendiente. Si falla el perfil, borra la cuenta creada.
     */
    override suspend fun registrar(nombre: String, correo: String, telefono: String, password: String, solicitud: SolicitudRol?): Usuario {
        val cuenta = auth.createUserWithEmailAndPassword(correo, password).await().user
            ?: throw SesionRequeridaException()
        val ahora = System.currentTimeMillis()
        val usuario = Usuario(
            id = cuenta.uid, nombre = nombre, correo = correo, telefono = telefono,
            rol = Rol.COMPRADOR, activo = true, fechaRegistro = ahora,
            nombreNegocio = solicitud?.nombreNegocio.orEmpty(),
            rolSolicitado = solicitud?.rol ?: Rol.VENDEDOR,
            solicitudRol = if (solicitud != null) EstadoSolicitud.PENDIENTE else EstadoSolicitud.NINGUNA,
            fechaSolicitud = if (solicitud != null) ahora else 0L
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

    /** Marca la solicitud como pendiente con el rol y el nombre del negocio; las reglas impiden cambiar el rol aquí. */
    override suspend fun solicitarRol(solicitud: SolicitudRol) {
        val uid = uidActual() ?: throw SesionRequeridaException()
        usuarios.document(uid).update(
            mapOf(
                "nombreNegocio" to solicitud.nombreNegocio,
                "rolSolicitado" to solicitud.rol.aTexto(),
                "solicitudRol" to EstadoSolicitud.PENDIENTE.aTexto(),
                "motivoSolicitud" to "",
                "fechaSolicitud" to System.currentTimeMillis()
            )
        ).await()
    }

    /** Perfiles con solicitud pendiente, ordenados por fecha de solicitud. */
    override suspend fun obtenerSolicitudesRol(): List<Usuario> =
        usuarios.whereEqualTo("solicitudRol", EstadoSolicitud.PENDIENTE.aTexto()).get().await().documents
            .map { it.aUsuario() }
            .sortedBy { it.fechaSolicitud }

    /**
     * Aprobar asigna el rol solicitado (y publica el perfil de mecánico si aplica);
     * rechazar conserva el rol comprador y guarda el motivo.
     */
    override suspend fun resolverSolicitudRol(usuario: Usuario, aprobar: Boolean, motivo: String) {
        val cambios = if (aprobar) {
            mapOf(
                "rol" to usuario.rolSolicitado.aTexto(),
                "solicitudRol" to EstadoSolicitud.APROBADA.aTexto(),
                "motivoSolicitud" to ""
            )
        } else {
            mapOf(
                "solicitudRol" to EstadoSolicitud.RECHAZADA.aTexto(),
                "motivoSolicitud" to motivo
            )
        }
        usuarios.document(usuario.id).update(cambios).await()
        if (aprobar) sincronizarMecanico(usuario.copy(rol = usuario.rolSolicitado))
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

    /** Actualiza el campo activo del perfil y del perfil público si es mecánico. */
    override suspend fun cambiarEstado(id: String, activo: Boolean) {
        usuarios.document(id).update("activo", activo).await()
        sincronizarMecanico(usuarios.document(id).get().await().aUsuario())
    }

    /** Actualiza el campo rol del perfil y publica o retira el perfil de mecánico. */
    override suspend fun cambiarRol(id: String, rol: Rol) {
        usuarios.document(id).update("rol", rol.aTexto()).await()
        sincronizarMecanico(usuarios.document(id).get().await().aUsuario())
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
            sincronizarMecanico(usuario)
            return usuario
        } finally {
            authSecundaria.signOut()
        }
    }

    /**
     * Mantiene "mecanicos/{uid}" (perfil público que ven los compradores) acorde al rol:
     * se publica si el usuario es mecánico y se retira si deja de serlo.
     */
    private suspend fun sincronizarMecanico(usuario: Usuario) {
        val publico = mecanicos.document(usuario.id)
        if (usuario.rol == Rol.MECANICO) publico.set(usuario.aMapaMecanico()).await() else publico.delete().await()
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
