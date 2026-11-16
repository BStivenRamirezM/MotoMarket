# MotoMarket: prompt del frontend y funcionamiento

Este documento tiene dos partes:

1. **El prompt completo**, actualizado con todo lo que quedó implementado: las versiones reales, el manejo de edge-to-edge y la eliminación de productos del carrito. Sirve para regenerar o documentar el frontend.
2. **Qué hace la app y cómo funciona**, explicado por capas y por rol.

---

## Parte 1: Prompt completo

```text
Crea desde cero un proyecto Android nativo con Views/XML (NO Jetpack Compose) para una tienda virtual llamada "MotoMarket" (marketplace de repuestos y accesorios para motos en Colombia). Es la entrega de frontend de un curso universitario: solo interfaces navegables con datos de ejemplo, SIN base de datos, SIN Firebase, SIN backend. El diseño debe ser FIEL a los mockups de la entrega anterior, que se describen abajo pantalla por pantalla.

ESTILO DE CÓDIGO: comentarios mínimos — máximo 1-2 comentarios de una línea por archivo, solo donde aporten. Código limpio que se explique solo.

CONFIGURACIÓN:
- Paquete: com.motomarket.app
- Kotlin, minSdk 26, compileSdk/targetSdk 37 (última estable), Gradle 9.8.2 con Kotlin DSL y catálogo de versiones (gradle/libs.versions.toml), ViewBinding habilitado
- Android Gradle Plugin 9.5 con Kotlin integrado (sin aplicar el plugin org.jetbrains.kotlin.android ni el bloque kotlinOptions); compileOptions en Java 17
- Debe compilar con el JDK que trae Android Studio (JBR, Java 25)
- Dependencias: core-ktx, appcompat, material (Material 3), constraintlayout, recyclerview, activity-ktx, fragment-ktx, lifecycle-runtime-ktx, biometric
- Tema heredando de Theme.Material3.DayNight.NoActionBar, con todos los colores de superficie forzados a la paleta propia
- Incluir el Gradle Wrapper (gradlew, gradlew.bat, gradle-wrapper.jar)

EDGE-TO-EDGE (obligatorio con targetSdk 36+):
- Android 16+ ignora la opción de desactivar edge-to-edge. Crear ui/common/BaseActivity (extiende AppCompatActivity) que en onPostCreate: desactiva decorFitsSystemWindows, pone íconos claros en las barras, pinta el fondo de la ventana en gris oscuro #1F2937 y aplica como padding del contenido los insets de systemBars + ime. Así las barras de estado y navegación se ven gris oscuro y el teclado no tapa los formularios.
- Todas las Activities extienden BaseActivity.

IDENTIDAD VISUAL (tomada de los mockups; nada de colores por defecto, es requisito de la rúbrica):
- Primario naranja #F97316, variante oscura #EA580C, tono medio #FB923C, tono claro #FDBA74, naranja suave #FFEDD5
- Secundario gris oscuro #1F2937 (barras de estado, botones secundarios, banners)
- Fondo de contenido #F3F4F6, superficies/tarjetas blancas, bordes #E5E7EB, texto #111111, texto secundario #6B7280
- Chips de estado: verde (#DCFCE7/#15803D) para Activo/Aprobado/Entregado, amarillo (#FEF9C3/#A16207) para Pendiente/En revisión/Por enviar, rojo (#FEE2E2/#B91C1C) para Agotado/Rechazado, naranja (#FFEDD5/#C2410C) para Enviado
- Las pantallas llevan appbar naranja con título blanco en negrita y flecha de regreso cuando aplica
- TODO centralizado: colors.xml, strings.xml (ni un texto quemado en layouts), dimens.xml, themes.xml y styles.xml con estilos propios: MotoMarket.Button (naranja), MotoMarket.Button.Secondary (gris oscuro), MotoMarket.Button.Outlined, MotoMarket.Button.Link, MotoMarket.Card, MotoMarket.Chip, MotoMarket.ChipCategoria, MotoMarket.Campo, MotoMarket.Campo.Buscador, MotoMarket.Label, MotoMarket.Toolbar, MotoMarket.BottomNav, MotoMarket.NavDrawer, MotoMarket.Fab y una familia MotoMarket.Text.* (Titulo, Subtitulo, Nombre, Secundario, Precio, PrecioGrande, Metrica, Seccion, Enlace)
- Drawables XML propios: botón redondeado naranja con ripple (radio 12dp), botón gris oscuro, fondo blanco redondeado para tarjetas, campo con borde, buscador redondeado, chips de estado (4 colores), placeholder gris de producto con ícono, círculos naranja, banner oscuro, opción de pago seleccionable (selector con borde naranja), barra de gráfico con esquinas superiores redondeadas
- Íconos vectoriales propios (moto, huella, carrito, papelera, etc.) e ícono adaptativo de la app con la moto naranja sobre gris oscuro

ARQUITECTURA PENSADA PARA EL FUTURO (luego se conectará Firebase sin reescribir pantallas):
- Paquete models/ con data classes: Producto(id, nombre, precio, categoria, marcaModeloCompatible, cilindraje, stock, vendedor, calificacionVendedor, estado), Usuario(id, nombre, correo, rol, activo), Tienda, Pedido(id, producto, valor, estado), ResumenVentas, ItemCarrito(producto, cantidad) con subtotal calculado, Envio, ResumenCarrito (subtotal y total calculados), Moderacion, MetricasAdmin, VentaMensual. Enums: Categoria, EstadoProducto, EstadoPedido, Rol.
- Paquete data/ con interfaces ProductoRepository, UsuarioRepository, PedidoRepository, CarritoRepository (obtenerResumen, agregar, eliminar, restaurar, vaciar) y ReporteRepository, todas con funciones suspend. Implementaciones FakeXxxRepository en data/fake/ que leen de un object DatosEjemplo y guardan cambios en memoria.
- object Repositorios en data/ entrega cada repositorio tipado como su interfaz: es el único punto a cambiar para usar Firebase. Las Activities/Fragments consumen SIEMPRE la interfaz con lifecycleScope / viewLifecycleOwner.lifecycleScope, nunca DatosEjemplo directo.
- util/Formato.kt: formatearCOP(valor: Int) → "$145.000", formatearMiles, formatearMillones, formatearCOPCompacto → "$18,4M", formatearCalificacion → "4,8"
- ui/common: BaseActivity, extensiones para pintar chips de estado según el enum (mostrarEstado), navegación por rol (abrirInicioPorRol, cerrarSesion), mostrarMensaje (Toast), habilitar(boolean) para botones, EditText.texto() e iniciales(nombre)

DATOS DE EJEMPLO (exactos, tomados de los mockups — usarlos tal cual):
- Productos catálogo: Pastillas de freno NKD 125 $38.000 (Frenos) · Kit de arrastre FZ 2.0 $145.000 (Transmisión, compatibilidad "Yamaha FZ 2.0 / FZN 150 (2014-2024)", vendedor "MotoRepuestos JD" calificación 4,8, stock 12) · Casco certificado DOT $210.000 (Cascos) · Llanta trasera 130/70-17 $185.000 (Llantas, stock 4) · Espejos NKD originales $45.000 (stock 0, Agotado) · Cadena reforzada 428H $68.000 (estado En revisión) · Aceite 20W-50 semisintético $42.000 (Aceites). Kit, Llanta, Espejos y Cadena son de MotoRepuestos JD.
- El catálogo del comprador solo muestra productos Activos o Agotados (no los que están en revisión).
- Carrito precargado: Kit de arrastre FZ 2.0 x1 ($145.000) + Pastillas de freno NKD x2 ($76.000) → Subtotal $221.000, Envío (Soacha) $9.000, Total $230.000
- Pedidos del vendedor: #1042 Kit arrastre $145.000 "Por enviar" · #1038 Llanta 130/70 $185.000 "Enviado" · #1031 Pastillas freno $76.000 "Entregado"; resumen "Ventas del mes: $1.250.000 · 14 pedidos"
- Usuarios (admin): Carlos Pérez (Vendedor, activo) · Laura Gómez (Compradora, activa) · Andrés Rojas (Vendedor, inactivo) · Diana León (Compradora, activa). Administrador: admin@motomarket.co
- Tienda del vendedor: MotoRepuestos JD, propietario Carlos Pérez, calificación 4,8, Soacha, Cundinamarca
- Direcciones del comprador: "Cra 7 # 12-34, Soacha · Casa" y "Cl 45 # 10-20, Bogotá · Trabajo"
- Métricas dashboard admin: 1.248 usuarios activos · 356 productos publicados · $18,4M ventas del mes · 97% pagos exitosos
- Ventas de los últimos 6 meses (millones): May 9,8 · Jun 11,2 · Jul 12,9 · Ago 14,6 · Sep 16,3 · Oct 18,4
- Moderación (admin): Cadena reforzada 428H (vendedor MotoRep JD) Pendiente con botones Aprobar/Rechazar · Casco sin certificación Rechazado ("Rechazado: normativa") · Kit arrastre FZ Aprobado 05/10

PANTALLAS (fieles a los mockups):

Transversales (ui/auth):
1. SplashActivity: fondo gris oscuro #1F2937, logo de moto y nombre "MotoMarket" en naranja, eslogan, 2 segundos → Login
2. LoginActivity: tarjeta blanca "Bienvenido de nuevo", círculo con ícono de huella (botón que dispara BiometricPrompt real de androidx.biometric con BIOMETRIC_WEAK; si el equipo no tiene huella muestra un aviso), campos correo y contraseña con validación, botón gris oscuro "Iniciar sesión" (busca el correo entre los usuarios activos), enlaces a registro y recuperar contraseña; tres botones temporales para entrar como Comprador/Vendedor/Admin mientras no hay backend
3. RecuperarPasswordActivity: campo correo validado + botón enviar con mensaje de confirmación (Toast)
4. RegistroActivity: nombre, correo, teléfono, contraseña, selector de rol comprador/vendedor; al crear la cuenta entra al módulo del rol elegido

Comprador — CompradorMainActivity con BottomNavigationView (Inicio, Carrito, Perfil):
5. InicioFragment: appbar naranja "MotoMarket" con ícono de moto, buscador redondeado "Buscar repuesto, marca o modelo..." que filtra en vivo por nombre o compatibilidad, chips de categorías (Todo seleccionado, Frenos, Llantas, Cascos, Aceites), RecyclerView en grilla de 2 columnas con tarjetas (placeholder gris, nombre en negrita, precio naranja en negrita, chip "Agotado" si aplica) y mensaje cuando no hay resultados. Tocar un producto abre el detalle.
6. DetalleProductoActivity ("Detalle del producto", con flecha atrás): imagen placeholder grande, nombre centrado, precio naranja grande, tarjetas de Compatibilidad (con cilindraje), Vendedor con ⭐ y calificación, Stock ("12 unidades disponibles" o "Agotado"), botón naranja "🛒 Agregar al carrito" (deshabilitado si no hay stock; si el producto ya está en el carrito suma 1 a la cantidad)
7. CarritoFragment ("Mi carrito"): lista de items (ícono, nombre, "Cant: X · $precio") con botón de papelera rojo en cada ítem para eliminarlo, que muestra un Snackbar "X eliminado del carrito" con acción "Deshacer" (devuelve el ítem a su posición); ícono de papelera en la appbar para "Vaciar carrito" con diálogo de confirmación (se oculta si el carrito está vacío); filas Subtotal / Envío (Soacha) / Total en negrita que se recalculan; mensaje "Tu carrito está vacío"; botón naranja "Ir a pagar" → PasarelaPagoActivity, deshabilitado si el carrito está vacío
8. PasarelaPagoActivity ("Pago seguro", flecha atrás): sección "Dirección de entrega" con tarjeta "📍 Mi ubicación actual (GPS) — Cra 7 # 12-34, Soacha" (decorativa), sección "Método de pago" con dos tarjetas seleccionables (Tarjeta crédito/débito y PSE, con etiqueta "Elegir" que cambia a "✓ Elegido" y borde naranja), banner gris oscuro "Procesado por pasarela externa — Pago 100% seguro" con la segunda línea en naranja, botón "Confirmar pago $230.000" (total calculado del carrito) que exige elegir un método y muestra diálogo de éxito; al aceptar vacía el carrito y vuelve
9. PerfilFragment: foto placeholder, nombre, correo, chip del rol, direcciones y botón cerrar sesión (vuelve al login limpiando la pila)

Vendedor — VendedorMainActivity con BottomNavigationView (Mis productos, Pedidos, Perfil):
10. MisProductosFragment ("Mis productos"): lista de productos de la tienda con chip de estado (Kit arrastre Activo stock 12 · Llanta Activo stock 4 · Espejos Agotado stock 0 · Cadena 428H "Revisión" con "Pendiente de aprobación") + FAB naranja "+" → PublicarProductoActivity; la lista se recarga al volver
11. PublicarProductoActivity ("Publicar producto", flecha atrás): zona superior con placeholder y botón "📷 Tomar foto con la cámara" (ActivityResultContracts.TakePicturePreview, muestra la miniatura; aviso si no hay cámara), campos Nombre, Marca/Modelo compatible, Precio y Stock en la misma fila, chips de categoría, botón naranja "Publicar producto". El producto se guarda "En revisión" y además se crea una solicitud pendiente en la moderación del admin.
12. PedidosFragment ("Pedidos recibidos"): lista de pedidos con número, producto, valor y chip de estado; banner gris oscuro "Ventas del mes — $1.250.000 · 14 pedidos" con la cifra en naranja
13. PerfilVendedorFragment ("Mi tienda"): datos de la tienda (MotoRepuestos JD, ⭐ 4,8, propietario, correo, ciudad) y cerrar sesión

Administrador — AdminMainActivity con NavigationDrawer (header naranja con logo y correo admin@motomarket.co leído del repositorio; ítems: Panel, Gestión de usuarios, Aprobación de productos, Reportes, Cerrar sesión). El título de la appbar cambia según la sección y el botón atrás cierra el drawer si está abierto:
14. DashboardFragment ("Panel de administración"): grilla 2x2 de tarjetas de métricas (cifra naranja grande + etiqueta gris) y debajo tres accesos con ícono: Gestión de usuarios, Gestión de productos, Reportes y auditoría, que navegan a cada sección
15. GestionUsuariosFragment ("Gestión de usuarios"): buscador por nombre o correo, texto auxiliar "Activar / desactivar cuentas y asignar roles", lista de usuarios (avatar con iniciales, nombre en negrita, "Rol · Activo/Inactivo" en gris, MaterialSwitch naranja que activa/desactiva y avisa con Toast), botón "Crear usuario" que abre un diálogo con formulario validado (nombre, correo, rol)
16. GestionProductosFragment ("Aprobación de productos"): sección "Pendientes de aprobación" con tarjetas y botones "✓ Aprobar" (naranja) y "✗ Rechazar" (gris oscuro); al decidir, el chip cambia y la tarjeta pasa a la sección "Historial" con la fecha (rechazado por normativa, aprobado 05/10)
17. ReporteVentasFragment ("Reportes"): título "Ventas últimos 6 meses", gráfico de barras hecho con Views a partir del repositorio (6 barras con altura proporcional al valor, etiqueta de valor y mes, tonos #FDBA74→#FB923C→#F97316→#EA580C), y dos tarjetas: "Auditoría de accesos — Registro de acciones por rol" y "Exportar reporte — PDF / Excel" (avisan que estarán disponibles al conectar el backend)

CALIDAD (rúbrica):
- Nomenclatura consistente: activity_*.xml, fragment_*.xml, item_*.xml, dialog_*.xml, nav_header_*.xml, menu_*.xml; carpeta ui/ organizada por rol (ui/comprador, ui/vendedor, ui/admin, ui/auth) + ui/common
- RecyclerViews con ListAdapter, DiffUtil y ViewHolders usando ViewBinding
- Fragments con binding nullable liberado en onDestroyView
- Todos los textos en strings.xml en español, con plurals para cantidades
- README.md: descripción, tecnologías, estructura de carpetas, pantallas por rol, paleta de colores, capa de repositorios (y nota de que permitirá conectar Firebase después), instrucciones para compilar, datos de prueba
- .gitignore apropiado para Android (excluye build/, .gradle/, .kotlin/, local.properties, .idea/)

Al final: compila con ./gradlew assembleDebug y corrige hasta que no haya errores ni warnings. Entrega los comandos git para que YO haga commits incrementales por etapa (configuración, identidad visual, capa de datos, autenticación, comprador, vendedor, administrador); no hagas commit ni push tú.
```

---

## Parte 2: Qué hace la app y cómo funciona

### Resumen

MotoMarket es una app Android que simula un marketplace de repuestos para motos con **tres roles**: comprador, vendedor y administrador. Toda la información sale de datos de ejemplo en memoria, pero las pantallas ya están construidas como si hubiera un backend real. Cuando se conecte Firebase, solo se reemplaza la capa de datos.

### Arquitectura por capas

```
Pantallas (Activities / Fragments)          ui/auth · ui/comprador · ui/vendedor · ui/admin
        │  usan solo interfaces, con corrutinas
        ▼
Repositorios (interfaces suspend)           data/ProductoRepository, UsuarioRepository, ...
        │  Repositorios.kt decide qué implementación se usa
        ▼
Implementaciones Fake (en memoria)          data/fake/Fake*Repository
        │
        ▼
DatosEjemplo (datos de los mockups)         data/fake/DatosEjemplo
```

| Capa | Qué hace |
|---|---|
| `models/` | Define los datos: `Producto`, `Usuario`, `Pedido`, `ItemCarrito`, etc. Algunos valores se calculan solos: el subtotal de un ítem es precio × cantidad, y el total del carrito es subtotal + envío. |
| `data/` (interfaces) | Lo que las pantallas pueden pedir: "dame el catálogo", "elimina este producto del carrito", "aprueba esta publicación"... Las funciones son `suspend`, listas para llamadas de red. |
| `data/fake/` | La versión de ejemplo de cada repositorio. Copia `DatosEjemplo` a listas en memoria y guarda ahí los cambios mientras la app esté abierta. |
| `data/Repositorios.kt` | Un solo lugar que dice qué implementación usar. Para Firebase basta con cambiar `FakeProductoRepository()` por `FirebaseProductoRepository()`. |
| `util/Formato.kt` | Convierte números al formato colombiano: `145000` → `$145.000`, `18400000` → `$18,4M`, `4.8` → `4,8`. |
| `ui/common/` | Lo que comparten todas las pantallas: `BaseActivity` (barras del sistema), colores de los chips según el estado, navegación por rol y cierre de sesión. |

### Flujo general

1. **Splash (2 s) → Login.**
2. En el login hay cuatro formas de entrar:
   - **Correo y contraseña:** busca el correo entre los usuarios activos (la contraseña aún no se valida) y abre el módulo de su rol.
   - **Huella:** usa el lector biométrico real del dispositivo; si la verificación es correcta, entra como comprador.
   - **Botones de prueba** Comprador / Vendedor / Admin: entran directo a cada módulo.
   - **Registro:** crea el usuario y entra al módulo del rol elegido.
3. **Cerrar sesión**, en cualquier rol, vuelve al login y borra el historial de pantallas, así que "atrás" no regresa a la sesión anterior.

### Comprador

| Pantalla | Cómo funciona |
|---|---|
| **Inicio** | Pide al repositorio los productos según el texto del buscador y el chip de categoría. Se actualiza mientras escribes o cambias de chip. Los productos "En revisión" no aparecen; los agotados sí, con su chip rojo. |
| **Detalle** | Recibe el id del producto y lo consulta. "Agregar al carrito" suma una unidad o crea el ítem. Si el stock es 0, el botón queda deshabilitado. |
| **Carrito** | Se recarga cada vez que se muestra, así refleja lo agregado desde el detalle. La **papelera de cada ítem** lo elimina y muestra "Deshacer", que lo devuelve a su misma posición. La **papelera de la barra** vacía todo tras confirmar. Subtotal, envío y total se recalculan siempre; con el carrito vacío, "Ir a pagar" se deshabilita. |
| **Pago seguro** | Calcula el total desde el carrito. Hay que elegir Tarjeta o PSE (la opción elegida queda resaltada) antes de confirmar. Al aceptar el diálogo de éxito, el carrito se vacía y vuelves a la pestaña Carrito, ya vacía. |
| **Perfil** | Muestra el usuario comprador de ejemplo (Laura Gómez), su rol y sus direcciones. |

### Vendedor

| Pantalla | Cómo funciona |
|---|---|
| **Mis productos** | Lista los productos de la tienda MotoRepuestos JD con su estado. Se recarga al volver de publicar. |
| **Publicar producto** | Abre la cámara del sistema y muestra la miniatura de la foto. Valida los campos y guarda el producto "En revisión". Al mismo tiempo crea una **solicitud pendiente** para el administrador: vendedor y admin quedan conectados. |
| **Pedidos** | Muestra los pedidos con su estado y el resumen de ventas del mes. |
| **Mi tienda** | Datos de la tienda y cierre de sesión. |

### Administrador

| Pantalla | Cómo funciona |
|---|---|
| **Menú lateral** | Cambia de sección y actualiza el título. El correo del encabezado se lee del repositorio. |
| **Panel** | Muestra las 4 métricas formateadas; las tarjetas de acceso llevan a cada sección. |
| **Gestión de usuarios** | El buscador filtra en vivo. El switch activa o desactiva la cuenta: un usuario inactivo ya no puede iniciar sesión con su correo. "Crear usuario" valida nombre y correo y agrega el usuario a la lista. |
| **Aprobación de productos** | Separa las solicitudes pendientes del historial. Aprobar o Rechazar cambia el estado, registra la fecha de hoy y mueve la tarjeta al historial. Lo publicado por el vendedor aparece aquí. |
| **Reportes** | Dibuja el gráfico con una barra por mes, con altura proporcional al mes de mayor venta. Auditoría y exportación muestran un aviso porque dependen del backend. |

### Detalles técnicos que conviene saber

- **Los datos viven en memoria:** al cerrar y abrir la app, todo vuelve a los datos de ejemplo (carrito precargado, usuarios, moderación). Es lo esperado sin base de datos.
- **Edge-to-edge:** con Android 16+ el sistema dibuja la app detrás de las barras. `BaseActivity` aparta el contenido y deja las barras en gris oscuro, y el teclado no tapa los campos de los formularios.
- **Sin textos fijos en los layouts:** todos los textos están en `strings.xml`, incluidos los plurales ("1 unidad" / "12 unidades").
- **Listas eficientes:** todas usan `ListAdapter` + `DiffUtil`, así que solo se redibujan los ítems que cambian (por ejemplo, al eliminar del carrito o activar un usuario).
- **Para conectar Firebase:** crear `FirebaseXxxRepository` que implemente cada interfaz y cambiar `Repositorios.kt`. Ninguna pantalla se modifica.
