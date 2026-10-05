# Sync autenticado y revisiones (protocolo 2)

Google verifica la identidad; los backups remotos viven en Cloudflare Durable Objects. El servidor ya no acepta email ni X-User-Email como autorización. Verifica la firma, issuer, audience, expiración, subject y email verificado de Google y mantiene el allowlist de pruebas. El almacenamiento nuevo se identifica por proveedor + subject.

El navegador y el fallback Android canjean el ticket PKCE por una credencial opaca de 12 horas. El servidor guarda su hash, permite revocarla y elimina las sesiones vencidas. Web la conserva en sessionStorage; Android conserva las credenciales solo en memoria. iOS usa el ID token del SDK Google. El cierre de sesión revoca las credenciales opacas si hay conexión; offline, dejan de usarse localmente y vencen en el servidor. Los ID tokens nativos siguen la expiración de Google; al vencer hay que volver a entrar. Android requiere reautorizar después de reiniciar el proceso. Apple todavía no autoriza sync remoto.

## Cambios y borrados

- UUIDs estables para shots, beans y máquinas. No se deduce identidad a partir del nombre, email o fecha.
- Cada dispositivo guarda el último snapshot confirmado, la cuenta y la revisión. Los cambios locales aún no confirmados se calculan comparando ese snapshot con la base local persistente, incluso después de reiniciar.
- Se comparan tres versiones de cada UUID: base confirmada, local y remota. Ediciones de registros diferentes se combinan. Dos cambios distintos en el mismo registro, o editar frente a borrar, producen conflicto sin modificar ninguno de los dos lados.
- El servidor aplica un compare-and-swap de la revisión del snapshot completo en una sola transacción. Si otro dispositivo escribió primero devuelve 409: reintentar vuelve a leer y combinar, sin sobrescribir la nueva revisión.
- Los IDs eliminados quedan en tombstones persistentes. Una instalación sin checkpoint o un backup viejo no puede resucitarlos silenciosamente. No hay purga automática de tombstones.
- Guardado local y checkpoint son atómicos. Si alguien edita mientras se envía, el resultado remoto no pisa esa edición. Un reintento reconcilia una respuesta perdida; no depende de relojes de dispositivos.
- Recuperar y combinar usa la misma política segura: una nube vacía no vacía el dispositivo. No hay reemplazo destructivo automático.

Los conflictos se notifican en Cuenta; no existe todavía una pantalla para elegir entre las dos versiones. Ante un conflicto, exportar la copia local y revisar ambos dispositivos antes de resolverlo. No se aplica una política automática de «último escritor gana».

## Migración y compatibilidad

SQLite pasa a schema 5 mediante 4.sqm. Reconstruye bean/shot conservando IDs y campos para quitar UNIQUE(name, roaster): dos dispositivos pueden crear cafés homónimos con UUIDs diferentes. Las referencias siguen siendo válidas. La creación local reutiliza un café existente cuando coincide nombre/tostador. La migración y el rollback tienen tests sobre SQLite con foreign keys habilitadas.

Web convierte el snapshot antiguo en un documento interno storageVersion=2 con checkpoint. El cambio es atómico en IndexedDB. La exportación/importación manual mantiene **backup v1**, independiente del protocolo de sync; los archivos existentes siguen funcionando. Importar no restaura ni suplanta la identidad de una cuenta ni su checkpoint.

En la primera lectura autenticada, el servidor valida y copia el backup antiguo del email verificado al espacio nuevo del subject. El original queda intacto. Una copia inválida o demasiado grande genera error; no se reemplaza por datos vacíos. No es posible verificar retroactivamente quién escribió las copias antiguas, porque el servidor anterior no autenticaba escrituras.

El límite conservador de sync es 1.500.000 bytes por documento (incluye tombstones), inferior al límite de la celda de almacenamiento. El backup manual conserva su límite de 10 MB. Los datos no se truncan al exceder el límite. Un dispositivo ya vinculado no sube automáticamente su historial a otra cuenta.

## Retorno de Cuenta

La plataforma y el destino se guardan dentro del intento OAuth. Android/iOS muestran solo el enlace móvil correspondiente. Web vuelve directamente al origen inicial: producción o localhost/127.0.0.1/[::1], conservando puerto. Otros destinos se rechazan. El callback registrado en Google sigue siendo `https://auth-coffee.muralooo.win/google/callback`; no hace falta registrar cada puerto local.

## Despliegue y verificación

Publicar primero coffee-dial-auth y luego coffee-dial-web. Actualizar Android/iOS y volver a entrar con Google. Los clientes anteriores quedan rechazados con 401 y deben actualizarse (algunos clientes viejos ocultaban errores como éxito). No volver a desplegar la versión que autoriza por email.

Pruebas: firmas/audience/expiración, PKCE, rutas permitidas, aislamiento entre cuentas, escrituras concurrentes, tombstones y reinicio del Worker real con Miniflare/SQLite; merge de modificaciones/borrados, fallos de red, ediciones durante el envío, cambio de cuenta, migración y rollback locales. No se usan datos reales para los tests.

Después de publicar, probar con dos dispositivos actualizados: iniciar sesión, crear un shot, sincronizar, editarlo desde el otro y sincronizar; borrar y comprobar que no reaparece. Dos ediciones incompatibles del mismo shot deben mostrar conflicto y conservar las dos copias.
