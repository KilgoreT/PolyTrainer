# Стратегия миграции lexemes → components

Детальный план миграции существующих данных translation/definition (v11) в новую модель `component_types` + `component_values` (v12). Дополнение к [`04_builtin_strategy.md`](04_builtin_strategy.md) и [`03_database_design.md`](03_database_design.md).

Возник как separate doc после обсуждения Performance #3 в `03_database_design_review.md` — миграция оказалась нетривиальной из-за разделения built-in vs user-defined и JSON-escape проблемы.

---

## ✅ Чеклист задач миграции (для верификации)

При реализации пройти каждый пункт.

### Bundled SQLite (закрыто prereq фичей — AGG-12)

Все подготовительные работы по bundled SQLite driver выполнены в prereq фиче [`IS481_lexeme_component_constructor_vPrepared`](../../IS481_lexeme_component_constructor_vPrepared/) (см. `infra_implement.md` + `check.md` + post-mortem). На момент старта IS481 main:

- ✅ `androidx.sqlite:sqlite-bundled:2.6.2` подключён (`core/core-db-impl/build.gradle.kts` + `deps/datastore.versions.toml`).
- ✅ `RoomModule` переписан на KMP-builder Room 2.7+: `Room.databaseBuilder<Database>(...).setDriver(BundledSQLiteDriver()).setQueryCoroutineContext(Dispatchers.IO).fallbackToDestructiveMigration(dropAllTables = true)`.
- ✅ `Database.Callback.onDestructiveMigration(connection: SQLiteConnection)` логирует через `LexemeLogger.e(tag = LogTags.DB, ...)` → `CrashlyticsSink.recordException` (edge case pre-0.1.0 install).
- ✅ ProGuard keep-rules для bundled native methods в `app/proguard-rules.pro` (узкий keep `androidx.sqlite.**` — есть запись в `docs/Backlog.md` про канонический широкий keep, B6 follow-up, не блокер IS481).
- ✅ Smoke test `BundledSqliteFeatureTest` (6 кейсов: `ALTER TABLE DROP COLUMN`, `json_object`, `json_insert append '$[#]'`, `json_each`, `json_remove`, `sqlite_version ≥ 3.45`) — пройден 6/6 за 1.601s на Pixel 7 Pro / Android 16 / API 36 (`./gradlew :core:core-db-impl:connectedDebugAndroidTest`). AGG-8 verify json_insert закрыт фактом успешного теста.
- ✅ **10 historical миграций v1→v11 дропнуты целиком** — production migration files, `BaseMigration`, `Schemable`, `DataProvider`, `Schema`, `AllMigrationTest`, 12 snapshot entities, utils. Testers на 0.1.0+ имеют `Database.version = 11` (verified через tags 0.1.0 + 0.1.5), миграции с pre-0.1.0 не нужны. Retrospective timeline удалённых миграций — `docs/db-migrations-history.md`.

**Для IS481 main M11→M12 следствие AGG-12:**

- [ ] **Одна новая** `core/core-db-impl/src/main/java/.../room/migrations/Migration_011_to_012.kt` пишется **directly** под `override fun migrate(connection: SQLiteConnection)` API. Никакого Room 2.8 compat layer (AGG-7/AGG-11 obsolete). Подключить через `.addMigrations(Migration_011_to_012)` в `RoomModule.kt` (текущий builder уже имеет destructive fallback — addMigrations добавляется отдельно, fallback остаётся для pre-0.1.0 path).
- [ ] **Один** `core/core-db-impl/src/androidTest/java/.../room/MigrationFrom11to12.kt` test с `MigrationTestHelper(driver = BundledSQLiteDriver(), ...)`. Использует helper API доступный в androidx.room 2.8.4 (verified через library source во время prereq).
- [ ] **`core/core-db-api/build.gradle.kts`** (MIN-2) — добавить `implementation(project(":modules:domain:lexeme"))`. Без этой dep IS481 не соберётся: `LexemeApiEntity` / `ComponentTypeApiEntity` / `ComponentValueApiEntity` начинают использовать domain типы `BuiltInComponent` / `ComponentTemplate` / `ComponentValueData`. После IS482 этой dep не было — `LexemeApiEntity` не использовал domain типы.
- [ ] **Import** `androidx.sqlite.execSQL` (extension `SQLiteConnection.execSQL(sql: String)`, B3) — в `Migration_011_to_012.kt`, seed-функциях и callback (если будет дополнен).
- [ ] **`Database.Callback.onCreate(connection: SQLiteConnection)`** (B1) — если потребуется seed built-in **translation** для fresh install path: переопределить exactly эту signature (NOT legacy `onCreate(db: SupportSQLiteDatabase)`, она под bundled driver не вызывается). Сейчас в `RoomModule` callback пустой кроме `onDestructiveMigration` — добавлять `onCreate` только если миграция M11→M12 не покрывает fresh install (т.е. если новые таблицы создаются только в миграции, а не через `@Entity` annotations + Room auto-create). По умолчанию Room сам создаёт таблицы из `@Entity` для fresh install — `onCreate` для seed нужен только для INSERT встроенного translation row в `component_types`.

### Схема и seed

- [ ] CREATE TABLE `component_types` (см. `04_builtin_strategy.md` § «Схема таблицы»).
- [ ] CREATE TABLE `component_values`.
- [ ] Стандартные индексы (`@Index`) + partial UNIQUE через `connection.execSQL` (extension `androidx.sqlite.execSQL`).
- [ ] Функция `seedBuiltIns(connection: SQLiteConnection)` — вставляет только **translation** (один built-in). Использует `connection.execSQL(...)` через extension `androidx.sqlite.execSQL`.
- [ ] `Callback.onCreate(connection: SQLiteConnection)` вызывает `seedBuiltIns(connection)` + создаёт partial UNIQUE index (для fresh install). НЕ переопределять legacy `onCreate(db: SupportSQLiteDatabase)` — под bundled driver не вызывается.

### Данные

- [ ] **Translation:** для каждой `lexemes.translation IS NOT NULL` — вставить ComponentValue со ссылкой на built-in translation (по `system_key`, не по id).
- [ ] **Definition:** для каждого словаря (`SELECT DISTINCT dictionary_id FROM lexemes JOIN words WHERE definition IS NOT NULL`):
  - Создать user-defined `ComponentType(dictionary_id=X, name="Definition", template_key="text", system_key=NULL, position=10)`.
  - Для каждой `lexemes.definition IS NOT NULL` в этом словаре — вставить ComponentValue со ссылкой на этот user-defined тип.
- [ ] Удалить колонки `translation` / `definition` из `lexemes` через `ALTER TABLE DROP COLUMN` (bundled SQLite 3.45+, без recreate-таблицы).
- [ ] **Имена индексов в миграции** должны совпадать с автогенерируемыми Room (pattern `index_<table>_<col1>_<col2>`). Проверить через `12.json` snapshot после первой генерации. Иначе Room validation падает.

### Quiz configs (шаг 6)

- [ ] CREATE TABLE `quiz_configs` со всеми колонками / FK CASCADE на `dictionaries(id)`.
- [ ] Индексы: `INDEX(dictionary_id)`, `UNIQUE INDEX(dictionary_id, quiz_mode)`. Имена через Room pattern (`index_quiz_configs_dictionary_id`, `index_quiz_configs_dictionary_id_quiz_mode`) — проверить совпадение с `12.json` snapshot.
- [ ] **INSERT для ВСЕХ словарей** default config `quiz_mode='write'`, `component_refs=[BuiltIn(translation)]`. БЕЗ фильтра WHERE — даже пустой dictionary получает default (F1 closed).
- [ ] **UPDATE для словарей с definition** — добавить `UserDefined("Definition")` в `component_refs` JSON (`json_insert($, '$[#]', ...)`). **Verify (AGG-8) — перед IS481 миграцией:** smoke test для синтаксиса `json_insert($, '$[#]', json_object(...))` против реальной bundled SQLite. Если работает — используем; если нет — fallback на собирать JSON в Kotlin строкой перед UPDATE.
- [ ] Helper `ComponentTypeRefJson.kt` в `core-db-impl` (см. `06.md`) — используется при чтении/записи `component_refs` через DAO. В миграции — прямой SQL `json_object`/`json_insert` (bundled SQLite JSON1).

### API совместимости (без переработки mate / UI)

- [ ] **CoreDbApi/UseCase** — добавить **новые generic-методы**: `addLexemeWithBuiltInComponent(wordId, dictionaryId, systemKey, data)`, **`addLexemeWithUserDefinedComponent(wordId, dictionaryId, name, data)`** (AGG-6), `addComponentValue(lexemeId, typeId, data)`, `updateComponentValue(id, data)`, `deleteComponentValue(id)`. Также **DAO `insertDefaultQuizConfig(dictionaryId, quizMode)`** (MIN-8): простой method, пишет hardcoded JSON `'[{"type":"builtin","key":"translation"}]'`. Без передачи доменного `QuizConfig` объекта.

- [ ] **Атомарность `addDictionary` + `insertDefaultQuizConfig`** (AGG-4 реверс): INSERT в `dictionaries` + INSERT в `quiz_configs` для каждого зарегистрированного `quiz_mode` — **в одной транзакции**. Реализация — либо `@Transaction`-аннотированный DAO method, либо `db.withTransaction { ... }` в UseCase. При падении любого INSERT — rollback всего, dictionary не создан. **Тест:** симулировать FK violation / corrupt JSON на `insertDefaultQuizConfig` → assert `dictionaries` row не создан, F1 invariant держится.

- [ ] **`CoreDbApi.LexemeApi` translation-обёртки** (data-API слой, остаются как @Deprecated): `addLexemeWithTranslation(wordId, dictionaryId, translation: TranslationApiEntity)` / `updateLexemeTranslation` / `deleteLexemeTranslation` — drop-in замена реальной сигнатуры, делегируют в generic метод.

- [ ] **Definition-обёртки УДАЛЕНЫ** (AGG-6): из `CoreDbApi.LexemeApi` убираются `addLexemeWithDefinition`, `updateLexemeDefinition`, `deleteLexemeDefinition`. Из `WordCardUseCase` interface убираются `addLexemeDefinition`, `deleteLexemeDefinition`.

- [ ] **Переписка definition callsite'ов на generic** (AGG-6, MIN-9):
  - `WordCardUseCaseImpl` — все вызовы `lexemeApi.addLexemeWithDefinition` / `.updateLexemeDefinition` → `addLexemeWithUserDefinedComponent(wordId, dictionaryId, name="Definition", data)` / `updateComponentValue(id, data)` / `deleteComponentValue(id)`.
  - `WordCardUseCaseImpl.restoreLexeme` (MIN-9) — переписать impl на generic component INSERT (translation built-in + если `definition != null` — user-defined "Definition" lookup в словаре). Сигнатура mate API не меняется.
  - `DatasourceEffectHandler` (wordcard, 2 точки) — переписать вызовы на новые методы UseCase.
  - 2 теста переписать (`DatasourceEffectHandlerTest`, `SpecializedLexemeExtTest` если задевает).

- [ ] **UI блок «Определение»** (AGG-6): добавить `WordCardState.hasDefinitionComponent: Boolean` флаг + загрузка `component_types` словаря при load wordcard (проверка наличия `name="Definition" AND system_key=NULL`). Composable `AddLexemeMeaningRow` / `LexemeMeaningField` скрывают chip definition если флаг false.
- [ ] Mate (wordcard / quiz / dictionaryTab) продолжает использовать `@Deprecated` методы без изменений.
- [ ] **Domain `Lexeme`** — после миграции получает `components: List<ComponentValue>`. Shim-поля `Lexeme.translation: Translation?` / `Lexeme.definition: Definition?` остаются в data class как `@Deprecated`, заполняются маппером `LexemeApiEntity.toDomain()` из `components` через built-in lookup (см. `06.md` § Mapper). Mate продолжает читать через `lexeme.translation?.value` без изменений.
- [ ] **`modules/domain/lexeme`** — value-классы `Translation` / `Definition` остаются как `@Deprecated`. Удаляются после mate refactor (отдельная фича в backlog).
- [ ] **Cascade-remove инварианты:** `canRemoveTranslation()` остаётся как `@Deprecated`; `canRemoveDefinition()` **удалён** (AGG-6); новый generic метод `canRemoveComponent(componentId): Boolean = components.size > 1`. Логика «нельзя оставить пустую лексему» обобщается (условие `>1` потому что проверка ДО удаления; после удаления должен остаться ≥1). // TODO: revisit при добавлении non-meaning built-in (pronunciation / transcription) — может потребоваться фильтр по template.
- [ ] **Атомарность `addLexemeWithBuiltInComponent` (IS479 F1):** реализация обязана выполнять INSERT в `lexemes` + `write_quiz` + первый `component_values` в **одной транзакции** — через `@Transaction` DAO-метод или `db.withTransaction { ... }`. Без этого регрессирует фикс IS479 F1 (битая лексема без перевода/без write_quiz). В `@Deprecated` обёртке `addLexemeWithTranslation` оставить комментарий-ссылку: `// Atomicity: см. addLexemeWithBuiltInComponent (IS479 F1)`.
- [ ] **Тест на rollback атомарности:** симулировать FK-violation (`componentTypeId` несуществующий) при вызове `addLexemeWithBuiltInComponent` → assert что `lexemes` и `write_quiz` не получили новых строк (rollback всей транзакции).
- [ ] **`Lexeme.toUiItem()`** в `modules/screen/dictionaryTab/entity/LexemeUiItem.kt` — **НЕ меняется**. После B4/C2 решения shim-поля `Lexeme.translation` / `.definition` оставлены типов `Translation?` / `Definition?` (value-classes остаются в `modules/domain/lexeme` как `@Deprecated`), поэтому существующий маппинг `translation?.let { TranslationUiEntity(it.value) }` работает без правок. Гэп-4 закрыт через B4 (shim полями) — отдельная правка не нужна.
- [ ] **В `docs/Backlog.md`** добавить новую фичу «Wordcard mate refactor: generic компоненты в Msg / State / Reducer» — когда выпиливать @Deprecated wrappers.

### Рефакторинг кода (после миграции)

- [ ] **Quiz wire (AGG-5 реверс):** `QuizConfig` lookup в `QuizChatUseCaseImpl` / `QuizGameImpl.fetchData`. Quiz session: для каждого `ComponentTypeRef` из `component_refs` config — резолв в `ComponentType` (через `component_types` table), потом подтянуть `component_values` лексем по этому типу. `toQuizItem(quizComponents, ...): QuizItem?` фильтрует лексемы где требуемые компоненты заполнены (graceful skip = null). Definition в квизе работает через `UserDefined("Definition")` lookup (не через старый `BuiltInComponent.DEFINITION`).
- [ ] Search / DictionaryTab — продолжают работать через shim-поля `lexeme.translation` / `lexeme.definition` (значения заполняются маппером). Grep на употребления не требуется.
- [ ] UI WordCard — chip «Определение» скрывается через `state.hasDefinitionComponent` flag (см. UI блок выше).

### Integration tests (MIN-4, MIN-10, M3)

- [ ] **MIN-4 — cascade chain после DROP COLUMN:** в `core-db-impl/androidTest/` migration test:
  - Setup: создать dictionary, word, lexeme (со старым `definition` колонкой v11), мигрировать БД на v12, добавить `component_value` через новый DAO.
  - Action: `DELETE FROM dictionaries WHERE id = X`.
  - Assert: `words`, `lexemes`, `component_values`, `component_types` (с FK на этот dictionary), `quiz_configs` — все удалены каскадом. Counts проверить через `SELECT COUNT(*)` для каждой таблицы.
- [ ] **MIN-10 — direct cascade `component_types → component_values`:** test case:
  - Setup: создать `component_type` (user-defined Definition) + 2 `component_values` ссылающихся на него.
  - Action: `DELETE FROM component_types WHERE id = X`.
  - Assert: `SELECT COUNT(*) FROM component_values WHERE component_type_id = X` → 0.
- [ ] **M3 — interrupted migration restart (idempotency):**
  - Setup: v11 БД с реальными данными (несколько dictionaries, words, lexemes с translation + definition).
  - Action: запустить миграцию Migration_11_12 с искусственным сбоем посередине (инжектировать exception в один из middle-шагов — например шаг 7 «INSERT definition data в component_values»).
  - Assert (post-failure): Room rollback всё → БД остаётся в v11 (`PRAGMA user_version` = 11, `lexemes.translation` / `.definition` колонки на месте, таблицы `component_types` / `component_values` / `quiz_configs` НЕ существуют).
  - Action (restart): перезапустить миграцию с реальным driver (без injection).
  - Assert (post-success): миграция проходит без `UNIQUE constraint` violation; финальное состояние v12 валидно (все таблицы созданы, данные мигрированы, default quiz_configs INSERT'ed).
  - Покрывает scenario: батарея разрядилась во время миграции, OOM на shаге 7, повторный запуск приложения — пользователь не теряет данные и БД не застревает в полусломанном состоянии.

---

## Цели и обоснование

### Что мигрируется куда

| Источник (v11) | Цель (v12) | Тип |
|---|---|---|
| `lexemes.translation` (String?) | `component_values` со ссылкой на **built-in** `system_key="translation"` | built-in global |
| `lexemes.definition` (String?) | `component_values` со ссылкой на **user-defined** «Definition» **per-dictionary** | user-defined per-dictionary |

### Почему такой раздел built-in / user-defined

- **Translation** — universal концепт для словаря (любой словарь «from X to Y» содержит переводы). Built-in оправдан: всегда нужен, защищён от удаления, локализованное имя.
- **Definition** — НЕ universal. Не все словари нуждаются (lexeme = просто пара слов). Логичнее как user-defined, который пользователь сам создаёт или удаляет.

### Trade-offs (осознанные)

| Trade-off | Решение |
|---|---|
| Quiz сейчас использует `definition` | После AGG-5 (реверс): quiz wire в IS481, definition работает через `UserDefined("Definition")` lookup. Existing definitions не теряются в квизе. |
| Имя user-defined «Definition» — на каком языке | Английский (литерал). Изменяемо через UI переименования (когда появится). |
| Алерты пользователю при первом запуске v12 | OK, не блокер. |
| Per-dictionary миграция definition — сложнее | OK, пользователей мало. |
| Definition не защищён от удаления | OK, пользователь может удалить (это user-defined, его выбор). |

---

## SQL миграция (шаги) — с bundled SQLite 3.45+

### 0. Транзакция

Room **автоматически** оборачивает `Migration.migrate(connection)` в транзакцию (см. `RoomOpenHelper.onUpgrade`). Явных `BEGIN/COMMIT` не нужно — это nested transaction которая работает через savepoints SQLite, но излишне и запутывает. Существующие миграции проекта (`Migration_010_to_011.kt`) — без явной транзакции, полагаются на Room.

**Сигнатура миграции** — `override fun migrate(connection: SQLiteConnection)`. Внутри — `connection.execSQL("...")` через extension `androidx.sqlite.execSQL`. Старый `migrate(db: SupportSQLiteDatabase)` под bundled driver не вызывается.

### 1. Создать новые таблицы

CREATE TABLE `component_types`, `component_values` со всеми колонками / FK / индексами (детали — `03_database_design.md` § 5).

### 2. Partial UNIQUE index

```sql
CREATE UNIQUE INDEX index_component_types_global_userdef_name
ON component_types(name)
WHERE dictionary_id IS NULL AND system_key IS NULL;
```

### 3. Seed built-in (только translation)

```sql
INSERT OR IGNORE INTO component_types (system_key, dictionary_id, name, template_key, position)
VALUES ('translation', NULL, NULL, 'text', 0);
```

Definition в built-in **не вставляем** — он становится user-defined per-dictionary.

### 4. Translation: data → component_values

`json_object()` из JSON1-модуля (включён в bundled SQLite) корректно экранирует все спецсимволы:

```sql
INSERT INTO component_values (lexeme_id, component_type_id, value)
SELECT
    l.id,
    (SELECT id FROM component_types WHERE system_key = 'translation'),
    json_object('v', 1, 'text', l.translation)
FROM lexemes l
WHERE l.translation IS NOT NULL;
```

JSON-escape проблема исчезает — `\n`, `\r`, `\t`, `"`, эмодзи и любые control chars экранируются автоматически правильно. Поле `v` (payload schema version) = 1 пишется с самого начала — страховка для будущих изменений формата `ComponentValueData` (см. `06_mapping_design.md` § «Правило эволюции»).

### 5. Definition: создать user-defined типы и мигрировать данные

```sql
-- 5.1: Создать user-defined тип "Definition" для каждого словаря где есть definition
INSERT INTO component_types (system_key, dictionary_id, name, template_key, position)
SELECT DISTINCT NULL, w.dictionary_id, 'Definition', 'text', 10
FROM words w
JOIN lexemes l ON l.word_id = w.id
WHERE l.definition IS NOT NULL;

-- 5.2: Мигрировать definition data со ссылкой на соответствующий user-defined тип
INSERT INTO component_values (lexeme_id, component_type_id, value)
SELECT
    l.id,
    (SELECT ct.id FROM component_types ct
     WHERE ct.dictionary_id = w.dictionary_id AND ct.name = 'Definition' AND ct.system_key IS NULL),
    json_object('v', 1, 'text', l.definition)
FROM lexemes l
JOIN words w ON l.word_id = w.id
WHERE l.definition IS NOT NULL;
```

### 6. Удалить колонки translation/definition (ALTER TABLE DROP COLUMN)

С bundled SQLite 3.45+ — простое:

```sql
ALTER TABLE lexemes DROP COLUMN translation;
ALTER TABLE lexemes DROP COLUMN definition;
```

**Никакого recreate-таблицы.** id'ы сохраняются, FK constraint'ы на lexemes (write_quiz, samples, hints) не задеваются, autoincrement не сбивается. Linear-fast на любом dataset.

### 7. Шаг 6 — Quiz configs

Создание таблицы `quiz_configs` + seed config'ов для существующих словарей. Контекст и принципы — [`07_quiz_strategy.md`](07_quiz_strategy.md). Под bundled SQLite через `connection.execSQL` (extension `androidx.sqlite.execSQL`).

#### 7.1. CREATE TABLE + индексы

```sql
CREATE TABLE quiz_configs (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    dictionary_id INTEGER NOT NULL REFERENCES dictionaries(id) ON DELETE CASCADE,
    quiz_mode TEXT NOT NULL,
    component_refs TEXT NOT NULL
);
CREATE INDEX index_quiz_configs_dictionary_id
    ON quiz_configs(dictionary_id);
CREATE UNIQUE INDEX index_quiz_configs_dictionary_id_quiz_mode
    ON quiz_configs(dictionary_id, quiz_mode);
```

#### 7.2. INSERT default config для ВСЕХ словарей

```sql
-- БЕЗ фильтра WHERE — даже пустой dictionary получает [BuiltIn(TRANSLATION)] (F1 closed).
INSERT INTO quiz_configs (dictionary_id, quiz_mode, component_refs)
SELECT id, 'write', json_array(
    json_object('type', 'builtin', 'key', 'translation')
)
FROM dictionaries;
```

#### 7.3. UPDATE — добавить UserDefined("Definition") где есть definition data

```sql
UPDATE quiz_configs
SET component_refs = json_insert(
    component_refs,
    '$[#]',
    json_object('type', 'user', 'name', 'Definition')
)
WHERE quiz_mode = 'write'
  AND dictionary_id IN (
    SELECT DISTINCT w.dictionary_id
    FROM words w JOIN lexemes l ON l.word_id = w.id
    WHERE l.definition IS NOT NULL
  );
```

После UPDATE для словаря с definition `component_refs` будет:
```json
[
    {"type": "builtin", "key": "translation"},
    {"type": "user", "name": "Definition"}
]
```

Порядок (translation, потом definition) — это **семантика приоритета** в quiz lookup (см. `07.md` § Order semantics).

Альтернатива (если SQLite `json_insert($[#]` поведёт себя иначе): Kotlin migration code с прямыми SELECT/UPDATE — для каждого словаря с definition прочитать `component_refs`, добавить элемент в JSON через `JSONArray.put(...)`, UPDATE. Решение по фактической реализации — на этапе implement.

### 8. Проверка имён индексов

Имена индексов в новой таблице — проверить через `12.json` snapshot после генерации (Room автогенерирует имена из `@Entity`). Если в `connection.execSQL` имена не совпадают — Room валидация упадёт. Этот шаг — checklist'ом перед merge, не SQL.

---

## Bundled SQLite — почему

| Аспект | Системный SQLite (minSdk 23 → 3.8.10) | Bundled SQLite (3.45+) |
|---|---|---|
| ALTER TABLE DROP COLUMN | ❌ (с 3.35+) | ✅ |
| JSON1 module (json_object, json_extract) | ⚠ часто включён, но не гарантировано | ✅ всегда |
| Partial / expression indexes | ✅ (3.8+) | ✅ |
| Версия на всех устройствах | разная (3.8 — 3.32) | одна (3.45+) |
| Размер APK | базовый | +~1MB per abi |
| Тестирование | разное на разных API | детерминированное |

Миграция без bundled — recreate-таблица lexemes + JSON-escape проблема + риск разной SQLite-семантики. С bundled — обе проблемы исчезают, +1MB APK.

---

## Edge cases

1. **Lexeme без translation и definition** — нет, не возможно (validation). Если есть — оставляем без component_values.
2. **Lexeme только с translation** — стандартный кейс, один component_value (translation).
3. **Lexeme только с definition** — один component_value на user-defined type «Definition» этого словаря.
4. **Словарь без definition** — user-defined «Definition» **не создаётся** для этого словаря.
5. **Definition с спецсимволами** — обрабатывается автоматически через `json_object()` (JSON1 в bundled SQLite). Эмодзи, переносы строк, кавычки, control chars — всё корректно эскейпится.
6. **Огромный dataset (10k+ lexemes)** — миграция в одной транзакции; время выполнения может быть несколько секунд, нужно тестировать на realistic dataset.
7. **Повторный запуск миграции** — теоретический сценарий; миграционные тесты должны быть строго однопроходные (v11 БД → migrate → v12 проверка). Не делаем defensive `OR IGNORE` для шага 5.1 — это маскировало бы неправильные тесты.
8. **Orphan lexeme** (translation IS NULL AND definition IS NULL) — миграция пропускает (WHERE IS NOT NULL фильтрует). Lexeme остаётся в БД без component_values. Тест: count(orphan lexemes) до == count(lexemes без component_values после).
9. **Огромная definition (>64KB)** — `json_object()` корректно эскейпит, payload size 2x. SQLite TEXT лимит `SQLITE_MAX_LENGTH` (default 1GB) не достижим.

---

## Влияние на код (после миграции)

### Quiz

Сейчас: использует `lexeme.translation` и `lexeme.definition`.

После: только `lexeme.translation` через `Lexeme.builtIn(BuiltInComponent.TRANSLATION)`. **Definition в квизе отключён** до отдельной фичи «компоненты в квизе» (там пользователь явно отметит какие компоненты использовать).

### DictionaryTab

Сейчас: отображает translation как «основное» и definition как «вспомогательное».

После: translation через built-in. Definition становится одним из user-defined типов словаря — отображается как обычный компонент в списке компонентов лексемы (без специального handling).

### Search

**Не затронут.** Поиск (`WordDao.searchTerms` / `searchTermsPaging` / `searchTermsManual`) фильтрует **только по `words.value LIKE :pattern`** — не обращается к translation / definition. Миграция компонентов на search не влияет.

Расширение поиска на text-компоненты (translation / definition / user-defined text-типы) — опциональная отдельная фича в будущем.

### write_quiz

Schema не меняется — колонка `write_quiz.lexeme_id` остаётся. Runtime — см. [`07_quiz_strategy.md`](07_quiz_strategy.md) (решение TBD по обработке definition-only лексем).

### Другие места

Полный grep `lexeme.definition` по проекту перед реализацией — найти все callsite и переписать (или явно отключить).

---

## Тестовые сценарии (миграционный тест)

### Базовые

1. Lexeme с translation и definition в словаре 1 → 2 component_values, user-defined «Definition» создан в словаре 1.
2. Lexeme только с translation → 1 component_value на built-in translation.
3. Lexeme только с definition → 1 component_value на user-defined «Definition» этого словаря, user-defined тип создан.
4. Словарь без definition → user-defined «Definition» **не существует** в `component_types` для этого словаря.

### Спецсимволы (через `json_object()`)

5. Translation с `"` внутри (`he said "hi"`) — корректно сохранено.
6. Definition многострочная (с `\n`) — корректно сохранено.
7. Definition с эмодзи (😀) — корректно сохранено.
8. Definition >64KB — корректно сохранено.

### Согласованность

9. После миграции: `getBySystemKey("translation")` возвращает row.
10. После миграции: `getTypesForDictionary(X)` возвращает translation (global) + «Definition» (per-dictionary, если есть данные).
11. После миграции: `lexemes` не содержит колонок translation/definition.
12. **Count match definition:** для каждого словаря X — `count(component_values JOIN component_types WHERE ct.name='Definition' AND ct.dictionary_id=X)` == `count(lexemes JOIN words WHERE w.dictionary_id=X AND l.definition IS NOT NULL)`.
13. **Count match translation:** `count(component_values WHERE component_type_id=built-in translation id)` == `count(lexemes WHERE translation IS NOT NULL)` (до миграции).
14. **Orphan lexemes:** count(lexemes без translation и definition до) == count(lexemes без component_values после).
15. **Partial UNIQUE index существует:** через `PRAGMA index_list('component_types')` + `PRAGMA index_info(...)`.

### Quiz configs (шаг 6)

16. **Default config (empty deps):** dictionary без лексем → row `(dictionary_id, 'write', '[{"type":"builtin","key":"translation"}]')` всё равно записан (F1 closed).
17. **Default config (translation-only):** dictionary с лексемами только-translation → config `[BuiltIn(TRANSLATION)]`.
18. **Definition-present:** dictionary где хотя бы одна лексема с definition → config `[BuiltIn(TRANSLATION), UserDefined("Definition")]` (порядок: translation первый).
19. **Definition-only dictionary:** все лексемы только с definition (translation IS NULL) → config всё равно `[BuiltIn(TRANSLATION), UserDefined("Definition")]`. Новая translation-лексема, добавленная после миграции, находит translation OK.
20. **Mixed dictionary:** часть лексем translation-only, часть с definition → config `[BuiltIn(TRANSLATION), UserDefined("Definition")]`.
21. **CASCADE on dictionary delete:** удаление dictionary → все его rows в `quiz_configs` удалены автоматически (FK CASCADE).
22. **UNIQUE constraint:** повторный INSERT для пары `(dictionary_id, 'write')` падает с UNIQUE violation.

### Edge

23. Пустой БД (нет lexemes, нет dictionaries) — миграция проходит без INSERT'ов в component_values и quiz_configs, built-in translation создан.
24. БД с 10000+ lexemes — миграция завершается за разумное время (< 5 секунд).

---

## Open questions

- [ ] Финальное решение по JSON-escape (Вариант A / B / C).
- [ ] Position для translation built-in (0) vs «Definition» user-defined (10). Норм или другой подход (например все «legacy» definitions с position=0 для visual continuity)?
- [ ] Имя user-defined «Definition» — английское «Definition» (текущий план) или ключ ресурса с fallback на текущую локаль?
- [ ] Что если в БД уже есть user-defined тип с именем «Definition» в каком-то словаре (`UNIQUE(dictionary_id, name)` сработает — ABORT)? Миграция упадёт. Защита — INSERT OR IGNORE с проверкой что данные definition потом попадут на правильный тип (existing или новый). На старте — нет user-defined, риск 0.
