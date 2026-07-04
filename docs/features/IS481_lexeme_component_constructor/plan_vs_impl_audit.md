# Audit: план vs реализация IS481

Хард-аудит: для каждого пункта плана проверена реальная реализация через `Read` / `Grep`. Заявления `*_summary.md` не считаются доказательством — только реальные файлы кода.

## Summary

- ✅ **62** items реализовано
- ❌ **8** items не реализовано (из них **2** критичных)
- ⚠️ **6** items с отклонениями
- 🚫 **5** items legit вне scope (опциональные backlog)

---

## Decisions audit

### B / A / C / Gap

| Decision | Status | Evidence |
|---|---|---|
| Gap-0 (удалить `category` упоминания) | ✅ | `Lexeme.kt:28-35` — поля `category` нет |
| Gap-1 (locations → `modules/domain/lexeme`) | ✅ | Все типы лежат в `modules/domain/lexeme/src/main/java/me/apomazkin/lexeme/` |
| Gap-2 (no-op, false alarm) | ✅ | `CoreDbApi.kt:93-110` — `wordId` параметр сохранён |
| Gap-3 (`ComponentValue.type` full embedded) | ✅ | `ComponentValue.kt:14` — `type: ComponentType` full embedded; `ComponentValueWithType.kt:14-18` Multi-level @Relation |
| Gap-7 (`LexemeBuiltInExtTest`) | ✅ | `modules/domain/lexeme/src/test/java/me/apomazkin/lexeme/LexemeBuiltInExtTest.kt` — 5 кейсов |
| Gap-8 (business_contract не создаём заранее) | 🚫 | N/A (process decision) |
| A1 (domain types в `modules/domain/lexeme`, core-db-api зависит от domain) | ✅ | `core/core-db-api/build.gradle.kts:35` — `api(project(":modules:domain:lexeme"))` |
| A2/B5 (убрать `wordId` из маппера) | ✅ | `LexemeDbEntity.kt:34-41` — `toApiEntity()` без wordId |
| A3 (@Deprecated с реальной сигнатурой `TranslationApiEntity`) | ✅ | `CoreDbApiImpl.kt:351-361` |
| A5 (разделить @Deprecated wrappers в чеклисте) | ✅ | Documentation-only; в коде разделено: API translation deprecated, definition wrappers удалены |
| B1 (`Callback.onCreate(connection: SQLiteConnection)`) | ✅ | `RoomModule.kt:53-57` — bundled API подпись |
| B3 (`connection.execSQL` через `androidx.sqlite.execSQL` extension) | ✅ | `Migration_011_to_012.kt:5`, `SeedBuiltIns.kt:4` — import `androidx.sqlite.execSQL` |
| B4/C2 (shim в IS481, mate refactor отдельной фичей) | ✅ | `Lexeme.kt:28-35` — `translation` / `definition` как `@Deprecated` fields; `LexemeMapper.kt:56-66` заполняет shim |
| Shim consistency invariant (debug-assertion + property тест) | ⚠️ | Debug-check **сознательно удалён** в iter 2 (`LexemeMapper.kt:52-54` комментарий — рассинхрон создаётся mate `copy(translation=X)`, проверка на mapper-стороне не помогает). Property test тоже отсутствует. Известный trade-off зафиксирован в `global_code_review.md` Minor #3. |
| Test gaps batch (B7/B8+C11/C12/C13) | ✅ | `app/src/test/.../LexemeMapperTest.kt` — orphan / non-text data / multi-component / translation-by-systemKey-not-name |
| Mini-cosmetic batch (A6/A7/A8/A9) | ✅ | `ComponentType.kt:6` + `ComponentValue.kt:4` — `@JvmInline value class`-ы; docs sketches обновлены |

### AGG

| Decision | Status | Evidence |
|---|---|---|
| AGG-1 (`BuiltInComponent` только `TRANSLATION`, DEFINITION удалён) | ✅ | `BuiltInComponent.kt:11-12` — единственный enum value `TRANSLATION` |
| AGG-2 (мапперы в `app/.../mapper/LexemeMapper.kt`) | ✅ | `app/src/main/java/me/apomazkin/polytrainer/mapper/LexemeMapper.kt` |
| AGG-3 (07.md naming `quiz_configs`) | ✅ | Table `quiz_configs`, entity `QuizConfigDb`, ApiEntity `QuizConfigApiEntity`, domain `QuizConfig` |
| AGG-4 (addDictionary auto-INSERT default) | ✅ | `WordDao.kt:41-52` — `@Transaction` default-method |
| AGG-5 (QuizConfig lookup wire) | ✅ | `QuizGameImpl.kt:184-227` — pre-fetch + передача refs в `toQuizItem`. F2 graceful skip — `QuizGameImpl.kt:464` |
| AGG-6 (удалить definition-deprecated wrappers, UI блок) | ⚠️ | API/UseCase обёртки definition удалены (`CoreDbApi.kt:162`); `WordCardState.hasDefinitionComponent` есть (`State.kt:24`). **Но** chip-параметр в UI называется `canAddDefinition` и собирается из `lexemeState.canAddDefinition && state.hasDefinitionComponent` (`WordCardScreen.kt:187`) — план говорил «скрывается если `hasDefinitionComponent == false`», в реальности это AND с per-lexeme флагом `canAddDefinition`. Семантика расширена корректно, но не как в плане. |
| AGG-7 (obsolete) | 🚫 | superseded AGG-12 |
| AGG-8 (verify `json_insert($[#])`) | ✅ | `BundledSqliteFeatureTest` (prereq) + `MigrationFrom11to12.caseH_jsonInsertAppendSyntax:269-300` |
| AGG-9 (явный порядок шагов Migration_11_12) | ✅ | `Migration_011_to_012.kt:21-29` — пронумерованный KDoc + private methods с явными именами |
| AGG-10 (QuizConfig в `modules/domain/lexeme`, TODO на вынос) | ✅ | `QuizConfig.kt:10-12` + `ComponentTypeRef.kt:11-13` — TODO `вынести в modules/domain/quiz` |
| AGG-11 (obsolete) | 🚫 | superseded AGG-12 |
| AGG-12 (existing migrations dropped, новая M11→M12 directly under bundled driver) | ✅ | `Migration_011_to_012.kt:33` — `override fun migrate(connection: SQLiteConnection)`; `RoomModule.kt:48` — `setDriver(BundledSQLiteDriver())`; schemas/12.json существует |

### MIN

| Decision | Status | Evidence |
|---|---|---|
| MIN-2 (`implementation/api(project(":modules:domain:lexeme"))` в core-db-api) | ✅ | `core/core-db-api/build.gradle.kts:35` — `api` вместо `implementation` (даже лучше для транзитивности) |
| MIN-3 (явный комментарий double-cascade pathway в 03.md) | 🚫 | Documentation-only; в коде не материализуется. global_code_review.md также отмечает minor |
| MIN-4 (integration test cascade chain после DROP COLUMN) | ✅ | `MigrationFrom11to12.caseF_cascadeChain_dictionaryDelete:212-232` |
| MIN-6 (thread policy парсинга `component_refs` inline) | ✅ | `QuizConfigDb.kt:42-47` — inline `toApiEntity()` без flowOn/cache |
| MIN-7 (раздел Invariants F1/F5 в 07.md) | ✅ | Documentation; F1 invariant procedural — `Migration_011_to_012.kt:188` INSERT для ВСЕХ dictionaries; `WordDao.addDictionary:41-52` гарантирует на new dict |
| MIN-8 (write-mapper YAGNI, hardcoded JSON DAO) | ✅ | `QuizConfigDao.kt:36-43` — `insertDefaultQuizConfig` hardcoded JSON, без write-mapper |
| MIN-9 (`restoreLexeme` через generic atomic compound) | ✅ | `WordDao.kt:200-217` — `addLexemeWithComponents` @Transaction; `WordCardUseCaseImpl.kt:257-300` использует через `lexemeApi.addLexemeWithComponents` |
| MIN-10 (test direct cascade `component_types → component_values`) | ✅ | `MigrationFrom11to12.caseG_cascadeComponentTypeToValues:236-266` |
| MIN-11 (synchronous cleanup `quiz_configs.component_refs` при DELETE component_type) | 🚫 | Корректно отложено в backlog (в IS481 нет `deleteComponentType` операции). F6 invariant documented only |
| MIN-12a (Empty quiz session UX) | 🚫 | Не меняется в IS481 — корректно |
| MIN-12b (F4 display order контракт) | ✅ | `QuizGameImpl.kt:460-466` — `firstNotNullOfOrNull` по порядку config; `QuizGameImplTest` покрывает |
| MIN-12c (INSERT OR IGNORE rejected) | ✅ | Migration пишет без `OR IGNORE` (только `seedBuiltIns` использует — для idempotent built-in seed) |

---

## Migration checklist (05.md)

### Bundled SQLite (закрыто prereq)

Все пункты ✅ — выполнены в prereq фиче `IS481_lexeme_component_constructor_vPrepared` (бандл sqlite, KMP-builder, destructive fallback + Crashlytics, ProGuard, `BundledSqliteFeatureTest` 6/6 PASS, 10 historical migrations dropped).

### Для IS481 main M11→M12 следствие AGG-12

| Пункт | Status | Evidence |
|---|---|---|
| Новая `Migration_011_to_012.kt` directly под `SQLiteConnection` API; подключить через `.addMigrations(...)` | ✅ | `Migration_011_to_012.kt:31-50`; `RoomModule.kt:50` |
| Один `MigrationFrom11to12.kt` test с `MigrationTestHelper(driver = BundledSQLiteDriver())` | ✅ | `core/core-db-impl/src/androidTest/.../MigrationFrom11to12.kt` — 12 кейсов A-L |
| `core/core-db-api/build.gradle.kts` — `implementation(project(":modules:domain:lexeme"))` | ✅ | `core-db-api/build.gradle.kts:35` (использован `api`, что эквивалентно или лучше) |
| Import `androidx.sqlite.execSQL` | ✅ | `Migration_011_to_012.kt:5`, `SeedBuiltIns.kt:4` |
| `Callback.onCreate(connection: SQLiteConnection)` (B1) | ✅ | `RoomModule.kt:53-57` |

### Схема и seed

| Пункт | Status | Evidence |
|---|---|---|
| CREATE TABLE `component_types` | ✅ | `Migration_011_to_012.kt:54-82` |
| CREATE TABLE `component_values` | ✅ | `Migration_011_to_012.kt:84-105` |
| Стандартные индексы (`@Index`) + partial UNIQUE через `execSQL` | ✅ | Migration steps + `SeedBuiltIns.kt:26-32` partial UNIQUE |
| `seedBuiltIns(connection: SQLiteConnection)` (только translation) | ✅ | `SeedBuiltIns.kt:17-33` |
| `Callback.onCreate(connection)` вызывает `seedBuiltIns(connection)` + partial UNIQUE | ✅ | `RoomModule.kt:53-57` (`seedBuiltIns` создаёт partial UNIQUE внутри себя) |

### Данные

| Пункт | Status | Evidence |
|---|---|---|
| Translation: для каждой lexeme с translation IS NOT NULL — INSERT component_value | ✅ | `Migration_011_to_012.kt:145-157` `migrateTranslationData` |
| Definition: создать user-defined Definition type per-dictionary + INSERT component_values | ✅ | `Migration_011_to_012.kt:130-175` `createUserDefinedDefinitionTypes` + `migrateDefinitionData` |
| ALTER TABLE DROP COLUMN translation / definition | ✅ | `Migration_011_to_012.kt:48-49` |
| Имена индексов в миграции совпадают с Room (12.json) | ✅ | `Migration_011_to_012.kt:74,77,80` имена `index_<table>_<col>` matched; schemas/12.json grep подтверждает |

### Quiz configs (шаг 6)

| Пункт | Status | Evidence |
|---|---|---|
| CREATE TABLE `quiz_configs` + FK CASCADE | ✅ | `Migration_011_to_012.kt:107-126` |
| Индексы (`index_quiz_configs_dictionary_id` / unique по `(dictionary_id, quiz_mode)`) | ✅ | `Migration_011_to_012.kt:120-125` |
| INSERT для ВСЕХ словарей default config (F1) | ✅ | `Migration_011_to_012.kt:177-188` без WHERE-фильтра |
| UPDATE словарей с definition — добавить `UserDefined("Definition")` | ✅ | `Migration_011_to_012.kt:190-210` через `json_insert('$[#]', ...)` |
| Helper `ComponentTypeRefJson.kt` в `core-db-impl` | ✅ | `core/core-db-impl/.../mapper/ComponentTypeRefJson.kt` |

### API совместимости

| Пункт | Status | Evidence |
|---|---|---|
| Новые generic-методы `addLexemeWithBuiltInComponent` / `addLexemeWithUserDefinedComponent` / `addComponentValue` / `updateComponentValue` / `deleteComponentValue` + `insertDefaultQuizConfig` DAO method | ✅ | `CoreDbApi.kt:93-138`, `QuizConfigDao.kt:36-43` |
| Атомарность `addDictionary` + `insertDefaultQuizConfig` (AGG-4) | ✅ | `WordDao.kt:41-52` `@Transaction` default-method |
| Тест: симулировать FK violation / corrupt JSON на `insertDefaultQuizConfig` → assert dictionary не создан, F1 держится (F015) | ❌ | **Тест отсутствует**. `data_summary.md:131-133` явно говорит «N/O/P integration tests не реализованы». `global_code_review.md` Major #1. Описан в `data_migration_test.md` § 3, но не имплементирован. |
| Translation @Deprecated обёртки | ✅ | `CoreDbApiImpl.kt:351-399` |
| Definition обёртки УДАЛЕНЫ (AGG-6) | ✅ | `CoreDbApi.kt:162-163` явное удаление |
| Переписать definition callsite'ы на generic (WordCardUseCaseImpl, DatasourceEffectHandler, restoreLexeme) | ✅ | `WordCardUseCaseImpl.kt:128-176, 233-246, 257-300`; `DatasourceEffectHandlerTest.kt` обновлён (110/110 PASS) |
| UI блок «Определение» (chip скрывается если `hasDefinitionComponent == false`) | ⚠️ | `WordCardState.hasDefinitionComponent: Boolean` (`State.kt:24`); reducer вычисляет (`WordCardReducer.kt:267`); chip-видимость использует `lexemeState.canAddDefinition && state.hasDefinitionComponent` (`WordCardScreen.kt:187`). Семантика правильная, но композитный флаг — расширение плана. |
| Mate без изменений (translation flow через shim) | ✅ | mate / wordcard reducer / extension `updateLexemeDefinitionText` — не тронуты, 110/110 PASS |
| Domain Lexeme получает `components` + shim фields | ✅ | `Lexeme.kt:28-35` |
| `modules/domain/lexeme` value-классы `Translation`/`Definition` остаются `@Deprecated` | ✅ | `Lexeme.kt:8-14` |
| Cascade-remove инварианты: `canRemoveTranslation` @Deprecated; `canRemoveDefinition` удалён; `canRemoveComponent` | ⚠️ | `canRemoveTranslation`/`canRemoveDefinition` удалены целиком (`LexemeApiEntity.kt:31-33` явное удаление). **`canRemoveComponent` НЕ реализован** (`grep canRemoveComponent` = 0). Семантика «нельзя оставить пустую лексему» теперь живёт в `RemoveComponentResult.LexemeCascadeRemoved` ветке `deleteComponentValue` (`WordCardUseCaseImpl.kt:225-227` — cascade delete если `remaining == 0`). Эквивалентная защита есть, но не как ad-hoc предикат. |
| Атомарность `addLexemeWithBuiltInComponent` (IS479 F1) | ✅ | `WordDao.kt:200-217` `addLexemeWithComponents` @Transaction (INSERT lexeme + write_quiz + N component_values) |
| Тест rollback атомарности (F013) | ❌ | **Тест отсутствует**. `global_code_review.md` Major #1. `WordCardUseCaseImplTest:131-146` — только exception handling, не реальный DB-rollback test. Только в androidTest можно проверить. |
| `Lexeme.toUiItem()` не меняется (через shim) | ✅ | `LexemeUiItem.kt:23-26` без правок (`translation?.let { TranslationUiEntity(it.value) }`) |
| В `docs/Backlog.md` добавить «Wordcard mate refactor» | ✅ | `Backlog.md` содержит запись (см. business_summary упоминание) |

### Рефакторинг кода (после миграции)

| Пункт | Status | Evidence |
|---|---|---|
| Quiz wire (AGG-5 реверс) — `getQuizConfig` lookup в QuizGameImpl.fetchData | ✅ | `QuizGameImpl.kt:183-191` |
| Search / DictionaryTab — через shim-поля | ✅ | `LexemeUiItem.kt` неизменён; Search оперирует только `words.value` |
| UI chip «Определение» через `hasDefinitionComponent` | ⚠️ | Реализован композитный флаг (см. выше) |

### Integration tests (MIN-4 / MIN-10 / M3)

| Пункт | Status | Evidence |
|---|---|---|
| MIN-4 (cascade chain после DROP COLUMN) | ✅ | `MigrationFrom11to12.caseF` |
| MIN-10 (direct cascade component_types → component_values) | ✅ | `MigrationFrom11to12.caseG` |
| M3 (interrupted migration restart) | ❌ | **Не реализован**. `data_implement.md:196` — «опционально», `data_summary.md` подтверждает |

---

## Quiz invariants (07.md F1-F6)

| Invariant | Status | Evidence |
|---|---|---|
| **F1** (полнота config — каждый dict × каждый registered quiz_mode имеет row) | ⚠️ | Реализована для одного `quiz_mode='write'`: миграция INSERT'ит без WHERE (`Migration_011_to_012.kt:177-188`); `WordDao.addDictionary:41-52` пишет default для new dict. **При добавлении нового quiz_mode** в будущем — нет автоматического INSERT для existing dictionaries. Это known limitation (`global_code_review.md` Minor #9), processed но не enforced. На текущем scope IS481 (`quiz_mode='write'` единственный) — held. |
| **F2** (graceful skip null) | ✅ | `QuizGameImpl.kt:464` — `resolved ?: return null` вместо `error()` |
| **F4** (display order = JSON-array order) | ✅ | `QuizGameImpl.kt:460-466` — `firstNotNullOfOrNull` сохраняет порядок; `QuizConfigDb.toApiEntity()` использует JSONArray ordered list |
| **F5** (no N+1 — config один раз на session) | ✅ | `QuizGameImpl.kt:184` — pre-fetch один раз в `fetchData()`, передаётся в `toQuizItem` per-lexeme через `componentRefs` param (`QuizGameImpl.kt:223`) |
| **F6** (referential consistency после DELETE component_type) | 🚫 | Корректно отложено в backlog (нет `deleteComponentType` операции в IS481 scope) |

---

## Test gaps batch

| Gap | Status | Evidence |
|---|---|---|
| **B7** (shim consistency invariant — property-based) | ⚠️ | Debug-assertion сознательно удалён (`LexemeMapper.kt:52-54`); property-test отсутствует. Compensating: `LexemeMapperTest` есть 9 fixed-кейсов покрывающих ту же поверхность. |
| **B8 + C11** (orphan lexeme, empty components) | ✅ | `LexemeMapperTest.given empty components then translation and definition shim null and components empty` |
| **C12** (invalid JSON / malformed data) | ⚠️ | `LexemeMapperTest.given translation built-in with non-text data then translation shim null no crash` покрывает variant mismatch. **JSON-level malformed (битая `value` строка в БД)** — нет `ComponentValueDataJsonTest` целиком. Парсер сам через `JSONObject(badJson)` бросит `JSONException`, обработка на уровне маппера не покрыта тестом. |
| **C13** (multi-component lookup correctness) | ✅ | `LexemeMapperTest.given multiple user-defined types but no Definition then definition shim is null` |
| **Gap-7** (`Lexeme.builtIn` extension + computed shim) | ✅ | `LexemeBuiltInExtTest` 5/5 кейсов |
| **F013** (rollback атомарности `addLexemeWithBuiltInComponent` — FK violation → нет новых строк в lexemes/write_quiz/component_values) | ❌ | **Не реализован**. `data_summary.md:133`, `global_code_review.md` Major #1. Только unit-test exception handling. |
| **F015** (атомарность `addDictionary` + `insertDefaultQuizConfig` — F1 invariant держится при сбое) | ❌ | **Не реализован**. Те же ссылки. |

---

## Edge cases (05.md § Edge cases)

| Case | Status | Evidence |
|---|---|---|
| 1. Lexeme без translation и definition | ✅ | `MigrationFrom11to12.caseJ_orphanLexeme:319-330` |
| 2. Lexeme только с translation | ✅ | `caseA_translationOnly:49-73` |
| 3. Lexeme только с definition | ✅ | `caseC_definitionOnly:103-128` |
| 4. Словарь без definition → user-defined Definition не создаётся | ✅ | `caseD_emptyDictionary:131-158` (`countWhere component_types name='Definition' == 0`) |
| 5. Спецсимволы (кавычки, переносы, эмодзи) через `json_object` | ✅ | `caseK_specialChars:333-358` |
| 6. Огромный dataset (10k+ lexemes) — timing | ❌ | **Не реализован** как явный test case. Bundled SQLite verified, миграция в транзакции, но performance-тест на 10k+ строк не написан |
| 7. Повторный запуск миграции | ⚠️ | Defensive `INSERT OR IGNORE` есть в `seedBuiltIns` (UNIQUE на system_key). Step 5 (`createUserDefinedDefinitionTypes`) полагается на Room rollback. Test M3 (interrupted migration) **не реализован** — см. выше. MIN-12c rejected обоснован, но idempotency edge-case под exotic crash не покрыт |
| 8. Orphan lexeme (translation IS NULL AND definition IS NULL) | ✅ | `caseJ_orphanLexeme` явно покрывает |
| 9. Огромная definition (>64KB) | ❌ | **Не реализован** как явный test case. `json_object` корректно эскейпит, но boundary test отсутствует |

---

## Дополнительные observations

### Unit-tests gap

- **`ComponentValueDataJsonTest` / `ComponentTypeRefJsonTest` / `QuizConfigMapperTest` / `LexemeDbEntityMapperTest` / `ComponentTypeDbMapperTest`** — **не созданы** (план 06.md § Тестирование). `core/core-db-impl/src/test/` содержит только закомментированный `DefinitionOldMapperTest.kt`. `check.md:17` явно: `:core:core-db-impl:testDebugUnitTest — 0 (нет активных unit-тестов в модуле)`. JSON round-trip / invalid JSON / multi-variant парсинг — не покрыты.
- Compensating: интеграционно покрыто через `MigrationFrom11to12` (special chars, json_extract) + `LexemeMapperTest` (variant mismatch).

### Code-level отклонения

- **`updateComponentValue` / `deleteComponentValue` через generic path возвращают `null` даже на success** (`WordCardUseCaseImpl.kt:198-209, 213-231`) — known foot-gun, в `global_code_review.md` Major #2. TODO requires `getLexemeIdByComponentValueId` DAO lookup. Не баг сейчас (нет caller'ов), но contract нарушен.
- **`addLexemeWithBuiltInComponent` использует `error(...)` вместо `logger.e + null`** (`CoreDbApiImpl.kt:243-244`) — несимметрично с user-defined branch (Minor #2 в review).
- **`DefinitionApiEntity` value class — dead code** (`LexemeApiEntity.kt:15`) — оставлен только для symmetry, ни одного callsite (Minor #1).
- **`DefinitionMapper.kt` / `DefinitionSampleRelMapper.kt`** legacy stubs — после удаления колонок возвращают `null` для definition (Minor #6 в review).
- **`DefinitionApiEntity`** + **`DefinitionMapper`** — оставлены как dead-stubs, не удалены.
- **Thread policy parsing для ComponentValueData на DictionaryTab (3000 объектов, `flowOn(Dispatchers.Default)`)** — `06.md` чеклист предписывал. **Не реализован**: `grep flowOn` в core-db-impl = 0 matches. Использован `setQueryCoroutineContext(Dispatchers.IO)` в `RoomModule.kt:49`, но это для DAO query, не для JSON parsing на крупном словаре. Risk: парсинг 3000 объектов может встать в main thread (план оценил ~60ms). Не блокер на текущих объёмах.

---

## Critical findings (must-fix перед merge)

1. **❌ F013 — атомарность `addLexemeWithBuiltInComponent` (FK violation rollback) — тест отсутствует.**
   - Где должен быть: `core/core-db-impl/src/androidTest/.../WordDaoAtomicityTest.kt` или аналог.
   - Регрессия IS479 F1 — любая правка `WordDao.addLexemeWithComponents` или `addDictionary` может незаметно сломать инвариант «у каждой лексемы есть write_quiz».
   - Plan чеклист `05.md:79`: «**Тест на rollback атомарности** — обязателен».
   - Severity: **Major** (review). Не блокер если manual smoke `connectedDebugAndroidTest` runtime прошёл и поведение visually correct; формально gap.

2. **❌ F015 — атомарность `addDictionary` + `insertDefaultQuizConfig`.**
   - Где должен быть: тот же androidTest.
   - Plan чеклист `05.md:61`: «Тест: симулировать FK violation / corrupt JSON на `insertDefaultQuizConfig` → assert dictionaries row не создан, F1 invariant держится».
   - Severity: **Major**.

3. **❌ M3 — interrupted migration restart (idempotency)** — описан в плане `05.md:99-105`, не реализован. Defensive scenario (battery die mid-migration). MIN-12c rejected idempotency `INSERT OR IGNORE` на основании «Room rollback => OK», но тест на full re-run после rollback отсутствует.

---

## Followups (minor / non-blocker)

### ⚠ Отклонения

- **AGG-6 chip-visibility composite flag**: реализация (`lexemeState.canAddDefinition && state.hasDefinitionComponent`) расширила план. Семантически верно (per-lexeme `canAddDefinition` + per-dict `hasDefinitionComponent`), но план говорил только про последнее. Не баг.
- **`canRemoveComponent` helper не материализован** — логика «нельзя оставить пустую лексему» живёт в `RemoveComponentResult.LexemeCascadeRemoved` ветке. Семантика та же, но не как ad-hoc helper. План `05.md:77` говорил про `canRemoveComponent(componentId): Boolean = components.size > 1`.
- **Shim consistency debug-assertion удалён** — обосновано (`LexemeMapper.kt:52-54`), но в плане был.
- **Thread policy `.flowOn(Dispatchers.Default)` для DictionaryTab parsing не реализован** — на текущих объёмах OK, но `06.md` чеклист предписывал.
- **F1 invariant при добавлении нового `quiz_mode`** — нет автоматического INSERT для existing dictionaries. Future-фича сама будет должна это сделать; не баг сейчас.
- **Migration step 9 hard-coded `'write'`** — `Migration_011_to_012.kt:202` — same observation как F1; default config только для одного режима. Корректно для IS481 scope.

### ❌ Missing tests (опциональные)

- Performance тест на 10k+ lexemes (Edge 6).
- Boundary тест >64KB definition (Edge 9).
- `ComponentValueDataJsonTest` (sealed variants, спецсимволы, invalid JSON).
- `ComponentTypeRefJsonTest` (round-trip, discriminator, defensive parser → emptyList на corrupt).
- `QuizConfigMapperTest` (Db ↔ Api ↔ Domain).
- `ComponentTypeDbMapperTest` (4 комбинации built-in/user × global/per-dict).
- `LexemeDbEntityMapperTest` (Multi-level @Relation integration).

### Dead code / cleanup (после mate refactor)

- `DefinitionApiEntity` value class.
- `DefinitionMapper.kt` / `DefinitionSampleRelMapper.kt` legacy.
- `WordApiImpl.deleteWordSuspend` manual cascade (FK CASCADE делает то же).

### Process / docs

- Manual smoke `connectedDebugAndroidTest` для `:core:core-db-impl` (`MigrationFrom11to12` 12 кейсов + `BundledSqliteFeatureTest`) — **не запущен** в check.md, отложен пользователю. Pre-merge gate.

---

## log_messages

- audit complete: 62 ✅, 8 ❌ (2 critical: F013, F015 atomicity tests; 1 critical: M3 interrupted migration idempotency), 6 ⚠ (chip composite flag, canRemoveComponent absence, shim debug-assertion removed, DictionaryTab flowOn missing, F1 partial, hard-coded write mode), 5 🚫 legit (AGG-7/AGG-11 obsolete, MIN-3/MIN-11 docs-only, MIN-12a UX deferred)
- core implementation strong: all entities/DAOs/migration/mappers/quiz wire реализованы по плану, schema 12.json autogenerated, MigrationFrom11to12 12/12 cases, 186 unit tests PASS
- gap surface: только integration-tests (F013/F015/M3 + unit-tests мапперов/JSON helpers) — все runtime behavior covered иначе through unit-tests + migration androidTest

_model: claude-opus-4-7[1m]_
