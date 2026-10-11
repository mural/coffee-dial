# Coffee Dial web

Target Kotlin/Wasm de `shared`, con las mismas pantallas Compose, el dominio, los backups y el sync de Android e iOS. El login web es el de Google. No hay service worker ni arranque offline.

## Ejecutar y publicar

Desde la raíz, con Java de Android Studio:

```sh
export JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home'
./gradlew :shared:wasmJsBrowserDevelopmentRun
# Compilación de producción:
./gradlew :shared:wasmJsBrowserDistribution
# Hosting estático (Wrangler fijado por el lockfile de auth-service):
npm ci --prefix auth-service
./auth-service/node_modules/.bin/wrangler deploy --config web/wrangler.jsonc
```

La tarea de desarrollo muestra su URL local. Producción se publica en https://coffee.muralooo.win, Worker `coffee-dial-web`, separado de `coffee-dial-auth`. La distribución contiene solo recursos públicos; no datos de usuarios ni secretos. El dominio debe mantenerse estable porque IndexedDB se separa por origen.

## Almacenamiento y backups

- Android/iOS mantienen SQLDelight/SQLite: implementación en `shared/src/mobileMain/kotlin`, incluida en ambos source sets.
- `ShotRepository` es el contrato común. Web usa `SnapshotRepository` + el adaptador IndexedDB de `browser.js`.
- IndexedDB versión 1 contiene un documento validado con el formato versionado de backup. Toda escritura es una transacción compare-and-swap; la UI solo refleja éxito después del commit. Una pestaña desactualizada no puede reemplazar silenciosamente una escritura simultánea. Se relee antes de cada operación; las pestañas no se refrescan automáticamente en tiempo real.
- Formatos futuros/corruptos fallan sin resetear datos. Al evolucionar el formato, conservar lectores y migraciones explícitas en BackupFormat. No cambiar la versión de IndexedDB por agregar un campo al backup; solo al cambiar su estructura de almacenamiento.
- Importación aditiva con vista previa; duplicados y conflictos se omiten. Hasta 10 MB por snapshot/backup. La escritura del documento completo prioriza simplicidad para este MVP; revisar almacenamiento por registros si el historial crece significativamente.
- El navegador puede borrar sus datos al limpiar almacenamiento o finalizar una sesión privada. Exportar backups sigue siendo necesario. La app necesita conexión para cargar; una vez cargada, guardar y leer datos no usa el servidor. El arranque offline/PWA queda para otra iteración.
- El selector lee archivos en el dispositivo y la exportación descarga JSON. Ese archivo no se sube solo por exportarlo. El sync autenticado sí envía el snapshot a Cloudflare, con las fotos como referencias y no como JPEG. Ver [docs/SYNC.md](../docs/SYNC.md).

## Pruebas

```sh
./gradlew ktlintCheck :shared:testAndroidHostTest :shared:iosSimulatorArm64Test :androidApp:assembleDebug :shared:wasmJsBrowserDistribution
npm ci --prefix web
npm test --prefix web
```

El repositorio prueba reapertura, roundtrip del backup, fallos de escritura, conflictos de pestañas y versiones desconocidas. El adaptador JS se prueba con fake-indexeddb (no sustituye verificar el navegador real). Se verificó visualmente Chrome con exportación/importación y deduplicación. Safari/Firefox y accesibilidad completa quedan por verificar. Compose Web/Wasm sigue en Beta; usar navegadores modernos.

Documentación: https://kotlinlang.org/docs/wasm-get-started.html y https://developers.cloudflare.com/workers/static-assets/.
