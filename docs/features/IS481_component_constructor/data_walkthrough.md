---
status: done
---

# Data walkthrough — IS481 component_constructor

Discovery шаг data sub-flow. Факты о текущем состоянии data-слоя проекта (`:core:core-db-impl`, `:core:core-db-api`, `:modules:datasource:prefs`). На основе этих фактов на `data_design_tree` проектируется M12→M13 переход, новые DAO-методы, mapper и cleanup-стратегия.

> Текущая БД — **v12** (M11→M12 в production). Domain / core-db-api уже обновлены business-фазой под M13 (`isMulti / createdAt / updatedAt / removedAt`, `TemplateValues`, новые `flow*` / CRUD методы LexemeApi). Data-слой (`:core:core-db-impl`) **не обновлён** — `ComponentTypeDb / ComponentValueDb / mapper / DAO / Migration` всё ещё M12. Это та граница, которая должна закрыться data sub-flow.

## 1. Версия схемы + расположение

- **Текущая** `RoomDatabase.version = 12`. Источник: `core/core-db-impl/src/main/java/me/apomazkin/core_db_impl/room/Database.kt:32`.
- **Schema export dir**: `core/core-db-impl/schemas/me.apomazkin.core_db_impl.room.Database/<version>.json`.
- **Доступные schema-снапшоты**: `1.json` … `12.json` (после prereq `IS481 prereq` дропа 10 legacy миграций — все интересные миграционные сценарии живут от 11+).
- **TypeConverters**: `DateTimeConverter` (Date↔Long Unix ms). Источник: `core/core-db-impl/src/main/java/me/apomazkin/core_db_impl/converters/DateTimeConverter.kt:6-13`. **Используется любым новым `Date` полем (`created_at` / `updated_at` / `removed_at`) автоматически — отдельный converter не нужен**.

## 2. Entity слой (`:core:core-db-impl/entity/`)

### 2.1 `ComponentTypeDb` — текущее состояние (M12)

Файл: `core/core-db-impl/src/main/java/me/apomazkin/core_db_impl/entity/ComponentTypeDb.kt`

- Table: `component_types` (line 25).
- **Поля M12** (lines 40-49):
  - `id` (PK autogen).
  - `system_key: String?` — `null` для user-defined, non-null для built-in.
  - `dictionary_id: Long?` — `null` для global, non-null для per-dictionary.
  - `name: String?` — обязателен для user-defined.
  - `template_key: String` — `"text" / "long_text" / "image"` (M12).
  - `position: Int`.
  - `remove_date: Date? = null` — soft-delete колонка (M12 имя).
- **Индексы M12** (lines 34-38):
  - `Index("dictionary_id")` — non-unique;
  - `Index(value = ["system_key"], unique = true)` — гарантирует один row на built-in key;
  - `Index(value = ["dictionary_id", "name"], unique = true)` — `(dictionary_id, name)` UNIQUE.
- **FK**: `dictionary_id → dictionaries.id ON DELETE CASCADE` (lines 27-32).
- **Mapper**: `fun ComponentTypeDb.toApiEntity()` (line 51) — собирает `ComponentTypeApiEntity` через **non-nullable** `ComponentTemplate.fromKey(templateKey)`; **домен / API уже обновлён под nullable `fromKey()`** (см. § 5), поэтому текущий mapper в `core-db-impl` будет fail-compile / нуждается в переписке.

### 2.2 `ComponentValueDb` — текущее состояние (M12)

Файл: `core/core-db-impl/src/main/java/me/apomazkin/core_db_impl/entity/ComponentValueDb.kt`

- Table: `component_values` (line 22).
- **Поля M12** (lines 42-48):
  - `id`, `lexeme_id`, `component_type_id`, `value: String` (JSON).
  - **Никаких timestamp / removed_at колонок**.
- **Индексы M12** (lines 37-40):
  - `Index("component_type_id")`;
  - `Index(value = ["lexeme_id", "component_type_id"], unique = true)` — UNIQUE на пару, блокирует multi-cardinality.
- **FK CASCADE** на оба родителя (lines 23-36): `lexemes.id` и `component_types.id`.

### 2.3 `ComponentValueWithType` (multi-level `@Relation` helper)

Файл: `core/core-db-impl/src/main/java/me/apomazkin/core_db_impl/entity/ComponentValueWithType.kt`

- `@Embedded ComponentValueDb` + `@Relation(parentColumn="component_type_id", entityColumn="id") ComponentTypeDb` (lines 14-18).
- `toApiEntity()` (line 20) — собирает `ComponentValueApiEntity`. **Прямой вызов `value.value.toComponentValueData(typeApi.template)`** — на M13 (где `ComponentValueData` упразднён) → compile fail. Также: `ComponentValueApiEntity` уже требует `createdAt/updatedAt/removedAt` (см. § 5) — текущая реализация их не передаёт.

### 2.4 `LexemeDbEntity` — `@Relation` чтение лексемы с components

Файл: `core/core-db-impl/src/main/java/me/apomazkin/core_db_impl/entity/LexemeDbEntity.kt`

- `componentValueListDb: List<ComponentValueWithType>` (lines 22-27) — `@Relation(entity = ComponentValueDb::class, parentColumn = "id", entityColumn = "lexeme_id")`. Room не поддерживает WHERE в `@Relation` — после M13 в read'е будут подтягиваться soft-deleted `component_values`.
- `toApiEntity()` (line 34) — пробрасывает `componentValueListDb.map { it.toApiEntity() }` без фильтра. **Это место для post-load filter стратегии (F031 best-guess A) либо для замены на custom `@Query` (альтернатива B).**

### 2.5 Прочие entity (soft-delete patterns)

- `HintDb.removeDate: Date? = null` — soft-delete pattern, упомянут в KDoc `ComponentTypeDb` как стиль-precedent. Источник: `entity/HintDb.kt:15`. **`HintDb` НЕ затрагивается M13** (rename только в `component_types`, см. F002).
- `SampleDb.removeDate: Date? = null` — также soft-delete (`entity/SampleDb.kt:18`). **НЕ затрагивается M13**.
- `WordDb` — `addDate / changeDate`, БЕЗ `removeDate` (`entity/WordDb.kt:23-30`). Подтверждает F038.
- `LexemeDb` — `addDate / changeDate`, БЕЗ `removeDate` (`entity/LexemeDb.kt:27-35`).
- `QuizConfigDb` — `id / dictionary_id / quiz_mode / component_refs: String (JSON)`, UNIQUE `(dictionary_id, quiz_mode)` (`entity/QuizConfigDb.kt:34-40`).

**Вывод по entity:** convention timestamp колонок — `Date?` (с `DateTimeConverter`); soft-delete колонка хранится как `Date? = null`; **существующая convention имени — `removeDate` (camelCase в Kotlin) ↔ `removeDate` либо `remove_date` в DB**. M13 предлагает rename `remove_date → removed_at` **только в `component_types`** (других точечных rename'ов в проекте не встречается).

## 3. DAO слой (`:core:core-db-impl/room/dao/`)

### 3.1 `ComponentTypeDao`

Файл: `core/core-db-impl/src/main/java/me/apomazkin/core_db_impl/room/dao/ComponentTypeDao.kt`

- **5 read-методов** — все фильтруют `WHERE remove_date IS NULL` (M12 convention):
  - `flowTypesForDictionary(dictId)` (line 31) — Flow + ORDER BY `(system_key IS NULL) ASC, position ASC`.
  - `getTypesForDictionary(dictId)` (line 41) — suspend list.
  - `getBuiltInTypes()` (line 44) — `WHERE system_key IS NOT NULL`.
  - `getBySystemKey(key)` (line 47).
  - `getById(id)` (line 50) — **БЕЗ** фильтра `remove_date IS NULL` (intentional? — единственный non-filtering read).
- **Write-методы**:
  - `@Insert insert(type)` (line 53) — простой INSERT.
  - `@Update update(type)` (line 56).
  - `softDelete(id, now)` (line 63) — `UPDATE ... SET remove_date = :now WHERE id = :id AND system_key IS NULL` (built-in защищён на SQL-уровне).
- **Reactive count методов нет**. `flowAllUserDefinedTypesWithUsage` (новый API M13) потребует JOIN + `valueCountByType: Map<Long, Int>` + `dictionaryIdsByType: Map<Long, Set<Long>>` + `dictionaryNames: Map<Long, String>` — **аналога в текущем DAO нет**, будет новый Flow с composite tuple либо combine() через несколько Flow.

### 3.2 `ComponentValueDao`

Файл: `core/core-db-impl/src/main/java/me/apomazkin/core_db_impl/room/dao/ComponentValueDao.kt`

- 7 методов (read/write/delete + count).
- **Никакого `removed_at` фильтра** — table M12 не имеет колонки `removed_at`. После M13 ВСЕ методы обязаны фильтровать (либо через JOIN на parent `component_types.removed_at`, либо собственный `removed_at`).
- **Нет `@Transaction` методов** — текущий стиль insert/update без транзакции. `insertSingleSafe` (для `is_multi=false` cardinality) — **новый паттерн, аналога в DAO нет**.
- **Нет ORDER BY** — нужен для multi-cardinality `ORDER BY created_at ASC` (см. § Aспекты сophere `template_model.md` § БД).

### 3.3 `QuizConfigDao`

Файл: `core/core-db-impl/src/main/java/me/apomazkin/core_db_impl/room/dao/QuizConfigDao.kt`

- Read: `getByDictionaryAndMode / getByDictionary` (lines 19, 22).
- Write: `@Insert insert / @Update update` (lines 26, 29) + `suspend fun insertDefaultQuizConfig(dictionaryId, quizMode)` (line 36) — default-method, hardcoded JSON `[{"type":"builtin","key":"translation"}]`.
- **Нет точечного UPDATE для `component_refs`** — UseCase cascade rename / soft-delete будет делать read-modify-write через existing `update(config)` (F037: финализация на data_design_tree — `@Transaction` DAO method с `json_replace`/`json_remove` vs UseCase composition через `update(config)`).
- **Reactive (Flow) метод**: отсутствует — `flowAllUserDefinedTypesWithUsage` (`previewDeletionImpact`) → потребует или новый `flowAll()` либо combine() с другими flow.

### 3.4 `WordDao` — где живёт atomic compound INSERT

Файл: `core/core-db-impl/src/main/java/me/apomazkin/core_db_impl/room/WordDao.kt`

- `addDictionary(dictionaryDb)` (lines 41-52) — `@Transaction` default-method: атомарно `_addDictionaryRow + _addQuizConfigRow` (F1 invariant default config). Pattern для будущих compound atomic INSERTs.
- `_insertComponentValue(value)` (line 178) — `@Insert` private (underscore convention).
- `addLexemeWithComponents(lexemeDb, dictionaryId, components: List<Pair<Long, String>>)` (lines 199-217) — `@Transaction` default-method: `addLexeme + addWriteQuiz + N x _insertComponentValue`. Closer всего к будущему `insertSingleSafe` — той же `@Transaction default-method` паттерн.
- `addLexemeWithQuiz(lexemeDb, dictionaryId)` (lines 184-189) — atomic INSERT lexeme + write_quiz.

**Вывод по DAO:** convention для atomic ops — `@Transaction` default-method в DAO interface, private `@Insert` helpers с underscore-prefix (`_insertComponentValue`, `_addDictionaryRow`, `_addQuizConfigRow`). `insertSingleSafe` (cardinality protection для `is_multi=false`) ложится в эту convention.

## 4. Migration слой (`:core:core-db-impl/room/migrations/`)

### 4.1 Существующая `Migration_011_to_012`

Файл: `core/core-db-impl/src/main/java/me/apomazkin/core_db_impl/room/migrations/Migration_011_to_012.kt`

- **Структура**: `object Migration_011_to_012 : Migration(11, 12)` + override `migrate(connection: SQLiteConnection)` (lines 31-35).
- **Driver**: bundled SQLite (`androidx.sqlite.SQLiteConnection` + `androidx.sqlite.execSQL`) — не legacy SupportSQLiteDatabase.
- **Test-hook pattern** (lines 33-86): `migrateImpl(connection, failAfterStep: Int? = null)` + `maybeFail(step, failAfterStep)` бросает `MigrationTestFailureException` для idempotency-тестов. Production `migrate()` вызывает с `failAfterStep = null`. **Прецедент для M3 idempotency-теста в M13** — повторное использование паттерна.
- **Структура SQL-шагов**:
  - CREATE TABLE component_types + 3 индекса (lines 90-118).
  - CREATE TABLE component_values + 2 индекса (lines 120-141).
  - CREATE TABLE quiz_configs + 2 индекса (lines 143-162).
  - `seedBuiltIns(connection)` (line 58) — общий entry point для fresh + migration path.
  - Data migration через `INSERT ... SELECT` + `json_object` SQL функция (lines 166-211).
  - `json_insert(arr, '$[#]', json_object(...))` для append в JSON-массив (line 230-235) — verified bundled SQLite ≥ 3.45.
  - В конце — `ALTER TABLE ... DROP COLUMN` (lines 77-78).
- **Транзакционность**: Room автоматически оборачивает `migrate()` в транзакцию через savepoint, явных `BEGIN/COMMIT` нет (line 30).
- **JSON SQL pattern для M12 формата**: `json_object('v', 1, 'text', l.translation)` → produces `{"v":1,"text":"..."}` (M12 формат). **На M13 нужно генерить `{"fields":{"value":{"type":"text","value":"..."}}}` — гораздо более вложенно**.

### 4.2 `seedBuiltIns`

Файл: `core/core-db-impl/src/main/java/me/apomazkin/core_db_impl/room/SeedBuiltIns.kt`

- Сейчас: один `INSERT OR IGNORE` с literal SQL (lines 24-30):
  ```sql
  INSERT OR IGNORE INTO component_types
      (system_key, dictionary_id, name, template_key, position, remove_date)
  VALUES ('translation', NULL, NULL, 'text', 0, NULL)
  ```
- **Используется в двух местах** (lines 12-13 KDoc):
  - `Migration_011_to_012.migrate()` — для upgrade path с v11.
  - `RoomDatabase.Callback.onCreate(connection)` — fresh install (см. § 5 `RoomModule.kt:53-56`).
- **F044 (upgrade-path schema mismatch problem):** старый юзер `user_version=11` пройдёт M11→M12 → вызовет `seedBuiltIns(connection)` (Migration_011_to_012.kt:58) → если `SeedBuiltIns.kt` обновлён под M13 (`removed_at`, `is_multi`, `created_at`, `updated_at`), миграция упадёт «no such column». Стратегия — **frozen seed copy в M11→M12** vs параметризация по schemaVersion vs перенести seedBuiltIns в M12→M13 (см. scope.md aspect `db_migration` Best-guess A). Решение — на `data_design_tree`.

### 4.3 Registry миграций — `RoomModule`

Файл: `core/core-db-impl/src/main/java/me/apomazkin/core_db_impl/di/module/RoomModule.kt`

- `.addMigrations(Migration_011_to_012)` (line 50) — нужно дополнить `Migration_012_to_013` (F040 — без этого upgrade 12→13 уйдёт по `fallbackToDestructiveMigration` → **data loss**).
- `.fallbackToDestructiveMigration(dropAllTables = true)` (line 51) — safety net + logging через `onDestructiveMigration` callback (lines 59-67). LexemeLogger ERROR + auto-Crashlytics non-fatal.
- `.addCallback(... onCreate ...)` (lines 52-69) — `seedBuiltIns(connection)` в `onCreate` для fresh install path.
- `DATABASE_NAME = "name"` (line 94) — корень БД.
- Bundled driver: `.setDriver(BundledSQLiteDriver())` (line 48).

## 5. Public API слой (`:core:core-db-api/`)

### 5.1 `CoreDbApi.LexemeApi` — что уже добавлено business-фазой

Файл: `core/core-db-api/src/main/java/me/apomazkin/core_db_api/CoreDbApi.kt`

- **5 BREAKING сигнатур** (M12→M13) — все принимают `TemplateValues` вместо `ComponentValueData`:
  - `addLexemeWithBuiltInComponent` (line 101).
  - `addLexemeWithUserDefinedComponent` (line 113).
  - `addLexemeWithComponents` (line 125).
  - `addComponentValue` (line 131).
  - `updateComponentValue` (line 137).
- **6 NEW методов** — добавлены business-фазой, **impl в `:core:core-db-impl` отсутствует**:
  - `flowAllUserDefinedTypesWithUsage(): Flow<UserDefinedTypesUsageSnapshot>` (line 161).
  - `flowUserDefinedTypesForDictionary(dictId): Flow<DictionaryTypesSnapshot>` (line 168).
  - `createUserDefinedComponent(name, template, isMulti, scope): CreateComponentOutcome` (line 177).
  - `renameComponentType(typeId, newName): RenameComponentOutcome` (line 188).
  - `previewDeletionImpact(typeId): DeletionImpact?` (line 198).
  - `softDeleteComponentType(typeId): SoftDeleteComponentOutcome` (line 207).
- **Deprecated shim** методов сохраняется: `addLexemeWithTranslation` / `updateLexemeTranslation` (lines 211-222).

### 5.2 Entity DTO — уже обновлены под M13

- `ComponentTypeApiEntity`: добавлены `isMulti: Boolean = false`, `createdAt: Date`, `updatedAt: Date`, `removedAt: Date? = null`. Источник: `core/core-db-api/src/main/java/me/apomazkin/core_db_api/entity/ComponentTypeApiEntity.kt:13-24`. **`template: ComponentTemplate` non-nullable** (несмотря на nullable `fromKey()` в M13) → mapper в `core-db-impl` обязан резолвить strategy (F019: best-guess B — `toApiEntity()` возвращает `ComponentTypeApiEntity?` skip-row).
- `ComponentValueApiEntity`: `data: TemplateValues` + `createdAt: Date / updatedAt: Date / removedAt: Date? = null`. Источник: `core/core-db-api/src/main/java/me/apomazkin/core_db_api/entity/ComponentValueApiEntity.kt:14-22`.
- `UserDefinedTypesUsageSnapshot` (entity/UserDefinedTypesUsageSnapshot.kt) — `types + valueCountByType: Map<Long, Int> + dictionaryIdsByType: Map<Long, Set<Long>> + dictionaryNames: Map<Long, String>`.
- `DictionaryTypesSnapshot` (entity/DictionaryTypesSnapshot.kt) — per-dict shape.
- `CreateComponentOutcome / RenameComponentOutcome / SoftDeleteComponentOutcome` (entity/ComponentOutcomeApiEntity.kt) — sealed; Success + collision varianty.

### 5.3 Domain `:modules:domain:lexeme/` — уже обновлён

- `ComponentTemplate`: drop `LONG_TEXT`; `fromKey: (String) → ComponentTemplate?` (nullable). Источник: `modules/domain/lexeme/src/main/java/me/apomazkin/lexeme/ComponentTemplate.kt:11-31`.
- `ComponentType`: добавлены `isMulti / createdAt / updatedAt`, rename `removeDate → removedAt`. Источник: `modules/domain/lexeme/src/main/java/me/apomazkin/lexeme/ComponentType.kt:16-37`. `NAME_MAX_LEN = 64`.
- `TemplateValues`: sealed + `TextValues / ImageValues`. Источник: `modules/domain/lexeme/src/main/java/me/apomazkin/lexeme/TemplateValues.kt`.
- `Primitive` (Text/Image/Color), `Field`, `PrimitiveType` (TEXT/IMAGE/COLOR), `Scope`, `DeletionImpact`, `AffectedQuizConfig`, `NameError`, `CreateOutcome / RenameOutcome / DeleteOutcome` — все в `modules/domain/lexeme/`.
- **`ComponentValueData.kt` ещё ЖИВ** (по `business_summary.md` § «Что вне scope»: file/usage будет удалён на data_design_tree как финальный узел DAG). Подтверждение: ссылается из `core-db-impl/CoreDbApiImpl.kt` + `mapper/ComponentValueDataJson.kt` + `entity/ComponentValueWithType.kt`. Источник: `modules/domain/lexeme/src/main/java/me/apomazkin/lexeme/ComponentValueData.kt` существует.

## 6. JSON mapper слой (`:core:core-db-impl/mapper/`)

### 6.1 `ComponentValueDataJson` — текущий M12 mapper

Файл: `core/core-db-impl/src/main/java/me/apomazkin/core_db_impl/mapper/ComponentValueDataJson.kt`

- **M12 формат**:
  - `TextValue / LongTextValue` → `{"v":1,"text":"<text>"}` (lines 20-25).
  - `ImageValue` → `{"v":1,"uri":"<uri>"}` (lines 27-28).
- **Variant выбирается по `template` родителя** (без discriminator в JSON) — line 18 + 39-43.
- **Текущий `toComponentValueData()` НЕ fail-soft**: throws `JSONException` при невалидном JSON либо отсутствии нужных полей (line 34). M13 best-guess — fail-soft возвращает `null` + Crashlytics.
- **`PAYLOAD_VERSION = 1`** — оставлен как страховка. M13 формат — другой envelope `{"fields":{...}}` — пока не использует `v` (по `template_model.md` § Open Q `v` будет переиспользован для будущих breaking changes).

**Что нужно на M13** (по scope.md aspect `template_model.md`):
- Полная замена → `parseTemplateValues(json: String, template: ComponentTemplate): TemplateValues?` + симметричный `TemplateValues.toJson(): String`.
- Формат: `{"fields": {"<fieldName>": {"type": "text|image|color", "value|uri|hex": "..."}}}` — см. business_summary §1.

### 6.2 `ComponentTypeRefJson` — quiz_configs JSON serializer (не трогаем)

Файл: `core/core-db-impl/src/main/java/me/apomazkin/core_db_impl/mapper/ComponentTypeRefJson.kt`

- `toJson()` (line 21) — массив `[{"type":"builtin","key":"..."} | {"type":"user","name":"..."}]`.
- `toComponentTypeRefList()` (line 41) — defensive parse, corrupt JSON → `emptyList()`.
- **На M13 формат не меняется** — но если выбирается альтернатива (F039 Best-guess B — обогатить `ComponentTypeRef.UserDefined` `typeId`) — оба mapper'а + миграция данных в `quiz_configs.component_refs` потребуют изменений. Best-guess A (two-prong SELECT) — оставляет mapper неизменным.

## 7. Реализация `CoreDbApiImpl.LexemeApiImpl` (текущая, до M13)

Файл: `core/core-db-impl/src/main/java/me/apomazkin/core_db_impl/CoreDbApiImpl.kt`

- `LexemeApiImpl` (lines 209-400) — ctor: `wordDao, componentTypeDao, componentValueDao, quizConfigDao, logger`.
- **5 callsite'ов `ComponentValueData`** (см. § «5 BREAKING» выше) — все принимают `data: ComponentValueData` и вызывают `data.toJson()` (`addLexemeWithBuiltInComponent` line 241; `addLexemeWithUserDefinedComponent` line 257; `addLexemeWithComponents` line 279; `addComponentValue` line 311; `updateComponentValue` line 324).
- `@Deprecated addLexemeWithTranslation` (line 352) — internally вызывает `addLexemeWithBuiltInComponent(...)` + `ComponentValueData.TextValue(translation.value)`.
- `@Deprecated updateLexemeTranslation` (line 364) — has 3 branches (null / insert / update) — каждый использует `ComponentValueData.TextValue(...).toJson()` (lines 386, 394).
- **Ни одного из 6 новых методов API ещё не реализовано** — `flowAllUserDefinedTypesWithUsage / flowUserDefinedTypesForDictionary / createUserDefinedComponent / renameComponentType / previewDeletionImpact / softDeleteComponentType` (interface методы → не имеют override в LexemeApiImpl → compile fail).
- **Module `:app:testDebugUnitTest` сломан** by design — business_summary говорит `app/.../ComponentsManagerUseCaseImplTest.kt` находится в `:app` source set и не достижим пока chain сломан.

## 8. DAO test patterns

Файлы: `core/core-db-impl/src/test/` + `src/androidTest/`:

- **Unit tests** — `core/core-db-impl/src/test/java/me/apomazkin/core_db_impl/mapper/DefinitionOldMapperTest.kt` (legacy mapper, не M13-relevant).
- **Instrumented tests**:
  - `core/core-db-impl/src/androidTest/java/me/apomazkin/core_db_impl/room/MigrationFrom11to12.kt` — pattern для M-тестов: `MigrationTestHelper(driver = BundledSQLiteDriver(), ...)` + `helper.createDatabase(11).use { ... }` + `helper.runMigrationsAndValidate(12, listOf(Migration_011_to_012))` + assertions через `countWhere / scalarText` helper extensions. Test-cases A-L по `data_migration_test.md`.
  - `MigrationFrom11to12IdempotencyTest.kt` — pattern для idempotency-теста: phase 1 inject failure через `Migration_011_to_012.migrateImpl(connection, failAfterStep = N)` + assert v11 целый; phase 2 retry без injection + assert v12 валиден.
  - `BundledSqliteFeatureTest.kt` — independent harness `BundledSQLiteDriver().open(":memory:")` без Room — для проверки features ≥ 3.45 (`json_object / json_insert / json_each / json_remove` все доступны, lines 63-94).
- **Чисто DAO unit-тестов (под in-memory Room) на данный момент НЕТ** — только migration-тесты + один mapper-тест.

**Вывод:** для M13 будет добавлен `MigrationFrom12to13.kt` + `MigrationFrom12to13IdempotencyTest.kt` + mapper unit tests (golden round-trip fixtures `fixtures/component_values/*.json`).

## 9. Prefs / Datasource (`:modules:datasource:prefs/`)

Файлы:
- `modules/datasource/prefs/src/main/java/me/apomazkin/prefs/PrefsProvider.kt`
- `modules/datasource/prefs/src/main/java/me/apomazkin/prefs/QuizPickerPrefKey.kt`

- **`PrefsProvider.getStringByRawKey(key)`** (line 78) + **`getStringFlowByRawKey(key)`** (line 82) + **`setStringByRawKey(key, value: String?)`** (line 87) — dynamic-key API. Передача `value = null` deletе entry (lines 90-92).
- **Scan-by-prefix API ОТСУТСТВУЕТ** — `findKeysWithPrefix(prefix: String): List<String>` нет (F049). Best-guess для prefs cleanup — UseCase композиция через `DictionaryApi.getDictionaryList()` (iterate по domain IDs, проверять каждый ключ через `getStringByRawKey`). Альтернатива (variant A) — расширить `PrefsProvider`.
- **`quizPickerPrefKey(dictionaryId: Long): String = "quiz_picker_dict_$dictionaryId"`** (line 10) — single source of truth helper.

## 10. Aspects mapping (scope.md → real artefacts)

Resume what's already in code vs new:

| Aspect | Real artefacts (existing M12) | Что новое нужно на M13 |
|---|---|---|
| `db_migration` | M11→M12 в `Migration_011_to_012.kt`; `addMigrations` в `RoomModule.kt:50` | `Migration_012_to_013.kt` (composite JSON rewrite, drop UNIQUE на `component_values` + `component_types(dict_id,name)`, add `is_multi/created_at/updated_at`, rename `remove_date → removed_at` в `component_types`, template-key consolidation `long_text → text`) + регистрация в `RoomModule.kt` + новый `13.json` |
| `migration_edge_cases` | M3 idempotency-pattern с `failAfterStep` хуком (Migration_011_to_012.kt:48) | повторить паттерн для M13 |
| `migration_timestamps_backfill` | Нет precedent — `created_at/updated_at` колонки впервые | UPDATE ... SET created_at = strftime('%s','now')*1000 в migration SQL |
| `public_contract_change` | API уже обновлен (5 BREAKING + 6 NEW в `CoreDbApi.LexemeApi`) | impl 6 NEW методов в `LexemeApiImpl` + rebind 5 BREAKING |
| `domain_rewrite` | Done in business-phase (Primitive/Field/PrimitiveType/TemplateValues/Scope/...) | Drop `modules/domain/lexeme/ComponentValueData.kt` после rebind всех data-callsite'ов |
| `dao_convention` | `WHERE remove_date IS NULL` в `ComponentTypeDao` (5 read'ов); `ComponentValueDao` без фильтра | Audit + добавить `removed_at IS NULL` фильтр + parent join filter на `LexemeDbEntity` |
| `cardinality_safety` | `addLexemeWithComponents` `@Transaction` default-method (WordDao.kt:200) — pattern | новый `insertSingleSafe` `@Transaction` default-method в `ComponentValueDao` (SELECT count `WHERE removed_at IS NULL` + reject + INSERT) |
| `soft_delete_unique_collision` | UNIQUE `(dictionary_id, name)` в `ComponentTypeDb` index — будет dropped в M13 | UseCase enforcement two-prong SELECT (per-dict + cross-scope) |
| `userdefined_identity_invariant` | UNIQUE на `(dictionary_id, name)` сейчас защищает identity — drop в M13 | Two-prong SELECT в UseCase (Best-guess A) либо обогащение `ComponentTypeRef.UserDefined.typeId` (B) |
| `quiz_configs_cleanup` | `QuizConfigDao.update(config)` (lines 29) — единственный mutator `component_refs` | новый `@Transaction` DAO method с `json_replace`/`json_remove` ИЛИ UseCase composition |
| `prefs_cleanup_on_soft_delete` | `PrefsProvider.getStringByRawKey / setStringByRawKey` + `quizPickerPrefKey(dictId)` helper | UseCase iterates `DictionaryApi.getDictionaryList()` → читает per-key → reset (Best-guess B); либо новый `findKeysWithPrefix` (A) |
| `forward_compat_unknown` | `ComponentTemplate.fromKey` уже nullable (domain) | mapper `parseTemplateValues` fail-soft с null + Crashlytics |
| `mapper_golden_fixtures` | Нет fixtures dir | `core-db-impl/src/test/resources/fixtures/component_values/*.json` (золотые round-trip) |
| `transactional_save_lexeme` | `addLexemeWithComponents` `@Transaction` (WordDao.kt:199-217) — уже atomic | rebind на `TemplateValues` boundary |

## Вердикт

**Аналог: частично найден.**

- **Migration pattern (single-direction Migration object, `failAfterStep` test hook, bundled SQLite driver, `MigrationTestHelper`):** найден — `Migration_011_to_012.kt`. M13 переиспользует тот же подход: новый `object Migration_012_to_013 : Migration(12, 13) + migrateImpl(connection, failAfterStep)`.
- **Atomic compound INSERT pattern (`@Transaction` default-method в DAO + `_` private inserts):** найден — `WordDao.addDictionary` (lines 41-52) + `addLexemeWithComponents` (lines 199-217). Используется как precedent для `insertSingleSafe`.
- **Soft-delete pattern (`removeDate` колонка + `WHERE remove_date IS NULL` в read queries):** найден — `ComponentTypeDb` + `ComponentTypeDao`. M13 — rename column ТОЛЬКО в `component_types` (`remove_date → removed_at`), остальные таблицы (`Hint`, `Sample`) НЕ трогаются.
- **JSON serialization pattern (variant by parent context, no JSON discriminator):** найден в `ComponentValueDataJson.kt`. **M13 полностью переписывает** под typed `TemplateValues` + discriminator-per-primitive (расширение, не reuse).
- **`@Relation` чтение лексемы с children:** найден — `LexemeDbEntity.componentValueListDb` (lines 22-27). **Аналога с фильтрацией WHERE в `@Relation` нет в проекте** — Room не поддерживает; M13 потребует выбора (post-load filter в `toApiEntity()` либо custom `@Query` с явным JOIN; решение на `data_design_tree`).
- **Reactive (Flow) DAO method с composite tuple / aggregated view:** **аналога нет** — все существующие Flow-методы возвращают `List<*>` либо `Int / Map<Int, Int>` (statistic). `flowAllUserDefinedTypesWithUsage` требует новый pattern (combine() либо composite tuple через SQL `json_group_array` / multiple queries).
- **Atomic cleanup `quiz_configs.component_refs` (cascade rename / soft-delete):** **аналога нет** — `QuizConfigDao` имеет только общий `update(config)`, без точечного UPDATE с `json_remove`/`json_replace`. M13 — либо новый `@Transaction` DAO method, либо UseCase composition через `getByDictionary + update`.
- **Prefs cleanup при soft-delete (scan-by-prefix):** API `PrefsProvider` не имеет `findKeysWithPrefix` (variant A); вариант B (iterate через `DictionaryApi.getDictionaryList()`) — каждое API уже существует, нужна композиция в UseCase, не в DAO.
- **Multi-cardinality (insertSingleSafe защита через @Transaction):** аналога нет — текущий DAO без cardinality checks. Будет новая convention `@Transaction default-method SELECT count + reject + INSERT` (использует existing `@Transaction` pattern из `WordDao`).

**Итого:** Migration/atomic-insert/soft-delete/JSON-by-template/`@Relation` patterns — все есть в проекте, M13 их **расширяет**. New shape patterns (`@Relation` WHERE-фильтр, composite reactive Flow с aggregated view, cascade `quiz_configs` cleanup, prefs scan-by-prefix, cardinality-safe insert) — **аналогов нет**, дизайн на `data_design_tree`.
