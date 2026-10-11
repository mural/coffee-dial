# Asistente de espresso

Accesible desde Home → Ajustar espresso y desde el detalle de cada shot.
Requiere registrar sabor, preparación, tueste (puede ser desconocido) y objetivo de intensidad. Permite distinguir espresso solo de bebida terminada y registrar el molinillo.

Propone ensayos explicados, cambiando una sola variable por vez. No diagnostica sub/sobreextracción a partir de tiempo o ratio. Los porcentajes de ajuste son heurísticas conservadoras del producto, no recetas universales. La dosis se mantiene; tiempo, primera gota, notas y evaluación deben medirse nuevamente. Cuando sugiere molienda, exige registrar el ajuste real sin inventar clicks.

Ejemplo: 16 g / 46 g, tueste medio, débil, objetivo fuerte → probar 36 g manteniendo dosis y molienda. Para bebidas terminadas ajusta primero una adición (agua o leche); sabor ácido/amargo pide evaluar el espresso solo.

Cada nuevo intento conserva el ID del anterior. La cadena tolera ancestros eliminados y ciclos importados. Se pueden marcar favoritos. Dos o más referencias comparables permiten usar un ratio personal: mismo café, máquina, molinillo, molienda, tueste conocido, objetivo, base de evaluación, temperatura, estilo y adiciones, con dosis dentro del 10%. Referencias: favoritos o shots equilibrados con al menos 4 puntos. Funciona localmente, sin proveedor de IA.

Persistencia: migración SQL 11, backup versión 8, campo opcional dialIn. Backups anteriores siguen admitidos; el servidor valida el nuevo campo. Publicar servidor antes del cliente. Clientes antiguos deben actualizarse para leer backups v8.

Fundamento de los ensayos: [Espresso Compass](https://www.baristahustle.com/the-espresso-compass/) y [Basic Espresso Theory](https://home.lamarzoccousa.com/basic-espresso-theory/). El sabor y la preferencia del usuario deciden el siguiente paso.
