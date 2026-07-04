# Scope analysis: IS481

## Замысел задачи

Превратить набор компонентов лексемы из жёстко зашитой пары (translation / definition) в обобщённый конструктор: домен оперирует `ComponentType` + `ComponentValue` с шаблоном (`text` / `long-text` / `image`). `translation` остаётся built-in; `definition` мигрирует в user-defined per-dictionary тип с сохранением пользовательских данных. Фактический скоуп IS481 main — data-слой (две новые таблицы + `quiz_configs`, одна миграция M11→M12 под bundled SQLite — AGG-12), API совместимости (`@Deprecated` translation-обёртки, удалённые definition-обёртки — AGG-6), расширение `modules/domain/lexeme` (component types, `QuizConfig`, shim-поля `Lexeme.translation` / `.definition`), точечные правки `wordcard` (новый флаг `hasDefinitionComponent` + сокрытие chip) и `quiz/chat` (lookup через `QuizConfig.componentRefs` с graceful skip). Infra (bundled driver, ProGuard, destructive fallback) уже закрыта в prereq фиче `IS481_..._vPrepared`.

## Spec target

Существующая `docs/features-spec/wordcard.md` — главный затронутый UI-контракт (chip definition, `WordCardState.hasDefinitionComponent`, переписка definition-flow через generic API). Потребуется обновление спеки в рамках IS481, но сам spec уже есть. Для quiz/chat спеки нет (см. `README.md` § «Известные пробелы») — новая спека не создаётся в IS481 (out-of-scope: пишем точечный API contract в business sub-flow, полная спека — будущая работа). `lexeme-domain.md` (IS482) тоже релевантен как baseline domain spec, но IS481 его не заменяет — расширяет.

## Затронутые слои

- **Infrastructure** — нет — DI / ProGuard / bundled driver / mate framework / navigation / theme — всё закрыто в prereq фиче `IS481_..._vPrepared` (commit 6d670d1). В IS481 main `RoomModule.provideDatabase` получает одну строку `.addMigrations(Migration_011_to_012)` к existing builder — это работа по слою data (модуль `core/core-db-impl/`), не infra-fix.
- **Business logic** — да — расширение `modules/domain/lexeme` (новые типы `ComponentValue` / `ComponentType` / `ComponentTypeId` / `ComponentValueId` / `BuiltInComponent` / `ComponentTemplate` / `ComponentValueData` / `QuizConfig` / `ComponentTypeRef` + computed extensions); `WordCardUseCase` interface — удалены definition-методы, добавлены generic; `WordCardUseCaseImpl` / `DatasourceEffectHandler` / `restoreLexeme` переписаны (AGG-6 / MIN-9); `WordCardReducer` — обработка нового `hasDefinitionComponent` флага; `QuizChatUseCase` interface + `QuizChatUseCaseImpl.fetchData` переписаны на `QuizConfig` lookup; `QuizGameImpl.toQuizItem` — graceful skip; маппер `LexemeApiEntity.toDomain` в `app/` (AGG-2) — заполняет shim-поля из components.
- **UI** — да (минимально) — `AddLexemeMeaningRow` / `LexemeMeaningField` в wordcard: chip «Определение» скрывается если `state.hasDefinitionComponent == false`. Translation chip без изменений. UI создания / редактирования user-defined типов — out of scope (отдельная backlog-фича «Quiz config UX»).
- **Data** — да (большой) — две новые таблицы `component_types` / `component_values` (Entity + DAO + Multi-level @Relation в `LexemeDbEntity`), `quiz_configs` (Entity + DAO + `insertDefaultQuizConfig`), удаление колонок `lexemes.translation` / `.definition` (через `ALTER TABLE DROP COLUMN` на bundled SQLite), новые ApiEntity (`ComponentTypeApiEntity` / `ComponentValueApiEntity` / `QuizConfigApiEntity`), `core-db-api` начинает зависеть от `modules/domain/lexeme` (MIN-2), `Migration_011_to_012.kt` directly под `SQLiteConnection` API (AGG-12), JSON helpers (`ComponentValueDataJson`, `ComponentTypeRefJson`) в `core-db-impl`, `Callback.onCreate(connection: SQLiteConnection)` + `seedBuiltIns` (B1), `WordDao.addDictionary` атомарный auto-INSERT default quiz config (AGG-4 реверс), `RoomModule.provideDatabase` регистрация миграции (`.addMigrations(Migration_011_to_012)`).

## Аспекты

- `db_migration` — да — миграция M11→M12 directly на `SQLiteConnection` API (AGG-12). Удаление колонок `lexemes.translation` / `.definition` (через `ALTER TABLE DROP COLUMN` на bundled SQLite), создание таблиц `component_types` / `component_values` / `quiz_configs`, FK + CASCADE, перенос существующих translation / definition в new schema без потери данных. Bundled SQLite (`androidx.sqlite:sqlite-bundled`) уже подключён в prereq.
- `public_contract_change` — да — `CoreDbApi.LexemeApi` сигнатуры меняются: translation-обёртки (`addLexemeTranslation`, `updateLexemeTranslation`, `deleteLexemeTranslation`) помечаются `@Deprecated` с делегацией на generic методы (AGG-6); definition-обёртки удалены целиком, callsites переписаны на generic. `LexemeApiEntity` теряет поля `translation` / `definition`, получает `components: List<ComponentValueApiEntity>`.
- `new_dependency` — да — новая Gradle dep edge: `core/core-db-api/build.gradle.kts` добавляет `implementation(project(":modules:domain:lexeme"))` (MIN-2) — нужно `core-db-api` для типов компонентов (ComponentTypeRef / ComponentValueData), которые шарятся между API и domain.
- `needs_tests` — да — Reducer-тесты для `hasDefinitionComponent` load + chip blocking; UseCaseImpl-тесты на переписанные generic вызовы; mapper-тесты `LexemeApiEntity.toDomain` (orphan, multi-component, shim consistency invariant — см. Test gaps batch); `LexemeBuiltInExtTest` (Gap-7); `QuizGameImpl.toQuizItem` тесты на graceful skip / order priority; **Atomicity rollback тесты:** (1) `addLexemeWithBuiltInComponent` — FK violation → rollback `lexemes` + `write_quiz` записей; регрессия IS479 F1 (F013); (2) **`WordDao.addDictionary` + `insertDefaultQuizConfig` — FK violation / corrupt JSON → `dictionaries` row не создан, F1 invariant `quiz_configs` держится (AGG-4 реверс, см. 05.md строка 61) (F015)**; **integration test `addDictionary` auto-INSERT default quiz config — runtime DAO операция post-migration (test 12 в 07.md): assert каждый созданный словарь получает `[BuiltIn(translation)]` config row (F014, перенесено из migration tests)**.
- `needs_migration_tests` — да — `MigrationFrom11to12.kt` через `MigrationTestHelper(driver = BundledSQLiteDriver())`. Кейсы: translation-only, translation+definition, definition-only, empty dictionary, FK cascade (MIN-4, MIN-10), default config полнота для existing dictionaries (F1 — test 1-4 в 07.md, наполнение существующих словарей через миграцию), AGG-8 verify `json_insert($, '$[#]', ...)` синтаксиса.
- `feature_has_ui_contract` — да — wordcard имеет State/Msg/Reducer + Composable; IS481 расширяет (`WordCardState.hasDefinitionComponent`); quiz/chat не имеет полной спеки, контракт пишется точечно в business sub-flow.

## Затронутые файлы

Опираясь на `02_design_sketch.md` § «Обзор изменений по слоям» + `_alignment_decisions.md`:

- **`core/core-db-impl/`** (большой) — новые `ComponentTypeDb` / `ComponentValueDb` / `ComponentValueWithType` / `QuizConfigDb` Entities; новые `ComponentTypeDao` / `ComponentValueDao` / `QuizConfigDao`; правки `LexemeDbEntity` (Multi-level @Relation + удаление колонок translation/definition); `Database.kt` (`version = 12`, `entities += [...]`); `Database.Callback.onCreate(connection: SQLiteConnection)` + `seedBuiltIns`; новые JSON helpers `ComponentValueDataJson.kt` / `ComponentTypeRefJson.kt`; `Migration_011_to_012.kt` directly на `SQLiteConnection` API; **`WordDao.addDictionary` транзакция расширяется auto-INSERT default quiz config (F004 — метод реально в `WordDao`, не `DictionaryDao`)**; **`RoomModule.provideDatabase` — `.addMigrations(Migration_011_to_012)` к existing builder (одна строка) (F003 — `core/core-db-impl/src/main/java/me/apomazkin/core_db_impl/di/module/RoomModule.kt`)**.
- **`core/core-db-api/`** (большой) — новые `ComponentTypeApiEntity` / `ComponentValueApiEntity` / `QuizConfigApiEntity`; правки `LexemeApiEntity` (+ `components: List<ComponentValueApiEntity>`, удаление `translation` / `definition` полей); `CoreDbApi.LexemeApi` — новые generic методы (`getComponentTypes`, `addLexemeWithBuiltInComponent`, `addLexemeWithUserDefinedComponent`, `addComponentValue`, `updateComponentValue`, `deleteComponentValue`), translation-обёртки `@Deprecated`, definition-обёртки удалены целиком; новая Gradle dep edge — `build.gradle.kts` добавляет `implementation(project(":modules:domain:lexeme"))` (MIN-2).
- **`modules/domain/lexeme/`** (средний) — добавление `BuiltInComponent` enum (только `TRANSLATION` — AGG-1), `ComponentTemplate` enum, sealed `ComponentValueData`, `ComponentType` / `ComponentValue` / `ComponentTypeId` / `ComponentValueId`, sealed `ComponentTypeRef` (BuiltIn / UserDefined), `QuizConfig` (AGG-10 trade-off + KDoc TODO на вынос в `modules/domain/quiz`), shim-поля `Lexeme.translation` / `.definition` (B4/C2), extensions `LexemeBuiltInExt.kt`.
- **`modules/screen/wordcard/`** (средний) — `WordCardUseCase` interface (удалить `addLexemeDefinition` / `deleteLexemeDefinition`, добавить generic); `WordCardState.hasDefinitionComponent: Boolean`; `WordCardReducer` — обработка нового флага при load; Composable `AddLexemeMeaningRow` / `LexemeMeaningField` — chip скрывается если `!state.hasDefinitionComponent`; translation flow остаётся через @Deprecated shim; **`DatasourceEffectHandler` (`mate/DatasourceEffectHandler.kt`) — 2 точки definition переписаны на generic (AGG-6) (F001 — реально лежит в `modules/screen/wordcard/src/main/java/me/apomazkin/wordcard/mate/`, не в `app/`)**.
- **`modules/screen/quiz/chat/`** (минимальный) — `QuizChatUseCase` interface получает новый метод `getQuizConfig(dictionaryId, quizType)`; `QuizGameImpl.toQuizItem` переписан на lookup через `QuizConfig.componentRefs` с graceful skip (`null` вместо `error()`). **`QuizChatUseCaseImpl` лежит НЕ в этом модуле — см. `app/` ниже (F002)**.
- **`modules/screen/dictionaryTab/`** (минимальный) — одна строка в `LexemeUiItem.toUiItem` (computed shim теперь `String?`). Остальное через shim-поля без правок.
- **`app/`** (минимальный) — `LexemeMapper.kt` (`LexemeApiEntity.toDomain` — AGG-2, заполняет shim-поля translation/definition из components, debug-invariant на consistency); `WordCardUseCaseImpl` (переписка definition-callsites на generic); **`QuizChatUseCaseImpl.fetchData` (`app/src/main/java/me/apomazkin/polytrainer/di/module/quizchat/QuizChatUseCaseImpl.kt`) — pre-fetches `QuizConfig` через `getQuizConfig(dictionaryId, "write")`, wire новый interface-метод (F002 — Impl лежит в `app/`, не в модуле; в модуле только interface)**.

## Релевантные спеки и гайды

- [`docs/features-spec/wordcard.md`](../../features-spec/wordcard.md) — UI-контракт wordcard, главный затронутый screen-spec. Будет расширен `hasDefinitionComponent` и переписан раздел definition-flow.
- [`docs/features-spec/lexeme-domain.md`](../../features-spec/lexeme-domain.md) — baseline domain spec (IS482). IS481 расширяет, не заменяет.
- [`docs/features-spec/dagger-di-principles.md`](../../features-spec/dagger-di-principles.md) — DI принципы (для проверки новых DAO / API инжектов).
- [`docs/features-spec/logger.md`](../../features-spec/logger.md) — logging контракт (`Database.Callback.onDestructiveMigration` через `LexemeLogger.e` → Crashlytics уже закрыт в prereq).
- [`docs/features-spec/dictionary-create.md`](../../features-spec/dictionary-create.md) — для `WordDao.addDictionary` транзакции (AGG-4 реверс расширяет существующий контракт).
- [`docs/db-migrations-history.md`](../../db-migrations-history.md) — retrospective timeline удалённых миграций (AGG-12 контекст).
- `docs/guides/*.md` — применимые гайды отметит conductor blockquote-пометками в finalize:after.
- Plan docs IS481: `00_task.md`, `plan/_alignment_decisions.md`, `plan/02_design_sketch.md`, `plan/03_database_design.md`, `plan/04_builtin_strategy.md`, `plan/05_migration_strategy.md`, `plan/06_mapping_design.md`, `plan/07_quiz_strategy.md` — canonical design docs.

## Sub-flow для запуска

| Sub-flow | Запускать? | Обоснование |
|---|---|---|
| Infrastructure | нет | DI / ProGuard / bundled driver / mate framework — всё закрыто в prereq `IS481_..._vPrepared`. В IS481 main только одна строка `.addMigrations(Migration_011_to_012)` в `RoomModule.provideDatabase` (модуль `core-db-impl`) — это data-работа в data-модуле, не infra. |
| Business | да | Расширение `modules/domain/lexeme` (новые типы + QuizConfig), переписка `WordCardUseCase` interface (удаление definition-обёрток, добавление generic — AGG-6), `WordCardUseCaseImpl` / `DatasourceEffectHandler` (в `modules/screen/wordcard/mate/`) / `restoreLexeme` переписки (MIN-9), `WordCardReducer` обработка `hasDefinitionComponent`, `QuizChatUseCase` interface + `QuizChatUseCaseImpl.fetchData` в `app/` + `QuizGameImpl.toQuizItem` переписка, mapper `LexemeApiEntity.toDomain` в `app/` с shim consistency invariant. |
| UI | да | Composable `AddLexemeMeaningRow` / `LexemeMeaningField` — chip definition скрывается по `state.hasDefinitionComponent`. Минимальный, но реальный UI-change (видимость элемента). |
| Data | да | Самый большой слой: две новые таблицы + quiz_configs, Multi-level @Relation, удаление колонок lexemes.translation/.definition, миграция M11→M12 directly на SQLiteConnection API (AGG-12), JSON helpers, `Callback.onCreate(connection)` + seedBuiltIns, `WordDao.addDictionary` транзакция расширяется auto-INSERT default quiz config, новые ApiEntity + generic методы CoreDbApi + Gradle dep edge core-db-api → domain/lexeme (MIN-2), регистрация миграции в `RoomModule.provideDatabase` (core-db-impl). |

## context_output

```yaml
infra_touched: false
business_touched: true
ui_touched: true
data_touched: true
needs_tests: true
needs_migration_tests: true
feature_has_ui_contract: true
spec_filename: wordcard.md
```

## log_messages
- scope_analysis iter 2 переписан: F001-F004 (file locations) + F007 (db_migration/public_contract_change/new_dependency аспекты) + F006 (context_output блок) + F013 (atomicity rollback тест) закрыты
- file locations verified через Grep/Read: DatasourceEffectHandler в modules/screen/wordcard/mate/, QuizChatUseCaseImpl в app/, RoomModule в core/core-db-impl/, addDictionary в WordDao
- sub-flow к запуску: business + ui + data; infra закрыта в prereq

_model: claude-opus-4-7[1m]_
