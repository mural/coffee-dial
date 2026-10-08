# Panel Admin

Acceso desde Cuenta → Panel Admin. Interfaz compartida Android/iOS/web; solo lectura.

## Autorización

El servidor valida la credencial habitual de Google (ID token firmado o sesión opaca emitida tras OAuth). `ADMIN_GOOGLE_EMAIL` autoriza la primera identidad Google verificada de esa dirección y el directorio fija su `sub` permanentemente. Un subject diferente, aun con el mismo email, no hereda el rol. Cada solicitud vuelve a comprobar ambos valores. Cambiar o vaciar la variable revoca el acceso; transferir el rol requiere un procedimiento explícito del operador. No hay contraseñas master, roles editables por clientes ni endpoints públicos para asignar permisos.

GET /api/admin/access confirma el permiso sin datos personales de terceros. GET /api/admin/overview y GET /api/admin/account?subject=… exigen permiso master. No hay operaciones de escritura administrativas. Las respuestas usan no-store; la app mantiene el resultado solo en memoria y lo descarta al cambiar de cuenta. Un identificador de cuenta en la URL no otorga acceso.

## Directorio y cobertura

El nuevo Durable Object SQLite `AdminDirectory`, binding ADMIN_DIRECTORY, instancia `directory-v1`, guarda una fila por cuenta y agregados por fecha UTC/estilo. Migración Cloudflare `v2-admin`: crea un namespace independiente, sin migrar ni borrar los snapshots originales. El índice vigente usa email_accounts/email_daily/email_styles y una clave por email verificado normalizado. Google y Apple con el mismo email comparten resumen, como ya comparten el snapshot de sync. identities conserva los identificadores de proveedores. Las tablas originales accounts/daily/styles se conservan; las vistas visible_accounts/visible_daily/visible_styles muestran una sola entrada por email, usando la confirmación más reciente del índice anterior hasta la próxima sincronización. No se suman snapshots ni se modifican shots durante esta transición. administrator conserva el subject autorizado. Un email privado de Apple distinto sigue siendo otra cuenta.

Cada lectura o escritura exitosa de sync actualiza el índice desde el snapshot del servidor. Una revisión atrasada no reemplaza una más nueva. Actualizar agregados es atómico. Un error del índice puede hacer fallar la confirmación de sync después de guardar el snapshot; reintentar la lectura repara el índice sin duplicar registros. No se indexan cambios locales sin sincronizar.

Las cuentas previas se descubren al sincronizar de nuevo. No existe un catálogo histórico de subjects que permita enumerarlas retroactivamente; la UI informa esta cobertura. El detalle lee el snapshot original actual, por lo que puede ser más reciente que el resumen si hubo un fallo de indexación. La fecha por cuenta es última confirmación del snapshot, no fecha de edición de los shots.

Resumen global: cuentas indexadas (incluye vacías), shots, puntuación media, últimos 7 días UTC, actividad diaria de 30 días, top 10 estilos. Cuentas paginadas de 50 en 50 por subject. Detalle con filtros por fechas UTC, estilo y puntuación, sin modificar historial. ALLOWED_EMAILS permite mural86@gmail.com y agustin.sgarlata.v@gmail.com. Solo la primera cuenta conserva el rol administrador. Cambiar a un email distinto no transfiere el historial local: se mantiene la protección de pertenencia de datos de SyncEngine.

## Verificación

Pruebas con Worker y SQLite locales: 401 sin sesión, 403 para usuario normal, suplantación por email/parámetros rechazada, identidad master fijada y persistente tras reinicio, 405 en escrituras Admin, incorporación de cuenta anterior, agregados tras edición/borrado, rechazo de revisión atrasada, paginación de 52 cuentas y revocación de sesión. Pruebas Kotlin: autoridad del servidor, ausencia de autorización basada en email del cliente y descarte de respuesta tras logout.

El detalle usa el snapshot canónico incluso si está vacío. Solo consulta snapshots anteriores cuando no existe una revisión canónica. Las pruebas de migración cubren duplicados Google/Apple, persistencia tras reinicio y conservación de los índices anteriores.
