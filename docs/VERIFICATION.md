# Verificación y entorno

Fecha: 2 de octubre de 2026. Inspección realizada antes de crear el scaffold. Los archivos sincronizados de `sources/` no se modificaron.

## Estado

Scaffold publicado en `main` mediante GitHub Desktop: commit `af194d6`. SHA remoto verificado y clon local sincronizado, sin builds ni archivos locales versionados.

Ubicación de trabajo elegida por el usuario: `/Users/agustin/Documents/GitHub/coffee-dial`. Android, tests, lint, formato, esquema SQLite y build iOS se volvieron a verificar correctamente desde este clon. Se restauraron `.gitignore` y `.editorconfig`, que no se habían copiado con Finder. Los builds y archivos locales quedan excluidos de Git.

Base mínima implementada para Android e iOS con lógica, persistencia y UI compartidas. Repositorio indicado por el usuario: `mural/coffee-dial`, público (creado y renombrado por el usuario). No se creó otro repositorio ni se cambió su visibilidad.

## Herramientas del usuario

| Herramienta | Estado | Evidencia / acción |
|---|---|---|
| macOS Apple Silicon | OK | macOS 27.0.1 |
| Android Studio | OK | 2026.2; build AI-262.9437.185.2621.16467767 |
| JDK de Android Studio | OK | JBR/OpenJDK 25.0.3; utilizado para los builds |
| Java del sistema | OK, no requiere cambio | Zulu 22.0.2; no fue el JDK usado. Elegir JBR en Gradle JDK |
| SDK Android | OK | Platform 37.0 y Build Tools 36.0.0 ya instalados; compile/target 37 |
| Android Platform Tools | OK | `adb` disponible |
| Android SDK command-line tools | No instalar para este MVP | `sdkmanager` no está en PATH; se puede usar SDK Manager del IDE |
| NDK / CMake | No instalar ni actualizar | La app no compila código C/C++ propio para Android |
| Kotlin Multiplatform IDE plugin | OK | `kmm-plugin` 262.9437.115-AS, compatible con IDE 262.* |
| Plugin Compose separado | No instalar | El flujo del IDE está cubierto por KMP; los plugins de compilación viven en Gradle |
| Kotlin / Compose | OK | Descargados por Gradle con versiones fijadas; no hay instalación global requerida |
| Gradle | OK | Wrapper 9.5.0 incluido con checksum. No hace falta Gradle global |
| Xcode | OK en build local; fuera de matriz oficial | 27.0 (27A266a); framework y app de simulador ARM64 compilaron. Matriz Kotlin publicada: Xcode 26.4 |
| Runtime de simulador | OK | iOS 27.0 instalado; app instalada y proceso iniciado en iPhone 18 Pro |
| CocoaPods | No instalar | Integración directa Xcode–Gradle; `pod` no está en PATH |
| Git | OK | 2.54.0; identidad de commits configurada |
| GitHub conector | Lectura OK; escritura bloqueada | Cuenta `mural`, metadata muestra permiso push, pero crear un blob devuelve HTTP 403 `Resource not accessible by integration` |
| Git local por HTTPS | Configurar si querés push desde terminal | `git push --dry-run` falló por falta de credencial local. Esto no bloquea los builds. La publicación se completó con la sesión existente de GitHub Desktop |
| GitHub CLI (`gh`) | Opcional, no instalar ahora | No está en PATH. GitHub Desktop está instalado y es otra opción para autenticar/publicar manualmente |

No hay instalaciones o actualizaciones obligatorias pendientes para compilar en esta Mac. Se descargaron dependencias de proyecto, Gradle 9.5 y Kotlin/Native/LLVM a sus cachés normales. No se reemplazaron Android Studio, Xcode o Java, ni se instalaron plugins del IDE.

## Comandos ejecutados y resultados

Se usó `JAVA_HOME` apuntando al JBR de Android Studio.

| Verificación | Resultado final |
|---|---|
| `./gradlew ktlintFormat` | OK; formato aplicado |
| `./gradlew ktlintCheck` | OK |
| `./gradlew :shared:testAndroidHostTest` | OK: 6 tests, 0 fallos (4 dominio, 2 SQLite/JDBC) |
| `./gradlew :shared:generateCommonMainCoffeeDatabaseSchema` | OK; baseline vacío `1.db` versionado |
| `./gradlew :shared:verifySqlDelightMigration` | OK; verifica el esquema inicial contra baseline. Aún no existen migraciones entre versiones |
| `./gradlew :androidApp:lintDebug` | OK; 0 errores y 0 advertencias después de regenerar el informe |
| `./gradlew :androidApp:assembleDebug` | OK; APK debug generado |
| `./gradlew :shared:linkDebugFrameworkIosSimulatorArm64` | OK; framework estático ARM64 |
| `xcodebuild -project iosApp/CoffeeDial.xcodeproj -scheme CoffeeDial -configuration Debug -sdk iphonesimulator -destination 'generic/platform=iOS Simulator' -derivedDataPath iosApp/build/DerivedData CODE_SIGNING_ALLOWED=NO build` | BUILD SUCCEEDED |
| `xcrun simctl install … CoffeeDial.app` + `xcrun simctl launch … com.coffeedial.app` | OK; proceso iniciado en simulador iOS 27 |

APK: `androidApp/build/outputs/apk/debug/androidApp-debug.apk`.
App iOS: `iosApp/build/DerivedData/Build/Products/Debug-iphonesimulator/CoffeeDial.app`.
Tests: `shared/build/reports/tests/testAndroidHostTest/`.
Lint: `androidApp/build/reports/lint-results-debug.html`.

## Ajustes encontrados y resueltos

- Compose 1.12.1 exige compile SDK 37. Se usó el SDK ya instalado y se actualizó target SDK a 37.
- ktlint CLI requiere seleccionar su variante completa con dependencias en Gradle 9.5.
- El destino genérico de Xcode incluyó x86_64 inicialmente. El proyecto declara ARM64, consistente con esta Mac y los targets KMP soportados.
- SQLDelight requiere un baseline `.db` para verificar migraciones: se generó y versionó el esquema inicial sin datos. Generar schemas es una operación explícita; no debe regenerarse `1.db` para ocultar cambios futuros del esquema.
- Se agregó icono provisional Android y reglas explícitas de exclusión de backup/transferencia Android.
- JDK 25 emite advertencias de APIs de acceso nativo/deprecadas de ktlint y SQLite JDBC. No impidieron las verificaciones; no se ocultaron.

## Límites de lo verificado

- Arranque en simulador iOS confirmado; no se realizó una prueba manual completa de todas las pantallas, accesibilidad o teclado.
- Android compilado y analizado; no se instaló ni recorrió en un dispositivo/emulador Android.
- Tests de SQLite ejecutados sobre JDBC en el host. No se afirma cobertura de persistencia instrumentada en Android o del driver iOS.
- No se compiló ni firmó para un iPhone físico, no se archivó Release ni se publicó en tiendas. Esos pasos requieren selección de Team/bundle ID y validación adicional.
- La matriz oficial de Kotlin aún no incluye Xcode 27. El build local es evidencia de esta combinación concreta, no una garantía para todas las APIs/targets.
- iOS usa el botón explícito Volver; no hay gesto interactivo de navegación implementado. Android maneja el botón/gesto Atrás y conserva el borrador durante navegación/recreación normal.
- Fechas mostradas en UTC en esta primera iteración. Edición, borrado y export/import están pendientes.

## Cuentas externas

**No hacen falta ahora:** Google Cloud, Firebase, Supabase, servidor, dominio, analytics, proveedor de auth, Play Console ni membresía paga Apple Developer para simulador. GitHub es solo alojamiento del código, no dependencia de la app.

**Después, según el objetivo:** Google Play Console para distribuir por Play; Apple ID/Team para dispositivo físico y Apple Developer Program para App Store/TestFlight; un backend solo si decidimos agregar sync. Elegir un proveedor en ese momento, sin crear cuentas por anticipado.

## Próximos tres pasos

1. Abrir ambos proyectos y registrar 5–10 shots reales; revisar teclado, navegación, unidades y legibilidad.
2. Agregar edición/borrado y export/import local para que el diario sea recuperable antes de usarlo como registro único.
3. Añadir un smoke test de UI por plataforma y CI de build/tests; extraer una base reusable solo cuando una segunda app confirme qué componentes comparten.

## Corrección de runtime iOS

Se corrigió un SIGABRT en `PlistSanityCheck.ios.kt`: Xcode no trasladaba el build setting personalizado `INFOPLIST_KEY_CADisableMinimumFrameDurationOnPhone` al plist generado. Compilar y obtener un PID no detectaba el error porque Compose lo lanza de forma asíncrona.

Ahora Debug y Release usan `iosApp/Info.plist` como entrada explícita, con `CADisableMinimumFrameDurationOnPhone` de tipo booleano `true`. Se conserva la validación estricta de Compose.

Verificado después del cambio: `xcodebuild` exitoso, lectura del booleano en el plist de la app compilada, reinstalación en iPhone 18 Pro / iOS 27 y proceso vivo tras 28 segundos (estado de launchctl activo, sin salida por abort). No equivale a una prueba manual de todo el flujo.

Chequeo de regresión después de compilar: `python3 scripts/check-ios-plist.py`. Acepta como argumento otra ruta a `CoffeeDial.app`, por ejemplo un build Release.

## Sync autenticado y revisiones — 2026-10-04

- `ktlintCheck`, `:shared:testAndroidHostTest` (38 tests), `:androidApp:assembleDebug` y `:shared:wasmJsBrowserDistribution`: OK.
- `:shared:verifySqlDelightMigration`: OK, incluyendo migración histórica hasta schema 5.
- Xcode Debug para simulador (`CODE_SIGNING_ALLOWED=NO`): BUILD SUCCEEDED.
- Node: 27 tests aprobados de servidor/navegador; 1 prueba opcional del endpoint local omitida. Incluye Worker real con SQLite, aislamiento, revisión concurrente, borrados, revocación y persistencia tras reiniciar.
- Worker desplegado: `9277a73c-5b05-4252-b0c4-2910c15f7723`.
- Web desplegada: `21e05908-da47-491a-8556-f1a7121c3103`.
- Producción: `/health` devuelve 200/ready; GET y POST `/api/sync` con email pero sin credencial devuelven 401. No se accedió a datos reales durante esas pruebas.
- `auth.js` y `shared.js` públicos coinciden byte a byte con el build validado. Cloudflare agrega su script de beacon al HTML (diferencia observada, ajena al bundle; el CSP actual no autoriza ese dominio).
- Falta la prueba interactiva de login real y edición entre dos dispositivos actualizados; no se simularon credenciales reales ni se reemplazaron datos del usuario para validar.

### Reporte de crash enviado al terminar

El reporte corresponde al 2026-10-02 18:31:59 -0300, proceso CoffeeDial del simulador. La hebra 7 aborta por una excepción Kotlin no controlada en `androidx.compose.ui.uikit.PlistSanityCheck`, coincidiendo con el fallo de plist ya corregido. No indica timeout ni terminación por memoria. El plist dentro del `.app` recién construido contiene `CADisableMinimumFrameDurationOnPhone` booleano true. El reporte no evidencia un fallo del sync nuevo.

## 2026-10-05 — machine cancellation and shot additions

- Machine editor is inline; cancellation clears focus and returns to the list without writing. Network sync no longer holds the shared local-saving flag.
- Optional `extraWater` (1–1000 ml) and free-text `style` (up to 100 characters), style suggestions, default grind `Medio`. Extra water does not change the extraction ratio.
- SQLite schema 6 adds nullable columns; backup v2 imports frozen v1 files. Sync accepts v1/v2 and rejects downgrading existing v2 cloud data.
- Passed: 44 Kotlin host tests; 24 Node tests including real Worker/SQLite persistence and v2 downgrade protection (one unrelated optional external local-server test skipped); ktlintCheck; Android debug, web production, and full iOS Simulator Xcode builds.
- Chrome Agustin, isolated localhost origin: cancel new machine with entered text; save a test machine; edit/cancel retains original; save Americano with 120 ml water and default Medio; detail shows ratio 1:2 and editing preloads both fields. Production user data was not changed by these checks.
- Native builds verified; this pass did not perform native runtime UI tests.
