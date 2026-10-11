# Coffee Dial

Diario de espresso local-first para Android, iOS y web. Un solo módulo Kotlin Multiplatform, `:shared`, contiene dominio, persistencia, sync y la UI Compose. Google puede guardar una copia en Cloudflare. Apple todavía no autoriza sync remoto.

La copia de trabajo es la raíz de este repositorio. El remoto es `mural/coffee-dial`. No hace falta otro módulo Gradle, un framework de inyección ni una interfaz por clase.

## Arquitectura KMP y convenciones

| Destino | Entrada | Datos locales |
|---|---|---|
| Android | `androidApp` → `App` | SQLite, `coffee-dial.db`, driver Android |
| iOS | `iosApp/CoffeeDial.xcodeproj`, framework estático `CoffeeDialShared` | SQLite, el mismo archivo, driver Native |
| Web | Kotlin/Wasm, `shared/src/wasmJsMain` | IndexedDB, un documento JSON con compare-and-swap |

`androidMain` e `iosMain` incorporan `shared/src/mobileMain/kotlin`. Ahí vive `SqlShotRepository`. Web implementa el mismo `ShotRepository` con `SnapshotRepository` y el adaptador de `browser.js`. La UI solo habla con el repositorio.

Paquetes en `shared/src/commonMain/kotlin/com/coffeedial/`:

- `domain/`: modelos, borradores y reglas puras. Sin SQL, red ni Compose.
- `data/`: `ShotRepository` y el snapshot de web.
- `backup/`: JSON manual, versión independiente de SQLite.
- `sync/`: protocolo 2, merge y conflictos.
- `ui/`: pantallas Compose compartidas por las tres plataformas.
- `auth/`, `photos/`, `admin/`: contratos comunes. Cada plataforma implementa lo que el sistema obliga.

`expect`/`actual` queda para HTTP, decodificar fotos, atrás del sistema y avisos que solo existen en web. Selectores de archivo, sesión de Google o Apple y el driver SQL también se quedan en la plataforma. No usar CocoaPods, XcodeGen ni Swift Package Manager. El `.xcodeproj` está versionado.

Identidad de un registro: UUID. No se deduce del nombre, del email ni de la fecha. Fechas de shots: milisegundos UTC. La molienda es texto (`42`, `2.3`, `12 clicks`). Los límites de entrada son guardrails del producto, no recetas. Los textos de la UI están en español.

El estilo es el de ktlint, ya configurado en la raíz (`kotlin.code.style=official`). No agregar detekt. Las versiones de Kotlin, AGP, Compose, SQLDelight y Gradle están fijadas en el README: no subir una sola sin revisar la matriz, y no usar rangos ni snapshots.

`local.properties` y `iosApp/Auth.xcconfig` no se versionan. El client id web de Google en `gradle.properties` es público. El client secret vive en Cloudflare y no entra a Git ni a un comando. Los tests no usan datos reales de usuarios.

## Compilar

En esta Mac, Gradle usa el JDK de Android Studio:

```sh
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
export ANDROID_HOME="$HOME/Library/Android/sdk"
```

Android. El segundo comando necesita un dispositivo o emulador API 26+:

```sh
./gradlew :androidApp:assembleDebug
./gradlew :androidApp:installDebug
```

APK: `androidApp/build/outputs/apk/debug/androidApp-debug.apk`.

iOS. El simulador no necesita cuenta de Apple. Un iPhone físico necesita Team y bundle id en Signing & Capabilities.

```sh
./gradlew :shared:linkDebugFrameworkIosSimulatorArm64
xcodebuild -project iosApp/CoffeeDial.xcodeproj -scheme CoffeeDial \
  -configuration Debug -sdk iphonesimulator \
  -destination 'generic/platform=iOS Simulator' \
  -derivedDataPath iosApp/build/DerivedData \
  CODE_SIGNING_ALLOWED=NO ARCHS=arm64 build
```

Web. La tarea de desarrollo imprime su URL. Producción es `https://coffee.muralooo.win`, Worker `coffee-dial-web`, separado de `coffee-dial-auth`.

```sh
./gradlew :shared:wasmJsBrowserDevelopmentRun
./gradlew :shared:wasmJsBrowserDistribution
```

Checks según lo que cambió:

```sh
./gradlew ktlintCheck
./gradlew :shared:testAndroidHostTest
./gradlew :shared:verifySqlDelightMigration
./gradlew :shared:iosSimulatorArm64Test
./gradlew :androidApp:lintDebug
npm test --prefix web
npm ci --prefix auth-service && npm test --prefix auth-service && npm run check --prefix auth-service
```

`:shared:testAndroidHostTest` usa SQLite en la JVM, sin emulador. Los tests de `web/` cubren el adaptador con fake-indexeddb; no reemplazan abrir el navegador. `npm run check` hace el dry-run de Wrangler y no publica.

Publicar, solo si se pidió: primero `npm run deploy` en `auth-service`, después el Worker de `web/wrangler.jsonc`, y entonces las apps nativas. Comprobar que `/health` responde ready y que `/api/sync` sin Bearer responde 401, también si el request trae un email. No escribir datos reales como prueba.

## Migraciones SQLDelight

El esquema actual está en `Coffee.sq`. `CoffeeDatabase.Schema.version` es **15**. Las bases ya instaladas están en esa versión. La próxima migración es `shared/src/commonMain/sqldelight/com/coffeedial/database/15.sqm`.

`databases/1.db` es la baseline. Cada `N.sqm` corre cuando `user_version <= N` y la versión nueva es mayor: `12.sqm` lleva una base 12 a la 13. No editar un `.sqm` ni la baseline que ya haya salido en una app. No reescribir el historial de migraciones para “ordenar” el archivo.

Preferir `ADD COLUMN` nullable, o `NOT NULL` con un default que conserve las filas. Si SQLite obliga a recrear una tabla, copiar todos los IDs y columnas anteriores dentro de la misma migración. `4.sqm` es el precedente: sacó `UNIQUE(name, roaster)` sin cambiar IDs. Dos cafés pueden llamarse igual. La creación local sigue reutilizando un café activo cuando coinciden nombre y tostador.

`verifyMigrations` está activo. Después de agregar un `.sqm`:

```sh
./gradlew :shared:verifySqlDelightMigration :shared:testAndroidHostTest
```

Si la migración copia, rellena o borra datos, agregar un test en `androidHostTest` que arme las tablas viejas y llame a `CoffeeDatabase.Schema.migrate`. `SyncPersistenceTest` muestra cómo. Activar `PRAGMA foreign_keys` en ese fixture.

Web no ejecuta estos `.sqm`. El mismo cambio de datos tiene que entrar al documento de IndexedDB por el lector de backup, sin subir la versión de IndexedDB. Esa versión cambia solo si cambia la estructura del almacén, no porque el JSON gane un campo.

## Compatibilidad del cloud sync

SQLite, el backup JSON y el protocolo de sync se versionan por separado. El detalle del merge, los tombstones y los códigos HTTP está en [docs/SYNC.md](docs/SYNC.md).

| Capa | Vigente | Regla |
|---|---|---|
| SQLite | 15 | Arriba |
| Backup JSON | 11, `BackupFormat.CURRENT_VERSION` | Manual y sync usan el mismo formato `coffee-dial-backup` |
| Protocolo | 2 | Compare-and-swap de la revisión del snapshot completo |

Versiones de backup ya aceptadas: 1 base; 2 agua extra y estilo; 3 tazas; 4 archivado; 5 referencia de foto; 6 primera gota; 7 perfil de sabor; 8 dial-in; 9 `machineId` y `cupId`; 10 molinillos (`grinder`, `grinderId`); 11 recetas. Una receta es una copia independiente del shot: no guarda su id, su tiempo ni su evaluación. Cliente y `auth-service/src/sync.js` aceptan 1–11. Un snapshot futuro o desconocido falla sin vaciar datos. Escribir una versión menor que la que ya está en la nube responde 426 `backup_upgrade_required`. No se trunca un documento que no entra: sync rechaza por encima de 1.500.000 bytes; el backup manual, por encima de 10 MB.

Un campo nuevo que viaje entre dispositivos sube `CURRENT_VERSION` y el arreglo de versiones del servidor en el mismo cambio. Los backups viejos siguen decodificando. Lo que no venía en el archivo queda null. No se inventan valores. Codificar o validar ese campo con una versión anterior es un error. El servidor tiene que conocer la versión nueva antes de que un cliente la escriba. Si no, publicar primero `coffee-dial-auth`.

El protocolo sigue en 2 mientras el merge y el compare-and-swap no cambien. Un campo opcional no es un protocolo nuevo. Los clientes viejos que omiten defaults de protocolo siguen siendo aceptados. La autorización es la credencial de Coffee Dial o el ID token de Google verificado en el servidor. Un email, `X-User-Email` o un perfil guardado en el dispositivo no autorizan. Apple no abre `/api/sync`.

Las fotos se suben a R2 antes de publicar la referencia. El JSON de sync lleva el id, no el JPEG. El backup manual sí puede llevar el JPEG en base64. Una falla de red deja el cambio local pendiente. El checkpoint y los datos se confirman juntos. 409 vuelve a leer y a combinar. No hay «último escritor gana».

`ConflictScreen`, abierta desde Cuenta, muestra las dos copias de un mismo UUID hasta que la persona elige una. Ediciones de registros distintos se combinan. Borrar deja un tombstone permanente. Un backup viejo o una instalación sin checkpoint no resucita ese UUID, salvo una restauración explícita que quite el tombstone. No reutilizar UUIDs.

Importar un backup no reemplaza la cuenta ni el checkpoint. Una nube vacía no vacía el dispositivo. Cambiar de cuenta no sube solo el historial anterior.

## Criterios para una feature nueva

1. Regla de producto: función pura en `domain/` y test en `commonTest`. Sin driver ni red.
2. Pantalla: Compose en `commonMain`, para Android, iOS y web a la vez. Plataforma solo si hace falta un API del sistema.
3. Dato guardado: columna en `Coffee.sq`, migración `15.sqm` o la que corresponda al día, y el mismo campo en `SnapshotRepository`. Nullable si las filas viejas no lo tienen.
4. Si el dato sale del dispositivo, subir el backup como dice la sección anterior y repetir la validación en `auth-service/src/sync.js`. Cubrir con test una versión vieja, la nueva, un valor inválido y el rechazo a bajar de versión.
5. Si el dato es un archivo, dejarlo fuera del snapshot de sync y subir el objeto antes de la referencia. Ver [docs/PHOTOS.md](docs/PHOTOS.md).
6. Conflictos: el merge tiene que poder comparar el campo. No pisar en silencio las dos ediciones.
7. Navegación: los cuatro pilares y la barra inferior están en [docs/NAVIGATION.md](docs/NAVIGATION.md). Un formulario, un timer o un detalle usan Volver y ocultan la barra.
8. No agregar módulo, plugin, dependencia ni permiso del sistema si la feature funciona sin eso.

Antes de dar por cerrada una feature que toca esquema o sync, correr la migración, los tests de host, los de `auth-service` y la distribución Wasm. Si cambia UI compartida, compilar Android y el simulador iOS. Documentar el comportamiento en `docs/` solo cuando la regla no cabe en el código ni en este archivo. No reescribir el registro histórico de [docs/VERIFICATION.md](docs/VERIFICATION.md).
