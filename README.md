# MotoMarket

Marketplace de repuestos y accesorios para motos en Colombia. App Android nativa (Kotlin + Views/XML) **completamente funcional con Firebase**: autenticación por roles, catálogo con imágenes, carrito persistente, pagos con una pasarela simulada (lista para Mercado Pago), geolocalización de la entrega, seguimiento de pedidos y panel de administración con métricas reales.

Es el proyecto final de un curso universitario. Las pantallas conservan el diseño de los mockups de la entrega de frontend.

## Tecnologías

| Elemento | Versión / detalle |
|---|---|
| Lenguaje | Kotlin 2.4 (compilador fijado sobre el Kotlin integrado de AGP 9) |
| UI | Views + XML con ViewBinding (sin Jetpack Compose), Material 3 |
| Backend | Firebase Auth (correo/contraseña), Cloud Firestore, Firebase Storage — BOM 35.2.0 |
| Pagos | Interfaz `PasarelaPagos` con implementación simulada (`PasarelaSimulada`); preparada para Mercado Pago Checkout Pro 
| Ubicación | Fused Location Provider (`play-services-location`) + `Geocoder` |
| Imágenes | Coil 3 para cargarlas; cámara (`FileProvider` + `TakePicture`) o galería (`PickVisualMedia`); compresión a ≤ 1 MB |
| Seguridad local | `BiometricPrompt` + `EncryptedSharedPreferences` |
| Asincronía | Corrutinas: repositorios `suspend` con `await()`, `lifecycleScope` en la UI |
| SDK / Build | minSdk 26 · compile/targetSdk 37 · AGP 9.5 · Gradle 9.8.2 · google-services 4.6.0 |
| JDK | El JBR incluido en Android Studio |

## Arquitectura

```
Pantallas (Activities / Fragments)        ui/auth · ui/comprador · ui/vendedor · ui/admin
        │  solo conocen interfaces; llamadas con corrutinas, errores y estados de carga en ui/common
        ▼
Interfaces de repositorio (suspend)       data/UsuarioRepository, ProductoRepository, CarritoRepository,
        │                                 PedidoRepository, ReporteRepository
        │  data/Repositorios.kt (localizador de servicios) decide la implementación
        ▼
Implementaciones Firebase                 data/firebase/Firebase*Repository + Mapeadores
        │
        ▼
Firebase Auth · Firestore · Storage
```

- Las pantallas **nunca** usan Firebase directamente: si mañana se cambia de backend, basta con otra implementación y una línea en `Repositorios.kt`.
- Toda llamada remota pasa por `intentar { }` o `conCarga(progreso, boton) { }` (`ui/common/Errores.kt`). Estas funciones muestran un indicador de carga, deshabilitan el botón y traducen cualquier error (credenciales, red, permisos, stock) a un mensaje en español.
- Cada clase y cada método relevante tiene un comentario KDoc breve en español.

### Estructura de carpetas

```
app/src/main/
├── java/com/motomarket/app/
│   ├── models/          Producto, Usuario, Pedido (+ItemPedido), ItemCarrito, Reportes
│   ├── data/            Interfaces, Repositorios (service locator), Errores, DatosEjemplo (semilla)
│   │   ├── firebase/    Implementaciones con Auth, Firestore y Storage + mapeadores
│   │   └── local/       SesionLocal (EncryptedSharedPreferences para la huella)
│   ├── util/            Formato (COP, fechas), CompresorImagen, ServicioUbicacion
│   ├── pagos/           PasarelaPagos (interfaz), Pasarelas (selector), modelos de pago
│   │   └── simulada/    PasarelaSimulada + PagoSimuladoActivity
│   └── ui/
│       ├── common/      BaseActivity, manejo de errores, chips de estado, Coil, navegación
│       ├── auth/        Splash, Login (huella), Registro, Recuperar contraseña
│       ├── comprador/   Catálogo, Detalle, Carrito, Pago seguro, Perfil, Historial
│       ├── vendedor/    Mis productos, Publicar/editar, Pedidos recibidos, Mi tienda
│       └── admin/       Panel, Usuarios, Aprobación de productos, Reportes
└── res/                 layouts, drawables, strings (todo centralizado), xml/file_paths.xml
firestore.rules · storage.rules · firebase.json   (en la raíz del repositorio)
```

## Colecciones de Firestore

| Colección | Documento | Campos |
|---|---|---|
| `usuarios` | `{uid}` | `uid, nombre, correo, telefono, rol (comprador\|vendedor\|admin), activo, fechaRegistro, calificacion, direcciones[], nombreTienda, solicitudVendedor (ninguna\|pendiente\|aprobada\|rechazada), motivoSolicitud, fechaSolicitud` |
| `productos` | `{id}` | `id, nombre, precio, categoria, marcaModeloCompatible, cilindraje, stock, vendedorId, vendedorNombre, calificacionVendedor, estado (pendiente\|aprobado\|rechazado\|agotado), imagenUrl, fechaCreacion, fechaRevision, motivoRechazo` |
| `pedidos` | `{id}` | `id, compradorId, compradorNombre, items[{productoId, nombre, precio, cantidad, vendedorId}], vendedorIds[], subtotal, envio, total, direccionEntrega, lat, lng, metodoPago, estado (pendiente\|pagado\|enviado\|entregado), refPago, fecha` |
| `carritos/{uid}/items` | `{productoId}` | `productoId, cantidad, agregado` |

- Las fechas se guardan en milisegundos (`Long`).
- Las imágenes se guardan en Storage en `productos/{uid}/{timestamp}.jpg`.
- `vendedorIds` permite que cada vendedor consulte sus pedidos con un solo `array-contains`.
- Ninguna consulta necesita índices compuestos: el ordenamiento se hace en el dispositivo.

## Configuración (pasos manuales)

### 1. Proyecto de Firebase y `google-services.json`
1. En la [consola de Firebase](https://console.firebase.google.com) crea (o usa) el proyecto y registra una app Android con el paquete **`com.motomarket.app`**.
2. Descarga `google-services.json` y colócalo en **`app/google-services.json`**.
   > TODO para quien clone el repo: este archivo **no se versiona** (está en `.gitignore`). Sin él, el proyecto compila igual, pero la app muestra "Firebase no está configurado" en el login.

### 2. Activar los servicios
- **Authentication** → Sign-in method → habilita **Correo electrónico/contraseña**.
- **Firestore Database** → crea la base de datos (modo producción).
- **Storage** → crea el bucket por defecto.

### 3. Publicar las reglas de seguridad
Elige una de estas dos opciones:
- **Consola:** copia el contenido de `firestore.rules` en Firestore → Reglas, y el de `storage.rules` en Storage → Reglas. Pulsa **Publicar** en cada una.
- **CLI:** con [Firebase CLI](https://firebase.google.com/docs/cli) instalada:
  ```bash
  firebase login
  firebase use --add
  firebase deploy --only firestore:rules,storage
  ```

Resumen de las reglas:
- Solo entran usuarios autenticados.
- Toda cuenta nueva se crea como **comprador**; Firestore rechaza un registro con rol `vendedor` o `admin`.
- Cada usuario lee y edita su perfil, pero no puede cambiar su rol, su estado ni aprobar su propia solicitud de vendedor (solo puede dejarla en `pendiente`).
- El catálogo (aprobados y agotados) es visible para todos los usuarios autenticados.
- Solo el vendedor dueño crea o edita sus productos, y siempre quedan pendientes.
- Solo el admin modera, cambia roles y activa o desactiva cuentas.
- Una orden la leen su comprador, los vendedores involucrados y el admin.
- Al comprar, un comprador solo puede descontar stock.

### 4. Crear el primer administrador
El rol `admin` no se puede obtener desde la app:
1. Regístrate en la app (la cuenta se crea como comprador).
2. En Firestore → `usuarios` → el documento con tu uid, cambia el campo `rol` a **`admin`**.
3. Cierra sesión y vuelve a entrar. Desde ese momento puedes aprobar vendedores, crear usuarios (también administradores) y cambiar roles en **Gestión de usuarios**.

## Roles y quién puede asignarlos

| Rol | Cómo se obtiene | Quién lo asigna |
|---|---|---|
| Comprador | Al registrarse; es el rol de toda cuenta nueva | Automático |
| Vendedor | El comprador **solicita** vender (al registrarse o desde su perfil) y un admin **aprueba** la solicitud | Solo un administrador |
| Administrador | Asignación manual del primer admin en Firestore; luego, otro admin desde *Gestión de usuarios* | Solo un administrador |

Flujo de la solicitud de vendedor (campo `solicitudVendedor` del perfil):
1. **Solicitar:** al registrarse, el usuario marca "Quiero vender en MotoMarket" y escribe el nombre de su tienda, o lo hace después desde *Perfil → Vender en MotoMarket*. La solicitud queda en `pendiente` y la cuenta sigue siendo de comprador.
2. **Revisar:** el admin ve las solicitudes en *Gestión de usuarios → Solicitudes de vendedor*.
   - **Aprobar** cambia el `rol` a `vendedor`. En su perfil, el usuario ve "Ir a mi tienda", y a partir del siguiente ingreso entra directamente al módulo de vendedor.
   - **Rechazar** pide un motivo, que el usuario ve en su perfil junto con la opción "Volver a solicitar".
3. **Revocar:** el admin puede devolver a un vendedor a comprador o desactivar su cuenta. El login lee el rol en cada ingreso, así que el cambio aplica en el siguiente inicio de sesión.

Todo esto lo garantizan las reglas de `firestore.rules` en el servidor, no solo la interfaz. Aunque alguien modifique la app o escriba directo en Firestore, no puede crear su cuenta como vendedor o admin, cambiar su `rol` ni pasar su solicitud a `aprobada`.

### 5. Pasarela de pagos
Hoy la app usa una **pasarela simulada**: no necesita cuentas ni llaves y no cobra dinero real. Ver la sección [Pasarela de pagos](#pasarela-de-pagos) para su funcionamiento y para integrar Mercado Pago más adelante.

### 6. Poblar datos de ejemplo
Con una cuenta **admin**, abre el menú lateral → **Cargar datos de ejemplo**. La app publica los 7 productos de los mockups (`data/DatosEjemplo.kt`) a nombre de esa cuenta:
- 5 aprobados,
- los Espejos NKD agotados,
- la Cadena 428H pendiente, para probar la moderación.

Las imágenes se pueden agregar después editando cada producto.

## Pasarela de pagos

### Arquitectura
```
PasarelaPagoActivity ──► PasarelaPagos.procesarPago(OrdenPago): ResultadoPago   (pagos/PasarelaPagos.kt)
                               ▲
          Pasarelas.para(activity)  ← único punto de cambio                     (pagos/Pasarelas.kt)
                               │
        ┌──────────────────────┴──────────────────────┐
  PasarelaSimulada (hoy)                     MercadoPagoPasarela (futuro)
  pagos/simulada/                            pagos/mercadopago/
```

- **`PasarelaPagos`** es una interfaz con un solo método `suspend fun procesarPago(orden: OrdenPago): ResultadoPago`. Cada implementación muestra su propia pantalla de cobro y devuelve un estado: `APROBADA`, `PENDIENTE`, `RECHAZADA` o `CANCELADA`, junto con la `referencia` del proveedor.
- **`OrdenPago`** lleva las líneas del carrito, el subtotal, el envío, el total, el método (tarjeta o PSE), el comprador, la dirección y una `referenciaInterna` (`MM-<timestamp>`).
- **`abrirPantallaDePago()`** (`pagos/PantallaDePago.kt`) abre la Activity del proveedor y suspende hasta su resultado. Cualquier pasarela nueva lo reutiliza.
- **`PasarelaPagoActivity` no conoce al proveedor.** Si el resultado es `APROBADA`:
  1. llama a `PedidoRepository.confirmarCompra(...)`, que en una **transacción de Firestore** valida el stock, lo descuenta y crea la orden con `refPago` = referencia de la pasarela;
  2. vacía el carrito;
  3. guarda la dirección;
  4. muestra el número de orden.

### Implementación actual: `PasarelaSimulada`
1. Abre `PagoSimuladoActivity`, que muestra el resumen: productos, subtotal, envío, total, método, pagador y dirección.
2. Al pulsar **Pagar** muestra un `ProgressBar`, espera 2 segundos y devuelve `APROBADA` con referencia **`SIM-<timestamp>`**.
3. **Cancelar** (o atrás) devuelve `CANCELADA`, y el carrito queda intacto.

No requiere configuración ni credenciales.

### Cómo integrar Mercado Pago (Checkout Pro) más adelante

**1. Credenciales de prueba**
1. En [Mercado Pago Developers](https://www.mercadopago.com.co/developers) crea una aplicación de tipo *Checkout Pro*.
2. Crea dos **usuarios de prueba**: uno vendedor y otro comprador.
3. Con el usuario vendedor, copia las **credenciales de prueba**: *Public Key* y *Access Token*, que empiezan por `TEST-`.
4. Agrégalas a `local.properties`, en la raíz del proyecto. Ese archivo está en `.gitignore` y nunca se sube:
   ```properties
   mercadopago.publicKey=TEST-xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
   mercadopago.accessToken=TEST-0000000000000000-000000-xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx-000000000
   ```

**2. Exponerlas a la app con `BuildConfig`** (`app/build.gradle.kts`)
```kotlin
import java.util.Properties

val propiedadesLocales = Properties().apply {
    val archivo = rootProject.file("local.properties")
    if (archivo.exists()) archivo.inputStream().use { load(it) }
}

android {
    defaultConfig {
        buildConfigField("String", "MP_PUBLIC_KEY", "\"${propiedadesLocales.getProperty("mercadopago.publicKey", "")}\"")
        buildConfigField("String", "MP_ACCESS_TOKEN", "\"${propiedadesLocales.getProperty("mercadopago.accessToken", "")}\"")
    }
    buildFeatures { buildConfig = true }
}
```

**3. Clases a crear**, en `app/src/main/java/com/motomarket/app/pagos/mercadopago/`:

| Archivo | Responsabilidad |
|---|---|
| `MercadoPagoPasarela.kt` | `class MercadoPagoPasarela(private val activity: ComponentActivity) : PasarelaPagos`. En `procesarPago` crea la preferencia y abre el checkout con `activity.abrirPantallaDePago(contrato, initPoint)`. |
| `PreferenciaMercadoPago.kt` | Crea la preferencia con `POST https://api.mercadopago.com/checkout/preferences` y la cabecera `Authorization: Bearer ${BuildConfig.MP_ACCESS_TOKEN}`. El cuerpo lleva `items`, `payer.email`, `external_reference = orden.referenciaInterna`, `back_urls` (success, failure, pending) y `auto_return = "approved"`. Devuelve el `sandbox_init_point`. |
| `CheckoutMercadoPagoActivity.kt` | Carga el `sandbox_init_point` en un `WebView` y, en `shouldOverrideUrlLoading`, intercepta las `back_urls`. |

Cómo traducir el resultado del checkout en `CheckoutMercadoPagoActivity`:
- Con los parámetros `status` (o `collection_status`) y `payment_id`:
  - `approved` → `APROBADA`, con `referencia = payment_id`
  - `pending` o `in_process` → `PENDIENTE`
  - `rejected` → `RECHAZADA`
- Si el usuario cierra o vuelve atrás → `CANCELADA`.

Además hay que registrar `CheckoutMercadoPagoActivity` en `AndroidManifest.xml`.

**4. Activarla**: en `pagos/Pasarelas.kt` cambia una sola línea:
```kotlin
fun para(activity: ComponentActivity): PasarelaPagos = MercadoPagoPasarela(activity)
```
No hay que modificar ninguna pantalla ni repositorio.

**5. Probar**: inicia sesión en el checkout con el **usuario comprador de prueba** y usa las tarjetas de prueba de Mercado Pago para Colombia. Los nombres del titular `APRO`, `CONT` y `OTHE` simulan aprobado, pendiente y rechazado.

> ⚠️ El *Access Token* es secreto. Tenerlo en `local.properties` solo es aceptable con credenciales **TEST** en este proyecto de curso. En producción la preferencia debe crearse en un backend (por ejemplo, una Cloud Function), y el pago debe confirmarse con los webhooks de Mercado Pago. En ese caso la app solo recibiría el `init_point`.

## Cómo compilar

**Android Studio:**
1. File → Open → selecciona esta carpeta.
2. Espera la sincronización de Gradle.
3. Pulsa ▶ Run en un emulador o dispositivo con Android 8.0 o superior.

Para probar el GPS en el emulador, fija una ubicación en *Extended controls → Location*.

**Terminal** (con `JAVA_HOME` apuntando al JBR de Android Studio):
```bash
./gradlew assembleDebug
```
En Windows: `gradlew.bat assembleDebug`. El APK queda en `app/build/outputs/apk/debug/`.

## Pantallas y flujos por rol

### Autenticación
- **Login:** correo y contraseña con Firebase Auth. Valida que la cuenta esté activa (si está desactivada cierra la sesión) y abre el módulo del rol. Tras el primer ingreso guarda el uid y el correo cifrados, y desde entonces **la huella** reabre la sesión de Firebase vigente.
- **Registro:** crea la cuenta en Auth y el perfil en `usuarios`, siempre como comprador. Con "Quiero vender en MotoMarket" deja además una solicitud de vendedor pendiente. La contraseña debe tener al menos 8 caracteres.
- **Recuperar contraseña:** envía el correo de restablecimiento.
- **Cerrar sesión**, en los tres roles: `signOut` y vuelta al login sin historial.

### Comprador
- **Inicio:** catálogo de productos aprobados con foto. Se filtra por categoría y por texto (nombre o compatibilidad).
- **Detalle:** foto, compatibilidad, vendedor, calificación y stock. Al agregar al carrito valida que no se supere el stock.
- **Carrito:** persiste en Firestore entre sesiones y dispositivos. Permite eliminar con "Deshacer" y vaciar el carrito.
- **Pago seguro:**
  - El botón GPS pide el permiso en tiempo de ejecución, obtiene la ubicación (con la última conocida como respaldo) y la convierte en dirección con Geocoder.
  - Si el permiso se niega o no hay ubicación, se puede escribir la dirección a mano.
  - Se elige tarjeta o PSE y se confirma el pago en la pantalla de la pasarela simulada.
  - Si el pago se aprueba, una **transacción** crea la orden y descuenta el stock; luego se vacía el carrito y se muestra el número de orden.
- **Perfil → Mis pedidos:** historial de órdenes con su estado. Las direcciones usadas se guardan en el perfil.

### Vendedor
- **Mis productos:** sus publicaciones con estado (pendiente, aprobado, rechazado con motivo, agotado). El FAB crea una nueva.
- **Publicar o editar:**
  - Foto con la cámara o desde la galería, comprimida a ≤ 1 MB y subida a Storage.
  - Valida campos obligatorios, precio > 0 y stock ≥ 0.
  - Todo producto guardado o editado vuelve a quedar **pendiente**.
  - Se puede eliminar el producto, junto con su imagen.
- **Pedidos recibidos:** órdenes que contienen sus productos. Al tocar una, avanza de *Por enviar* a *Enviado* y luego a *Entregado*. El banner muestra las ventas del mes.
- **Mi tienda:** nombre, calificación, teléfono, correo y fecha de registro.

### Administrador
- **Panel:** usuarios activos, productos publicados, ventas del mes y % de órdenes pagadas, todo calculado en Firestore.
- **Gestión de usuarios:**
  - Buscador y Switch para activar o desactivar cuentas (el admin no puede desactivarse a sí mismo).
  - Al tocar un usuario se cambia su rol.
  - "Crear usuario" usa una instancia secundaria de Firebase para no cerrar la sesión del admin.
- **Aprobación de productos:** pendientes con ✓ Aprobar o ✗ Rechazar (con motivo) e historial de decisiones. El catálogo solo muestra los aprobados.
- **Reportes:** gráfico de barras con las ventas reales de los últimos 6 meses.

## Prueba de punta a punta

1. **Futuro vendedor:** regístrate marcando "Quiero vender en MotoMarket". Entras como comprador y en *Perfil* ves la solicitud **Pendiente**.
2. **Admin:**
   - En *Gestión de usuarios → Solicitudes de vendedor* aprueba la tienda.
   - El usuario aprobado toca "Ir a mi tienda" en su perfil, publica un producto con foto y en *Mis productos* lo ve **Pendiente**.
   - De vuelta en la cuenta admin, abre *Aprobación de productos*.
   - Aprueba el producto; si quieres, pulsa *Cargar datos de ejemplo*.
   - Revisa el *Panel*.
3. **Comprador:**
   - Regístrate como comprador y busca el producto.
   - Agrégalo al carrito, ve a *Pago seguro*, usa el GPS, elige el método y confirma el pago simulado (2 segundos de procesamiento).
   - Verás el número de orden; el stock bajó y el pedido aparece en *Perfil → Mis pedidos*.
4. **Vendedor:** en *Pedidos recibidos* toca la orden y márcala como *Enviado* y luego *Entregado*. El comprador ve el cambio en su historial.
5. **Admin:**
   - Desactiva al comprador en *Gestión de usuarios*. Al intentar entrar, ese comprador verá "Tu cuenta está desactivada".
   - Revisa *Reportes* y las métricas del *Panel*.
