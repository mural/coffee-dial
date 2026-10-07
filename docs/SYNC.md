# Sync autenticado y revisiones (protocolo 2)

Google verifica la identidad; los backups remotos viven en Cloudflare Durable Objects. El servidor ya no acepta email ni X-User-Email como autorización. Verifica la firma, issuer, audience, expiración, subject y email verificado de Google y mantiene el allowlist de pruebas. El almacenamiento nuevo se identifica por proveedor + subject.

Web y Android usan una credencial opaca de Coffee Dial con vencimiento después de 30 días de inactividad autenticada. El servidor renueva el plazo al usarla (como máximo una escritura por día), guarda solo su hash como identificador y la revoca al cerrar sesión. Las credenciales ya vencidas no se reactivan: requieren un nuevo login. Google nativo Android canjea un ID token verificado por esa sesión mediante `/api/session`; nunca se usa email/ID como credencial. Android cifra la sesión persistente con Android Keystore; web la conserva en localStorage del origen (accesible al JavaScript de la app) y valida la identidad con el servidor al reabrirse. PKCE sigue limitado a la pestaña y a cinco minutos. iOS Google renueva el ID token con `refreshTokensIfNeeded` antes de cada solicitud autenticada; la persistencia la gestiona el SDK. Cerrar sesión sin conexión borra la credencial local, pero su copia remota solo vence por plazo. Apple todavía no autoriza sync remoto.

## Cambios y borrados

- UUIDs estables para shots, beans, máquinas y tazas. No se deduce identidad a partir del nombre, email o fecha.
- Cada dispositivo guarda el último snapshot confirmado, la cuenta y la revisión. Los cambios locales aún no confirmados se calculan comparando ese snapshot con la base local persistente, incluso después de reiniciar.
- Se comparan tres versiones de cada UUID: base confirmada, local y remota. Ediciones de registros diferentes se combinan. Dos cambios distintos en el mismo registro, o editar frente a borrar, producen conflicto sin modificar ninguno de los dos lados.
- El servidor aplica un compare-and-swap de la revisión del snapshot completo en una sola transacción. Si otro dispositivo escribió primero devuelve 409: reintentar vuelve a leer y combinar, sin sobrescribir la nueva revisión.
- Los IDs eliminados quedan en tombstones persistentes. Una instalación sin checkpoint o un backup viejo no puede resucitarlos silenciosamente. No hay purga automática de tombstones.
- Guardado local y checkpoint son atómicos. Si alguien edita mientras se envía, el resultado remoto no pisa esa edición. Un reintento reconcilia una respuesta perdida; no depende de relojes de dispositivos.
- Recuperar y combinar usa la misma política segura: una nube vacía no vacía el dispositivo. No hay reemplazo destructivo automático.

Los conflictos se notifican en Cuenta; no existe todavía una pantalla para elegir entre las dos versiones. Ante un conflicto, exportar la copia local y revisar ambos dispositivos antes de resolverlo. No se aplica una política automática de «último escritor gana».

## Migración y compatibilidad

SQLite pasó a schema 5 mediante 4.sqm. Reconstruye bean/shot conservando IDs y campos para quitar UNIQUE(name, roaster): dos dispositivos pueden crear cafés homónimos con UUIDs diferentes. Las referencias siguen siendo válidas. La creación local reutiliza un café existente cuando coincide nombre/tostador. La migración y el rollback tienen tests sobre SQLite con foreign keys habilitadas.

Web convierte el snapshot antiguo en un documento interno storageVersion=2 con checkpoint. El cambio es atómico en IndexedDB. La exportación/importación manual usa **backup v4**, independiente del protocolo de sync. El lector migra v1 con agua extra y estilo vacíos; los archivos existentes siguen funcionando. Importar no restaura ni suplanta la identidad de una cuenta ni su checkpoint.

Schema 6 agrega columnas nullable `extra_water` y `style` mediante `5.sqm`, sin cambiar IDs ni valores anteriores. Schema 7 agrega tazas y la taza del shot mediante `6.sqm`. Backup v3 incluye ambos campos; el lector acepta v1, v2 y la versión intermedia v2 con tazas. El servidor acepta backups v1, v2 y v3, pero no permite bajar de versión: actualizar las apps móviles antes de sincronizar con datos nuevos. Una app antigua falla de forma segura en lugar de omitir campos. El lector conserva la validación estricta de versiones futuras y campos desconocidos.

En la primera lectura autenticada, el servidor valida y copia el backup antiguo del email verificado al espacio nuevo del subject. El original queda intacto. Una copia inválida o demasiado grande genera error; no se reemplaza por datos vacíos. No es posible verificar retroactivamente quién escribió las copias antiguas, porque el servidor anterior no autenticaba escrituras.

El límite conservador de sync es 1.500.000 bytes por documento (incluye tombstones), inferior al límite de la celda de almacenamiento. El backup manual conserva su límite de 10 MB. Los datos no se truncan al exceder el límite. Un dispositivo ya vinculado no sube automáticamente su historial a otra cuenta.

## Retorno de Cuenta

La plataforma y el destino se guardan dentro del intento OAuth. Android/iOS muestran solo el enlace móvil correspondiente. Web vuelve directamente al origen inicial: producción o localhost/127.0.0.1/[::1], conservando puerto. Otros destinos se rechazan. El callback registrado en Google sigue siendo `https://auth-coffee.muralooo.win/google/callback`; no hace falta registrar cada puerto local.

## Despliegue y verificación

Publicar primero coffee-dial-auth y luego coffee-dial-web. Actualizar Android/iOS y volver a entrar con Google. Los clientes anteriores quedan rechazados con 401 y deben actualizarse (algunos clientes viejos ocultaban errores como éxito). No volver a desplegar la versión que autoriza por email.

Pruebas: firmas/audience/expiración, PKCE, rutas permitidas, aislamiento entre cuentas, escrituras concurrentes, tombstones y reinicio del Worker real con Miniflare/SQLite; merge de modificaciones/borrados, fallos de red, ediciones durante el envío, cambio de cuenta, migración y rollback locales. No se usan datos reales para los tests.

Después de publicar, probar con dos dispositivos actualizados: iniciar sesión, crear un shot, sincronizar, editarlo desde el otro y sincronizar; borrar y comprobar que no reaparece. Dos ediciones incompatibles del mismo shot deben mostrar conflicto y conservar las dos copias.

Schema 8 (`7.sqm`) añade `bean.archived`: eliminar del catálogo conserva la referencia y los datos del café en shots anteriores. Backup v4 conserva ese estado en exportación, importación y merge; clientes anteriores deben actualizarse antes de sincronizar con v4.


## Reparación de sesiones y sync — 7/10/2026

El cliente envía explícitamente los defaults (protocol=2, máquinas y tazas vacías).
El servidor sigue aceptando clientes que omitían esos defaults. Los errores se separan:
401 requiere renovar identidad; 409 indica concurrencia; 422 datos inválidos; 426 una
app anterior al backup de la nube; 413 tamaño; 503 servicio temporalmente indisponible.
Un fallo de red o 503 no descarta la cuenta.

Un 401 permite una sola renovación/reintento. iOS valida la sesión propia restaurada
contra el proveedor y subject actual; Google puede renovar mediante su SDK. Apple
requiere consentimiento otra vez si ya no hay sesión propia ni token Apple vigente.
Web puede recuperar una sesión más reciente guardada por otra pestaña. Android/web
ofrecen volver a entrar cuando la credencial ya no es renovable, sin borrar los datos.
La sesión propia mantiene 30 días de inactividad y se extiende con el uso.

Sobrescribir requiere leer la revisión actual; incluso force respeta CAS. Restaurar
aplica datos y checkpoint juntos y se cancela si hubo ediciones/cambio de cuenta durante
la descarga. Restaurar IDs borrados explícitamente elimina sus tombstones. Un fallo del
índice Admin no convierte un guardado correcto en un sync fallido.
