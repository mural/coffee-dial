# Revisión de IDs, sync y retorno de Cuenta — 2026-10-04

> Revisión histórica previa a la corrección. Autenticación y revisiones ya implementadas: ver [SYNC.md](SYNC.md). La relación de máquina por nombre y la UI de resolución de conflictos siguen pendientes.

## Resultado de la revisión

Los IDs UUID de Shot, Bean y Machine son adecuados. Editar un shot conserva su ID y createdAt en SQLite y en el snapshot web; hay pruebas de regresión. Cambiar el café de un shot cambia su referencia a Bean: no renombra globalmente el café anterior. No regenerar IDs al importar ni usar nombres, email o timestamps como identidad.

El sync actual almacena backups en Cloudflare Durable Objects; Google identifica al usuario, no almacena estos backups. El protocolo actual NO garantiza sincronización segura de modificaciones.

### Hallazgos pendientes (no corregidos por el cambio de callback)

1. **Crítico: autorización por email.** `/api/sync` acepta X-User-Email o el parámetro email sin verificar una credencial. Quien conozca una dirección puede solicitar o sobrescribir su copia. Usar un token de sesión validado en servidor, vinculado a issuer+subject; el email no es una credencial. Los perfiles locales guardados tampoco autorizan operaciones remotas.
2. **Pérdida de ediciones.** `mergeBackups` hace ganar siempre al snapshot que llega último, incluso si viene de un dispositivo desactualizado. No hay revisión por registro. Además SnapshotRepository.forceImportBackup conserva primero la versión local con distinctBy, mientras SQLite reemplaza shots: los targets divergen.
3. **Borrados que reaparecen.** Se eliminan filas sin tombstones y el servidor une ambas listas. La ausencia no expresa un borrado. `exportedAt` no es la fecha de edición de cada entidad.
4. **Beans incompatibles entre dispositivos.** SQLite tiene UNIQUE(name, roaster), pero el servidor une por UUID. Dos dispositivos pueden crear el mismo café con distintos UUID; insertBean ignora uno y un shot remoto queda con una referencia inexistente (o falla con FK habilitadas). La importación manual sí planifica remapeos; forceImportBackup los omite.
5. **Éxitos falsos y concurrencia.** SyncEngine sustituye un fallo HTTP por el backup local y muestra Success. El servidor también ignora errores de guardado. Leer y guardar en llamadas separadas al Durable Object puede perder escrituras concurrentes.
6. **Restauración destructiva.** replaceWithBackup sustituye el historial; no hay protección frente a una edición local durante la descarga ni confirmación/copia previa. Un GET sin backup devuelve uno vacío y puede vaciar el historial local.
7. **Relación con máquina por nombre.** Shot.machine es texto, no machineId; un cambio de nombre no tiene relación estable. Una migración debe mantener el nombre antiguo como fallback cuando no haya coincidencia inequívoca.

## Diseño recomendado antes de habilitar sync confiable

Mantener UUID; separar el protocolo de sync del backup manual v1. Añadir revisión del servidor y baseRevision por entidad, mutationId idempotente, outbox local persistente y deletedAt/tombstones. Una transacción del servidor valida la revisión esperada y aplica mutaciones; un conflicto conserva ambas versiones para resolución, nunca sobrescribe silenciosamente. Los cambios locales durante una petición permanecen en outbox. Identificar cuentas por proveedor+subject verificado.

Migrar datos antiguos conservando IDs y referencias, marcar registros sin metadatos como aún no sincronizados, resolver/remapear colisiones de Bean de forma transaccional. Backup v2 debe incluir metadatos y tombstones con lectura compatible de v1; no cambiar el significado de v1 ni inventar fechas históricas. La restauración explícita requiere copia previa, resumen/confirmación y una nueva revisión; no debe confundirse con merge automático.

## Callback implementado

`platform` y `return_to` se validan al iniciar OAuth y quedan ligados al intento. Web recibe únicamente el código de un solo uso en el fragmento y lo canjea con PKCE; no inicia sesión por email/name de la URL. Android envía platform=android. Para clientes móviles antiguos se conserva un fallback de user-agent, con Android por defecto; el login nativo iOS no cambia.

Destinos web permitidos: https://coffee.muralooo.win/ y http/https en localhost, 127.0.0.1 o [::1], conservando el puerto. Se rechazan credenciales en URL, rutas distintas de /, queries, fragmentos y otros hosts. Localhost se selecciona automáticamente desde location.origin: no necesita un cliente OAuth adicional. El redirect registrado en Google sigue siendo https://auth-coffee.muralooo.win/google/callback.

La página de callback móvil muestra solamente el enlace de su plataforma. Web redirige directamente a la dirección de inicio. Las sesiones web antiguas basadas solo en email requieren iniciar sesión nuevamente.

No desplegar este conjunto sobre producción antes de resolver la autorización del sync: los cambios de callback se verifican localmente y requieren publicar Worker y web juntos. No se modificaron datos remotos.
