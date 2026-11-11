# ObraTrack

App móvil para el control de avance, personal y gastos en obras de construcción pequeñas y medianas.
Hecha en **Kotlin + Jetpack Compose** (Android Studio). Todo se guarda de forma local en el celular y funciona sin internet.

## Qué incluye (primera versión completa)

| Requisito | Qué hace la app |
|---|---|
| RF-01 Gestión de obras | Crear, editar y consultar obras (nombre, dirección, fecha de inicio, presupuesto, descripción). |
| RF-02 Avance por etapas | Registrar etapas con estado (pendiente, en proceso, completado) y fecha. |
| RF-03 Foto con ubicación y hora | Toma la foto con la cámara del celular y guarda la hora y las coordenadas GPS. |
| RF-04 Registro de personal | Trabajadores con cargo, tipo de pago (jornal o destajo) y valor. |
| RF-05 Control de asistencia | Asistencia diaria con horas o unidades; consolidado por trabajador y por fecha. |
| RF-07 Registro de gastos | Gastos con descripción, monto, fecha y categoría. |
| RF-08 Foto de facturas | Foto del recibo o factura asociada a cada gasto. |
| RF-09 Balance automático | Gastado, disponible y % ejecutado por categoría y en total, con alertas al 80 % y 100 %. |
| RF-10 Línea de tiempo | Etapas ordenadas por fecha con sus fotos y marca de ubicación. |

Requisitos no funcionales: almacenamiento local, funciona sin conexión, Android 8.0+ (minSdk 26), interfaz grande y en español, código por capas.

## Cómo abrirlo
1. Android Studio → File → Open → carpeta `ObraTrack`.
2. Esperar el Gradle Sync (la primera vez necesita internet para descargar dependencias).
3. Elegir un emulador o celular con Android 8.0 o superior y presionar Run.

Si ya habías instalado la versión anterior, la base local se recrea sola (el esquema cambió).

## Para probar rápido
- En la pantalla inicial, el botón **"Cargar una obra de ejemplo"** crea una obra con etapas, personal, asistencia y gastos.
- **Cámara:** al tocar "Tomar foto" se abre la cámara del celular. En el emulador usa una imagen virtual.
- **GPS en el emulador:** ⋮ (Extended controls) → Location → poner un punto y "Set location". En un celular real, aceptar el permiso de ubicación y tener el GPS activado.
- Si se niega el permiso de ubicación, la foto se guarda igual pero sin coordenadas.

## Estructura
```
app/src/main/java/com/obratrack/app/
├── MainActivity.kt
├── data/            Modelos, Daos y AppDatabase (almacenamiento local)
└── ui/
    ├── Navegacion.kt, ObraViewModel.kt
    ├── Formato.kt, Camara.kt (cámara + GPS), Componentes.kt, FotoVisor.kt
    ├── theme/Theme.kt
    └── screens/
        ├── ObrasScreen, ObraFormScreen           (listado y formulario de obras)
        ├── ObraScreen                            (pantalla de una obra, 5 secciones)
        ├── TabAvance, EtapaFormScreen            (avance, fotos, línea de tiempo)
        ├── TabPersonal, TrabajadorFormScreen     (personal)
        ├── TabAsistencia                         (asistencia)
        ├── TabGastos, GastoFormScreen            (gastos y facturas)
        └── TabBalance                            (balance)
```
