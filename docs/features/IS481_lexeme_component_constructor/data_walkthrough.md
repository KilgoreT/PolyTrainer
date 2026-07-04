# Data walkthrough — IS481

Discovery текущего состояния data-слоя `core/core-db-impl/` ПЕРЕД реализацией IS481 data sub-flow. Цель — зафиксировать существующие patterns / locations / shape, чтобы design tree и migration test plan опирались на реальный код, а не на план.

Snapshot момент: HEAD ветки `IS481_lexeme_component_constructor` (после business_implement + ui_implement, до data_implement). Бизнес-слой уже расширен generic component API; в `core-db-impl` лежит **STUB-реализация** `LexemeApiImpl` (synthetic id `-1L` / `-2L` для built-in TRANSLATION и user-defined Definition) которая ещё работает через legacy колонки `lexemes.translation` / `.definition`.

---

## 1. Состояние БД (pre-IS481 main)

### Версия и entities

`core/core-db-impl/src/main/java/me/apomazkin/core_db_impl/room/Database.kt`:

```kotlin
@Database(
    entities = [
        WordDb::class,
        LexemeDb::class,
        HintDb::class,
        SampleDb::class,
        WriteQuizDb::class,
        DictionaryDb::class
    ],
    version = 11
)
@TypeConverters(DateTimeConverter::class)
abstract class Database : RoomDatabase() {
    abstract fun wordDao(): WordDao
}
```

- `version = 11` — нужно поднять до `12`.
- entities = 6 — нужно добавить `ComponentTypeDb`, `ComponentValueDb`, `QuizConfigDb` (итого 9).
- Один DAO `wordDao()` — нужно либо расширить, либо добавить `componentTypeDao()` / `componentValueDao()` / `quizConfigDao()`.
- TypeConverters: только `DateTimeConverter` — `Date ↔ Long`. Новые TypeConverter'ы НЕ нужны (`system_key` / `template_key` хранятся как String, маппинг в enum на уровне API-маппера).

### RoomModule (post-prereq)

`core/core-db-impl/src/main/java/me/apomazkin/core_db_impl/di/module/RoomModule.kt`:

```kotlin
@Singleton
@Provides
fun provideDatabase(context: Context, logger: LexemeLogger): Database {
    return Room.databaseBuilder<Database>(context = context, name = DATABASE_NAME)
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .fallbackToDestructiveMigration(dropAllTables = true)
        .addCallback(object : RoomDatabase.Callback() {
            override fun onDestructiveMigration(connection: SQLiteConnection) {
                logger.e(tag = LogTags.DB, message = "Destructive migration...")
            }
        })
        .build()
}
```

- KMP-builder Room 2.7+: `Room.databaseBuilder<Database>(...)`.
- `BundledSQLiteDriver()` — bundled SQLite 3.45+ активен.
- `setQueryCoroutineContext(Dispatchers.IO)` — coroutines IO для query.
- `fallbackToDestructiveMigration(dropAllTables = true)` — edge case pre-0.1.0.
- `addCallback` с overridden `onDestructiveMigration(connection: SQLiteConnection)` — для логирования destructive.
- **НЕТ** `.addMigrations(...)` вызова — будет добавлен в IS481 (`.addMigrations(Migration_011_to_012)`).
- **НЕТ** override `onCreate(connection: SQLiteConnection)` — нужно добавить если будет seed built-in для fresh install.

### Existing entities

| Файл | tableName | shape |
|---|---|---|
| `entity/DictionaryDb.kt` | `dictionaries` | `id, numericCode?, name, addDate, changeDate?` — БЕЗ FK, БЕЗ `@ColumnInfo` (camelCase column names). |
| `entity/WordDb.kt` | `words` | `id, dictionary_id (FK → dictionaries.id CASCADE), value, add_date, change_date`. Snake_case через `@ColumnInfo`. Index на `dictionary_id`. |
| `entity/LexemeDb.kt` | `lexemes` | `id, word_id (FK → words.id CASCADE), translation?, definition?, word_class?, options, add_date, change_date`. Snake_case через `@ColumnInfo`. Index на `word_id`. Колонки `translation` / `definition` будут **удалены** в M11→M12. |
| `entity/SampleDb.kt` | `samples` | `id, lexemeId, value, source?, addDate, changeDate?, removeDate?`. БЕЗ FK, camelCase column names (legacy). |
| `entity/HintDb.kt` | `hints` | `id, lexemeId?, value, addDate, changeDate?, removeDate?`. БЕЗ FK, camelCase. |
| `entity/WriteQuizDb.kt` | `write_quiz` | `id, dictionary_id, lexeme_id (FK → lexemes.id CASCADE), grade, score, error_count, add_date, last_select_date?`. Snake_case через `@ColumnInfo`. Index на `lexeme_id`. |

**Pattern для новых entities (ComponentTypeDb / ComponentValueDb / QuizConfigDb):**
- Использовать `@ColumnInfo(name = "snake_case")` (как WordDb / LexemeDb / WriteQuizDb).
- `@PrimaryKey(autoGenerate = true) @ColumnInfo(name = "id") val id: Long = 0`.
- `@ForeignKey(entity = DictionaryDb::class, parentColumns = ["id"], childColumns = ["dictionary_id"], onDelete = ForeignKey.CASCADE)`.
- `@Index("col")` для FK; `@Index(value = ["col"], unique = true)` для UNIQUE.

### Existing @Relation entities (Multi-level patterns)

`entity/LexemeDbEntity.kt`:
```kotlin
data class LexemeDbEntity(
    @Embedded val lexemeDb: LexemeDb,
    @Relation(parentColumn = "id", entityColumn = "lexemeId")
    val sampleDbList: List<SampleDb>
)
```
- Pattern для @Relation на child collection.
- `entityColumn = "lexemeId"` — camelCase т.к. SampleDb legacy без @ColumnInfo.
- **Расширение IS481:** добавить второй @Relation на `componentValueListDb: List<ComponentValueWithType>` с `entity = ComponentValueDb::class, parentColumn = "id", entityColumn = "lexeme_id"` (snake_case через @ColumnInfo).

`entity/TermDbEntity.kt`:
```kotlin
data class TermDbEntity(
    @Embedded val wordDb: WordDb,
    @Relation(entity = LexemeDb::class, parentColumn = "id", entityColumn = "word_id")
    val lexemeListDb: List<LexemeDbEntity>
)
```
- Pattern для @Relation с подгрузкой child entity (LexemeDbEntity вместо плоского LexemeDb) — Room делает batched JOIN.
- **Multi-level @Relation работает:** `TermDbEntity → LexemeDbEntity (с @Relation на samples) → SampleDb`. IS481 расширит до `TermDbEntity → LexemeDbEntity → ComponentValueWithType → (ComponentValueDb + ComponentTypeDb)`.

`entity/LexemeDbWithWordDbRelation.kt`:
```kotlin
data class LexemeDbWithWordDbRelation(
    @Embedded val lexemeDb: LexemeDbEntity,
    @Relation(parentColumn = "word_id", entityColumn = "id")
    val wordDb: WordDb,
)
```
- Pattern для embedded entity → parent (LexemeDb → WordDb).

`entity/WriteQuizDbEntity.kt`:
```kotlin
data class WriteQuizDbEntity(
    @Embedded val writeQuizDb: WriteQuizDb,
    @Relation(entity = LexemeDb::class, parentColumn = "lexeme_id", entityColumn = "id")
    val lexemeDbWithWordDbRelation: LexemeDbWithWordDbRelation,
)
```
- Multi-level: `write_quiz → lexeme → samples (+ word)`.

**Pattern для нового `ComponentValueWithType`:**
```kotlin
data class ComponentValueWithType(
    @Embedded val value: ComponentValueDb,
    @Relation(parentColumn = "component_type_id", entityColumn = "id")
    val type: ComponentTypeDb,
)
```
Используется внутри `LexemeDbEntity.componentValueListDb: List<ComponentValueWithType>` (Multi-level). Маппер `toApiEntity()` пробрасывает оба слоя в один `ComponentValueApiEntity` (с `type` full embedded).

### WordDao — единственный DAO

`core/core-db-impl/src/main/java/me/apomazkin/core_db_impl/room/WordDao.kt`:

**Что есть:**
- Dictionaries: `addDictionary`, `getDictionaryByNumeric/ById`, `getDictionaries`, `updateDictionary`, `deleteDictionary`, `flowDictionaries`.
- Words: `addWordSuspend`, `updateWorldSuspend`, `removeWordSuspend`.
- Terms (через @Relation): `getTermList`, `searchTerms*` (incl. paging), `getTermById`, `getWordSuspend`.
- Lexemes: `addLexeme`, `updateLexeme`, `getLexemeById`, `updateLexemeTranslation`, `updateLexemeDefinition`, `updateLexemeCategory`, `deleteLexemeById`, `deleteDefinitionSuspend`, `deleteDefinitionsSuspend`.
- Samples: `removeSampleSuspend`.
- Quiz: `addWriteQuiz`, **`addLexemeWithQuiz`** (`@Transaction default method` — atomic INSERT lexeme + write_quiz), `updateWriteQuiz`, `getWriteQuizIds`, `getWriteQuizByIds`, `getEarliest`, `getFrequentMistakes`, `removeWriteQuiz`.
- Statistic flow: `flowWordCount`, `flowLexemeCount`, `flowQuizCount`.

**Patterns:**
- `@Insert(onConflict = OnConflictStrategy.ABORT)` для `addDictionary` (явный ABORT).
- `@Insert` без onConflict (default ABORT) для прочих.
- `@Update(onConflict = OnConflictStrategy.REPLACE)` для `updateWriteQuiz`.
- `@Query("UPDATE ...")` для partial updates по полю (`updateLexemeTranslation` / `Definition` / `Category`).
- `@Transaction` + `@Query` для @Relation-чтения (`getTermById`, `getLexemeById`).
- `@Transaction suspend fun ...default-method...` для atomic compound (`addLexemeWithQuiz`).
- `Flow<...>` для reactive query.

**Решение IS481 (по плану):**
- ВАРИАНТ A: расширить `WordDao` методами для `component_types` / `component_values` / `quiz_configs` (минимально invasive).
- ВАРИАНТ B: отдельные `ComponentTypeDao` / `ComponentValueDao` / `QuizConfigDao` (по плану 03_database_design.md § 5).
- **План 03 явно требует B** — отдельные DAO + дополнительный метод `Database.componentTypeDao()` / `componentValueDao()` / `quizConfigDao()`.
- НО: `WordDao.addDictionary` транзакцию расширять надо в любом случае (AGG-4 реверс) — atomic `INSERT dictionaries + INSERT quiz_configs(default)`. Это default-method на `WordDao` (по аналогии с `addLexemeWithQuiz`).
- Аналогично `WordDao.addLexemeWithComponents` (новый MIN-9 compound INSERT) — лежит на `WordDao` т.к. требует доступ к `lexemes` + `write_quiz` + `component_values` в одной транзакции.

### CoreDbApiImpl — current state

`core/core-db-impl/src/main/java/me/apomazkin/core_db_impl/CoreDbApiImpl.kt`:

**Структура:**
- `CoreDbApiImpl` (root класс) implements `CoreDbApi`.
- Inner `DbInstanceImpl`, `DictionaryApiImpl`, `TermApiImpl`, `WordApiImpl`, `LexemeApiImpl`, `QuizApiImpl`, `StatisticApiImpl` — каждый @Inject c WordDao (+ logger где нужно).
- DI binding в `ApiModule.kt` через @Binds.

**`LexemeApiImpl` — текущие STUB методы (lines 202-435):**

| Method | Текущая реализация | Что нужно в IS481 |
|---|---|---|
| `getLexemeById(id)` | `wordDao.getLexemeById(id)?.toApiEntity()` — через synthetic mapper в LexemeDbEntity | Honest mapper с reading из `component_values` table |
| `addLexeme(wordId)` | `wordDao.addLexeme(LexemeDb(wordId, addDate))` — only lexeme, no quiz | OK (низкоуровневый, оставить) |
| `deleteLexeme(id)` | `wordDao.deleteLexemeById(id)` | OK |
| `addLexemeWithBuiltInComponent(wordId, dictionaryId, systemKey, data)` | Shim: extract `text` from `TextValue`, call `wordDao.addLexemeWithQuiz(LexemeDb(wordId, translation = text))` — игнорирует `systemKey != TRANSLATION` (throws NotImplementedError для non-text) | Реальный atomic INSERT: lexeme + write_quiz + component_values (lookup typeId by systemKey) в одной транзакции через new `WordDao.addLexemeWithComponents` |
| `addLexemeWithUserDefinedComponent(wordId, dictionaryId, name, data)` | Shim: только для name="Definition" — INSERT в `lexemes.definition` колонку через `addLexemeWithQuiz` (throws для других name) | Реальный atomic: lexeme + write_quiz + component_values (lookup typeId by `(dictionary_id, name, system_key=NULL)`) |
| `addLexemeWithComponents(wordId, dictionaryId, components)` | Shim: extract translation + definition из pair-list, INSERT через `addLexemeWithQuiz` (одна транзакция legacy путём) | Реальный compound: lexeme + write_quiz + N component_values атомарно |
| `addComponentValue(lexemeId, componentTypeId, data)` | Shim: для synthetic `componentTypeId == -1L` → `updateLexemeTranslation`; `-2L` → `updateLexemeDefinition`; throws иначе | `componentValueDao.insert(ComponentValueDb(lexemeId, componentTypeId, data.toJson()))` |
| `updateComponentValue(componentValueId, data)` | Shim: decode synthetic id (parity check), вызов `updateLexemeTranslation` / `Definition` | `componentValueDao.update(...)` |
| `deleteComponentValue(componentValueId)` | Shim: nullify legacy колонку, recount остаток через `getLexemeById` | `componentValueDao.delete(id)` + `componentValueDao.getForLexeme(lexemeId).size` |
| `getComponentTypes(dictionaryId)` | Shim: возвращает hardcoded synthetic `[ComponentTypeApiEntity(id=-1L, systemKey=TRANSLATION), ComponentTypeApiEntity(id=-2L, name="Definition")]` | `componentTypeDao.getTypesForDictionary(dictionaryId).map { it.toApiEntity() }` |
| `getQuizConfig(dictionaryId, quizMode)` | Stub: hardcoded `QuizConfigApiEntity(componentRefs = [BuiltIn(TRANSLATION)])` | `quizConfigDao.getByDictionaryAndMode(dictionaryId, quizMode)?.toApiEntity()` |
| `addLexemeWithTranslation(wordId, dictionaryId, translation)` (@Deprecated) | Delegate to `addLexemeWithBuiltInComponent(...)` | OK (остаётся @Deprecated, без правок) |
| `updateLexemeTranslation(id, translation)` (@Deprecated) | `wordDao.updateLexemeTranslation(id, translation?.value)` — direct legacy | Переписать на generic path через `updateComponentValue` либо оставить как @Deprecated raw SQL UPDATE (план 06 предлагает оставить — translation built-in lookup) |

**`DictionaryApiImpl.addDictionary`** (lines 85-94):
```kotlin
override suspend fun addDictionary(name: String, numericCode: Int?): Long {
    val currentDate = Date(System.currentTimeMillis())
    return wordDao.addDictionary(DictionaryDb(...))
}
```
- Сейчас просто INSERT в `dictionaries`.
- **AGG-4 расширение:** заменить на atomic `WordDao.addDictionary` (новый default-method) который делает `INSERT dictionaries + INSERT quiz_configs(default)` в одной транзакции. Существующий метод `wordDao.addDictionary(dictionaryDb)` останется низкоуровневым, новый default-method обернёт его + INSERT quiz_configs.

### Mappers

`core/core-db-impl/src/main/java/me/apomazkin/core_db_impl/entity/LexemeDbEntity.kt:29-74`:
- `toApiEntity()` — **сейчас synthetic** — конструирует `LexemeApiEntity(components = [...])` из `lexemeDb.translation` / `.definition` колонок через synthetic `ComponentTypeApiEntity(id=-1L)` / `id=-2L`.
- Будет переписан на честный путь: `componentValueListDb.map { it.toApiEntity() }` через Multi-level @Relation.

`core/core-db-impl/src/main/java/me/apomazkin/core_db_impl/mapper/` — папка существующих маpper'ов:
- `DefinitionMapper.kt`, `DefinitionSampleRelMapper.kt`, `SampleMapper.kt`, `HintMapper.kt`, `Mapper.kt`, `WordDumpMapper.kt`, `WriteQuizDumpMapper.kt`, `SampleDumpMapper.kt`, `DefinitionDumpMapper.kt`, `HintDumpMapper.kt`, `DictionaryDumpMapper.kt`.
- Pattern: `Db → ApiEntity` через extension function (`fun XxxDb.toApiEntity()` или `fun XxxDbEntity.toApiEntity()`).
- IS481 добавит: `ComponentValueDataJson.kt`, `ComponentTypeRefJson.kt` в эту же папку (JSON helpers, по контракту 06.md).
- Существующие мапперы entity ↔ ApiEntity для component types/values живут **рядом с data class** (как `WordDb.kt` / `DictionaryDb.kt`), а не в `mapper/` — оба паттерна допустимы в кодовой базе.

### Existing converters

`core/core-db-impl/src/main/java/me/apomazkin/core_db_impl/converters/DateTimeConverter.kt` — единственный TypeConverter (`Date ↔ Long`). IS481 НЕ добавляет новых.

---

## 2. core-db-api (DTO + interface)

### CoreDbApi.LexemeApi (lines 81-164)

После business_implement в IS481 уже расширен:
- Удалены: `addLexeme(wordId, translation)`, `addLexeme(wordId, definition)`, `addLexemeWithDefinition`, `updateLexemeDefinition`, `deleteLexemeDefinition`.
- Добавлены generic: `addLexemeWithBuiltInComponent`, `addLexemeWithUserDefinedComponent`, `addLexemeWithComponents` (MIN-9), `addComponentValue`, `updateComponentValue`, `deleteComponentValue`, `getComponentTypes`, `getQuizConfig`.
- Остались @Deprecated: `addLexemeWithTranslation`, `updateLexemeTranslation`.

**Контракт IS481 data sub-flow** — переписать STUB-реализацию в `LexemeApiImpl` (см. таблицу выше).

### Existing API DTOs (созданы business_implement)

- `entity/ComponentTypeApiEntity.kt` — `id, systemKey: BuiltInComponent?, dictionaryId: Long?, name: String?, template: ComponentTemplate, position, removeDate?`.
- `entity/ComponentValueApiEntity.kt` — `id, lexemeId, type: ComponentTypeApiEntity (full embedded), data: ComponentValueData`.
- `entity/QuizConfigApiEntity.kt` — `id, dictionaryId, quizMode, componentRefs: List<ComponentTypeRef>`.
- `entity/LexemeApiEntity.kt` — `id, components: List<ComponentValueApiEntity>, wordClass?, options, addDate, changeDate?`. Поля `translation` / `definition` УДАЛЕНЫ.

**Gradle dep edge (MIN-2):** `core-db-api/build.gradle.kts` уже содержит:
```kotlin
// IS481 (MIN-2): api (не implementation) — типы domain видны транзитивно
api(project(":modules:domain:lexeme"))
```

Эта dep edge **уже подключена** (business_implement добавил). IS481 data sub-flow её НЕ дублирует.

---

## 3. Domain types (modules/domain/lexeme/)

Все нужные типы созданы в business_implement:

- `BuiltInComponent.kt` — `enum { TRANSLATION("translation") }` + `fromKey()`.
- `ComponentTemplate.kt` — `enum { TEXT, LONG_TEXT, IMAGE }` + `fromKey()` (fallback TEXT).
- `ComponentValueData.kt` — sealed `{ TextValue, LongTextValue, ImageValue }`.
- `ComponentType.kt` — `data class ComponentType(id: ComponentTypeId, systemKey, dictionaryId, name, template, position, removeDate)` + `ComponentTypeId`.
- `ComponentValue.kt` — `data class ComponentValue(id: ComponentValueId, lexemeId, type, data)` + `ComponentValueId`.
- `ComponentTypeRef.kt` — sealed `{ BuiltIn, UserDefined }`.
- `QuizConfig.kt` — `data class QuizConfig(dictionaryId, quizMode, componentRefs)`.
- `Lexeme.kt` — расширен `components`, shim `translation` / `definition` @Deprecated.
- `LexemeBuiltInExt.kt` — `builtIn(key)` + computed shim extensions.

IS481 data sub-flow эти файлы **НЕ трогает** — только использует в мапперах.

---

## 4. Существующие migration tests (post-prereq)

`core/core-db-impl/src/androidTest/java/me/apomazkin/core_db_impl/room/`:
- `ExampleInstrumentedTest.kt` — placeholder, не используется.
- `BundledSqliteFeatureTest.kt` — 6 smoke кейсов на bundled SQLite features (ALTER TABLE DROP COLUMN, json_object, json_insert append `$[#]`, json_each, json_remove, sqlite_version ≥ 3.45). Уже PASS — AGG-8 verify закрыт.

**Что нужно добавить в IS481:**
- `MigrationFrom11to12.kt` — migration test через `MigrationTestHelper(driver = BundledSQLiteDriver(), schema = "schemas/...", file = Database::class.java)`.

10 historical migration tests (v1→v11) и harness (BaseMigration, Schemable, DataProvider, Schema) **удалены в prereq фиче** — restart от нуля только под v11→v12.

---

## 5. Имена индексов — pattern Room auto-gen

Существующие schemas `11.json` подтверждают паттерн Room автогенерации:
```json
{
  "name": "index_words_dictionary_id",
  "unique": false,
  "columnNames": ["dictionary_id"]
}
```

Pattern: `index_<table>_<col1>_<col2>...`. Для UNIQUE — то же имя, `unique=true`.

**Имена индексов в Migration_011_to_012.kt** ДОЛЖНЫ совпадать с тем что Room сгенерирует в `12.json`:
- `index_component_types_dictionary_id`
- `index_component_types_system_key` (UNIQUE)
- `index_component_types_dictionary_id_name` (UNIQUE)
- `index_component_types_global_userdef_name` (partial UNIQUE — Room не моделирует WHERE, индекс создаётся вручную в migration + onCreate)
- `index_component_values_component_type_id`
- `index_component_values_lexeme_id_component_type_id` (UNIQUE)
- `index_quiz_configs_dictionary_id`
- `index_quiz_configs_dictionary_id_quiz_mode` (UNIQUE)

Проверка имён — после первой генерации `12.json` через KSP build.

---

## 6. Schema export config

`core/core-db-impl/build.gradle.kts:9-14`:
```kotlin
room {
    schemaDirectory("$projectDir/schemas")
}
sourceSets {
    getByName("androidTest").assets.srcDirs("$projectDir/schemas")
}
```

Room schema export уже настроен:
- Schema snapshots пишутся в `core/core-db-impl/schemas/me.apomazkin.core_db_impl.room.Database/`.
- AndroidTest assets читают эту же папку — `MigrationTestHelper` найдёт `12.json` после генерации.

**Существующие snapshots:** 1.json — 11.json. После `@Database(version = 12)` Room автоматически сгенерирует `12.json` при первом build.

---

## 7. Verdict — аналог найден / не найден

**Аналог НАЙДЕН** для нескольких ключевых patterns — переиспользуем:

1. **Entity с @ColumnInfo (snake_case) + FK CASCADE + @Index** — `WordDb` / `LexemeDb` / `WriteQuizDb`. Pattern для `ComponentTypeDb` / `ComponentValueDb` / `QuizConfigDb`.
2. **@Relation single + collection** — `LexemeDbEntity.sampleDbList` (collection), `LexemeDbWithWordDbRelation.wordDb` (single). Pattern для `ComponentValueWithType` (single Relation на ComponentTypeDb) + добавление `componentValueListDb` в `LexemeDbEntity`.
3. **Multi-level @Relation (3+ слоя)** — `TermDbEntity → LexemeDbEntity → SampleDb` уже работает. Расширение до `LexemeDbEntity → ComponentValueWithType → ComponentTypeDb` — тот же паттерн.
4. **Atomic compound INSERT через `@Transaction default-method` в DAO** — `WordDao.addLexemeWithQuiz` (lexeme + write_quiz). Pattern для `addLexemeWithComponents` (lexeme + write_quiz + N component_values) и `WordDao.addDictionary` extended (dictionary + quiz_config).
5. **`@Query("UPDATE ...")` для partial update** — `updateLexemeTranslation` / `Definition`. Pattern для `ComponentValueDao.update`, soft-delete `ComponentTypeDao.softDelete(id, now)`.
6. **TypeConverter `Date ↔ Long`** — `DateTimeConverter`. Для `ComponentTypeDb.removeDate: Date?` переиспользуем.
7. **DI binding через @Binds в `ApiModule`** — pattern для `ComponentTypeApiImpl` etc, если будут separate API impl. По плану 03 все методы остаются в `LexemeApiImpl`, separate API impl НЕ нужен.

**Аналог НЕ НАЙДЕН** для:

1. **JSON-storage через `json_object()` в SQL** — bundled SQLite 3.45 даёт, но в codebase нет prior usage. Будет first use в Migration_011_to_012 (steps 4, 5, 7.2, 7.3) + helpers `ComponentValueDataJson.kt` / `ComponentTypeRefJson.kt` через `org.json.JSONObject`.
2. **Partial UNIQUE index с WHERE clause** — в `11.json` нет таких. Будет first use в Migration step 2 (`CREATE UNIQUE INDEX ... WHERE dictionary_id IS NULL AND system_key IS NULL`). Создаётся вручную в migration + onCreate callback.
3. **Multi-level @Relation с пробросом обоих слоёв в один API DTO** — TermDbEntity делает 3 слоя, но каждый слой остаётся отдельным embedded полем. IS481 паттерн — `ComponentValueWithType` (Embedded + Relation) пробрасывается в `ComponentValueApiEntity(type = ComponentTypeApiEntity, data = ComponentValueData)` где `type` уже embedded внутри. Это first use of "flatten 2 DB layers into 1 API DTO".
4. **`Database.Callback.onCreate(connection: SQLiteConnection)`** для seed built-in — сейчас callback переопределяет только `onDestructiveMigration`. Override `onCreate(connection)` — first use.
5. **`MigrationTestHelper` с bundled driver** — prereq feature удалила всю migration test infrastructure. Restart от нуля.

**Что переиспользуется (паттерны без правок):**
- Snake_case через `@ColumnInfo` для column names.
- FK + Index + onDelete CASCADE паттерн (`WordDb` шаблон).
- `@Transaction` default-method для compound INSERT (`addLexemeWithQuiz` шаблон).
- @Relation collection + Multi-level (TermDbEntity шаблон).
- DI binding через @Binds в ApiModule.
- Mapper extension function на data class (`fun XxxDb.toApiEntity()`).
- Room schema export config (`core/core-db-impl/build.gradle.kts`).
- KMP-builder Room 2.7+ (`Room.databaseBuilder<Database>(...)`) + bundled driver.

---

## log_messages

- core-db-impl state inventarized: Database v11, 6 entities, single WordDao, RoomModule post-prereq (bundled driver + destructive fallback + onDestructiveMigration callback, no migrations registered)
- CoreDbApiImpl.LexemeApiImpl — все 11 IS481 methods existing as STUB (synthetic ids -1L/-2L), нужно переписать на DAO calls после миграции
- patterns переиспользуются: @ColumnInfo snake_case, FK CASCADE + @Index, Multi-level @Relation (TermDbEntity), @Transaction default-method (addLexemeWithQuiz)

_model: claude-opus-4-7[1m]_
