# Perfil de sabor

Opcional, independiente del puntaje general de 1 a 5. Una impresión entre Demasiado ácido, Demasiado amargo, Débil, Equilibrado o Demasiado intenso. Cuerpo, Acidez y Amargor se registran individualmente como enteros de 1 a 5 (migrados desde la escala inicial de 0 a 10 en SQLite 18 / Backup v14 usando `ceil(valor / 2)` y 0 → 1). Sin tocar un control se conserva null; el botón Registrar permite elegir explícitamente el punto medio. Cada dato se puede quitar.

Formulario compartido Android/iOS/web para creación y edición; el borrador preserva el perfil al navegar al timer o recrear la pantalla. La tarjeta muestra la impresión elegida y el detalle muestra todos los datos registrados. El puntaje y los análisis existentes mantienen su significado.

SQLDelight migración 10 agrega shot.taste como JSON nullable y migración 17 (`taste_scale`) normaliza la escala a 1–5. Backup v7 incorpora taste con impression (código estable), body, acidity y bitterness opcionales, y Backup v14 adopta la escala 1–5 convirtiendo backups v7–v13 al importar. Se siguen importando versiones anteriores sin inventar valores; servidor y cliente validan rangos y el servidor rechaza escribir una versión anterior sobre datos más nuevos. Actualizar móviles antes de sincronizar nuevos datos.

Verificación 2026-10-09: 64 tests Kotlin aprobados; 41 tests servidor aprobados y uno omitido por depender de origen externo. Migraciones SQLDelight, Android Debug, web producción e iOS Simulator ARM64 compilados. Pruebas de campos ausentes y parciales, límites, round-trip backup, SQL, edición para quitar perfil y conservación del perfil en sync. Sin prueba manual de UI en dispositivos en esta iteración.
