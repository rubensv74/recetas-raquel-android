# 08 — LEGACY INGREDIENT MIGRATION REPORT

**Phase:** 3 — Room v1 -> v2
**Branch:** `program/ingredient-library-food-safety`
**Validation date:** 2026-08-08

## Scope

This report records the controlled migration validation performed on the new Room v1 -> v2 path.

It is **not** a statistical analysis of the user's production database. The repository does not contain that private runtime database. Production-wide counts will only be known after a real device/database upgrade is observed.

## Migration policy implemented

Every legacy recipe ingredient is preserved as recipe data and is assigned a deterministic custom origin:

```text
legacy:<ingredientId>
```

No fuzzy matching, partial-string inference or automatic clinical linking to the future master catalog is performed.

## Controlled fixture used by the migration test

The instrumented test creates a real SQLite database reproducing Room schema v1, inserts:

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

The database is then opened through Room v2 with `MIGRATION_1_2`.

## Results

```text
compileDebugAndroidTestKotlin     PASS
connectedDebugAndroidTest         PASS — 34/34
skipped                           0
failed                            0
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

Local build generated both exported schemas:

```text
1.json
2.json
```

`2.json` must be committed to source control before Phase 3 is closed.

## Remaining Phase 3 gate items

1. Version `app/schemas/.../2.json` in Git.
2. Execute final `assembleRelease` regression.
3. Confirm working tree clean after the schema commit/push.
4. Review the exported v2 schema against the intended entity model.

No catalog population or UI implementation is authorized by this report.
