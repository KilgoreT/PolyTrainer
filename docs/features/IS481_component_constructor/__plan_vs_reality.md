# Plan vs Reality — IS481 component_constructor

## Краткая сводка

Совпадение план↔факт: **~90% по коду / ~7 из 8 root scenarios coded** (verdict: ready-for-merge с явными soft-debt'ами). Главное архитектурное ядро (M13 миграция + `TemplateValues` sealed + CRUD UseCase + soft-delete cascade + двойной entry-point) воплощено как планировалось. Расхождения сосредоточены в трёх категориях: (a) **cardinality downgrade** (F-N5a / scenario 6) — выпал из business_contract как process gap (IS481cc-F6); (b) **Multi-dict picker scope=PerDictionaries** в Create-диалоге Manager-экрана — MVP-trim до `Scope.Global` only (F158); (c) **logging tag** `###ComponentConstructor###` — заменён на модуль-локальные `ComponentsManager` / `PerDictComponents`. Manual smoke + instrumented migration tests — pending (артефакты есть, runner не запускался).

## Что планировалось vs воплощено

### Domain rewrite (concept/template_model.md + typed_views.md)

- **Planned:** sealed `TemplateValues` per-template (`TextValues`, `QuoteWithSourceValues`, `ImageWithCaptionValues`, ...); `Primitive` sealed (Text/Image/Color); `Field`+`PrimitiveType`; `ComponentTemplate` с computed `fields: List<Field>`, nullable `fromKey`; полная замена legacy `ComponentValueData` sealed-иерархии (M12 `TextValue/LongTextValue/ImageValue`); typed-view сразу из JSON parser, без промежуточного `Map<String, Primitive>`.
- **Implemented:** `:modules:domain:lexeme` — `Primitive.kt` (Text/Image/Color), `PrimitiveType.kt` (enum), `Field.kt`, `TemplateValues.kt` sealed (`TextValues`, `ImageValues` — MVP), модификации `ComponentTemplate.kt` (drop `LONG_TEXT`, nullable `fromKey`, computed `fields`), `ComponentType.kt` (`isMulti`, `createdAt`, `updatedAt`, `removeDate → removedAt`), `ComponentValue.kt` (rebind на `TemplateValues`). `ComponentValueData.kt` удалён. Parser `parseTemplateValues(json, template, logger): TemplateValues?` собирает typed view сразу (fail-soft контракт).
- **Расхождение:** нет. Полное соответствие plan'у. Concept-доки упоминали `QuoteWithSourceValues` / `ImageWithCaptionValues` как примеры будущих composite — они корректно не реализованы (MVP-trim per `template_model.md` Open Q6 «только TEXT в MVP»; ImageValues добавлен сверх MVP для покрытия M12 `image` template).

### CRUD user-defined components (concept/ui_placement.md)

- **Planned:** `createUserDefinedComponent(name, template, isMulti, scope)`, `renameComponent(typeId, newName)`, `softDeleteComponent(typeId)`, `previewDeletionImpact(typeId)`. Uniqueness enforcement в UseCase (two-prong SELECT для cross-scope global/per-dict identity — aspect `userdefined_identity_invariant`). Cardinality downgrade проверка (F-N5a) при `is_multi: true → false`.
- **Implemented:** все 4 метода в `LexemeApi` + `ComponentsManagerUseCaseImpl` + `PerDictionaryComponentsUseCaseImpl` (delegation через DI composition). `CreateOutcome` / `RenameOutcome` / `DeleteOutcome` sealed-иерархии с типизированными ошибками (`NameEmpty`, `NameTooLong`, `SameScopeCollision`, `CrossScopeCollision`, `BuiltInProtected`, `Failure`). Two-prong SELECT через `findActiveUserDefinedByName` (per-dict) + `findActiveGlobalByName` (global) в `ComponentTypeDao`. Cascade `quiz_configs.component_refs` (rename + soft-delete) через `QuizConfigDao.updateComponentRefs`. Prefs cleanup через `resetQuizPickerPrefsBestEffort` (best-effort wrap). `softDeleteAtomic` обёрнут в `withTransaction { ... }` (F173).
- **Расхождение 1 (major):** `downgradeCardinality(typeId)` UseCase + `DowngradeCheck`/`DowngradeBlocked` outcomes — **не реализованы**. См. § «Главные расхождения» #1.
- **Расхождение 2 (minor):** `RenameOutcome.NameTooLong` не имплементирован (только `CreateOutcome.NameTooLong`). Minor-gap.

### Soft-delete cascade (concept/deletion_concept.md)

- **Planned:** soft-delete без recovery в фиче; `removed_at` на `component_types` (rename из `remove_date`) и `component_values` (новая колонка); JOIN-based hiding cascade (без UPDATE values); cascade `quiz_configs.component_refs` cleanup + prefs reset; `previewDeletionImpact` показывает values/dicts/quizConfigs/affectedPrefs.
- **Implemented:** `removed_at` добавлен на оба таблицы (rename + new column). Все active DAO queries в `ComponentTypeDao`/`ComponentValueDao` фильтруют `WHERE removed_at IS NULL` (audit done). `LexemeDbEntity.toApiEntity(logger)` делает post-load filter `value.removedAt == null && type.removedAt == null` (Option A; `@Relation` не поддерживает WHERE — F031 best-guess A). `DeletionImpact(valueCount, dictionaryIds, affectedQuizConfigs, affectedPrefs)` возвращается `previewDeletionImpact`. Cascade: `QuizConfigDao.updateComponentRefs` чистит refs; `resetQuizPickerPrefsBestEffort` сбрасывает prefs. `softDeleteAtomic` атомарный через `withTransaction`.
- **Расхождение:** нет. Полное соответствие plan'у. Best-guess (A) для `LexemeDbEntity` подтверждён в реализации.

### UI placement (concept/ui_placement.md)

- **Planned:** Settings entry `ComponentsManageWidget` рядом с `LangManageWidget` (drill-in через `SettingsNavigator` → `ComponentsManagerScreen`); icon-button «молоток» в `DictionaryAppBar.actions` перед `DictDropDownWidget` (видим при `currentDict != null`; автоматически на всех 3 табах Vocabulary/Quiz/Statistic). Create form: имя + шаблон (radio) + scope (radio «На все»/«На конкретные» с multi-select) + is_multi checkbox.
- **Implemented:** `ComponentsManageWidget.kt` в `SettingsTabScreen` между `LangManageWidget` и `ExportDataWidget` (drill-in via `SettingsNavigator.openComponentsManager()`). `ComponentsToolsIconButton.kt` (hammer) добавлен в `DictionaryAppBar.actions` перед `DictDropDownWidget` с условием `currentDict != null && !isLoading`. Виден на всех 3 табах автоматически (shared widget через `CompositionRootImpl`). `CreateComponentDialog.kt` рендерит: имя input + template radio (через новый `LexemeRadioRow` primitive, F162) + is_multi checkbox.
- **Расхождение (major):** **Scope-control в Create dialog UI отключён** (F158 MVP). Reducer на `OpenCreateDialog` жёстко инициализирует scope: Manager → `Scope.Global`, PerDict → `Scope.PerDictionaries([currentDict])`. Multi-dict picker (chip-list для выбора подмножества словарей из Manager) — backlog phase 2. `Msg.CreateScopeChange` в контракте остаётся, но UI его не отправляет. См. § «Главные расхождения» #2.

### Checklist scenarios (8 root + 8 manual)

- **Root scenarios:** 7 из 8 done (1, 2, 3, 4, 5, 7, 8); сценарий 6 (cardinality downgrade) — **out-of-scope**, deferred в Backlog phase 2. Все остальные имеют code-paths под каждый пункт; manual smoke не запускался.
- **Manual scenarios:** 7 pending + 1 blocked (cardinality downgrade — невозможен, edit-flow не реализован). Manual smoke не запускался в этом flow.
- **Расхождения:**
  - Scenario 6 целиком blocked — см. § «Главные расхождения» #1.
  - Логи `###ComponentConstructor###` (специфический tag-стрим из checklist) — **не внедрены**. Фактически используются модуль-локальные tags `ComponentsManager` / `PerDictComponents` (`LogTags.kt` в обоих screen-модулях). Логи в `Migration_012_to_013` (счётчики rewrite'нутых rows / дроп индексов / backfill timestamps) — отсутствуют. См. § «Главные расхождения» #3.

## Главные расхождения

### 1. Cardinality downgrade (F-N5a / Scenario 6)

- **Planned:** `02_scope.md` aspect `multi_to_single_downgrade` + `template_model.md` Open Q9 «жёсткий запрет при наличии лексем с count > 1» + checklist root scenario 6 («Юзер пытается понизить cardinality `is_multi=true→false` при наличии лексем с count>1 → операция блокируется с показом списка»).
- **Реальность:** UseCase метод `downgradeCardinality(typeId)` / `DowngradeCheck` / `DowngradeBlocked` outcomes / edit-dialog в UI — **отсутствуют**. `UserDefinedRowWidget` имеет только Rename / Delete actions, нет Edit-action. `is_multi` после create не редактируется.
- **Почему:** Process gap между `scope_analysis` → `business_contract`. Aspect `multi_to_single_downgrade` был отмечен в scope, но `business_contract` / `business_design_tree` пропустили его при формировании списка методов UseCase. Зафиксировано в FlowBacklog как **IS481cc-F6** (recurring process finding).
- **Перенесено:** `docs/Backlog.md → IS481 phase 2` + `docs/FlowBacklog.md → IS481cc-F6`. Phase 2 требует отдельного design + edit-flow + UseCase метода + UI edit-dialog.

### 2. Multi-dict picker scope=PerDictionaries (F158)

- **Planned:** `concept/ui_placement.md` § «Создать новый» → Scope = radio «На все словари» (global, `dictionaryId=null`) / «На конкретные» (выбор одного или нескольких dict; в БД = N независимых записей с `dictionaryId=X`). `ui_layout.md` iter 1 содержал chip-list dictionaries для multi-select.
- **Реальность:** UI scope-control в `CreateComponentDialog` отключён. Reducer на `OpenCreateDialog` жёстко инициализирует scope по контексту экрана: Manager → `Scope.Global`, PerDict → `Scope.PerDictionaries([currentDict])`. `Msg.CreateScopeChange` остаётся в Msg-контракте, UI его не вызывает.
- **Почему:** state не несёт `availableDictionaries` (для рендеринга multi-select chip-list); добавление потребовало бы новых `Msg`/`Effect` и нарушило бы invariant фиксированного контракта после iter 3 ui_design_tree. F158 explicitly trimmed scope до MVP-варианта.
- **Перенесено:** `docs/Backlog.md → IS481 phase 2` (Multi-dict picker для Manager-экрана + загрузка availableDictionaries в state).

### 3. Логи `###ComponentConstructor###` tag-стрим

- **Planned:** `checklist.md` § «Примечание о логах» — новый уникальный tag `###ComponentConstructor###` для всех UseCase methods (`createUserDefinedComponent`, `renameComponent`, `softDeleteComponent`, `previewDeletionImpact`, `downgradeCheck`), Reducer/EffectHandler navigation effects, Migration M12→M13 (счётчики rewrite'нутых rows / дроп индексов / backfill timestamps), DAO cascade-методы.
- **Реальность:** Используются модуль-локальные tags `ComponentsManager` (в `:modules:screen:components_manager/.../LogTags.kt`) + `PerDictComponents` (зеркало в per_dictionary_components). Логи в `Migration_012_to_013` отсутствуют (счётчики не пишутся). Логи в DAO cascade — отсутствуют.
- **Почему:** Logger convention в проекте — module-scoped tags, не feature-scoped. Tag `###ComponentConstructor###` не предусмотрен `LexemeLogger` API. Решение принято на implementation phase без отдельного finding.
- **Перенесено:** soft-debt в `global_code_review.md § Открытые вопросы`. Возможные follow-up: либо отдельный лог-pass с rename на `###ComponentConstructor###` (если оркестратор настаивает на checklist-formality), либо обновить checklist под фактические теги.

### 4. Дублирование Dialog'ов между screen-модулями

- **Planned:** infra phase создаёт `:modules:widget:component_widgets` (Tier 2 widget-модуль) для shared widget'ов между двумя screen-модулями.
- **Реальность:** `:modules:widget:component_widgets` создан как gradle setup + AndroidManifest, но source files пустые. Dialog'и (`CreateComponentDialog`, `RenameComponentDialog`, `DeleteComponentConfirmDialog`, `CreateComponentFab`, `ComponentsEmptyStateWidget`, `ComponentTemplateLabel`, `NameErrorLabel`) **дублированы** между `:modules:screen:components_manager/widget/` и `:modules:screen:per_dictionary_components/widget/`.
- **Почему:** Разные mate-пакеты (`components_manager.mate.*` vs `per_dictionary_components.mate.*`) — shared widget потребовал бы вынести `CreateDialogState`/`RenameDialogState`/`DeleteConfirmState` в shared interface'ы либо рефакторинга mate-пакетов обоих модулей. Out-of-scope UI sub-flow.
- **Перенесено:** soft-debt в `docs/Backlog.md → Архитектура` (вынос в `:modules:widget:component_widgets`).

## Что планировалось как opt но не сделано

- **Manual smoke testing** (8 manual checklist сценариев) — pending. Все code-paths под каждый сценарий присутствуют, но визуальная проверка, logcat-проверка и БД-инспекция (через `adb shell sqlite3`) — отложены.
- **Instrumented migration tests НЕ запущены.** `MigrationFrom12to13.kt` (13 кейсов A-L) + `MigrationFrom12to13IdempotencyTest.kt` (M3/M3b) написаны со спецификацией, но `connectedDebugAndroidTest` требует emulator runner. До запуска M12→M13 не имеет hands-on validation на реальном Android SQLite. Риск контролируется через `fallbackToDestructiveMigration` + Crashlytics (зафиксированы в prereq IS481).
- **`13.json` schema export cross-проверка** через `runMigrationsAndValidate(13, ...)` — требует instrumented runner (см. выше).
- **Stale-reference sweep** на `ComponentValueData` / `ComponentValueDataJson` — Grep по проекту не выявил production-ссылок, release build verify рекомендован.
- **`RenameOutcome.NameTooLong`** — minor-gap, impl TODO в `ComponentsManagerUseCaseImpl`.
- **`BuiltInProtected` conflation для soft-deleted** — semantic-fix; упоминалось в business_summary как soft-debt.

## Итого

- **Code-level coverage:** ~90% планировавшихся узлов done. Infra (37 узлов done), Business (~10 типов domain + 5 BREAKING + 6 NEW LexemeApi + 2 UseCase + 2 Mate-стека + 146 unit tests), UI (19 узлов done: 14 widgets + 2 modify + 2 Screens + 2 Tier 1 primitives + ~33 strings), Data (23 узла done: Migration_012_to_013 9-шаговая + DAO audit + new methods + nullable mappers + frozen seed v12 + schema export 13.json + 10 unit tests + 13+2 instrumented тестов не запущены).
- **Functional coverage:** 7/8 root scenarios coded (1-5, 7, 8); scenario 6 (cardinality downgrade) — out-of-scope. 0/8 manual scenarios verified — pending.
- **Quality gates:** 3/3 PASS — lintDebug PASS, testDebugUnitTest PASS, assembleDebug PASS (см. `global_code_review.md § Quality gates`).
- **Verdict:** **ready-for-merge с явно перечисленными soft debts.** Главные открытые: (a) cardinality downgrade phase 2; (b) multi-dict picker phase 2; (c) logging tag rename либо checklist update; (d) instrumented migration tests runner; (e) manual smoke 7 сценариев. Ни один из них не блокирует merge — все либо backlog phase 2, либо process follow-up, либо ручная верификация.

_model: claude-opus-4-7[1m]_
