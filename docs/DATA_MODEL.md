# Modelo de datos conceptual

Este documento orienta el diseño futuro. No define todavía entidades Room ni un esquema físico.

## Recipe

Agregado principal de una receta. Tendrá identificador estable generado localmente, título obligatorio, descripción opcional, raciones, tiempos opcionales, notas y marcas de creación/modificación. Es propietaria del orden de ingredientes y pasos. Podrá referenciar etiquetas y fotos. La estrategia de borrado y posibles marcas de sincronización se decidirán antes de implementar persistencia.

## Ingredient

Elemento perteneciente a una receta: texto o nombre obligatorio, cantidad y unidad opcionales, notas y posición. La cantidad debe admitir valores prácticos de cocina sin forzar una precisión engañosa. Su ciclo de vida depende de `Recipe`.

## RecipeStep

Instrucción ordenada perteneciente a una receta, con texto obligatorio, posición y duración opcional. El orden será explícito y estable para edición y modo cocina.

## Tag

Etiqueta reutilizable con identificador y nombre normalizado único. Sirve para organizar y filtrar recetas sin imponer una jerarquía inicial.

## RecipeTag

Relación muchos-a-muchos entre `Recipe` y `Tag`, identificada conceptualmente por ambos IDs. No contiene contenido duplicado y debe preservar integridad referencial.

## Photo

Referencia a una imagen local asociada a una receta, con identificador, URI o ruta gestionada, tipo MIME, posición, texto alternativo opcional y metadatos mínimos. Se deberá definir propiedad, copia, eliminación y comportamiento en backup antes de implementarla; no se guardarán bitmaps grandes dentro de Room.

## SyncQueue (futura)

Registro persistente de una operación pendiente para una sincronización opcional futura: identificador, tipo de agregado, ID local, operación, versión o instante, número de intentos y último error. No forma parte del MVP ni se implementará hasta definir protocolo, conflictos, privacidad y destino. Room seguirá siendo la fuente de verdad.

## Reglas transversales pendientes

- IDs locales estables y aptos para exportación.
- Fechas almacenadas de forma inequívoca y convertidas solo en presentación.
- Orden mediante posiciones explícitas.
- Validación de campos obligatorios en dominio y restricciones equivalentes en almacenamiento.
- Migraciones versionadas y backups con versión de formato.
