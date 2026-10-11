# Shot Timer

Acceso desde Home y Nuevo/Editar Shot. Iniciar, pausar, continuar y reiniciar; marca opcional de primera gota con posibilidad de quitarla. Usar los tiempos pasa total y primera gota al borrador sin guardar el shot ni alterar los demás campos. Volver pausa y conserva la medición. Tras recrear la pantalla/actividad se restaura pausado. Máximo 10 minutos, consistente con la validación del shot.

El cronómetro usa reloj monotónico; el refresco visual no acumula el tiempo. Primera gota se mide desde el inicio y está incluida en el total. Se puede editar manualmente en el formulario y aparece en el detalle. Debe estar entre cero y el tiempo total.

SQLDelight migración 9 agrega first_drop nullable. Backup v6 y sync conservan firstDrop; los backups anteriores siguen importándose y los clientes anteriores no pueden sobrescribir snapshots v6. Reconstruir las apps móviles antes de sincronizar datos nuevos.

Verificación 2026-10-09: 62 tests Kotlin aprobados, 40 servidor aprobados y 1 omitido; migraciones SQLDelight, Android Debug, web producción y Xcode Simulator ARM64 aprobados. Pruebas de pausas/reanudación, marca única, reinicio, límite, conservación en SQL/backup y validación del servidor. Falta prueba manual en dispositivos.
