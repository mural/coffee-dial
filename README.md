# Coffee Dial

Diario de espresso offline para Android e iOS. Primera app experimental: una base pequeña para aprender con un producto real y extraer piezas reutilizables cuando aparezca una segunda app.

## Qué incluye

- Home, registro de shot, historial y detalle con ratio de extracción.
- Café/Bean (nombre y tostador) y Shot: dosis, output, tiempo, molienda, temperatura opcional, notas y rating 1–5.
- SQLite local, guardado transaccional, historial más reciente primero y reutilización del mismo café por nombre/tostador.
- Validación de valores, coma decimal, estados vacío/cargando/error y protección ante doble toque de guardar.
- Sin cuenta, conexión de red, backend, analytics ni suscripciones. Los datos no se envían a ningún servicio.

## Stack fijado al 2 de octubre de 2026

| Componente | Versión | Criterio |
|---|---|---|
| Kotlin + Compose compiler | 2.4.20 | Estable; las dos versiones coinciden |
| Compose Multiplatform | 1.12.1 | Estable Android/iOS |
| Android Gradle Plugin | 9.3.1 | Dentro de la matriz validada de Kotlin 2.4.20 |
| Gradle wrapper | 9.5.0 | Mínimo de AGP 9.3, ZIP validado por SHA-256 |
| SQLDelight | 2.4.0 | Estable; SQL tipado y drivers Android/Native |
| Coroutines | 1.11.0 | Estable |
| Activity Compose | 1.13.0 | Estable |
| ktlint CLI | 1.8.0 | Formato sin otro plugin Gradle |
| Android | min 26 / compile 37 / target 37 | SDK estable instalado; Compose exige compile 37 |
| iOS | 16+, dispositivos y simulador Apple Silicon | Integración directa de framework estático |

No elegimos automáticamente el AGP más nuevo: 9.4 aparece en documentación reciente, pero la matriz publicada de Kotlin 2.4.20 valida hasta 9.3.1. Las dependencias están fijadas, sin rangos ni snapshots.

## Abrir y correr Android

1. Abrir esta carpeta en Android Studio y sincronizar Gradle.
2. Usar el JDK incluido con Android Studio como **Gradle JDK**. No hace falta instalar Java aparte.
3. Tener Android SDK Platform 37 y Build Tools 36.0.0. Android Studio puede crear `local.properties`; no se versiona.
4. Seleccionar `androidApp`, un emulador/dispositivo API 26+ y Run.

Desde terminal en esta Mac:

```sh
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
export ANDROID_HOME="$HOME/Library/Android/sdk"
./gradlew :androidApp:assembleDebug
./gradlew :androidApp:installDebug
```

El segundo comando necesita un dispositivo/emulador conectado. APK: `androidApp/build/outputs/apk/debug/androidApp-debug.apk`.

## Abrir y correr iOS

1. Abrir `iosApp/CoffeeDial.xcodeproj` en Xcode.
2. Seleccionar el scheme compartido **CoffeeDial** y un simulador iPhone ARM64.
3. Run. La fase de build invoca Gradle y enlaza `CoffeeDialShared`; la primera ejecución descarga Kotlin/Native.
4. Para un iPhone físico, configurar tu **Team** de Apple y un bundle ID propio en Signing & Capabilities. El simulador no necesita cuenta.

```sh
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
xcodebuild -project iosApp/CoffeeDial.xcodeproj -scheme CoffeeDial \
  -configuration Debug -sdk iphonesimulator \
  -destination 'generic/platform=iOS Simulator' \
  -derivedDataPath iosApp/build/DerivedData CODE_SIGNING_ALLOWED=NO build
```

No se usa CocoaPods, XcodeGen ni Swift Package Manager. El `.xcodeproj` está versionado. Kotlin/Native tiene soporte oficialmente validado hasta Xcode 26.4 en la matriz consultada; esta Mac tiene 27.0. Ver el resultado real y cualquier limitación en [verificaciones](docs/VERIFICATION.md).

## Estructura

```text
androidApp/  Entrada Android, configuración y manifest
shared/
  commonMain/kotlin/com/coffeedial/
    domain/  Bean, Shot, borrador y validación pura
    data/    Repositorio SQLDelight; transacción y Flow
    ui/      Compose: Home, formulario, historial y detalle
  commonMain/sqldelight/  Esquema y consultas SQL
  androidMain/           Creación del driver Android
  iosMain/               Driver Native y UIViewController
  commonTest/            Reglas de dominio
  androidHostTest/       SQLite real en JVM, sin emulador
iosApp/     Entrada SwiftUI y proyecto Xcode
```

Un solo módulo compartido: las carpetas dan separación sin módulos artificiales, framework de DI, capas vacías o una interfaz por clase. Las entradas de plataforma construyen el repositorio una vez. Para una segunda app, extraer convenciones de build, tema o componentes solamente cuando haya uso compartido real.

## Persistencia: por qué SQLDelight

Se compararon SQLDelight 2.4.0 y Room KMP (Room 3.0.3 estable; la guía actual también muestra ejemplos 3.1 alpha, que no adoptamos). Ambos son opciones válidas y usan SQLite.

SQLDelight encaja con dos tablas y consultas pequeñas: deja el esquema explícito, verifica SQL al compilar, genera modelos y tiene drivers nativos y JDBC para tests. No requiere KSP. Room ofrece entidades/DAO por anotaciones, SQLite bundled y herramientas de migración; sería una buena elección para un equipo con mayor inversión en Room. No hay necesidad de migrar de uno a otro ahora. JSON/preferences se descartó para el historial relacional y sus transacciones.

La molienda es texto porque los ajustes varían por molino (`42`, `2.3`, `12 clicks`). Los IDs son UUID y las fechas son instantes UTC; esto no implementa sync. Los límites de entrada son guardrails de producto, no recomendaciones de preparación.

No hay migración destructiva. Antes de cambiar el esquema en una versión instalada, agregar una migración SQLDelight versionada y un fixture de la base anterior, y probar la actualización. Los tests actuales comprueban creación, lectura, reapertura y rechazo de entradas inválidas. No confundirlos con cobertura de una migración futura.

## Calidad

```sh
./gradlew ktlintCheck :shared:testAndroidHostTest :androidApp:lintDebug :androidApp:assembleDebug
./gradlew :shared:verifySqlDelightMigration
./gradlew :shared:linkDebugFrameworkIosSimulatorArm64
./gradlew ktlintFormat
```

`ktlintFormat` modifica formato; los demás verifican/compilan. No se agregó detekt: duplicaría mantenimiento para esta base pequeña. El repositorio no activa CI remoto automáticamente ni consume minutos del usuario.

## Alcance pendiente

Esta es una base de desarrollo, no una publicación en tiendas. Branding e identificadores son provisionales. No incluye edición/borrado, exportación, restauración de datos ni sync. Desinstalar la app elimina la base local; Android tiene el backup automático deshabilitado. Antes de uso como diario único, implementar export/import. Los datos de prueba deben ingresarse desde la UI; la app arranca vacía.

## Fuentes oficiales

- [Matriz Kotlin, Gradle, AGP y Xcode](https://kotlinlang.org/docs/multiplatform/multiplatform-compatibility-guide.html)
- [Compatibilidad Compose](https://kotlinlang.org/docs/multiplatform/compose-compatibility-and-versioning.html)
- [Nuevo plugin Android KMP](https://developer.android.com/kotlin/multiplatform/plugin)
- [Requisitos de AGP 9.3](https://developer.android.com/build/releases/agp-9-3-0-release-notes)
- [SQLDelight 2.4.0](https://github.com/sqldelight/sqldelight/releases/tag/2.4.0)
- [Room KMP](https://developer.android.com/kotlin/multiplatform/room) y [versiones AndroidX](https://developer.android.com/jetpack/androidx/versions)
- [Setup y plugin KMP](https://kotlinlang.org/docs/multiplatform/multiplatform-setup.html)
- [Integración directa iOS](https://kotlinlang.org/docs/multiplatform/multiplatform-direct-integration.html)

Herramientas detectadas y resultado de builds: [docs/VERIFICATION.md](docs/VERIFICATION.md).

## Cuenta (Google / Apple)

Integración opcional preparada; requiere registrar los proveedores antes del primer acceso. Ver [configuración y limitaciones](docs/AUTH.md). Iniciar sesión todavía no activa sincronización.
