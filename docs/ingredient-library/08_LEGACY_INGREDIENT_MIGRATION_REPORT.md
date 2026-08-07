# 08 — LEGACY INGREDIENT MIGRATION REPORT

**Phase:** 3 — Room v1 -> v2  
**Branch:** `program/ingredient-library-food-safety`  
**Validation date:** 2026-08-08  
**Status:** CLOSED / PASS

## Scope

This report records the controlled migration validation performed on the new Room v1 -> v2 path.

It is **not** a statistical analysis of the user's production database. The repository does not contain that private runtime database. Production-wide counts will only be known after a real device/database upgrade is observed.

## Migration policy implemented

Every legacy recipe ingredient is preserved as recipe data and assigned a deterministic custom origin:

```text
legacy:<ingredientId>
```

No fuzzy matching, partial-string inference or automatic clinical linking to the future master catalog is performed.

## Controlled fixture used by the migration test

The instrumented test creates a real SQLite database reproducing Room schema v1 and inserts:

- 1 recipe;
- 2 recipe ingredients;
- quantity and unit data;
- ingredient notes;
- ingredient order;
- favorite state;
- recipe timestamps;
- cover photo path;
- 1 preparation step;
- step timer;
- step photo path.

The same physical database is then opened through Room v2 with `MIGRATION_1_2` registered.

## Results

```text
assembleDebug                     PASS
testDebugUnitTest                 PASS
lintDebug                         PASS
compileDebugAndroidTestKotlin     PASS
connectedDebugAndroidTest         PASS — 34/34
skipped                           0
failed                            0
assembleRelease                   PASS — user-confirmed in closure sequence
```

Migration fixture accounting:

```text
legacy recipe ingredients analyzed       2
linked automatically to catalog          0
kept as custom legacy origins            2
ambiguous auto-links                     0
discarded                                0
lost                                     0
```

Required invariant:

```text
lost = 0
```

**PASS.**

## Data preservation verified

The migration test verifies that the following survive v1 -> v2 unchanged:

- recipe identity and name;
- description and category;
- servings;
- preparation and cooking minutes;
- recipe notes;
- favorite state;
- cover photo path;
- `createdAt` and `updatedAt`;
- recipe ingredient identity;
- quantity;
- unit;
- ingredient name snapshot;
- ingredient notes;
- ingredient sort order;
- preparation step instruction;
- timer;
- step photo path;
- step sort order.

It also verifies:

- each migrated legacy ingredient points to its `legacy:<ingredientId>` custom origin;
- no catalog origin is invented;
- foreign-key integrity through `PRAGMA foreign_key_check`;
- the migrated database can be read through the normal Room/DAO stack.

## Room schema validation

Both exported schemas are now versioned:

```text
1.json
2.json
```

The reviewed Room v2 schema declares version 2 and contains 13 tables: the three legacy tables plus the catalog, alias, safety, custom-ingredient and metadata structures designed for the priority program.

The `ingredients` table retains all legacy fields and adds:

```text
catalogIngredientId: nullable FK -> catalog_ingredients.id
customIngredientId: nullable FK -> custom_ingredients.id
```

with indexes for both new references.

## Migration safety conclusion

The controlled v1 -> v2 migration gate is green:

```text
schema validation                 PASS
legacy preservation              PASS
photo-path preservation          PASS
favorite/timestamp preservation  PASS
legacy custom-origin assignment  PASS
foreign-key integrity            PASS
loss                             0
```

## Transitional integration requirement

Room v2 intentionally allows nullable origin references. Migrated v1 rows always receive a custom legacy origin, but the old pre-library editor can still create new ingredient rows with no origin until the runtime catalog/custom-ingredient flow is implemented.

Before the complete priority program can be merged to `master`, application logic must enforce:

```text
catalogIngredientId XOR customIngredientId
```

for all newly persisted recipe ingredients.

This is not a failure of the migration; it is a tracked requirement for the following implementation phases.

## Final Phase 3 decision

```text
Room v1 -> v2 migration           APPROVED
Phase 3                           CLOSED ✅
Catalog population                NOT PART OF THIS GATE
Merge to master                   NOT AUTHORIZED YET
```
