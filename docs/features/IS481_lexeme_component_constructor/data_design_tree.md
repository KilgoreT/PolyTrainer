# Data design tree — IS481

DAG узлов data-слоя с типом изменения `[+]` (создаётся), `[~]` (модифицируется), `[-]` (удаляется). Все `[~]` verified через Read реального исходника (см. `data_walkthrough.md`). Полные пути от workspace root.

Граф — направленные deps между узлами. `A → B` означает «A зависит от B (A нужно сначала иметь B)».

---

## Узлы

### Domain — НЕ трогаем (создано в business_implement)

Только для контекста deps в графе. Все эти типы — `[=]` (без изменений в data sub-flow), не входят в счёт узлов.

- `modules/domain/lexeme/src/main/java/me/apomazkin/lexeme/BuiltInComponent.kt` `[=]`
- `modules/domain/lexeme/src/main/java/me/apomazkin/lexeme/ComponentTemplate.kt` `[=]`
- `modules/domain/lexeme/src/main/java/me/apomazkin/lexeme/ComponentValueData.kt` `[=]`
- `modules/domain/lexeme/src/main/java/me/apomazkin/lexeme/ComponentType.kt` `[=]`
- `modules/domain/lexeme/src/main/java/me/apomazkin/lexeme/ComponentValue.kt` `[=]`
- `modules/domain/lexeme/src/main/java/me/apomazkin/lexeme/ComponentTypeRef.kt` `[=]`
- `modules/domain/lexeme/src/main/java/me/apomazkin/lexeme/QuizConfig.kt` `[=]`
- `modules/domain/lexeme/src/main/java/me/apomazkin/lexeme/Lexeme.kt` `[=]`

### core-db-api — НЕ трогаем (создано в business_implement)

- `core/core-db-api/src/main/java/me/apomazkin/core_db_api/entity/ComponentTypeApiEntity.kt` `[=]`
- `core/core-db-api/src/main/java/me/apomazkin/core_db_api/entity/ComponentValueApiEntity.kt` `[=]`
- `core/core-db-api/src/main/java/me/apomazkin/core_db_api/entity/QuizConfigApiEntity.kt` `[=]`
- `core/core-db-api/src/main/java/me/apomazkin/core_db_api/entity/LexemeApiEntity.kt` `[=]`
- `core/core-db-api/src/main/java/me/apomazkin/core_db_api/CoreDbApi.kt` `[=]` (interface, business_implement его уже расширил)
- `core/core-db-api/build.gradle.kts` `[=]` (`api(project(":modules:domain:lexeme"))` уже добавлен)

### Data — узлы IS481 main

#### 1. `entity/ComponentTypeDb.kt` `[+]`
- Path: `/Users/kilg/AndroidStudioProjects/PolyTrainer/core/core-db-impl/src/main/java/me/apomazkin/core_db_impl/entity/ComponentTypeDb.kt`
- Shape: `@Entity(tableName = "component_types", foreignKeys = [DictionaryDb FK CASCADE on dictionary_id], indices = [Index("dictionary_id"), Index(["system_key"], unique=true), Index(["dictionary_id", "name"], unique=true)])` data class `ComponentTypeDb(id, systemKey: String?, dictionaryId: Long?, name: String?, templateKey: String, position: Int, removeDate: Date?)`.
- Маппер: `fun ComponentTypeDb.toApiEntity(): ComponentTypeApiEntity` рядом (extension в том же файле).
- Deps: `DictionaryDb` (FK), `DateTimeConverter` (для `removeDate`).
- Domain mapping: `systemKey?.let(BuiltInComponent::fromKey)`, `ComponentTemplate.fromKey(templateKey)`.

#### 2. `entity/ComponentValueDb.kt` `[+]`
- Path: `/Users/kilg/AndroidStudioProjects/PolyTrainer/core/core-db-impl/src/main/java/me/apomazkin/core_db_impl/entity/ComponentValueDb.kt`
- Shape: `@Entity(tableName = "component_values", foreignKeys = [LexemeDb FK CASCADE on lexeme_id, ComponentTypeDb FK CASCADE on component_type_id], indices = [Index("component_type_id"), Index(["lexeme_id", "component_type_id"], unique=true)])` data class `ComponentValueDb(id, lexemeId: Long, componentTypeId: Long, value: String)`.
- Deps: `LexemeDb` (FK), `ComponentTypeDb` (FK), `ComponentValueDataJson.kt` (mapper использует JSON parser).

#### 3. `entity/ComponentValueWithType.kt` `[+]`
- Path: `/Users/kilg/AndroidStudioProjects/PolyTrainer/core/core-db-impl/src/main/java/me/apomazkin/core_db_impl/entity/ComponentValueWithType.kt`
- Shape: data class `ComponentValueWithType(@Embedded val value: ComponentValueDb, @Relation(parentColumn="component_type_id", entityColumn="id") val type: ComponentTypeDb)`.
- Маппер: `fun ComponentValueWithType.toApiEntity(): ComponentValueApiEntity` рядом — конструирует `ComponentValueApiEntity(id, lexemeId, type = type.toApiEntity(), data = value.value.toComponentValueData(type.template))`.
- Deps: `ComponentValueDb`, `ComponentTypeDb`, `ComponentValueDataJson.kt` (для парсинга JSON).

#### 4. `entity/QuizConfigDb.kt` `[+]`
- Path: `/Users/kilg/AndroidStudioProjects/PolyTrainer/core/core-db-impl/src/main/java/me/apomazkin/core_db_impl/entity/QuizConfigDb.kt`
- Shape: `@Entity(tableName = "quiz_configs", foreignKeys = [DictionaryDb FK CASCADE on dictionary_id], indices = [Index("dictionary_id"), Index(["dictionary_id", "quiz_mode"], unique=true)])` data class `QuizConfigDb(id, dictionaryId: Long, quizMode: String, componentRefs: String)`.
- Маппер: `fun QuizConfigDb.toApiEntity(): QuizConfigApiEntity` рядом — парсит `componentRefs` через `String.toComponentTypeRefList()`.
- Deps: `DictionaryDb` (FK), `ComponentTypeRefJson.kt` (для парсинга `component_refs` JSON).

#### 5. `mapper/ComponentValueDataJson.kt` `[+]`
- Path: `/Users/kilg/AndroidStudioProjects/PolyTrainer/core/core-db-impl/src/main/java/me/apomazkin/core_db_impl/mapper/ComponentValueDataJson.kt`
- Содержание: `fun ComponentValueData.toJson(): String` + `fun String.toComponentValueData(template: ComponentTemplate): ComponentValueData` через `org.json.JSONObject`. PAYLOAD_VERSION = 1.
- Deps: domain `ComponentValueData`, `ComponentTemplate`, `org.json.JSONObject`.

#### 6. `mapper/ComponentTypeRefJson.kt` `[+]`
- Path: `/Users/kilg/AndroidStudioProjects/PolyTrainer/core/core-db-impl/src/main/java/me/apomazkin/core_db_impl/mapper/ComponentTypeRefJson.kt`
- Содержание: `fun List<ComponentTypeRef>.toJson(): String` + `fun String.toComponentTypeRefList(): List<ComponentTypeRef>` через `org.json.JSONObject` / `JSONArray`. Discriminator `type: "builtin" | "user"`. Defensive parser (corrupt JSON → emptyList + logger.e).
- Deps: domain `ComponentTypeRef`, `BuiltInComponent`, `org.json.JSONObject`, `LexemeLogger`.

#### 7. `room/dao/ComponentTypeDao.kt` `[+]`
- Path: `/Users/kilg/AndroidStudioProjects/PolyTrainer/core/core-db-impl/src/main/java/me/apomazkin/core_db_impl/room/dao/ComponentTypeDao.kt`
- Shape: `@Dao interface ComponentTypeDao` с методами: `flowTypesForDictionary(dictionaryId): Flow<List<ComponentTypeDb>>`, `getTypesForDictionary(dictionaryId): List<ComponentTypeDb>`, `getBuiltInTypes(): List<ComponentTypeDb>`, `getBySystemKey(key): ComponentTypeDb?`, `getById(id): ComponentTypeDb?`, `insert(type): Long`, `update(type)`, `softDelete(id, now: Date): Int` (UPDATE removeDate WHERE system_key IS NULL — защита built-in от удаления).
- Deps: `ComponentTypeDb`.

#### 8. `room/dao/ComponentValueDao.kt` `[+]`
- Path: `/Users/kilg/AndroidStudioProjects/PolyTrainer/core/core-db-impl/src/main/java/me/apomazkin/core_db_impl/room/dao/ComponentValueDao.kt`
- Shape: `@Dao interface ComponentValueDao` с: `getForLexeme(lexemeId): List<ComponentValueDb>`, `getById(id): ComponentValueDb?`, `getForLexemeAndType(lexemeId, typeId): ComponentValueDb?`, `insert(value): Long`, `update(value)`, `delete(id): Int`, `deleteByLexemeAndType(lexemeId, typeId): Int`.
- Deps: `ComponentValueDb`.

#### 9. `room/dao/QuizConfigDao.kt` `[+]`
- Path: `/Users/kilg/AndroidStudioProjects/PolyTrainer/core/core-db-impl/src/main/java/me/apomazkin/core_db_impl/room/dao/QuizConfigDao.kt`
- Shape: `@Dao interface QuizConfigDao` с: `getByDictionaryAndMode(dictId, mode): QuizConfigDb?`, `insert(config): Long`, `update(config)`. Плюс default-method `insertDefaultQuizConfig(dictionaryId: Long, quizMode: String): Long` который пишет hardcoded JSON `'[{"type":"builtin","key":"translation"}]'` через `insert(QuizConfigDb(...))` (MIN-8 — без mapper'а, без передачи доменного объекта).
- Deps: `QuizConfigDb`.

#### 10. `entity/LexemeDbEntity.kt` `[~]`
- Path: `/Users/kilg/AndroidStudioProjects/PolyTrainer/core/core-db-impl/src/main/java/me/apomazkin/core_db_impl/entity/LexemeDbEntity.kt`
- **Verified** через Read (см. walkthrough §1).
- Изменения:
  1. Добавить `@Relation(entity = ComponentValueDb::class, parentColumn = "id", entityColumn = "lexeme_id") val componentValueListDb: List<ComponentValueWithType>` (Multi-level).
  2. Переписать `toApiEntity()` (lines 29-74) — убрать synthetic translation/definition построение, заменить на `components = componentValueListDb.map { it.toApiEntity() }`.
  3. Удалить imports `BuiltInComponent`, `ComponentTemplate`, `ComponentValueData` (synthetic mapping ушёл).
  4. Сохранить existing `sampleDbList: List<SampleDb>` @Relation как есть.
- Deps: `LexemeDb` (Embedded), `SampleDb` (Relation), `ComponentValueWithType` (Relation, новое).

#### 11. `entity/LexemeDb.kt` `[~]`
- Path: `/Users/kilg/AndroidStudioProjects/PolyTrainer/core/core-db-impl/src/main/java/me/apomazkin/core_db_impl/entity/LexemeDb.kt`
- **Verified** через Read (см. walkthrough §1).
- Изменения:
  1. Удалить `@ColumnInfo(name = "translation") val translation: String? = null` (line 26).
  2. Удалить `@ColumnInfo(name = "definition") val definition: String? = null` (line 27).
  3. Остальные поля (`id`, `wordId`, `wordClass`, `options`, `addDate`, `changeDate`) — без правок.
  4. FK + Index — без правок.
- Эти удаления синхронизированы с Migration step 6 (ALTER TABLE DROP COLUMN).

#### 12. `room/WordDao.kt` `[~]`
- Path: `/Users/kilg/AndroidStudioProjects/PolyTrainer/core/core-db-impl/src/main/java/me/apomazkin/core_db_impl/room/WordDao.kt`
- **Verified** через Read (см. walkthrough §1).
- Изменения:
  1. Удалить `updateLexemeTranslation(id, translation): Int` (line 121-122) — будет переписан через generic `componentValueDao.update`. ИЛИ оставить как legacy `@Deprecated` raw SQL для translation shim (план 06 предлагает оставить — translation built-in lookup быстрее через колонку). **Решение по плану:** оставить @Deprecated raw SQL для translation, удалить `updateLexemeDefinition`.
  2. Удалить `updateLexemeDefinition(id, definition): Int` (line 124-125) — definition больше нет в колонке, теперь только через component_values.
  3. **Добавить** новый default-method `addLexemeWithComponents(wordId, dictionaryId, components: List<Pair<Long, String>>): Long` с `@Transaction` — atomic INSERT `lexemes` + `write_quiz` + N `component_values`. Принимает уже разрешённые typeId + JSON-сериализованный value. Используется из `LexemeApiImpl.addLexemeWithComponents` и `addLexemeWithBuiltInComponent` / `addLexemeWithUserDefinedComponent`.
  4. **Расширить** `addDictionary` — заменить `@Insert` на default-method с `@Transaction` который делает INSERT в `dictionaries` + INSERT в `quiz_configs(default [BuiltIn(TRANSLATION)])` для quiz_mode='write'. Внутренний `_addDictionaryRow(dictionaryDb): Long` остаётся `@Insert` (low-level). Атомарность через `@Transaction` (AGG-4 реверс).

  **Альтернатива:** держать `addDictionary` низкоуровневым в WordDao, оборачивать в UseCase (`DictionaryApiImpl.addDictionary`) через `db.withTransaction { ... }` + `quizConfigDao.insertDefaultQuizConfig(...)`. План явно требует **DAO @Transaction** (см. 05.md строка 61). Решение — `WordDao` метод (consistency с `addLexemeWithQuiz` pattern).

#### 13. `room/Database.kt` `[~]`
- Path: `/Users/kilg/AndroidStudioProjects/PolyTrainer/core/core-db-impl/src/main/java/me/apomazkin/core_db_impl/room/Database.kt`
- **Verified** через Read.
- Изменения:
  1. `entities += [ComponentTypeDb::class, ComponentValueDb::class, QuizConfigDb::class]` — список 6 → 9.
  2. `version = 11` → `version = 12`.
  3. Добавить abstract `componentTypeDao(): ComponentTypeDao`, `componentValueDao(): ComponentValueDao`, `quizConfigDao(): QuizConfigDao`.

#### 14. `di/module/RoomModule.kt` `[~]`
- Path: `/Users/kilg/AndroidStudioProjects/PolyTrainer/core/core-db-impl/src/main/java/me/apomazkin/core_db_impl/di/module/RoomModule.kt`
- **Verified** через Read.
- Изменения:
  1. Добавить `.addMigrations(Migration_011_to_012)` к existing builder (одна строка между `.fallbackToDestructiveMigration(...)` и `.addCallback(...)`).
  2. **Расширить** `addCallback` — добавить override `onCreate(connection: SQLiteConnection)` который вызывает `seedBuiltIns(connection)` (для fresh install path, B1). Existing `onDestructiveMigration` callback сохраняется.
  3. Добавить `@Provides` методы для новых DAO: `provideComponentTypeDao(db: Database): ComponentTypeDao = db.componentTypeDao()` и аналогично для ComponentValueDao / QuizConfigDao.
  4. KDoc-комментарий top-of-file (lines 17-35) обновить — убрать «текущая схема v11, новая миграция = TODO» (раздел «Когда понадобится новая миграция»), заменить на «текущая схема v12, миграция M11→M12 в migrations/Migration_011_to_012.kt».

#### 15. `room/migrations/Migration_011_to_012.kt` `[+]`
- Path: `/Users/kilg/AndroidStudioProjects/PolyTrainer/core/core-db-impl/src/main/java/me/apomazkin/core_db_impl/room/migrations/Migration_011_to_012.kt`
- Shape: `object Migration_011_to_012 : Migration(11, 12) { override fun migrate(connection: SQLiteConnection) { ... } }` — directly на `SQLiteConnection` API (AGG-12), `connection.execSQL(...)` через extension `androidx.sqlite.execSQL`.
- Содержание (по 05.md §§ 1-7):
  1. CREATE TABLE `component_types` (со всеми колонками / FK / индексами).
  2. CREATE TABLE `component_values`.
  3. CREATE UNIQUE INDEX `index_component_types_global_userdef_name` partial WHERE.
  4. `seedBuiltIns(connection)` — INSERT OR IGNORE built-in translation.
  5. Translation data: INSERT в `component_values` для `lexemes.translation IS NOT NULL` через `json_object('v', 1, 'text', l.translation)`.
  6. Definition: создать user-defined `Definition` тип для каждого словаря с `lexemes.definition IS NOT NULL`. Migrate definition data в `component_values`.
  7. `ALTER TABLE lexemes DROP COLUMN translation; ALTER TABLE lexemes DROP COLUMN definition;`.
  8. CREATE TABLE `quiz_configs` + индексы.
  9. INSERT default `[BuiltIn(TRANSLATION)]` для ВСЕХ dictionaries (F1 — без WHERE).
  10. UPDATE добавить `UserDefined("Definition")` для словарей с definition (через `json_insert($, '$[#]', json_object(...))` — AGG-8 verified).
- Helper inline: `private fun seedBuiltIns(connection: SQLiteConnection) { connection.execSQL("INSERT OR IGNORE INTO component_types ...") }` — вызывается из migration и из `RoomModule.onCreate(connection)`. Либо отдельный файл `room/SeedBuiltIns.kt` `[+]` если переиспользование. **Решение:** отдельный файл `room/SeedBuiltIns.kt` — переиспользуется из callback и migration.
- Deps: `androidx.sqlite.SQLiteConnection`, `androidx.sqlite.execSQL`, `androidx.room.migration.Migration`.

#### 16. `room/SeedBuiltIns.kt` `[+]`
- Path: `/Users/kilg/AndroidStudioProjects/PolyTrainer/core/core-db-impl/src/main/java/me/apomazkin/core_db_impl/room/SeedBuiltIns.kt`
- Shape: top-level `internal fun seedBuiltIns(connection: SQLiteConnection)` — INSERT OR IGNORE built-in translation row + создание partial UNIQUE index (для fresh install path). Идемпотентна.
- Используется: `Migration_011_to_012.migrate(connection)` (step 4) + `RoomModule.Callback.onCreate(connection)` (fresh install).
- Deps: `androidx.sqlite.SQLiteConnection`, `androidx.sqlite.execSQL`.

#### 17. `CoreDbApiImpl.kt` `[~]`
- Path: `/Users/kilg/AndroidStudioProjects/PolyTrainer/core/core-db-impl/src/main/java/me/apomazkin/core_db_impl/CoreDbApiImpl.kt`
- **Verified** через Read (см. walkthrough §1).
- Изменения по секциям:

  **`DictionaryApiImpl.addDictionary` (lines 85-94):**
  - Заменить простой `wordDao.addDictionary(DictionaryDb(...))` на новый `WordDao.addDictionary` default-method который атомарно вставляет dictionary + default quiz_config (AGG-4 реверс).
  - Сигнатура UseCase не меняется.

  **`LexemeApiImpl` (lines 202-435) — полностью переписан STUB → реальный DAO путь:**

  - Inject дополнительно: `componentTypeDao: ComponentTypeDao`, `componentValueDao: ComponentValueDao`, `quizConfigDao: QuizConfigDao` в primary ctor.

  - `getLexemeById(id)` — без изменений (existing маппер в LexemeDbEntity станет honest).

  - `addLexemeWithBuiltInComponent(wordId, dictionaryId, systemKey, data)`:
    1. `val typeDb = componentTypeDao.getBySystemKey(systemKey.key) ?: error("Built-in $systemKey not found")`.
    2. `wordDao.addLexemeWithComponents(wordId, dictionaryId, listOf(typeDb.id to data.toJson()))` — atomic transaction.

  - `addLexemeWithUserDefinedComponent(wordId, dictionaryId, name, data)`:
    1. `val typeDb = componentTypeDao.getTypesForDictionary(dictionaryId).firstOrNull { it.systemKey == null && it.name == name } ?: return null` — lookup user-defined.
    2. `wordDao.addLexemeWithComponents(wordId, dictionaryId, listOf(typeDb.id to data.toJson()))`.

  - `addLexemeWithComponents(wordId, dictionaryId, components: List<Pair<ComponentTypeRef, ComponentValueData>>)`:
    1. Resolve каждый ref в typeId: BuiltIn → `getBySystemKey(ref.key.key)?.id`, UserDefined → lookup `getTypesForDictionary` + filter.
    2. Если хоть один не resolved → return null.
    3. `wordDao.addLexemeWithComponents(wordId, dictionaryId, resolvedPairs)` (MIN-9 — atomic compound INSERT).

  - `addComponentValue(lexemeId, componentTypeId, data)`:
    1. `componentValueDao.insert(ComponentValueDb(lexemeId, componentTypeId, data.toJson()))`.

  - `updateComponentValue(componentValueId, data)`:
    1. `val cv = componentValueDao.getById(componentValueId) ?: return 0`.
    2. `componentValueDao.update(cv.copy(value = data.toJson()))`.
    3. Return 1.

  - `deleteComponentValue(componentValueId)`:
    1. `val cv = componentValueDao.getById(componentValueId) ?: return 0`.
    2. `componentValueDao.delete(componentValueId)`.
    3. `componentValueDao.getForLexeme(cv.lexemeId).size`.

  - `getComponentTypes(dictionaryId)`:
    1. `componentTypeDao.getTypesForDictionary(dictionaryId).map { it.toApiEntity() }` — реальный DAO путь.

  - `getQuizConfig(dictionaryId, quizMode)`:
    1. `quizConfigDao.getByDictionaryAndMode(dictionaryId, quizMode)?.toApiEntity()`.

  - `addLexemeWithTranslation(wordId, dictionaryId, translation)` (@Deprecated) — delegate to `addLexemeWithBuiltInComponent(...)` без правок.

  - `updateLexemeTranslation(id, translation)` (@Deprecated):
    - Сейчас вызывает `wordDao.updateLexemeTranslation(id, translation?.value)` — после удаления колонки `lexemes.translation` это сломается.
    - **Переписать на:** lookup component_value по lexemeId + built-in TRANSLATION typeId → `componentValueDao.update(...)` либо `delete(...)` если translation = null.
    - Implementation: `val builtIn = componentTypeDao.getBySystemKey("translation") ?: return null; if (translation == null) { componentValueDao.deleteByLexemeAndType(id, builtIn.id); return id } else { val cv = componentValueDao.getForLexemeAndType(id, builtIn.id); if (cv == null) { componentValueDao.insert(ComponentValueDb(0, id, builtIn.id, ComponentValueData.TextValue(translation.value).toJson())); return id } else { componentValueDao.update(cv.copy(value = ComponentValueData.TextValue(translation.value).toJson())); return id } }`.

#### 18. `di/module/ApiModule.kt` `[=]`
- Path: `/Users/kilg/AndroidStudioProjects/PolyTrainer/core/core-db-impl/src/main/java/me/apomazkin/core_db_impl/di/module/ApiModule.kt`
- **Verified** через Read — НЕ требует правок. Все API binding'и уже подключены (LexemeApiImpl etc). Добавление DAO injection в `LexemeApiImpl` обрабатывается Dagger автоматически через `@Provides` в `RoomModule`.

#### 19. `core/core-db-impl/build.gradle.kts` `[=]`
- Path: `/Users/kilg/AndroidStudioProjects/PolyTrainer/core/core-db-impl/build.gradle.kts`
- **Verified** через Read (см. walkthrough §6).
- Room schema export УЖЕ настроен (`room { schemaDirectory("$projectDir/schemas") }` + `androidTest.assets.srcDirs("$projectDir/schemas")`).
- Dep на `androidx.sqlite:sqlite-bundled` и `roomTesting` УЖЕ подключены.
- **Никаких правок не требуется** в IS481 data sub-flow. Узел `[=]`, не входит в счёт.

#### 20. `schemas/me.apomazkin.core_db_impl.room.Database/12.json` `[+]`
- Path: `/Users/kilg/AndroidStudioProjects/PolyTrainer/core/core-db-impl/schemas/me.apomazkin.core_db_impl.room.Database/12.json`
- Создаётся **автоматически** через Room KSP при build после `@Database(version = 12)`.
- Включает таблицы `component_types` / `component_values` / `quiz_configs` + изменённую `lexemes` (без translation/definition колонок).
- НЕ моделирует partial UNIQUE index `index_component_types_global_userdef_name` — Room не знает про WHERE. Migration test должен проверить наличие индекса явно через `PRAGMA index_list`.

#### 21. `androidTest/.../room/MigrationFrom11to12.kt` `[+]`
- Path: `/Users/kilg/AndroidStudioProjects/PolyTrainer/core/core-db-impl/src/androidTest/java/me/apomazkin/core_db_impl/room/MigrationFrom11to12.kt`
- Shape: `@RunWith(AndroidJUnit4::class) class MigrationFrom11to12` с `@get:Rule val helper = MigrationTestHelper(driver = BundledSQLiteDriver(), ...)`.
- Кейсы — см. `data_migration_test.md`.
- Deps: `androidx.room.testing.MigrationTestHelper`, `androidx.sqlite.driver.bundled.BundledSQLiteDriver`, `12.json` snapshot, `Migration_011_to_012`.

---

## Граф deps (DAG)

```
[+] 12.json (auto-gen) ← Database.kt [~]
                          ↑
                          │ entities += [...]
[+] ComponentTypeDb.kt ←──┤
[+] ComponentValueDb.kt ←─┤
[+] QuizConfigDb.kt ←─────┘

[+] ComponentValueWithType.kt
    ├─ Embedded: ComponentValueDb
    └─ @Relation: ComponentTypeDb

[~] LexemeDbEntity.kt
    ├─ Embedded: LexemeDb [~]
    ├─ @Relation: List<SampleDb> (existing)
    └─ @Relation: List<ComponentValueWithType> (NEW)

[+] ComponentValueDataJson.kt — used by:
    ├─ ComponentValueWithType.toApiEntity()
    ├─ ComponentValueDao callers (LexemeApiImpl)
    └─ Migration_011_to_012 (косвенно, через SQL json_object — не использует Kotlin helper)

[+] ComponentTypeRefJson.kt — used by:
    ├─ QuizConfigDb.toApiEntity()
    └─ Migration_011_to_012 (косвенно, через SQL json_object)

[+] ComponentTypeDao ← ComponentTypeDb
[+] ComponentValueDao ← ComponentValueDb
[+] QuizConfigDao ← QuizConfigDb

[+] SeedBuiltIns.kt — used by:
    ├─ Migration_011_to_012.migrate() step 4
    └─ RoomModule.Callback.onCreate()

[+] Migration_011_to_012.kt
    ├─ depends on: SeedBuiltIns
    └─ creates schema for: ComponentTypeDb, ComponentValueDb, QuizConfigDb, LexemeDb [~]

[~] RoomModule.kt
    ├─ .addMigrations(Migration_011_to_012)
    ├─ Callback.onCreate(connection) → seedBuiltIns()
    └─ @Provides для трёх новых DAO

[~] WordDao.kt
    ├─ removes: updateLexemeDefinition, updateLexemeTranslation (legacy)
    ├─ adds: addLexemeWithComponents (@Transaction default-method)
    └─ extends: addDictionary → atomic INSERT + quiz_config

[~] CoreDbApiImpl.kt (LexemeApiImpl, DictionaryApiImpl)
    ├─ depends on: WordDao [~], ComponentTypeDao, ComponentValueDao, QuizConfigDao
    ├─ depends on: ComponentValueDataJson, ComponentTypeRefJson, mappers in entity files
    └─ replaces STUB synthetic-id paths with real DAO calls

[+] MigrationFrom11to12.kt (androidTest)
    ├─ depends on: 12.json snapshot
    └─ depends on: Migration_011_to_012
```

---

## Счёт узлов

| Type | Count | Files |
|---|---|---|
| `[+]` create | 12 | ComponentTypeDb, ComponentValueDb, ComponentValueWithType, QuizConfigDb, ComponentValueDataJson, ComponentTypeRefJson, ComponentTypeDao, ComponentValueDao, QuizConfigDao, Migration_011_to_012, SeedBuiltIns, MigrationFrom11to12, 12.json (auto-gen — учитывается как артефакт) |
| `[~]` modify | 5 | LexemeDbEntity, LexemeDb, WordDao, Database, RoomModule, CoreDbApiImpl |
| `[-]` delete | 0 | — |

**Итого узлов IS481 data sub-flow: 13 `[+]` + 6 `[~]` = 19 узлов.**

(12.json auto-gen + ApiModule [=] + build.gradle.kts [=] и domain/api узлы [=] не входят в счёт изменений data sub-flow — это либо derived artifacts, либо unchanged context.)

---

## log_messages

- 19 узлов design tree: 13 [+] new + 6 [~] modified; [-] нет (M11→M12 удаляет колонки, не файлы)
- все [~] узлы verified через Read: LexemeDbEntity (synthetic mapper → honest), LexemeDb (drop translation/definition columns), WordDao (drop updateLexemeDefinition + add addLexemeWithComponents + extend addDictionary), Database (v11→v12 + 3 entities + 3 DAOs), RoomModule (.addMigrations + onCreate seedBuiltIns + provides), CoreDbApiImpl (STUB synthetic → real DAO в LexemeApiImpl + addDictionary atomic в DictionaryApiImpl)
- key reuse patterns: @Transaction default-method (addLexemeWithQuiz template), @ColumnInfo snake_case, FK CASCADE + @Index, Multi-level @Relation (TermDbEntity template), DAO extension mappers рядом с data class

_model: claude-opus-4-7[1m]_
