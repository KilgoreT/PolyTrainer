# Migration tests — IS481 component_constructor (M12 → M13)

## Решение

**Тесты нужны — да.** Миграция M12 → M13 критична для user data:

- Composite JSON rewrite в `component_values.value` (M12 `{"v":1,"text":"..."}` / `{"v":1,"uri":"..."}` → M13 `{"fields":{"value":{"type":"text","value":"..."}}}` / `{"fields":{"value":{"type":"image","uri":"..."}}}`) — затрагивает все existing user lexemes.
- DROP UNIQUE на двух индексах (`index_component_values_lexeme_id_component_type_id` + `index_component_types_dictionary_id_name`) — необратимая DDL; после удаления UNIQUE-проверки переезжают в UseCase.
- Rename `remove_date → removed_at` в `component_types` — потеря данных при ошибке rename.
- ADD COLUMN с timestamps backfill (`is_multi`, `created_at`, `updated_at` в `component_types`; `created_at`, `updated_at`, `removed_at` в `component_values`).
- Template-key consolidation `long_text → text` — без этого rows с `long_text` после M13 молча скрываются (F046).
- Phantom `Index(lexeme_id)` после drop UNIQUE — нужен для производительности `getForLexeme(lexemeId)`.

Без миграционных тестов любая ошибка в одном из 9 шагов миграции (см. `data_design_tree.md` #13) приводит к silent data corruption либо runtime crash на чтении.

## Затронутые миграции

- `from 12 to 13` — `Migration_012_to_013` (см. `data_design_tree.md` #13). 9 шагов:
  1. ALTER TABLE `component_types` RENAME COLUMN `remove_date → removed_at`.
  2. ALTER TABLE `component_types` ADD `is_multi / created_at / updated_at` + backfill.
  3. DROP INDEX `index_component_types_dictionary_id_name`.
  4. ALTER TABLE `component_values` ADD `created_at / updated_at / removed_at` + backfill.
  5. DROP INDEX `index_component_values_lexeme_id_component_type_id`.
  6. CREATE INDEX `index_component_values_lexeme_id` (phantom после drop UNIQUE).
  7. UPDATE `component_types SET template_key='text' WHERE template_key='long_text'`.
  8. UPDATE `component_values` JSON rewrite для `text` template.
  9. UPDATE `component_values` JSON rewrite для `image` template.

## Существующий test pattern reference

- **Основной шаблон:** `/Users/kilg/AndroidStudioProjects/PolyTrainer/core/core-db-impl/src/androidTest/java/me/apomazkin/core_db_impl/room/MigrationFrom11to12.kt` (375 строк, 12 Cases A-L).
  - `MigrationTestHelper(driver = BundledSQLiteDriver(), databaseClass = Database::class)`.
  - `helper.createDatabase(N).use { vN -> ... }` + `helper.runMigrationsAndValidate(N+1, listOf(Migration_N_to_Np1))`.
  - `@After cleanUp()` — удалить `-shm/-wal/-journal` файлы (важно для повторных прогонов).
  - File-private extension helpers: `insertDictionary / insertWord / insertLexeme / countWhere / scalarText / scalarLong`.
  - `PRAGMA foreign_keys=ON` явно (helper не делает автоматически).
- **Idempotency pattern:** `/Users/kilg/AndroidStudioProjects/PolyTrainer/core/core-db-impl/src/androidTest/java/me/apomazkin/core_db_impl/room/MigrationFrom11to12IdempotencyTest.kt` (322 строки).
  - `migrateImpl(connection, failAfterStep = N)` test-hook → `MigrationTestFailureException` → `BEGIN / ROLLBACK` через прямой `BundledSQLiteDriver().open(...)`.
  - Phase 1: failure → assert v11 целый (user_version=11, новые колонки/таблицы отсутствуют, данные сохранены).
  - Phase 2: retry через `helper.runMigrationsAndValidate` → assert v12 валиден.
  - `MigrationTestFailureException` reuse — internal class в `migrations/` package, доступен из `Migration_012_to_013` без нового объявления.
- **Гайд:** `/Users/kilg/AndroidStudioProjects/PolyTrainer/docs/guides/testing-migrations.md` — обязательное чтение.

## Созданные тесты

### Файл 1: `MigrationFrom12to13.kt` (instrumented, основной)

**Путь:** `/Users/kilg/AndroidStudioProjects/PolyTrainer/core/core-db-impl/src/androidTest/java/me/apomazkin/core_db_impl/room/MigrationFrom12to13.kt`

**Соответствует узлу #19 в `data_design_tree.md` (Tier 8).**

10 кейсов A-J, каждый — отдельный `@Test`. Сетап data на v12 → `runMigrationsAndValidate(13, listOf(Migration_012_to_013))` → assertions против v13 БД.

#### Case A — Translation-only (built-in row backfill)

**Сценарий.** Built-in `component_types` row (`system_key='translation'`, `dictionary_id=NULL`) с одним `component_values` row на M12 формате (text).

**Сетап v12 (Variant A — v11 → M11→M12, seed translation внутри migration):**
```kotlin
val v11 = helper.createDatabase(11)
v11.insertDictionary(id = 1, name = "EN")
v11.insertWord(id = 1, dictionaryId = 1, value = "cat")
v11.insertLexeme(id = 1, wordId = 1)
v11.close()

// M11→M12 seedит built-in `translation` row в component_types
val v12 = helper.runMigrationsAndValidate(12, listOf(Migration_011_to_012))
v12.insertComponentValueText(
    id = 1,
    lexemeId = 1,
    typeId = v12.lookupBuiltInTypeId("translation"),
    text = "кошка",
) // INSERT с `{"v":1,"text":"кошка"}`
v12.close()

// Затем M12→M13:
val v13 = helper.runMigrationsAndValidate(13, listOf(Migration_012_to_013))
// assertions directly on v13
v13.close()
```

**Setup convention (см. § Helpers):** built-in `translation` row невозможно засеять через `helper.createDatabase(12)` — Room test helper не вызывает migration callback / seed. Для тестов которые требуют built-in типы — обязательно стартовать с v11 и проходить через M11→M12, либо использовать explicit `insertBuiltInComponentType` helper (если built-in semantics не критичны для теста).

**Assertions v13:**
- `component_types` row `system_key='translation'` получил: `is_multi=0`, `created_at > 0`, `updated_at > 0`, `removed_at IS NULL`.
- `SELECT COUNT(*) FROM component_types WHERE system_key='translation'` == 1 (built-in row не задвоился после миграции).
- `component_values` row id=1: `value = '{"fields":{"value":{"type":"text","value":"кошка"}}}'`, `created_at > 0`, `updated_at > 0`, `removed_at IS NULL`.

#### Case B — User-defined text-template с rewrite

**Сценарий.** User-defined `component_types` row (`system_key=NULL`, `dictionary_id=1`, `name='Definition'`, `template_key='text'`) с двумя `component_values` (разные lexemes).

**Сетап v12:**
```kotlin
v12.insertDictionary(1, "EN")
v12.insertWord(1, 1, "cat"); v12.insertWord(2, 1, "dog")
v12.insertLexeme(1, 1); v12.insertLexeme(2, 2)
val definitionTypeId = v12.insertUserDefinedComponentType(
    dictionaryId = 1, name = "Definition", templateKey = "text",
)
v12.insertComponentValueText(id = 1, lexemeId = 1, typeId = definitionTypeId, text = "pet animal")
v12.insertComponentValueText(id = 2, lexemeId = 2, typeId = definitionTypeId, text = "доме́шнее")
```

**Assertions v13:**
- `Definition` type получил `is_multi=0`, timestamps backfilled, `removed_at IS NULL`.
- Оба `component_values` rows: M13 JSON формат `{"fields":{"value":{"type":"text","value":"..."}}}` с правильным content (raw text и unicode сохранены, escape корректен).

#### Case C — User-defined image-template с rewrite

**Сценарий.** User-defined тип с `template_key='image'` + один value M12 формата (`{"v":1,"uri":"file:///tmp/photo.jpg"}`).

**Сетап v12:**
```kotlin
v12.insertDictionary(1, "EN")
v12.insertWord(1, 1, "cat")
v12.insertLexeme(1, 1)
val imageTypeId = v12.insertUserDefinedComponentType(
    dictionaryId = 1, name = "Photo", templateKey = "image",
)
v12.insertComponentValueImage(
    id = 1, lexemeId = 1, typeId = imageTypeId, uri = "file:///tmp/photo.jpg",
) // INSERT с `{"v":1,"uri":"file:///tmp/photo.jpg"}`
```

**Assertions v13:**
- `component_values` row id=1: `value = '{"fields":{"value":{"type":"image","uri":"file:///tmp/photo.jpg"}}}'`.
- Type `Photo` остался с `template_key='image'` (НЕ переписан на `text`).

#### Case D — `long_text` template consolidation (F046)

**Сценарий.** User-defined тип с `template_key='long_text'` (legacy) + один value M12 формата (`{"v":1,"text":"..."}`).

**Сетап v12:**
```kotlin
v12.insertDictionary(1, "EN")
v12.insertWord(1, 1, "cat")
v12.insertLexeme(1, 1)
val longTextTypeId = v12.insertUserDefinedComponentType(
    dictionaryId = 1, name = "Notes", templateKey = "long_text",
)
v12.insertComponentValueText(id = 1, lexemeId = 1, typeId = longTextTypeId, text = "long note here")
```

**Assertions v13:**
- Type `Notes` получил `template_key='text'` (consolidated).
- `component_values` row id=1: M13 JSON `{"fields":{"value":{"type":"text","value":"long note here"}}}` (rewrite text подхватил после consolidation).

**Важно:** проверить порядок шагов миграции — `consolidateLongTextTemplateKey` обязан выполниться **до** `rewriteTextJson`, иначе `WHERE component_type_id IN (SELECT id FROM component_types WHERE template_key='text')` не подхватит former `long_text` rows.

#### Case D2 — Mixed `long_text` + `text` в одном dictionary (F180)

**Сценарий.** Dictionary имеет одновременно type с `template_key='long_text'` И type с `template_key='text'` + values для обоих. После consolidation оба matches `WHERE template_key='text'` → rewrite должен корректно обработать оба без duplicates / losses.

**Сетап v12:**
```kotlin
v12.insertDictionary(1, "EN")
v12.insertWord(1, 1, "cat"); v12.insertWord(2, 1, "dog")
v12.insertLexeme(1, 1); v12.insertLexeme(2, 2)
val longTextTypeId = v12.insertUserDefinedComponentType(
    dictionaryId = 1, name = "Notes", templateKey = "long_text",
)
val textTypeId = v12.insertUserDefinedComponentType(
    dictionaryId = 1, name = "Definition", templateKey = "text",
)
v12.insertComponentValueText(id = 1, lexemeId = 1, typeId = longTextTypeId, text = "long note A")
v12.insertComponentValueText(id = 2, lexemeId = 2, typeId = longTextTypeId, text = "long note B")
v12.insertComponentValueText(id = 3, lexemeId = 1, typeId = textTypeId, text = "short def A")
v12.insertComponentValueText(id = 4, lexemeId = 2, typeId = textTypeId, text = "short def B")
```

**Assertions v13:**
- Оба type'а имеют `template_key='text'` (consolidated).
- Все 4 values rewritten в M13 формат `{"fields":{"value":{"type":"text","value":"..."}}}` с правильным content по id.
- `SELECT COUNT(*) FROM component_values` == 4 (no duplicates, no losses).
- `SELECT COUNT(*) FROM component_values WHERE value LIKE '%"v":1%'` == 0 (нет остатков M12 формата).
- `SELECT COUNT(*) FROM component_types WHERE dictionary_id=1 AND template_key='text'` == 2 (оба type'а сохранены, ни один не потерян / не задвоен).

#### Case E — M12 soft-deleted type (rename column сохраняет значение)

**Сценарий.** User-defined тип с `remove_date != NULL` на M12 (soft-deleted в M12 семантике).

**Сетап v12:**
```kotlin
v12.insertDictionary(1, "EN")
val removedTypeId = v12.insertUserDefinedComponentType(
    dictionaryId = 1, name = "Archived", templateKey = "text", removeDate = 1700000000000L,
)
```

**Assertions v13:**
- Колонка `remove_date` отсутствует (`pragma_table_info('component_types') WHERE name='remove_date'` → 0 rows).
- Колонка `removed_at` существует.
- Type `Archived` имеет `removed_at = 1700000000000` (значение сохранено через RENAME COLUMN).
- `created_at > 0`, `updated_at > 0` (backfill сработал даже для soft-deleted type).
- `is_multi = 0`.

#### Case F — DROP UNIQUE `(dictionary_id, name)` на `component_types`

**Сценарий.** После M13 INSERT двух active rows с одинаковым `(dictionary_id, name)` не падает.

**Сетап v12:** один dictionary, один user-defined type `Foo` (per-dict).

```kotlin
v12.insertDictionary(1, "EN")
v12.insertUserDefinedComponentType(dictionaryId = 1, name = "Foo", templateKey = "text")
```

**Assertions v13:**
- INSERT второго row с теми же `(dictionary_id=1, name='Foo')` (через raw SQL) — НЕ кидает UNIQUE constraint violation.
- После INSERT — `countWhere("component_types", "dictionary_id=1 AND name='Foo'") == 2`.

**Важно:** этот тест валидирует именно DDL (drop UNIQUE), а не business-level enforcement. Business enforcement (`findActiveUserDefinedByName` + reject в UseCase) — на business test шаге.

#### Case G — DROP UNIQUE `(lexeme_id, component_type_id)` на `component_values`

**Сценарий.** После M13 INSERT двух active rows с одинаковым `(lexeme_id, component_type_id)` не падает (это валидно для `is_multi=true` типов).

**Сетап v12:** один lexeme, один user-defined type, один value на M12 (UNIQUE constraint на v12 не позволяет вставить второй row с теми же ключами).

**Timeline.** Setup на v12: 1 row (UNIQUE constraint M12 действует, нельзя вставить второй). После migration (v13): INSERT второго row с теми же `(lexeme_id, component_type_id)` через raw SQL — НЕ кидает UNIQUE, так как M13 step 5 dropped UNIQUE индекс `index_component_values_lexeme_id_component_type_id`.

**Assertions v13:**
- INSERT второго row с теми же `(lexeme_id=1, component_type_id=X)` через raw SQL — НЕ кидает UNIQUE.
- `countWhere("component_values", "lexeme_id=1 AND component_type_id=X") == 2`.

#### Case H — Valid-но-без-key JSON row (edge case, fail-soft)

**Сценарий.** Сетап содержит row с валидным JSON, но без обязательного ключа `text` / `uri` (например `'{"v":1}'` либо `'{"other":"x"}'`). Используем именно valid JSON — на bundled SQLite `json_extract` от malformed строки кидает `SQLITE_ERROR` вместо возврата NULL, что уронит миграцию вместо skip (см. F178).

**Сетап v12:**
```kotlin
v12.insertDictionary(1, "EN")
v12.insertWord(1, 1, "cat"); v12.insertLexeme(1, 1)
val typeId = v12.insertUserDefinedComponentType(dictionaryId = 1, name = "Bad", templateKey = "text")
v12.execSQL(
    "INSERT INTO component_values (id, lexeme_id, component_type_id, value) " +
        "VALUES (1, 1, $typeId, '{\"v\":1}')"
)
```

**Assertions v13:**
- Row id=1 НЕ упал миграцию (миграция прошла без exception).
- `value = '{"v":1}'` (остался в исходном виде; rewrite skip через `WHERE json_extract(value, '$.text') IS NOT NULL` фильтр — `json_extract` от valid JSON без ключа возвращает NULL).
- Timestamps backfilled (`created_at > 0`, `updated_at > 0`) — это применяется ко всем rows независимо от content.

**Важно:** парсер на чтении (`parseTemplateValues`) обработает row как `null` + Crashlytics log — это покрывается mapper test (#21 в `data_design_tree.md`), не здесь.

#### Case I — Phantom `Index(lexeme_id)` создан после drop UNIQUE

**Сценарий.** Verify что после M13 индекс `index_component_values_lexeme_id` существует и используется query planner'ом для `WHERE lexeme_id = ?`.

**Assertions v13:**
- `v13.scalarText("SELECT type FROM sqlite_master WHERE name='index_component_values_lexeme_id'") == "index"` (индекс существует и зарегистрирован как `index`).
- Старый composite UNIQUE индекс `index_component_values_lexeme_id_component_type_id` отсутствует (`SELECT COUNT(*) FROM sqlite_master WHERE name='index_component_values_lexeme_id_component_type_id'` → 0).
- Аналогично `index_component_types_dictionary_id_name` отсутствует.

**Намеренно без EXPLAIN QUERY PLAN:** формат вывода `EXPLAIN QUERY PLAN` зависит от bundled SQLite version и параметров query planner; substring assertion `"USING INDEX index_component_values_lexeme_id"` fragile. `sqlite_master` check — единственный надёжный способ verify что индекс действительно создан.

**Важно (F177):** конкретное имя `index_component_values_lexeme_id` зависит от Room/KSP convention (см. Open Q #6 в `data_design_tree.md`). Если при апгрейде Room schema будет генерироваться индекс с другим именем — assert надо adjust. Альтернатива (более устойчивая): использовать `LIKE 'index_%lexeme_id%'` либо `name LIKE 'index_component_values%lexeme_id%' AND name NOT LIKE '%component_type_id%'`. В тесте оставить comment с пометкой «KSP convention dependency».

#### Case J — Timestamps backfill всех existing rows

**Сценарий.** Verify что `created_at` / `updated_at` backfilled для всех rows в обеих таблицах.

**Сетап v12:** 2 dictionaries, 3 user-defined types (разные dictionaries), 5 component_values (mix translation + user-defined).

**Assertions v13:**
- `SELECT COUNT(*) FROM component_types WHERE created_at = 0` → 0 (все row'ы получили backfill).
- `SELECT COUNT(*) FROM component_types WHERE updated_at = 0` → 0.
- `SELECT COUNT(*) FROM component_values WHERE created_at = 0` → 0.
- `SELECT COUNT(*) FROM component_values WHERE updated_at = 0` → 0.
- **Semantic invariant (F183):** `SELECT COUNT(*) FROM component_types WHERE updated_at < created_at` → 0. Аналогично `SELECT COUNT(*) FROM component_values WHERE updated_at < created_at` → 0. Backfill UPDATE даёт `updated_at = created_at` (same timestamp) — допустимо равенство, но никогда `updated_at < created_at`.
- `removed_at IS NULL` для всех active rows (за исключением Case E с soft-deleted `remove_date != NULL` сохранённым через RENAME).

#### Case K — Special chars в text rewrite (F176, parity M11→M12)

**Сценарий.** Regression coverage для JSON rewrite с спец-символами (single quotes, double quotes, newlines, Cyrillic, emoji). Гарантирует что rewrite корректно escape'ит content при формировании M13 JSON.

**Сетап v12:**
```kotlin
v12.insertDictionary(1, "EN")
v12.insertWord(1, 1, "cat"); v12.insertLexeme(1, 1)
val typeId = v12.insertUserDefinedComponentType(
    dictionaryId = 1, name = "Notes", templateKey = "text",
)
v12.insertComponentValueText(
    id = 1, lexemeId = 1, typeId = typeId,
    text = "a 'b' \"c\" \nЯ 🦊",
)
```

**Assertions v13:**
- `component_values` row id=1: `value` после rewrite — valid JSON, который при `json_extract(value, '$.fields.value.value')` возвращает исходную строку `a 'b' \"c\" \nЯ 🦊` без потерь (правильный escape для quotes, newline, Cyrillic, emoji).
- Проверка через `scalarText("SELECT json_extract(value, '\$.fields.value.value') FROM component_values WHERE id=1") == "a 'b' \"c\" \nЯ 🦊"`.

#### Case L — FK CASCADE chain после M13 (F185)

**Сценарий.** ALTER TABLE операции в SQLite могут терять FK при recreate. M13 RENAME / ADD COLUMN формально не затрагивают FK, но без regression test нет гарантии что FK chain `dictionaries → words → lexemes → component_values` остался активен.

**Сетап v12:**
```kotlin
v12.insertDictionary(1, "EN")
v12.insertWord(1, 1, "cat")
v12.insertLexeme(1, 1)
val typeId = v12.insertUserDefinedComponentType(dictionaryId = 1, name = "Def", templateKey = "text")
v12.insertComponentValueText(id = 1, lexemeId = 1, typeId = typeId, text = "pet")
```

**После миграции v13 (`runMigrationsAndValidate(13, ...)`):**
- Использовать helper-returned connection напрямую + явно выставить `PRAGMA foreign_keys=ON` (helper не делает автоматически):
```kotlin
val v13 = helper.runMigrationsAndValidate(13, listOf(Migration_012_to_013))
v13.execSQL("PRAGMA foreign_keys=ON")
v13.execSQL("DELETE FROM dictionaries WHERE id=1")
// assert cascade (см. ниже)
v13.close()
```

**Pattern «helper-returned connection»:** `MigrationTestHelper.runMigrationsAndValidate(target, migrations)` возвращает `SQLiteConnection` после успешной валидации схемы. Использовать этот connection напрямую — никакого reopen через raw driver не нужно. Для FK / pragma-зависимого поведения после migration: `v13.execSQL("PRAGMA foreign_keys=ON")` на helper-returned connection.

**Assertions:**
- `countWhere("words", "id=1") == 0` (cascade удалил word).
- `countWhere("lexemes", "id=1") == 0` (cascade удалил lexeme).
- `countWhere("component_values", "id=1") == 0` (cascade дошёл до component_value).
- `countWhere("component_types", "id=$typeId") == 0` (component_type с `dictionary_id=1` тоже удалён cascade'ом от dictionaries).

### Файл 2: `MigrationFrom12to13IdempotencyTest.kt` (instrumented, idempotency)

**Путь:** `/Users/kilg/AndroidStudioProjects/PolyTrainer/core/core-db-impl/src/androidTest/java/me/apomazkin/core_db_impl/room/MigrationFrom12to13IdempotencyTest.kt`

**Соответствует узлу #20 в `data_design_tree.md` (Tier 8).**

#### M3 — Interrupted migration restart (idempotency)

Паттерн полностью повторяет `MigrationFrom11to12IdempotencyTest.m3_interruptedMigrationRestart_idempotency`:

**Phase 1 — failure injection:**
- Setup v12 с реалистичным dataset (Variant A — v11 → M11→M12 для seed built-in `translation`):
  ```kotlin
  val v11 = helper.createDatabase(11)
  v11.insertDictionary(1, "EN"); v11.insertDictionary(2, "DE")
  v11.insertWord(1, 1, "cat"); v11.insertWord(2, 1, "dog")
  v11.insertWord(3, 2, "Katze"); v11.insertWord(4, 2, "Hund")
  v11.insertLexeme(1, 1); v11.insertLexeme(2, 2); v11.insertLexeme(3, 3); v11.insertLexeme(4, 4)
  v11.close()

  // M11→M12 seedит built-in `translation` row в component_types
  val v12 = helper.runMigrationsAndValidate(12, listOf(Migration_011_to_012))
  val definitionTypeId = v12.insertUserDefinedComponentType(1, "Definition", "text")
  val photoTypeId = v12.insertUserDefinedComponentType(2, "Photo", "image")
  val translationId = v12.lookupBuiltInTypeId("translation")
  v12.insertComponentValueText(1, 1, translationId, "кошка")
  v12.insertComponentValueText(2, 1, definitionTypeId, "pet animal")
  v12.insertComponentValueText(3, 2, definitionTypeId, "barker")
  v12.insertComponentValueText(4, 3, translationId, "Katze")
  v12.insertComponentValueImage(5, 3, photoTypeId, "file:///tmp/katze.jpg")
  v12.insertComponentValueImage(6, 4, photoTypeId, "file:///tmp/hund.jpg")
  v12.close()
  ```
- Открыть raw connection через `BundledSQLiteDriver().open(instrumentation.targetContext.getDatabasePath(DB_NAME).absolutePath)` → `phase1Conn.execSQL("BEGIN")` → `Migration_012_to_013.migrateImpl(phase1Conn, failAfterStep = N)` → ловим `MigrationTestFailureException` → `phase1Conn.execSQL("ROLLBACK")`. Raw driver нужен именно для phase 1 — `migrateImpl` test-hook вызывается напрямую, минуя helper.
- Assert post-failure: `PRAGMA user_version == 12`, новые колонки отсутствуют (`columnExists("component_types", "is_multi") == false`, `columnExists("component_values", "removed_at") == false`), данные v12 целы (`SELECT value FROM component_values WHERE id=1` возвращает оригинальный `{"v":1,"text":"кошка"}`).
- **RENAME rollback proof (F179)** — обязательно для failAfterStep=1+ (когда RENAME уже был выполнен внутри транзакции до failure):
  - `columnExists("component_types", "remove_date") == true` (старое имя сохранилось после ROLLBACK).
  - `columnExists("component_types", "removed_at") == false` (новое имя не создалось).
  - Без этих assert'ов retry на step 1 упадёт «duplicate column» — нечем доказать что RENAME действительно откатан.

**Phase 2 — retry без injection:**
- `helper.runMigrationsAndValidate(13, listOf(Migration_012_to_013))`.
- Assert v13 валиден: `PRAGMA user_version == 13`, schema validated helper'ом, данные мигрированы корректно (JSON rewrite применился, timestamps backfilled).

**Кейсы failAfterStep** — отдельные `@Test` методы (по одному на каждый из 9 шагов миграции):
- `m3_failAfterStep1_renameRemoveDate` — failure после RENAME `remove_date → removed_at`. Phase 2 проверяет финальный rename.
- `m3_failAfterStep2_addComponentTypesColumns` — failure после ADD COLUMN `is_multi/created_at/updated_at`.
- `m3_failAfterStep3_dropUniqueComponentTypesDictName` — failure после DROP INDEX.
- `m3_failAfterStep4_addComponentValuesColumns` — failure после ADD COLUMN `created_at/updated_at/removed_at`.
- `m3_failAfterStep5_dropUniqueComponentValuesLexemeType` — failure после DROP INDEX.
- `m3_failAfterStep6_createComponentValuesLexemeIdIndex` — failure после CREATE INDEX phantom.
- `m3_failAfterStep7_consolidateLongTextTemplateKey` — failure после template-key consolidation. Phase 2 проверяет что long_text не приведёт к double-consolidate.
- `m3_failAfterStep8_rewriteTextJson` — failure после text JSON rewrite. **Критично** — Phase 2 повторяет text rewrite; idempotency через `WHERE json_extract(value, '$.text') IS NOT NULL` гарантирует skip уже переписанных M13 rows.
- `m3_failAfterStep9_rewriteImageJson` — failure после image JSON rewrite.

Минимум — **два теста**: failAfterStep=**1** (RENAME — самый рискованный DDL, F186) + failAfterStep=**8** (JSON rewrite — самый сложный по семантике). Допустимо расширить до 3-4 (добавить failAfterStep=2 ADD COLUMN backfill и/или failAfterStep=7 long_text consolidation). Полная 9-кратная матрица — overkill для MVP, но желательна для production confidence.

**Обоснование min=2 (F186):** step 1 (RENAME) — единственный DDL, который при partial-state приводит к «duplicate column» на retry; не покрыть его означает оставить regression-дыру на самом рискованном шаге. Step 8 — единственный шаг с complex data transformation (JSON rewrite). Эта пара даёт максимальный coverage за минимум test'ов: DDL risk + data transformation risk. Failures на других steps дают эквивалентный assertions pattern (state перед failed step vs финальный state).

#### M3b — WHERE filter idempotency на partial rewrite (F175)

Отдельный test без rollback, который реально exercise'ит `WHERE json_extract(value, '$.text') IS NOT NULL` идемпотентность фильтра на partial state (а не на полностью v12 state как M3 после ROLLBACK).

**Сценарий:**
- Setup v12: 3 lexemes, один user-defined `text` тип, 3 component_values на M12 формате (`{"v":1,"text":"A"}`, `{"v":1,"text":"B"}`, `{"v":1,"text":"C"}`).
- **Manual partial rewrite (commit, без rollback):** вручную через raw SQL переписать subset (например 2 из 3 rows) в M13 формат (`{"fields":{"value":{"type":"text","value":"A"}}}`), COMMIT.
- **Run migration снова** через `helper.runMigrationsAndValidate(13, listOf(Migration_012_to_013))`.

**Assertions:**
- Все 3 rows в финальном M13 формате (`json_extract(value, '$.fields.value.value')` возвращает соответствующее content для каждой).
- 2 уже-переписанные rows НЕ задвоились (count = 3, не 5).
- `json_extract` от М13 row даёт NULL на `$.text` → WHERE filter скипает уже-переписанные rows → нет double-wrap (нет `{"fields":{"value":{"type":"text","value":"{\"fields\":...}"}}}`).
- 1 оставшаяся M12 row корректно переписана в M13.

**Note:** этот test работает с partial state на disk (commit, не rollback), что недостижимо чистым failure-injection paths. Это второй угол coverage для idempotency — после M3 (ROLLBACK paths) и до полного matrix.

### Cascade tests

**Soft-delete cascade** (`softDeleteByTypeId` DAO, `component_type` → `component_values.removed_at`) — это business-логика, не миграция. Покрывается DAO тестами (`ComponentValueDao` test шага `data_test` либо unit-тестами в `LexemeApiImplTest`, не androidTest).

**FK CASCADE chain** (`dictionaries → words → lexemes → component_values`) после M13 покрыт **Case L** (F185). M13 не меняет FK definitions, но ALTER TABLE в SQLite потенциально может терять FK при recreate — Case L даёт regression guarantee что chain работает после ADD COLUMN / RENAME / DROP INDEX.

### Schema validation

**Не требует отдельного теста.** `helper.runMigrationsAndValidate(13, ...)` сам validate'ит schema через сравнение с `core/core-db-impl/schemas/me.apomazkin.core_db_impl.room.Database/13.json` (узел #15 `data_design_tree.md`). Если schema export не matches migration result — `runMigrationsAndValidate` кидает `IllegalStateException` с diff. Это покрывает требование «13.json export matches migration result».

**Edge-case (F187):** `13.json` отсутствует на момент написания тестов (генерируется KSP при build после bump `version=13`). На TDD-фазе тест **должен падать** именно на этом step с понятным error message — это ожидаемо и валидирует tooling.

**Expected TDD-фаза exception:** `IllegalStateException` (либо `FileNotFoundException`, оборачивающийся в `IllegalStateException` через `MigrationTestHelper`) с substring `"Cannot find the schema file"` в message. Зафиксировать в TDD-комментарии файла теста, чтобы агент `data_implement` понимал какую именно ошибку trigger'ить TDD-фазой и какую — игнорировать как "ожидаемо до schema export". После первого build с `version=13` KSP генерирует `13.json` → ожидаемая exception исчезает → тесты переходят к нормальным assertions.

### Helpers (file-private extensions)

Дублируем паттерн из `MigrationFrom11to12.kt` (file-private):

```kotlin
private fun SQLiteConnection.insertDictionary(id: Long, name: String) { ... }
private fun SQLiteConnection.insertWord(id: Long, dictionaryId: Long, value: String) { ... }
private fun SQLiteConnection.insertLexeme(id: Long, wordId: Long, wordClass: String? = null) {
    // M12 шейп — без translation/definition колонок (они удалены в M11→M12),
    // колонки: id, word_id, word_class (nullable), options, add_date, change_date (nullable).
    val wc = wordClass?.let { "'$it'" } ?: "NULL"
    execSQL("INSERT INTO lexemes (id, word_id, word_class, options, add_date) VALUES ($id, $wordId, $wc, 0, 0)")
}
private fun SQLiteConnection.insertUserDefinedComponentType(
    dictionaryId: Long?,
    name: String,
    templateKey: String,
    removeDate: Long? = null,
): Long {
    val dictId = dictionaryId?.toString() ?: "NULL"
    val rd = removeDate?.toString() ?: "NULL"
    execSQL(
        "INSERT INTO component_types (system_key, dictionary_id, name, template_key, position, remove_date) " +
            "VALUES (NULL, $dictId, '$name', '$templateKey', 0, $rd)"
    )
    return scalarLong("SELECT last_insert_rowid()")
}
private fun SQLiteConnection.lookupBuiltInTypeId(systemKey: String): Long =
    scalarLong("SELECT id FROM component_types WHERE system_key='$systemKey'")
private fun SQLiteConnection.insertComponentValueText(
    id: Long, lexemeId: Long, typeId: Long, text: String,
) {
    // Backslash first — иначе literal `\` в input будет интерпретирован как escape
    // для последующего n/r/t (e.g. `\n` literal → `\\n`, JSON requires `\\\\n`).
    val escaped = text
        .replace("\\", "\\\\")
        .replace("'", "''")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
        .replace("\t", "\\t")
    execSQL(
        "INSERT INTO component_values (id, lexeme_id, component_type_id, value) " +
            "VALUES ($id, $lexemeId, $typeId, '{\"v\":1,\"text\":\"$escaped\"}')"
    )
}
// Альтернатива (cleaner): `JSONObject(mapOf("v" to 1, "text" to text)).toString()` для построения JSON,
// затем SQL-escape только single quotes. Выбран manual escape ради zero dependencies в test fixture.
private fun SQLiteConnection.insertComponentValueImage(
    id: Long, lexemeId: Long, typeId: Long, uri: String,
) {
    execSQL(
        "INSERT INTO component_values (id, lexeme_id, component_type_id, value) " +
            "VALUES ($id, $lexemeId, $typeId, '{\"v\":1,\"uri\":\"$uri\"}')"
    )
}
private fun SQLiteConnection.columnExists(table: String, column: String): Boolean {
    prepare("PRAGMA table_info($table)").use { stmt ->
        while (stmt.step()) {
            if (stmt.getText(1) == column) return true
        }
    }
    return false
}

private fun SQLiteConnection.scalarLong(sql: String): Long {
    prepare(sql).use { stmt ->
        if (stmt.step()) return stmt.getLong(0)
    }
    return 0L
}

private fun SQLiteConnection.scalarText(sql: String): String? {
    prepare(sql).use { stmt ->
        if (stmt.step() && !stmt.isNull(0)) return stmt.getText(0)
    }
    return null
}

private fun SQLiteConnection.countWhere(table: String, where: String): Long =
    scalarLong("SELECT COUNT(*) FROM $table WHERE $where")
```

Дублирование между `MigrationFrom12to13.kt` и `MigrationFrom12to13IdempotencyTest.kt` — повторяем convention `MigrationFrom11to12*.kt` (file-private; вынос в shared util не делается чтобы не менять existing test classes).

## TDD контракт

Все тесты сейчас **падают** на compile / runtime:
- Файлы `Migration_012_to_013.kt`, `MigrationFrom12to13.kt`, `MigrationFrom12to13IdempotencyTest.kt` не существуют до `data_implement`.
- Schema export `13.json` отсутствует — тесты упадут с `IllegalStateException` от `runMigrationsAndValidate` на первом прогоне после реализации (после первого build с `version=13` KSP сгенерирует `13.json`).

После `data_implement` (#13 миграция + #11/#14 seed updates + #16 RoomModule registration + #3-#5 entity updates) — все тесты должны пройти.

## Запуск

```bash
# Локально на эмуляторе / устройстве
./scripts/cc-build.sh connectedDebugAndroidTest

# Только этот модуль
./scripts/cc-build.sh :core:core-db-impl:connectedDebugAndroidTest

# Один test class (через test orchestrator с filter; точный syntax зависит от gradle config)
./gradlew :core:core-db-impl:connectedDebugAndroidTest -PandroidTestClass=me.apomazkin.core_db_impl.room.MigrationFrom12to13
```

CI runs androidTest через connected emulator в GitHub Actions (см. `.github/workflows/`).

## Counts

- **Файлов:** 2 (`MigrationFrom12to13.kt` + `MigrationFrom12to13IdempotencyTest.kt`).
- **Кейсов в основном тесте:** 13 (A, B, C, D, D2, E, F, G, H, I, J, K, L).
  - A-J — изначальный coverage.
  - D2 — mixed `long_text` + `text` (F180).
  - K — special chars JSON rewrite (F176).
  - L — FK CASCADE chain после M13 (F185).
- **Кейсов idempotency:** минимум 2 (`failAfterStep=1` RENAME + `failAfterStep=8` JSON rewrite, F186) + 1 M3b test для WHERE-filter partial state (F175). Желательно расширить до steps 2, 7, 8 для full coverage; полная матрица — 9.
- **Total `@Test` методов:** 16-25 в зависимости от idempotency coverage.

## История ревью

### iter 1 (2026-06-17): 13 findings → 12 approved + 1 rejected

- Critical: F178 (malformed JSON risk), F179 (RENAME rollback proof), F180 (mixed dataset).
- Minor: F175, F176, F177, F181, F183, F184, F185, F186, F187.
- Rejected: F182 (dup F176).

### iter 2 (2026-06-21): 12 findings fixed.

- F178: Case H setup `'malformed-not-json'` → `'{"v":1}'` (valid JSON без key).
- F179: post-rollback RENAME assertions (`remove_date` exists, `removed_at` absent).
- F180: Case D2 — mixed `long_text` + `text` types в одном dictionary.
- F175: M3b test — WHERE filter partial-rewrite idempotency без rollback.
- F176: Case K — special chars (quotes, newline, Cyrillic, emoji).
- F177: Case I — KSP convention dependency comment + LIKE-альтернатива.
- F181: Case G — timeline уточнён (UNIQUE на v12, dropped в M13 step 5).
- F183: Case J — invariant `updated_at >= created_at`.
- F184: Case A — `COUNT(translation) == 1`.
- F185: Case L — FK CASCADE chain regression.
- F186: idempotency min coverage расширен до steps 1 + 8.
- F187: 13.json TDD — `IllegalStateException` + substring `"Cannot find the schema file"`.

### iter 3 (2026-06-21): F188-F192 fixed (5 findings: 2 critical + 3 minor)

- F188 + F189: setup pattern переключен на v11 → M11→M12 (Variant A, тестирует full upgrade path).
- F190: insertLexeme + word_class param.
- F191: Case L explicit reopen connection + PRAGMA pattern.
- F192: insertComponentValueText escape расширен (newline / tab / backslash).

### iter 4 (2026-06-21): F193-F199 fixed (4 critical + 3 minor)

- F193: API signatures rewrite на real Room 2.8.4 (1-arg createDatabase, 2-arg runMigrationsAndValidate returns connection).
- F194 + F196: удалён «reopen via raw driver» pattern (false premise), все setup blocks на helper-returned connection.
- F195: `getDatabasePath` через instrumentation.targetContext либо удалён.
- F197: escape comment переписан с правильным rationale.
- F198: EXPLAIN substring drop, sqlite_master only.
- F199: helper bodies inlined.

_model: claude-opus-4-7[1m]_
