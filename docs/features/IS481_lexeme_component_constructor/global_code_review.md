# Global Code Review — IS481 Lexeme Component Constructor

## Verdict

**APPROVED WITH FOLLOWUPS**

Архитектура цельная, decisions из `plan/_alignment_decisions.md` (AGG-1..12, MIN-2..12,
B4/C2/B1/D1..D4) применены корректно, dependency rule соблюдается, layer attribution
без утечек. 186 unit tests PASS, lint 0 errors. Найдено **2 Major** (один — отсутствие
atomicity rollback тестов, заявленных в плане как обязательные F013/F015; второй —
runtime поведение `updateComponentValue` / `deleteComponentValue` через generic path
возвращает null даже на success, что не баг сейчас потому что эти ветки нигде в IS481
не вызываются, но это foot-gun для будущих caller'ов) и несколько Minor / Followups.
**Critical блокеров merge нет.**

Manual smoke миграции M11→M12 на устройстве (`connectedDebugAndroidTest`) не запускался —
это **необходимое pre-merge действие**, без него Critical commit'ы могут проскочить.
Migration suite (12 кейсов A-L) реализован грамотно, но реальный прогон обязателен.

## Findings (by severity)

### Critical (блокеры merge)

Critical блокеров нет. Условный pre-merge gate:

- **Запустить `connectedDebugAndroidTest` для `:core:core-db-impl`**
  (`MigrationFrom11to12` 12 кейсов A-L + `BundledSqliteFeatureTest` 6 кейсов).
  Без зелёного прогона миграция M11→M12 не проверена on-device — это main core IS481
  work. В check.md (строки 26-30) явно отложено пользователю. Это не bug, но
  merge-gate.

### Major (нужно исправить, но не блокер)

1. **Atomicity rollback тесты F013/F015 не реализованы.**
   В `business_test.md` (test gap categories) и в `02_scope.md:23` явно перечислены
   как **обязательные** unit-тесты:
   - F013: `addLexemeWithBuiltInComponent` — FK violation → rollback `lexemes` +
     `write_quiz` записей (регрессия IS479 F1).
   - F015: `WordDao.addDictionary` + `insertDefaultQuizConfig` — FK violation /
     corrupt JSON → `dictionaries` row не создан, F1 invariant держится.

   В `WordCardUseCaseImplTest.kt:131-146` есть только `addLexemeWithBuiltInComponent
   exception returns null` — это unit-test exception handling, **не atomicity
   rollback**. Реальный rollback DB-транзакции (что обе записи откатываются
   когерентно) можно проверить только в `androidTest` против реальной БД.

   Проверка `grep -rn "atomicity\|rollback" .../test .../androidTest` — 0 матчей в
   тестовом коде, только в docs/build artifacts. Заявленный в `business_summary.md:77`
   atomicity test (F013) фактически отсутствует.

   **Risk:** регрессия IS479 F1 (атомарность INSERT lexeme + write_quiz) — высокий
   impact, происходит на FK violation runtime. Без теста любая будущая правка
   `WordDao.addLexemeWithComponents` или `addDictionary` может незаметно сломать
   инвариант.

   **Recommendation:** добавить `:core:core-db-impl/androidTest`:
   `WordDaoAtomicityTest.kt` с 2 case'ами — `addLexemeWithComponents` с FK violation
   на `components` (например component_type_id не существует), `addDictionary` с
   проверкой что `dictionaries` row отсутствует на симулированной ошибке
   `_addQuizConfigRow`. Это followup за пределами merge gate если manual smoke прошёл.

2. **`updateComponentValue` / `deleteComponentValue` через generic path возвращают
   `null` даже при успехе.**
   Файл `WordCardUseCaseImpl.kt:190-231`. На каждом методе `// TODO: requires
   DAO-level getLexemeIdByComponentValueId(componentValueId) lookup`. На success
   path возвращается `null` (см. строки 204, 224). KDoc оправдывает: «callers идут
   через lexemeId-based path в IS481».

   Это не баг **сейчас** (no caller использует generic path), но caller, который
   увидит signature `suspend fun updateComponentValue(...): Lexeme?` и интерпретирует
   `null` как «ошибка», будет ввести в заблуждение — null на success path
   выглядит как silent failure. Foot-gun для следующего разработчика.

   **Recommendation:** либо выкинуть из interface эти методы до появления callers
   (YAGNI), либо изменить return type на `Unit?` чтобы semantics стала
   «success/failure», либо реализовать DAO lookup (TODO материализуется). Текущий
   статус-кво — implicit contract нарушен.

### Minor (полировка)

1. **`DefinitionApiEntity` value class — dead code.**
   `core-db-api/.../LexemeApiEntity.kt:15`. Комментарий в самом файле:
   «больше нигде не используется в LexemeApi (AGG-6: definition wrappers удалены)».
   Существует только для symmetry с `TranslationApiEntity`. Поиск
   `grep -rn "DefinitionApiEntity\b"` в `src/main` подтверждает — 0 реальных
   callsite'ов. Можно удалить, ничего не сломается.

2. **`addLexemeWithBuiltInComponent` (CoreDbApiImpl:243) использует `error(...)`
   вместо logger.e + null.**
   В отличие от `addLexemeWithUserDefinedComponent` (которая на miss типа делает
   `logger.e` + `null`), `addLexemeWithBuiltInComponent` бросает
   `IllegalStateException("Built-in component type not found...")`. Caller
   (`WordCardUseCaseImpl`) ловит exception в try/catch и логирует, но shape
   ошибки несимметричен с user-defined branch. Built-in отсутствует — это
   serious data corruption (seedBuiltIns не сработал), но всё же лучше унифицировать
   error handling (logger.e + return специальный exception class или error code).

3. **`Lexeme` data class имеет `@Deprecated` поля `translation` / `definition` в
   ctor.** Любой `copy(translation = ...)` создаёт рассинхрон с `components` без
   warning'а (компилятор Kotlin не warning'ит copy с deprecated параметрами в
   data class). `Lexeme.copy(translation = X)` найдён в mate (см.
   `State.kt:152-158`, `WordCardReducer.kt:312, 326, etc`). Trade-off задокументирован
   в B4/C2 как «mate refactor отдельной фичей», но это **активный технический долг**
   — invariant держится только если mate не вызывает copy на shim-поля; сейчас
   вызывает в десятке мест. После TranslationDeleted / DefinitionDeleted в reducer
   `state.updateLexeme(...) { it.copy(translation = null) }` обновляет ТОЛЬКО UI
   `LexemeState` (не domain `Lexeme`), поэтому в практике рассинхрон не материализуется
   на текущих use cases. Но добавлять новых caller'ов в `Lexeme.copy(translation=...)`
   опасно.

4. **`resolveDictionaryIdForLexeme(lexemeId)` (WordCardUseCaseImpl:330-332)
   delegates to current dictionary.**
   Комментарий «Текущая реализация: delegates to current dictionary — лексемы в
   word card сцене всегда из current dictionary». Это hidden assumption — если
   когда-нибудь wordcard будет открываться для лексемы не из current dict (deep-link,
   notification, search across dicts), будет тихая корректировка writes в wrong
   dictionary. TODO задокументирован. Минор сейчас, но риск-маркер.

5. **WordDao содержит legacy методы `deleteDefinitionSuspend` / `deleteDefinitionsSuspend`
   / `removeSampleSuspend`.** Используются в `WordApiImpl.deleteWordSuspend` через
   ручной cascade — но в текущей схеме v12 FK CASCADE (`lexemes.word_id` →
   `words.id`) и (`component_values.lexeme_id` → `lexemes.id`) делают это
   автоматически. Ручной cascade в `deleteWordSuspend` это **legacy дубликат**
   FK CASCADE. Не баг (idempotent), но dead complexity. Followup cleanup.

6. **`DefinitionMapper.kt:68-76` / `DefinitionSampleRelMapper.kt:77` — TODO
   mapper-refactor.** legacy маппер возвращает `null` для `definition` поля после
   удаления колонки. Комментарии явно говорят «backward-compat, TODO». Это
   признак того, что весь `DefinitionMapper` после IS481 потерял смысл (поле
   удалено) — оставлен только для compile. Followup на cleanup.

7. **`@Insert` на `_addQuizConfigRow` (WordDao:34) без `OnConflictStrategy`.**
   Default = ABORT. Это ОК для семантики «уникальный default config на словарь»,
   но явный `OnConflictStrategy.ABORT` сделает intent очевидным.

8. **Migration step 4 (`seedBuiltIns`) и step 8 (`insertDefaultQuizConfigsForAllDictionaries`)
   зависят от UNIQUE индексов из step 1/3.** В реальности порядок выполнен корректно
   и Room автоматически оборачивает migrate() в транзакцию. Защита от частичного
   apply работает (rollback на ошибке). Никаких bug'ов, просто хрупкость — если
   кто-то будущий переставит шаги, FK violations могут проявиться runtime'но без
   compile error.

9. **`Migration_011_to_012.insertDefaultQuizConfigsForAllDictionaries` (строка 177)
   жёстко хардкодит `'write'` режим.** Если в будущем появится `'card'` / `'recall'`
   режим, нужно будет миграцией ещё раз пройти по всем dictionaries и добавить.
   F1 invariant («каждый dict × каждый mode имеет config»), описанный в
   `_alignment_decisions.md` MIN-7, держится **только пока есть один mode**.
   Followup для future mode-adding фичи.

### Followups (backlog)

- **Wordcard mate refactor** — заменить `LexemeState.translation/definition` /
  `Lexeme.translation/.definition` shim на коллекцию components.
  Trigger: после IS481 (B4/C2). Уже в backlog (см. business_summary.md:101).

- **Quiz config UX** — вынос `QuizConfig` / `ComponentTypeRef` из `modules/domain/lexeme`
  в `modules/domain/quiz` + UI редактирования. Уже в backlog (AGG-10 TODO).

- **DAO `getLexemeIdByComponentValueId`** — позволит реализовать generic
  `updateComponentValue` / `deleteComponentValue` honestly. Закрывает Major #2.

- **`deleteWordSuspend` cleanup** — удалить ручной cascade, положиться на FK CASCADE
  v12. Minor #5.

- **Удалить `DefinitionApiEntity`, `DefinitionMapper.kt`, `DefinitionSampleRelMapper.kt`,
  `DefinitionDumpMapper.kt` legacy stubs** после mate refactor.

- **WordDaoAtomicityTest** — рекомендованный новый androidTest для F013/F015.

- **F6 referential consistency cleanup** — `deleteComponentType` cleanup
  `quiz_configs.component_refs`. Закрывается в Quiz config UX (MIN-11).

- **N+1 schema-level guard** — F1 invariant («каждый dict × каждый mode имеет
  config») при добавлении новых quiz_mode'ов. Minor #9.

## Coverage analysis

**Хорошо покрыто:**

- `LexemeMapperTest` (app, ~10 кейсов) — все shim consistency invariant'ы
  (B7/B8/C11/C12/C13), translation/definition built-in / user-defined lookup,
  order preservation, non-text data → null shim.
- `WordCardUseCaseImplTest` (app, ~13 кейсов) — happy paths, exception → null,
  lookup miss → null, shim делегирует, deleteDefinitionComponent ComponentRemoved /
  LexemeCascadeRemoved / null.
- `QuizGameImplTest` (quiz/chat, ~8 кейсов) — graceful skip null, order priority
  F4, empty refs/components, BuiltIn vs UserDefined matching.
- `LexemeBuiltInExtTest` (domain/lexeme, 5 кейсов) — Gap-7 закрыт.
- `WordLoadedTest` (wordcard, 4 кейса) — `hasDefinitionComponent` reducer computation.
- `DatasourceEffectHandlerTest` (wordcard) — LoadWord sequential prefetch, reroute
  generic.
- `MigrationFrom11to12` (core-db-impl androidTest, 12 cases A-L) — все ключевые
  миграция-сценарии задокументированы и реализованы (translation-only, mixed, FK
  cascade, AGG-8 json_insert syntax, partial UNIQUE existence, special chars).
- `BundledSqliteFeatureTest` (core-db-impl androidTest, 6 кейсов) — DROP COLUMN,
  json_object, json_insert, json_each, json_remove, sqlite_version >= 3.45.

**Gap'ы:**

- **Atomicity rollback тесты F013/F015** — отсутствуют целиком (см. Major #1).
- **Integration test `addDictionary` runtime** (auto-INSERT default quiz_config) —
  упомянут в `data_summary.md:133` как «не реализован как test class. Followup для
  check». Реально может проверяться индирectно через миграционные кейсы (если
  существующие dicts получают default config), но не для нового `addDictionary`
  через DAO post-migration.
- **`updateComponentValue` / `deleteComponentValue` через generic path** — не
  тестируются, потому что не используются (Major #2 — return null дефект).
- **Migration step 8 idempotency** — что если migration падает между step 8 и step 9
  и пересоздаётся? Room rollback'нёт всю migrate() и попробует ещё раз — но
  стабильность под такой crash сценарий не тестируется явно (это уже за пределами
  scope IS481).
- **`updateLexemeTranslation` @Deprecated shim** в `CoreDbApiImpl:364-399` —
  не имеет unit-теста (метод не вызывается, см. Major #2 analogue).

## Migration safety analysis

**Положительное:**

- Порядок шагов корректный (D1, AGG-9). SELECT из legacy `translation` /
  `definition` колонок до `ALTER TABLE DROP COLUMN` — выполнено (steps 6-9 ДО
  step 10).
- FK ready на каждом шаге: step 1-3 создают таблицы → step 4 seedBuiltIns создаёт
  built-in translation type → step 5 user-defined Definition types → step 6-7
  миграция данных в component_values (FK на типы готов).
- Room оборачивает `migrate()` в транзакцию — атомарность есть. Частичный apply
  невозможен.
- Bundled SQLite 3.45+ verified через `BundledSqliteFeatureTest` (6 кейсов, DROP
  COLUMN + json функции + version assert).
- `MigrationTestHelper(driver = BundledSQLiteDriver(), databaseClass = Database::class)` —
  корректно использует Room 2.8.x KMP API.
- Schema 12.json автогенерирован, validation отработает на `runMigrationsAndValidate`.
- FK CASCADE chains покрыты test F (dictionary → ...) и test G (component_type →
  component_values).

**Рискованные места:**

- **Step 9 `addDefinitionToQuizConfigsForDictionariesWithDefinitionData`** —
  выполняет SELECT по `l.definition IS NOT NULL` (legacy колонка), потом UPDATE
  через `json_insert`. Если в lexemes есть definition data, но
  `createUserDefinedDefinitionTypes` (step 5) почему-то промазал (race? bug?
  невозможно при текущей логике, но всё же) — quiz_config будет ссылаться на
  user-defined "Definition" тип, которого нет. Runtime quiz просто graceful
  skip'нёт по AGG-5/F2, но это hidden data invariant. Защита через
  `INSERT ... SELECT DISTINCT ... WHERE l.definition IS NOT NULL` в step 5
  работает корректно (тот же предикат). Defensive — OK.

- **Step 8 INSERT для ВСЕХ dictionaries без фильтра** — даже если в словаре нет
  лексем (case D в migration test). Это правильно по F1, но создаёт config для
  возможно «забытых» / deprecated словарей. Не баг, просто observation — что
  если у словаря `removeDate IS NOT NULL` (но dictionaries entity не имеет такого
  поля сейчас) — будет visible. На текущей схеме N/A.

- **`fallbackToDestructiveMigration(dropAllTables = true)`** — если когда-нибудь
  встретится install с DB version < 11, ВСЕ данные дропаются. Логирование через
  `LexemeLogger.e` → Crashlytics, нон-фатал. Это приемлемо по docs/db-migrations-history.md
  (никто из internal testers не имеет такой версии), но если внезапно кто-то
  обновится с очень старой версии — silently потеряет данные с notification
  только в Crashlytics. Не блокер.

- **Partial UNIQUE index `index_component_types_global_userdef_name`** создаётся
  в `seedBuiltIns`, **не описан в Room schema 12.json**. `runMigrationsAndValidate`
  будет принимать любой набор индексов поверх задекларированных Room'ом — потенциально
  partial index может быть не создан и runtime UNIQUE проверка пропустит дубликаты.
  Test I (`caseI_partialUniqueIndexExists`) явно проверяет через `sqlite_master`,
  что index создан. Защищено.

- **Migration не идемпотентна на повторный запуск.** Если каким-то магическим
  способом `migrate()` запустится дважды (Room не должен этого делать, но), step 5
  упадёт на UNIQUE `(dictionary_id, name)` для второго прохода. Это OK потому что
  Room сделает rollback и transaction abort.  MIN-12c явно отверг defensive
  `INSERT OR IGNORE` как FP — позиция обоснована.

**Recommendations:**

- Manual smoke на устройстве с реальной production DB v11 (с translation+definition
  data) перед merge.
- Document в release notes возможность Crashlytics non-fatal от destructive
  migration (для info QA).

## Architecture compliance

### AGG decisions

| Decision | Реализация | Статус |
|---|---|---|
| AGG-1 (definition → user-defined, только TRANSLATION built-in) | `BuiltInComponent.kt` содержит только TRANSLATION. Mapper использует `systemKey == null && name == "Definition"` для definition shim. | ✅ Соблюдено |
| AGG-2 (мапперы в `app/.../mapper/LexemeMapper.kt`) | Все API → Domain мапперы IS481 в одном файле. | ✅ |
| AGG-3 (7.md naming `quiz_configs`) | Таблица `quiz_configs`, entity `QuizConfigDb`, ApiEntity `QuizConfigApiEntity`. | ✅ |
| AGG-4 (addDictionary auto-INSERT default quiz_config) | `WordDao.addDictionary` @Transaction default-method, INSERT + INSERT default `[BuiltIn(translation)]`. | ✅ |
| AGG-5 (QuizConfig pre-fetch один раз на session) | `QuizGameImpl.fetchData` вызывает `getQuizConfig` единожды, передаёт в `toQuizItem` per-lexeme. | ✅ |
| AGG-6 (translation @Deprecated shim, definition wrappers удалены) | `CoreDbApi.LexemeApi` — `addLexemeWithTranslation`/`updateLexemeTranslation` @Deprecated; definition обёртки удалены. Mate / WordCardUseCase переписаны на generic. | ✅ Соблюдено, но deprecated CoreDbApi shims не имеют callers — dead code (Minor) |
| AGG-7/AGG-11 (obsolete) | Истории отменённых решений сохранены в docs. | ✅ N/A |
| AGG-8 (verify json_insert syntax) | `BundledSqliteFeatureTest.jsonInsertAppend_isSupported` (smoke test) + `caseH_jsonInsertAppendSyntax` (migration test). | ✅ |
| AGG-9 (явный порядок migration steps) | 10 шагов с rationale в KDoc и в коде разнесены по приватным методам с явными именами. | ✅ |
| AGG-10 (QuizConfig/ComponentTypeRef в lexeme module с TODO) | Оба типа в `modules/domain/lexeme` с TODO-комментариями на вынос. | ✅ |
| AGG-12 (existing migrations dropped, новая M11→M12 directly на bundled driver) | `Migration_011_to_012` написана directly на `SQLiteConnection` API, без `SupportSQLiteDatabase`. RoomModule использует `setDriver(BundledSQLiteDriver())`. | ✅ |

### MIN decisions

| Decision | Реализация | Статус |
|---|---|---|
| MIN-2 (Gradle dep core-db-api → modules/domain/lexeme) | `core/core-db-api/build.gradle.kts:34` — `api(project(":modules:domain:lexeme"))`. | ✅ |
| MIN-3 (double cascade pathway комментарий) | Не нашёл явный комментарий в `03_database_design.md`, не критично. | ⚠ Документная мелочь |
| MIN-4 (integration test cascade chain dictionary delete) | `MigrationFrom11to12.caseF_cascadeChain_dictionaryDelete`. | ✅ |
| MIN-6 (thread policy quiz_configs парсинга — inline) | `QuizConfigDb.toApiEntity()` парсит inline через `toComponentTypeRefList`. | ✅ |
| MIN-7 (Invariants section в 07.md) | Документ. F1 invariant покрыт миграцией (step 8 для всех dictionaries). | ✅ |
| MIN-8 (`insertDefaultQuizConfig` hardcoded JSON без mapper) | `QuizConfigDao.insertDefaultQuizConfig` default-method с hardcoded JSON. | ✅ |
| MIN-9 (restoreLexeme atomic compound INSERT) | `WordDao.addLexemeWithComponents` @Transaction; `WordCardUseCaseImpl.restoreLexeme` вызывает через `lexemeApi.addLexemeWithComponents`. | ✅ |
| MIN-10 (test cascade component_types → component_values) | `MigrationFrom11to12.caseG_cascadeComponentTypeToValues`. | ✅ |
| MIN-11 (F6 cleanup quiz_configs.component_refs) | Не реализован в IS481 (нет операции `deleteComponentType` в этом scope, по плану в backlog). | ✅ Корректно отложено |
| MIN-12 (UX/F4/idempotency сабвопросы) | F4 order priority покрыт `QuizGameImplTest`. UX оставлен. Idempotency reject обоснован. | ✅ |

### B4/C2 (shim contract)

`Lexeme.translation` / `.definition` помечены `@Deprecated`, value-classes `Translation` /
`Definition` тоже `@Deprecated`. Mapper заполняет из components. mate использует
shim-поля без правок. Trade-off documented. ✅

### Dependency Rule

- `modules/domain/lexeme` — leaf (нет deps на data/api/UI). ✅
- `core-db-api` зависит от `modules/domain/lexeme` (по A1 — данные знают domain). ✅
- `core-db-impl` зависит от `core-db-api` + `modules/domain/lexeme`. ✅
- `app/` зависит от `core-db-api` + `modules/domain/lexeme` (мапперы в app/). ✅
- Циклов нет.

### Layer attribution

- Domain types в `modules/domain/lexeme` — ✅.
- API DTO в `core-db-api` (с зависимостью на domain типы) — ✅.
- DAO/Entity/Migration/JSON helpers в `core-db-impl` — ✅.
- Mappers API→Domain в `app/.../mapper/LexemeMapper.kt` (AGG-2) — ✅.
- UseCaseImpl в `app/.../di/module/wordCard/` и `app/.../di/module/quizchat/` — ✅.
- UseCase interface в `modules/screen/wordcard/.../deps/` и `modules/screen/quiz/chat/.../deps/` — ✅.

**Утечек нет.** Domain не знает про data/api. API не знает про UI. UI не знает про
data implementation.

---

_model: claude-opus-4-7[1m]_
