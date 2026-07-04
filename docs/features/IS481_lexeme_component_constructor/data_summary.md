---
status: done
---

# Summary — data

## Что сделано

Реализован data слой IS481 — переход от legacy колонок
`lexemes.translation/.definition` к таблицам `component_types` (built-in +
user-defined per-dictionary типы) / `component_values` (per-lexeme значения, JSON
payload) / `quiz_configs` (per-(dictionary, quiz_mode), JSON массив componentRefs)
с миграцией M11→M12. STUB-реализация `LexemeApiImpl` (synthetic id `-1L`/`-2L`)
полностью заменена на честные DAO calls. Миграция данных: translation → built-in
TRANSLATION, definition → user-defined "Definition" per-dictionary; DROP COLUMN
`lexemes.translation/.definition`. 19 узлов design tree (13 `[+]` + 6 `[~]`), 186
unit-тестов PASS.

### Room entities (`core/core-db-impl/.../entity/`)

Создано 4: `ComponentTypeDb.kt` (FK CASCADE → dictionaries; UNIQUE `system_key`,
UNIQUE `(dictionary_id, name)`; `id, systemKey?, dictionaryId?, name?, templateKey,
position, removeDate?`); `ComponentValueDb.kt` (FK CASCADE → lexemes + component_types;
UNIQUE `(lexeme_id, component_type_id)`; `value: String` JSON);
`ComponentValueWithType.kt` (`@Embedded` + `@Relation` — Multi-level flatten в 1
API DTO); `QuizConfigDb.kt` (FK CASCADE → dictionaries; UNIQUE `(dictionary_id,
quiz_mode)`; `componentRefs: String` JSON array). Модифицировано: `LexemeDb.kt` —
удалены `translation`/`definition` колонки; `LexemeDbEntity.kt` — добавлен
`@Relation val componentValueListDb: List<ComponentValueWithType>`, `toApiEntity()`
переписан honest (synthetic ушёл).

### Mappers + DAOs (`core/core-db-impl/.../mapper/`, `.../room/dao/`)

JSON: `ComponentValueDataJson.kt` (`toJson()`/`toComponentValueData(template)` через
`org.json.JSONObject`, PAYLOAD_VERSION = 1); `ComponentTypeRefJson.kt`
(discriminator `type: "builtin"|"user"`, defensive parser → `emptyList()` на
corrupt JSON — D4). 3 новых DAO (отдельные интерфейсы): `ComponentTypeDao` (flow/get,
`getBySystemKey`, `softDelete` с защитой built-in через `WHERE system_key IS NULL`);
`ComponentValueDao` (`getForLexeme/AndType`, CRUD, `countForLexeme` — D3);
`QuizConfigDao` (`getByDictionaryAndMode`, CRUD, default-method
`insertDefaultQuizConfig` пишет hardcoded JSON
`[{"type":"builtin","key":"translation"}]` — MIN-8).

### Migration + seed (`core/core-db-impl/.../room/`)

`Migration_011_to_012.kt` — `object : Migration(11, 12)` directly на
`SQLiteConnection` API (AGG-12). 10 шагов в порядке (D1): CREATE TABLES (1-3) →
seedBuiltIns (4) → createUserDefinedDefinitionTypes (5) → migrateTranslationData
через `json_object('v',1,'text',...)` (6) → migrateDefinitionData (7) →
insertDefaultQuizConfigs для ВСЕХ dictionaries (8, F1) → addDefinitionToQuizConfigs
через `json_insert($, '$[#]', ...)` (9) → ALTER TABLE DROP COLUMN (10). Все SELECT
из legacy колонок — ДО DROP. `SeedBuiltIns.kt` — top-level `internal fun
seedBuiltIns(connection)`: INSERT OR IGNORE built-in TRANSLATION + CREATE partial
UNIQUE INDEX `index_component_types_global_userdef_name` WHERE `dictionary_id IS
NULL AND system_key IS NULL`. Идемпотентна, переиспользуется из migration step 4 +
из `RoomModule.Callback.onCreate` (B1 fresh install).

### WordDao + Database + DI

`WordDao.kt`: удалены legacy `updateLexemeTranslation/Definition` raw-SQL; добавлен
`@Transaction addLexemeWithComponents(lexemeDb, dictionaryId, components:
List<Pair<Long, String>>)` — atomic INSERT lexeme + write_quiz + N component_values
(MIN-9 + IS479 F1); `addDictionary` переписан на `@Transaction` default-method:
INSERT dictionary + INSERT default quiz_config (AGG-4 atomic). `Database.kt` —
`version = 12`, entities 6→9, +3 abstract DAO. `RoomModule.kt`:
`.addMigrations(Migration_011_to_012)` подключён; `Callback.onCreate(connection)`
→ `seedBuiltIns(connection)` (B1); `@Provides` для трёх новых DAO; KDoc под v12.

### CoreDbApiImpl

`DictionaryApiImpl.addDictionary` — через `wordDao.addDictionary` (atomic +
default quiz_config). `LexemeApiImpl` — synthetic STUB удалён, inject 3 новых DAO;
9 методов переписаны: `addLexemeWithBuiltInComponent` (lookup `getBySystemKey` →
atomic); `addLexemeWithUserDefinedComponent` (lookup `getTypesForDictionary`
filter, miss → `logger.e` + null); `addLexemeWithComponents` (resolve refs, unresolved
→ null, иначе compound atomic MIN-9); `add/update/deleteComponentValue` (delete →
remaining через `countForLexeme`); `getComponentTypes/getQuizConfig` (DAO +
`toApiEntity`); `addLexemeWithTranslation` @Deprecated A3 (delegate);
`updateLexemeTranslation` @Deprecated (lookup built-in TRANSLATION typeId →
update/delete/insert через `componentValueDao`).

### Migration tests + schema

`MigrationFrom11to12.kt` (androidTest) — 12 кейсов A-L реализованы (harness
`MigrationTestHelper(driver = BundledSQLiteDriver(), databaseClass = Database::class)`,
Room 2.8.x KMP API): A. translation-only; B. translation + definition; C.
definition-only; D. empty dictionary (F1); E. quiz_config с обоими ref; F. FK
cascade chain (MIN-4); G. cascade `component_types → component_values` (MIN-10); H.
AGG-8 `json_insert` syntax; I. partial UNIQUE exists; J. orphan lexeme; K. special
chars; L. UNIQUE `(dictionary_id, quiz_mode)`. Schema `12.json` автогенерируется
Room KSP; имена индексов matched с Migration (D2); partial UNIQUE — Room не
моделирует WHERE, test I проверяет через `sqlite_master`.

### Тесты

| Module | Tests | Status |
|---|---|---|
| `:app` | 59 | PASS |
| `:modules:screen:wordcard` | 110 | PASS |
| `:modules:screen:quiz:chat` | 12 | PASS |
| `:modules:domain:lexeme` | 5 | PASS |

**Total: 186 unit tests passed.** Backward-compat не сломан, business shim
работает на переписанной data layer. Migration A-L реализованы;
`connectedDebugAndroidTest` не запускался (followup на check).

## Ключевые решения

- **D1 — Порядок шагов миграции.** Все SELECT из legacy колонок ДО `ALTER TABLE
  DROP COLUMN`. User-defined Definition типы ДО component_values INSERT (FK ready).
  Default quiz_config INSERT (F1) ДО UPDATE через `json_insert`. Всё под Room
  auto-tx savepoint.
- **D2 — Index names совпадают с Room KSP.** `index_<table>_<col1>_<col2>` matched
  с `12.json`, иначе `runMigrationsAndValidate` падает. Partial UNIQUE через
  `seedBuiltIns` (Room не моделирует WHERE).
- **D3 — `deleteComponentValue` honest return.** Helper
  `ComponentValueDao.countForLexeme` добавлен — не было в планах.
- **D4 — Defensive parser `ComponentTypeRefJson`.** try/catch → `emptyList()` на
  corrupt JSON. Quiz session graceful skip'ает сломанные конфиги (AGG-5).
- **MIN-8 — Default quiz_config через DAO default-method.** Hardcoded JSON
  напрямую без domain `QuizConfig` — снимает зависимость DAO на domain bootstrap.
- **MIN-9 — `addLexemeWithComponents` через DAO @Transaction.** WordDao метод
  (consistency с `addLexemeWithQuiz`), не `db.withTransaction` в UseCase.
- **MIN-10 — Direct cascade `component_types → component_values`** через FK
  CASCADE. Test G покрывает.
- **Multi-level @Relation flatten в 1 API DTO** — `ComponentValueWithType` →
  `ComponentValueApiEntity(type embedded, data)`. First use паттерна.
- **`seedBuiltIns` переиспользуется fresh install + migration** — идемпотентный
  top-level fun из `Migration.migrate()` step 4 и `Callback.onCreate` (B1).

### Известные ограничения / followup

- **N/O/P integration runtime tests** (`addDictionary` auto-INSERT, atomicity
  rollback, `addLexemeWithComponents` FK rollback) — описаны в migration_test § 3,
  не реализованы как test class. Followup для check.
- **M3 interrupted migration** — сложный injection failure mid-migration; опционально.
- **F6 cleanup `quiz_configs.component_refs`** при DELETE component_type — нет
  операции в IS481; backlog «Quiz config UX».
- **`connectedDebugAndroidTest` / `:app:lintDebug` / `:app:assembleDebug`** не
  запускались (запрет prompt'а) — шаг check должен запустить.

## Артефакты

- [`data_walkthrough.md`](data_walkthrough.md) — discovery 7 секций (Database v11,
  entities, RoomModule post-prereq, WordDao, CoreDbApiImpl STUB inventory, mappers,
  schema export, patterns verdict).
- [`data_design_tree.md`](data_design_tree.md) — DAG из 19 узлов (13 `[+]` + 6 `[~]`).
- [`data_migration_test.md`](data_migration_test.md) — 12 migration cases A-L + 3
  integration N-P + harness skeleton; M3 опционально.
- [`data_implement.md`](data_implement.md) — отчёт реализации; verify+gap-fill
  approach; decisions D1-D4; 186 unit tests PASS.

_model: claude-opus-4-7[1m]_
