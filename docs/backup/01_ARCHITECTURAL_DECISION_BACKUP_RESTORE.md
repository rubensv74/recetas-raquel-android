# ADR — Backup/Restore para Recetas de Raquel v1

**Estado:** DECISIÓN PENDIENTE  
**Fecha:** 2026-08-11

## Contexto

La aplicación es completamente local-first. Los datos de valor de la usuaria viven en dos lugares:

1. Room (`recipes.db`): recetas, usos de ingredientes, pasos, ingredientes personalizados y declaraciones asociadas.
2. Almacenamiento privado (`filesDir/recipe_photos/...`): fotografías de portada y pasos.

El catálogo maestro v12 se distribuye con la aplicación y puede reconstruirse desde assets. La copia no debe depender de conectividad ni de un servicio cloud.

La decisión importante no es solo cómo exportar, sino cómo permitir que una copia creada hoy pueda restaurarse de forma segura después de que Room o el catálogo evolucionen.

## Requisitos no negociables

- Export manual iniciado por la usuaria.
- Archivo que pueda guardarse fuera del sandbox mediante Android Storage Access Framework.
- Fotografías incluidas.
- Formato versionado.
- Validación completa antes de modificar datos actuales.
- Restore con confirmación explícita.
- Fallo seguro: una copia corrupta no puede destruir la instalación vigente.
- Sin red, cuentas ni permisos globales de almacenamiento.
- No depender del nombre físico o layout interno de SQLite para siempre.
- Pruebas automáticas de round-trip.

## Opción A — Copia física de Room + fotografías

Archivo ZIP aproximado:

```text
backup.zip
  recipes.db
  photos/...
  manifest.json
```

### Ventajas

- Exportación conceptualmente simple.
- Conserva exactamente el estado físico de la base.
- Restauración rápida cuando la versión de Room coincide.

### Problemas

- Fuerte acoplamiento al esquema físico de Room/SQLite.
- Restaurar una base v6 sobre una app futura v8/v9 requiere coordinación delicada de migraciones y lifecycle de Room.
- Hay que controlar WAL/SHM y cerrar/reabrir la base de forma segura.
- Dificulta inspección, compatibilidad y evolución del formato.
- Un fallo durante sustitución física de la base tiene un radio de daño alto.

**Valoración:** válida como snapshot técnico, pero poco apropiada como contrato de backup personal a largo plazo.

## Opción B — Backup lógico versionado + fotografías — RECOMENDADA

Archivo ZIP transportable:

```text
recetas-raquel-backup.zip
  manifest.json
  user-data.json
  photos/
    ...
```

`manifest.json` incluiría como mínimo:

- `backupFormatVersion`;
- versión de app;
- Room version de origen;
- catalogVersion de origen;
- fecha/hora de creación;
- contadores esperados;
- checksums de archivos relevantes.

`user-data.json` contendría únicamente información que debe sobrevivir a una reinstalación:

- recetas;
- ingredientes usados en cada receta;
- pasos;
- favoritos/metadatos de receta;
- ingredientes personalizados;
- alias personalizados;
- declaraciones de seguridad personalizadas necesarias para reconstruirlos.

Las identidades maestras de catálogo se referencian por IDs estables, no se duplica todo el catálogo regulatorio dentro del backup.

### Restore propuesto para v1

**Replace all**, no merge.

Flujo:

1. usuaria selecciona archivo;
2. copiar ZIP a staging privado;
3. validar estructura, versión, JSON, checksums y fotografías;
4. validar referencias de catálogo/grupos requeridas;
5. construir un plan de restore sin tocar datos actuales;
6. pedir confirmación explícita indicando que sustituirá el archivo local;
7. importar datos de usuario transaccionalmente;
8. promover fotografías desde staging;
9. si algo falla, conservar/restaurar el estado previo;
10. limpiar staging.

### Ventajas

- Desacopla el backup del esquema físico de Room.
- Permite migrar el formato de backup de forma independiente.
- Más fácil de validar, auditar y testear.
- Puede restaurarse en versiones futuras mediante adaptadores de formato.
- Reduce riesgo de corrupción por sustitución directa de SQLite.
- Encaja con Gson ya presente en el proyecto.

### Coste

- Más código de export/import.
- Hay que diseñar IDs, orden de inserción y rollback correctamente.
- Requiere definir compatibilidad de referencias de catálogo.

**Valoración:** mejor equilibrio entre seguridad, mantenibilidad y portabilidad para una app personal offline.

## Opción C — Backup híbrido lógico + snapshot físico

ZIP con:

```text
manifest.json
user-data.json
recipes.db
photos/...
```

El JSON sería el contrato portable y `recipes.db` un snapshot de diagnóstico/recuperación rápida.

### Ventajas

- Máxima información conservada.
- Snapshot útil para soporte técnico.

### Problemas

- Duplica datos y aumenta tamaño/complejidad.
- Dos representaciones pueden divergir.
- Obliga a decidir cuál es autoritativa.
- Añade superficie de test y seguridad que no aporta valor claro para v1.

**Valoración:** excesiva para el caso de uso actual.

## Decisión adicional incluida en la Opción B

Para v1 se recomienda **Replace all** en restore, no mezcla incremental.

El merge exige resolver:

- colisiones de UUID;
- recetas duplicadas;
- ingredientes personalizados equivalentes;
- conflictos de fotografías;
- relaciones de seguridad divergentes;
- orden y semántica de actualización.

Ese coste no está justificado para una aplicación de una sola usuaria cuyo backup representa una fotografía completa de su archivo personal.

## Compatibilidad propuesta

- Backup con `backupFormatVersion` desconocida: rechazar sin modificar datos.
- Backup creado por una app claramente más nueva con dependencias de catálogo no disponibles: rechazar con mensaje comprensible.
- IDs de catálogo ausentes en la instalación destino: no hacer fuzzy matching automático. El restore debe fallar de forma segura o aplicar una política explícita futura.
- Fotografías ausentes/corruptas respecto al manifest: rechazar el restore completo en v1; no producir restauraciones parciales silenciosas.

## Recomendación

**Opción B — Backup lógico versionado + fotografías, con restore Replace all.**

Es la opción que mejor conserva el principio local-first y evita convertir la estructura interna de Room v6 en un formato de archivo público permanente.

## Gate

Elegir:

- **A** — snapshot físico Room + fotos;
- **B** — backup lógico versionado + fotos, restore Replace all (**recomendado**);
- **C** — híbrido lógico + snapshot físico.

No se implementará Backup/Restore hasta cerrar esta decisión.