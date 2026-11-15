# MotoMarket

Marketplace de repuestos y accesorios para motos en Colombia. Este repositorio contiene la **entrega de frontend** del proyecto: una app Android nativa con interfaces navegables y datos de ejemplo, fiel a los mockups de la entrega anterior.

> Esta versión **no usa base de datos, Firebase ni backend**. Toda la información sale de un repositorio en memoria (`DatosEjemplo`), detrás de interfaces pensadas para cambiar a Firebase sin reescribir pantallas.

## Tecnologías

| Elemento | Versión / detalle |
|---|---|
| Lenguaje | Kotlin (integrado en AGP 9) |
| UI | Views + XML con ViewBinding (sin Jetpack Compose) |
| Componentes | Material Components 1.14 (Material 3), RecyclerView, ConstraintLayout, DrawerLayout |
| Autenticación biométrica | `androidx.biometric` (BiometricPrompt) |
| Asincronía | Corrutinas (`lifecycleScope`) con repositorios `suspend` |
| SDK | minSdk 26 · compile/targetSdk 37 (edge-to-edge manejado en `BaseActivity`) |
| Build | AGP 9.5 · Gradle 9.8.2 con Kotlin DSL y catálogo de versiones (`gradle/libs.versions.toml`) |
| JDK | El JBR incluido en Android Studio (Java 25) |

## Estructura de carpetas

```
app/src/main/
├── java/com/motomarket/app/
│   ├── models/                 Data classes: Producto, Usuario, Pedido, ItemCarrito, Moderacion, Reportes
│   ├── data/                   Interfaces de repositorio + Repositorios (punto único de inyección)
│   │   └── fake/               DatosEjemplo y las implementaciones Fake*Repository
│   ├── util/                   Formato.kt → formatearCOP(), formatearCalificacion(), etc.
│   └── ui/
│       ├── common/             Chips de estado, navegación por rol, extensiones de vistas
│       ├── auth/               Splash, Login, Registro, Recuperar contraseña
│       ├── comprador/          Inicio, Detalle, Carrito, Pasarela de pago, Perfil
│       ├── vendedor/           Mis productos, Publicar, Pedidos, Perfil de tienda
│       └── admin/              Panel, Usuarios, Aprobación de productos, Reportes
└── res/
    ├── layout/                 activity_*.xml · fragment_*.xml · item_*.xml · dialog_*.xml · nav_header_*.xml
    ├── drawable/               Íconos vectoriales y formas propias (botones, tarjetas, chips, placeholder)
    ├── color/                  Selectores de color (chips, navegación)
    ├── menu/                   Bottom navigation (comprador, vendedor) y drawer (admin)
    └── values/                 colors.xml · strings.xml · dimens.xml · themes.xml · styles.xml
```

## Pantallas por rol

### Transversales (`ui/auth`)
1. **Splash** — fondo gris oscuro con el logo naranja, 2 segundos y pasa al login.
2. **Login** — tarjeta "Bienvenido de nuevo", botón de huella con `BiometricPrompt` real, correo y contraseña, enlaces a registro y recuperación. Incluye tres accesos temporales (Comprador / Vendedor / Admin) mientras no hay backend.
3. **Recuperar contraseña** — correo + envío con confirmación por Toast.
4. **Registro** — nombre, correo, teléfono, contraseña y tipo de cuenta (comprador o vendedor).

### Comprador (`CompradorMainActivity`, bottom navigation)
5. **Inicio** — buscador, chips de categorías (Todo, Frenos, Llantas, Cascos, Aceites) y grilla de 2 columnas.
6. **Detalle del producto** — precio, compatibilidad, vendedor con calificación, stock y "Agregar al carrito".
7. **Mi carrito** — ítems, subtotal, envío (Soacha) y total; botón "Ir a pagar".
8. **Pago seguro** — dirección por GPS (decorativa), selección de tarjeta o PSE, banner de pasarela externa y diálogo de pago exitoso.
9. **Perfil** — datos del usuario, direcciones y cierre de sesión.

### Vendedor (`VendedorMainActivity`, bottom navigation)
10. **Mis productos** — lista con chip de estado (Activo, Agotado, Revisión) y FAB para publicar.
11. **Publicar producto** — foto con la cámara del dispositivo (miniatura), nombre, compatibilidad, precio, stock y categoría.
12. **Pedidos recibidos** — pedidos con estado (Por enviar, Enviado, Entregado) y banner de ventas del mes.
13. **Mi tienda** — MotoRepuestos JD, calificación 4,8, datos de contacto y cierre de sesión.

### Administrador (`AdminMainActivity`, navigation drawer)
14. **Panel de administración** — 4 métricas y accesos rápidos.
15. **Gestión de usuarios** — buscador, activar/desactivar con Switch y diálogo para crear usuarios.
16. **Aprobación de productos** — pendientes con Aprobar/Rechazar e historial de decisiones.
17. **Reportes** — gráfico de barras de ventas de 6 meses hecho con Views, auditoría y exportación.

## Paleta de colores

| Uso | Color |
|---|---|
| Primario (naranja) | `#F97316` |
| Primario oscuro | `#EA580C` |
| Naranja claro | `#FDBA74` |
| Secundario (gris oscuro) | `#1F2937` |
| Fondo de contenido | `#F3F4F6` |
| Superficies | `#FFFFFF` |
| Texto principal / secundario | `#111111` / `#6B7280` |
| Chip verde (Activo, Aprobado, Entregado) | `#DCFCE7` / `#15803D` |
| Chip amarillo (Pendiente, Revisión, Por enviar) | `#FEF9C3` / `#A16207` |
| Chip rojo (Agotado, Rechazado) | `#FEE2E2` / `#B91C1C` |
| Chip naranja (Enviado) | `#FFEDD5` / `#C2410C` |

Todos los colores, textos, dimensiones y estilos están centralizados en `res/values`. Los estilos propios (`MotoMarket.Button`, `MotoMarket.Button.Secondary`, `MotoMarket.Card`, `MotoMarket.Chip`, `MotoMarket.Campo`, `MotoMarket.Toolbar`, etc.) están en `styles.xml`.

## Capa de repositorios

Las pantallas **nunca** leen `DatosEjemplo` directamente: siempre consumen una interfaz.

```
ProductoRepository   UsuarioRepository   PedidoRepository   CarritoRepository   ReporteRepository
        │                    │                   │                   │                  │
FakeProductoRepository  FakeUsuarioRepository  ...  (leen de DatosEjemplo)
```

Las instancias se entregan desde `data/Repositorios.kt`. Todas las funciones son `suspend`, así que para conectar **Firebase** más adelante basta con:

1. Crear, por ejemplo, `FirebaseProductoRepository : ProductoRepository` usando Firestore con `await()`.
2. Cambiar la línea correspondiente en `Repositorios.kt`.

Ninguna Activity ni Fragment necesita modificarse.

## Cómo compilar

**Con Android Studio (recomendado):**
1. Abrir la carpeta del proyecto con *File → Open*.
2. Esperar la sincronización de Gradle (descarga Gradle 9.8.2 y las dependencias automáticamente).
3. Ejecutar la configuración `app` en un emulador o dispositivo con Android 8.0 o superior.

**Desde la terminal** (con `JAVA_HOME` apuntando al JBR de Android Studio y el Android SDK con la plataforma 37):

```bash
./gradlew assembleDebug
```

En Windows se usa `gradlew.bat assembleDebug`. El APK queda en `app/build/outputs/apk/debug/`.

## Datos de prueba

- Accesos rápidos: botones **Comprador**, **Vendedor** y **Admin** en el login.
- Login con correo: cualquier usuario activo de los datos de ejemplo, por ejemplo `laura.gomez@correo.co` o `admin@motomarket.co` (la contraseña no se valida en esta versión).
- La huella requiere un dispositivo o emulador con huella registrada; al validarla entra como comprador.
