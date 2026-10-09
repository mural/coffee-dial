# Fotos de Cafés

Una foto opcional por café. Android (incluye API 26/27), iOS y web usan selectores del sistema; no se pide acceso completo a la galería. Se normaliza orientación y se reencodifica a JPEG, lado máximo 640 px, hasta 100 KB, descartando el original y sus metadatos. La UI permite reemplazar, quitar y ampliar la referencia.

Persistencia: SQLDelight migración 8 agrega bean.photo. Web conserva la imagen pequeña en su snapshot IndexedDB. Los shots no duplican la imagen; archivar un café quita la foto del catálogo y conserva sus shots.

Backup v5: JSON autocontenido con JPEG base64. Se mantiene el límite existente de 10 MB y la importación v1–v4. No se exporta un backup que solo contenga referencias sin los archivos. Al importar, cafés con imágenes distintas no se colapsan solamente por nombre. Clientes anteriores no pueden sobrescribir snapshots v5.

Sync: los objetos se suben antes de publicar referencias; descargas se completan antes del commit local atómico. Una falla deja los datos locales pendientes. IDs inmutables y comparación de contenido hacen idempotente el reintento. El JSON de sync lleva solo id, no jpeg. Sesión y aislamiento por email verificado usan el mismo mecanismo del historial; Google y Apple con igual email comparten fotos.

Infraestructura: R2 privado coffee-dial-photos, binding PHOTOS. GET/POST /api/photos/:id requieren autenticación y resuelven la clave desde el hash del email verificado. Sin dominio público de R2 ni enlaces públicos. Validación de JPEG, dimensiones y tamaño en servidor; comprobación de existencia antes de aceptar referencias. El endpoint devuelve JSON privado no-store. La eliminación/reemplazo quita la referencia sincronizada; los objetos anteriores se conservan privados por ahora para no romper restauraciones concurrentes. La recolección física de objetos huérfanos queda pendiente.

Publicación: habilitar R2 en la cuenta, crear bucket, desplegar auth-service y luego web; reconstruir móviles. R2 habilitado y bucket creado el 2026-10-09. Acceso r2.dev deshabilitado y sin dominios públicos conectados. Desplegar siempre servidor antes de web.

Pruebas: persistencia SQL/IndexedDB, backup con imagen, retiro sin borrar shots, fallo de subida sin confirmar checkpoint, transferencia por separado y recuperación en otro dispositivo, runtime Workers+R2 local con aislamiento entre cuentas y rechazo de referencias inexistentes. iOS Simulator requiere ARCHS=arm64 para este framework KMP.

Verificación local 2026-10-09: 60 tests Kotlin, 39 del servidor y 5 web aprobados (1 test de servidor omitido por requerir origen externo). Migraciones SQLDelight, Android Debug, distribución web y Xcode iOS Simulator ARM64 aprobados. Empaquetado Worker dry-run aprobado; lint de archivos modificados aprobado. El lint global conserva errores previos en ShotForm/TipsScreen y otros archivos ajenos a fotos. Falta prueba manual de selectores en dispositivos. Servidor publicado y comprobado remotamente: POST sin credencial, POST con token inválido y GET sin credencial devuelven 401. Tests locales verifican también sesión revocada y aislamiento entre cuentas. La UI deshabilita agregar/cambiar fotos sin login; la autorización efectiva se valida siempre en el servidor.
