# Approved findings — data_migration_test.md, iteration 3

7 approved (4 critical + 3 minor). Все про неверный API MigrationTestHelper, введённый iter 2 sub-agent без verify через source.

## Critical — API alignment с real Room 2.8.4

### F193 — incorrect runMigrationsAndValidate / createDatabase signatures

Spec lines 60, 66, 79, 314, 350 используют:
- `helper.createDatabase("test.db", 11)` — 2-arg.
- `helper.runMigrationsAndValidate("test.db", 12, true, Migration_011_to_012)` — 4-arg.

**Реальный API Room 2.8.4 (verified MigrationFrom11to12.kt:50,55):**
- `helper.createDatabase(11): SQLiteConnection` — 1-arg.
- `helper.runMigrationsAndValidate(12, listOf(Migration_011_to_012)): SQLiteConnection` — 2-arg, **returns connection**.

**Что исправить:** заменить все вызовы на real signature.

### F194 — false «не возвращает connection» claim

Spec line 322 утверждает «`runMigrationsAndValidate` не возвращает connection». Это false (verified). Real API возвращает SQLiteConnection (см. MigrationFrom11to12.kt:55).

**Что исправить:** удалить раздел «Pattern: reopen connection after migration» (он целиком построен на false premise). Заменить на «использовать возвращённую connection напрямую».

### F195 — `getDatabasePath` free-standing

Spec lines 69, 315, 353: `BundledSQLiteDriver().open(getDatabasePath("test.db").absolutePath)`. `getDatabasePath` — Context instance method.

**Что исправить:** убрать вообще (F194 → reopen pattern избыточен), либо если где-то реально нужен (idempotency raw driver): использовать pattern `instrumentation.targetContext.getDatabasePath(DB_NAME)` как в MigrationFrom11to12IdempotencyTest.kt:37.

### F196 — «reopen via raw driver to seed» избыточен

Sections lines 311-320 и 353-363: «open second `BundledSQLiteDriver()` connection after runMigrationsAndValidate to seed/assert». Pattern построен на F194 false premise.

Real pattern (MigrationFrom11to12.kt:218-230): использовать helper-returned connection напрямую (`v12.execSQL("PRAGMA foreign_keys=ON")`, `v12.insertComponentValueText(...)`, и т.д.).

**Что исправить:** переписать ВСЕ setup и assertion blocks (Case A, Case L, M3 idempotency phase 1) на real pattern:

```kotlin
val v11 = helper.createDatabase(11)
v11.insertDictionary(...)
v11.insertLexeme(...)
v11.close()

val v12 = helper.runMigrationsAndValidate(12, listOf(Migration_011_to_012))
v12.insertComponentValueText(1, 1, v12.lookupBuiltInTypeId("translation"), "кошка")
v12.close()

val v13 = helper.runMigrationsAndValidate(13, listOf(Migration_012_to_013))
// assertions on v13 directly
v13.close()
```

Для Case L где нужен FK ON: `v13.execSQL("PRAGMA foreign_keys=ON"); v13.execSQL("DELETE FROM dictionaries WHERE id=1"); ...`. **Без** reopen via raw driver.

## Minor

### F197 — escape order comment

`insertComponentValueText` helper escape comment line 454: «backslash first, иначе double-escape для последующих `\n`/`\t`» — misleading. Real reason: literal backslash в input должен escapиться раньше, иначе `\\n` smешается с literal `n` после backslash.

**Что исправить:** заменить комментарий на:
```
// Backslash first — иначе literal `\` в input будет интерпретирован как escape
// для последующего n/r/t (e.g. `\n` literal → `\\n` JSON requires `\\\\n`).
```

### F198 — EXPLAIN substring fragile

Case I line 255: substring `USING INDEX index_component_values_lexeme_id`. SQLite EXPLAIN format varies.

**Что исправить:** убрать EXPLAIN assertion полностью, оставить только sqlite_master check (line 259 уже допускает это как «упрощённая версия»). Либо использовать `assertTrue(output.contains("index_component_values_lexeme_id"))` без полной формулы.

### F199 — helper bodies placeholders

Lines 477-480: `columnExists`, `countWhere`, `scalarText`, `scalarLong` объявлены с body `{ ... }`.

**Что исправить:** либо inline implementations (parity с MigrationFrom11to12IdempotencyTest.kt:289-321), либо явная ссылка «// reuse from MigrationFrom11to12IdempotencyTest.kt:289-321» для каждого helper.

## Lesson (IS481cc-F8 repeat)

Iter 2 sub-agent ввёл bogus API без verify через existing project test file. Iter 3 sub-agent fix должен **ОБЯЗАТЕЛЬНО** Read `MigrationFrom11to12.kt` + `MigrationFrom11to12IdempotencyTest.kt` ДО переписывания setup pattern. Real API убедительно отличается от того что есть в spec сейчас.
