# Catálogo v12 — roles de visibilidad y gate Room v6

Fecha de cierre: 2026-08-10

## Objetivo

Cerrar la decisión arquitectónica de separar los ingredientes culinarios que deben aparecer en la biblioteca normal de las identidades técnicas creadas únicamente para representar condiciones regulatorias específicas.

La solución implementada corresponde a la opción B aprobada: mantener una única infraestructura de catálogo, pero asignar un rol explícito a cada identidad.

## Contrato de roles

El catálogo admite dos roles:

- `CULINARY`: ingrediente real utilizable y seleccionable desde la biblioteca normal.
- `REGULATORY_TECHNICAL`: identidad técnica necesaria para expresar una cláusula jurídica o un uso/proceso regulatorio concreto. Permanece disponible para el motor regulatorio, pero no se ofrece como ingrediente culinario normal.

La existencia de una exención regulatoria no convierte automáticamente un ingrediente en técnico. Ingredientes reales como lactitol, jarabes de glucosa, maltodextrinas, aceites de soja totalmente refinados u otros derivados siguen siendo `CULINARY` cuando representan ingredientes que una persona puede utilizar o encontrar en una receta.

## Catálogo v12

Se creó `ingredient-catalog/v12` como nueva fotografía a partir de v11. La fotografía v11 permanece inmutable.

v12 conserva el contenido regulatorio de v11 y añade el metadato opcional `catalogRole`. Cuando el campo no está presente, el importador aplica de forma conservadora `CULINARY`.

Las cinco identidades clasificadas como `REGULATORY_TECHNICAL` son:

1. `ing-gluten-cereals-alcoholic-distillates`
2. `ing-fish-gelatine-vitamin-carotenoid-carrier`
3. `ing-fish-gelatine-isinglass-beer-wine-fining`
4. `ing-whey-alcoholic-distillates`
5. `ing-nuts-alcoholic-distillates`

`ing-lactitol` permanece expresamente como `CULINARY`.

## Frontera de visibilidad

La consulta utilizada por la biblioteca normal exige ahora:

```sql
ci.catalogRole = 'CULINARY'
```

Por tanto, las identidades técnicas no aparecen en resultados de búsqueda ni siquiera cuando se busca su nombre exacto.

Esta restricción se aplica únicamente a la búsqueda destinada al usuario. La consulta interna por identificador no filtra por rol, por lo que las identidades `REGULATORY_TECHNICAL` continúan accesibles para resolución regulatoria, trazabilidad y auditoría.

No se ha modificado la separación entre:

- linaje culinario;
- evidencia de seguridad;
- exenciones regulatorias.

Una exención legal continúa sin equivaler a una afirmación de ausencia de alérgeno o de seguridad clínica.

## Room v5 → v6

`RecipeDatabase` pasa a versión 6.

La migración añade a `catalog_ingredients`:

```sql
catalogRole TEXT NOT NULL DEFAULT 'CULINARY'
```

El modelo Room declara el mismo valor por defecto mediante `@ColumnInfo(defaultValue = "'CULINARY'")`.

Consecuencia de compatibilidad: todos los ingredientes existentes en una base Room v5 migran inicialmente como `CULINARY`. La importación posterior de v12 reclasifica exclusivamente las identidades técnicas declaradas en el asset.

El contrato generado por Room está versionado en:

`app/schemas/com.rmm.recetasraquel.data.local.RecipeDatabase/6.json`

Datos de contrato:

- Room version: `6`
- identity hash: `7371cdda6f45ff9d90227b3b3b2d581d`
- esquemas históricos conservados: `1.json` a `6.json`

## Pruebas específicas

Se añadieron dos gates principales:

### `IngredientCatalogRoleMigration56Test`

Verifica que una base v5:

- migra correctamente a v6;
- conserva los datos anteriores del ingrediente;
- asigna `CULINARY` como valor inicial;
- mantiene integridad de claves foráneas;
- termina con `user_version = 6`.

### `CatalogV12RoleVisibilityTest`

Verifica que:

- v12 contiene exactamente las cinco identidades técnicas previstas;
- cada identidad técnica sigue siendo recuperable internamente por ID;
- ninguna de ellas aparece en la búsqueda culinaria normal;
- `Lactitol` sigue visible;
- `Jarabe de glucosa a base de trigo` sigue visible.

También se actualizaron los tests históricos para que cada uno mantenga su intención original:

- el gate de v11 lee explícitamente `ingredient-catalog/v11`;
- las rutas históricas de migración 1→, 2→ y 4→ continúan ahora hasta Room v6.

## Incidencia detectada durante el gate

La primera ejecución completa en emulador detectó cuatro fallos en tests históricos. No correspondían a defectos de producción de la opción B:

- un test de v11 estaba leyendo el catálogo por defecto, que ahora es v12;
- tres tests de migración terminaban en Room v5 aunque la base actual ya es v6.

Se corrigieron los contratos de prueba sin modificar la lógica de producción. La siguiente ejecución completa quedó verde.

## Evidencia final

GitHub Actions:

- workflow: `Android CI`
- run: `31389987870`
- run number: `115`
- commit validado: `2bae418d10fcd901868d1af79e9a07a61a35043c`
- resultado: `success`

Fase de calidad:

- `assembleDebug`: success
- unit tests: success
- lint: success
- compilación de tests instrumentados: success
- `assembleRelease`: success
- guard de Room: success

Emulador Android API 36:

- 67 tests ejecutados
- 0 omitidos
- 0 fallidos
- `BUILD SUCCESSFUL`
- guard de Room posterior: success
- conjunto de esquemas después de las pruebas: exactamente `1.json` … `6.json`, sin diferencias respecto al contrato versionado.

## Estado

**GATE SUPERADO.**

La biblioteca culinaria queda protegida frente a identidades jurídicas artificiales sin perder la capacidad del motor regulatorio para resolverlas internamente. v11 permanece inmutable y v12 pasa a ser la fotografía activa del catálogo.
