# Scope analysis: IS481 component_constructor phase 2

## Замысел задачи

Phase 2 закрывает 5 расхождений phase 1 vs concept одним flow: (1) Edit existing user-defined компонента с template-immutability и cardinality-downgrade guard (`isMulti: true→false` блокируется если есть лексемы с count>1); (2) multi-dict scope picker в Create-диалоге Manager-экрана (radio Global / PerDictionaries + chip-list dictionaries); (3) наполнение `:modules:widget:component_widgets` — вынос 6+ дублированных диалогов и виджетов из обоих screen-модулей в shared module через примитивы + callbacks (без mate-зависимостей), плюс фундамент per-template architecture (`TextWidget` / `ComponentBlock` / `ComponentByTemplate` resolver); (4) разделение `RenameOutcome.BuiltInProtected` для soft-deleted типов (добавить `Removed` variant в `RenameOutcome` / `DeleteOutcome` / новый `EditOutcome`); (5) feature-scoped tag `###ComponentConstructor###` в shared logger + логи в Migration_012_to_013, `QuizConfigDao.updateComponentRefs` cascade и в prefs reset для adb-фильтрации smoke-событий. Миграция M13→M14 НЕ требуется: все изменения phase 2 — на уровне UseCase / SQL-логики (existing schema), domain sealed types и UI; БД-инвариантов не меняем.

**Асимметрия `Removed` variant.** `Removed` добавляется только в Rename/Delete/Edit outcomes; в `CreateOutcome` не нужен — Create не оперирует existing `type.id`, а soft-deleted name-коллизия покрывается `SameScopeCollision` / `CrossScopeCollision` (existing `removed_at IS NULL` фильтр уже исключает soft-deleted rows из уникальности). [F004]

**Template в Edit-сигнатуре.** `editComponent` принимает `template: ComponentTemplate` параметр (наряду с `name` и `isMulti`); `Msg.EditTemplateChange(template)` обновляет UI state. Immutability обрабатывается **на UseCase-уровне**: если `Submit` приходит с изменённым `template` относительно current — UseCaseImpl возвращает `EditOutcome.TemplateImmutable` без обращения к data API. SQL `editComponentType(typeId, name, template, isMulti)` физически принимает template (data API не падает, чтобы UseCase мог делать immutability check как business-rule, а не как структурный invariant). [F017]

> 📎 guide: docs/guides/data-layer.md — "Template immutable после релиза: после того как ComponentTemplate entry попал в релиз — его fields изменять запрещено; нужны другие поля → создаётся новый template"
>
> 📎 guide: docs/guides/logging.md — "Формат тега: ###СЛОВО### — тройные решётки, БОЛЬШИЕ буквы; слово = область/модуль"
>
> 📎 guide: docs/guides/naming.md — "R-N-012: варианты sealed/enum связанных с БД — только добавлять, никогда не удалять"

## Spec target

`docs/features-spec/component-constructor.md` (update existing). Файл: `component-constructor.md`.

## Затронутые слои

- **Infrastructure** — **да** — `LogTags.COMPONENT_CONSTRUCTOR = "###ComponentConstructor###"` константа в shared logger (см. § Логгер — best-guess решение фиксирует `:modules:core:logger/LogTags.kt`); DI-связки в `ComponentsManagerModule` для нового `flowDictionaries` зависимости (если выбран subscribe-pattern); опционально compose-tooling dep для preview-функций в `:modules:widget:component_widgets`. **`:modules:widget:component_widgets/build.gradle.kts` уже содержит** android{} blok + deps на theme / ui / lexeme / core-resources (зарегистрирован в `settings.gradle.kts` line 59, `app/build.gradle.kts` line 132 implementation'ит) — «наполнение» сводится к ревизии deps (опц. добавить compose-tooling, опц. исключить лишние) при extract widgets. Mate-обвязка по топологии не меняется — только новые Msg/Effect внутри существующих файлов. [F002]

> 📎 guide: docs/guides/logging.md — "Каждый модуль имеет LogTags.kt; использовать ТОЛЬКО константы, не хардкодить строки"
>
> 📎 guide: docs/guides/project-architecture.md — "Widget модули могут иметь собственные Mate инстансы; widget уровень между core/ui и screen/*/widget"

- **Business logic** — **да** — расширение Reducer (Edit-ветка с epoch correlation parity F124/F136 + multi-dict scope branch + cardinality-downgrade preview UI state); расширение Msg (`Open/Submit/Close EditDialog`, `EditNameChange/EditTemplateChange/EditMultiToggle`, `EditResult`, `DictionariesLoaded`); новый sealed `EditOutcome { Success / NameEmpty / SameScopeCollision / CrossScopeCollision / CardinalityDowngradeBlocked(List<Long>) / TemplateImmutable / BuiltInProtected / Removed / Failure }`; добавление `Removed` в `RenameOutcome` и `DeleteOutcome`; новые UseCase методы (`editComponent`, `flowDictionaries`); UseCase mapping API outcome → domain (включая `Removed` semantics на data-уровне — `removed_at IS NOT NULL` check); UseCaseImpl содержит **template-immutability gate**: на Submit сравнивает `template` параметра с current `type.template` — при mismatch возвращает `EditOutcome.TemplateImmutable` до вызова data API. [F017]

> 📎 guide: docs/guides/reducer-patterns.md — "State мутируется только через extension chain; каждая extension меняет ровно одну сущность"
>
> 📎 guide: docs/guides/messages.md — "Open* — намерение открыть; Close* — закрыть локальный UI-элемент; *Loaded — данные получены"
>
> 📎 guide: docs/guides/state-modeling.md — "Sum types через sealed class для взаимоисключающих состояний; считать варианты"
>
> 📎 guide: docs/guides/data-layer.md — "Три слоя маппинга: DB → API → Domain. Каждый через extension-функцию; UseCase mapping API outcome → domain"

- **UI** — **да** — chip-list scope picker (radio Global / PerDictionaries + multi-select chip-list dictionaries) в Create-диалоге; новый Edit-диалог; downgrade-preview UI (top-3 impacted lexemes inline + «Показать все»); вынос 6+ существующих widgets из обоих screen-модулей в `:modules:widget:component_widgets` (dialogs / row widgets / fab / empty state / template resolver) + удаление дубликатов и переключение import paths. **БЕЗ figma** (`feature_has_figma=false`) — UI layout проектируется по существующим composable patterns + concept docs `ui_placement.md`.

> 📎 guide: docs/guides/ui-patterns.md — "Виджет никогда не принимает sendMessage; события через плоские callbacks (onClick, onValueChange, onConfirm)"
>
> 📎 guide: docs/guides/ui-patterns.md — "Три уровня виджетов: core/ui (примитивы) / modules/widget (несколько экранов) / screen/*/widget (один экран); зависимости строго послойно"
>
> 📎 guide: docs/guides/ui-primitives.md — "Виджет = иерархия layouts с atoms в слотах; слот именуется по семантике"

- **Data** — **да** — новый SQL метод `editComponentType(typeId, name, template, isMulti)` (UPDATE name / template / isMulti с downgrade-check SELECT + cascade `quiz_configs.component_refs` на rename + `updated_at = now()`); cardinality downgrade SELECT: `SELECT lexeme_id, COUNT(*) FROM component_values WHERE component_type_id = :typeId AND removed_at IS NULL GROUP BY lexeme_id HAVING COUNT(*) > 1`; orchestration (collision check / cascade / downgrade SELECT / soft-deleted `removed_at IS NOT NULL` check / `withTransaction`) живёт в `LexemeApiImpl` (`CoreDbApiImpl.kt:524`-area, рядом с `renameComponentType`); добавление `Removed` ветки в API outcome для `renameComponentType / softDeleteComponentType / editComponentType` (check `removed_at IS NOT NULL` перед обычной защитой `system_key IS NULL`); логи в `Migration_012_to_013` per-step (rows affected counters) и в `QuizConfigDao.updateComponentRefs` (before/after refs). Миграция M13→M14 **не требуется** (schema стабильна). [F013]

> 📎 guide: docs/guides/data-layer.md — "Timestamps в таблицах с пользовательскими данными — convention created_at / updated_at / removed_at; updated_at обновляется в repository/DAO, не БД-trigger"
>
> 📎 guide: docs/guides/data-layer.md — "Все DB-операции через withContext(Dispatchers.IO)"
>
> 📎 guide: docs/guides/logging.md — "logger.d/i/w/e с обязательным tag; сообщение лаконичное с контекстом action: data"

## Аспекты

- `edit_component` — новый CRUD-метод (UseCase + data API + sealed outcome + Reducer Edit-ветка + EditDialog UI), включает template-immutability check (на UseCaseImpl уровне — Submit с changed template vs current → `EditOutcome.TemplateImmutable` без обращения к data API) и cardinality-downgrade guard. Сигнатура: `editComponent(typeId, name, template, isMulti): EditOutcome`. [F017]

  **UI-поведение на `EditOutcome.Failure` (F028, parity с Rename/Delete Failure handling):** Reducer на `Msg.EditResult(Failure)` → close EditDialog + emit generic error snackbar (минимальный UI complexity для MVP; нет inline retry). Тест: `whenEditResultFailure_thenDialogClosed_andGenericErrorSnackbarEmitted` (см. § Tests Reducer).

> 📎 guide: docs/guides/data-layer.md — "Template immutable после релиза"

- `edit_race_with_delete` — параллельная soft-delete пока EditDialog открыт. Scenario: user открыл EditDialog для type X → в другом контексте (другой process / другой экран через cascade) type X получает `removed_at = now()` → user жмёт Submit → UseCase возвращает `EditOutcome.Removed` (data-уровень: `removed_at IS NOT NULL` check срабатывает перед update). UI-реакция: Reducer ветка `Msg.EditResult(Removed)` → close EditDialog + snackbar «Компонент удалён»; список (через `flowAllUserDefinedTypes` подписку) перерендерится автоматически без removed item. Зеркально F101/F124 invariants из phase 1 для Rename/Delete. [F007]

> 📎 guide: docs/guides/ui-patterns.md — "Snackbar / toast / vibration — one-shot side-effects, не часть data state; канон через UiHost"
>
> 📎 guide: docs/guides/effect-handlers.md — "Реактивное обновление через FlowHandler: подписка на Room Flow перерисует список при INSERT/DELETE без ручной перезагрузки"

- `cardinality_downgrade_guard` — SQL-проверка `is_multi: true → false` blocked при наличии лексем с count>1 на этом type; `EditOutcome.CardinalityDowngradeBlocked(impactedLexemeIds)` → UI preview (top-3 inline + drill-in). **Правило сортировки top-3 (deterministic для тестируемости):** `ORDER BY component_values.updated_at DESC, lexeme_id ASC` (последние затронутые сверху; tie-break по id). LIMIT 3 для preview; полный список для drill-in без LIMIT. [F010]

  **Precondition для downgrade SELECT (F018):** SELECT выполняется **ТОЛЬКО при actual transition** `new.isMulti=false AND current.isMulti=true`. Случаи когда SELECT **НЕ запускается**:
    - edit только `name` (isMulti unchanged) — SELECT skip.
    - upgrade `isMulti: false → true` — SELECT skip.
    - edit только `template`-immutability check fail (early return на UseCaseImpl) — до data API не доходит.
  Это снижает БД-нагрузку для типичного сценария (rename without cardinality change) и делает контракт явным для тестирования (2 негативных UseCaseImpl-кейса в § Tests — handler/UseCase-инвариант, spy on DAO; см. F022).

  **Edge-cases preview UI rendering (F023):** размер `impactedLexemeIds` определяет UI ветку:
    - `size == 0` — guard: возврат `EditOutcome.Success` (downgrade проходит); preview **не показывается**. Теоретически не должно случаться при корректной precondition, но фиксируем как explicit guard.
    - `1 ≤ size ≤ 3` — inline preview **всех** lexeme entries; drill-in кнопка «Показать все» **скрыта** (показывать нечего сверх inline).
    - `size > 3` — top-3 inline (per deterministic sort выше) + drill-in кнопка «Показать все» (open bottom-sheet / drill-in screen с полным списком).
  Эти три ветки покрываются reducer-тестами cardinality preview rendering (см. § Tests, F023).

> 📎 guide: docs/guides/data-layer.md — "Timestamps в таблицах с пользовательскими данными — updated_at используется для сортировки производных запросов"
>
> 📎 guide: docs/guides/testing-reducers.md — "Граничные случаи: пустые списки, NOT_IN_DB id, null значения; deterministic ordering для повторяемости тестов"

- `multi_dict_scope_picker` — Create-диалог Manager-экрана: radio Global / PerDictionaries + chip-list dictionaries с multi-select; `flowDictionaries()` подписка либо subscribe-pattern через новый FlowHandler; submit с 0 selected → disabled / NameEmpty-like error.

> 📎 guide: docs/guides/effect-handlers.md — "FlowHandler для долгоживущих подписок; обычный хендлер для одноразовых эффектов"
>
> 📎 guide: docs/guides/state-and-extensions.md — "Computed properties для derived полей: если значение выводится из других — оформлять как extension val, не хранить (canSubmit и т.п.)"

- `dictionary_chip_staleness` — выбранный chip dictionary удалён out-of-band пока CreateDialog/EditDialog открыт. Scenario: user выбрал PerDictionaries=[dict_A, dict_B] → в другом контексте dict_A удалён → приходит `Msg.DictionariesLoaded(updated)` где dict_A.id отсутствует. Best-guess Reducer behavior: фильтровать `state.createDialog.selectedDictionaryIds ∩ updated.ids` — выкинуть stale id из selection; если после фильтра selected опустел И scope=PerDictionaries → submit disabled (computed `canSubmit`); show inline-error «Выберите хотя бы один словарь». Аналогично EditDialog если он будет включать dictionary-scope (в MVP Edit не меняет scope — пункт фиксирует invariant). Тест-сценарий для UI / business sub-flow (reducer test: `whenChipDictionaryRemovedOutOfBand_thenSelectionFiltered_andSubmitDisabledIfEmpty`). [F006]

> 📎 guide: docs/guides/state-and-extensions.md — "Explicit state flags must be explicit fields in state, not computed in composable"
>
> 📎 guide: docs/guides/state-modeling.md — "State как БД: index intersection через Set, без денормализации; selectors для derived (canSubmit)"
>
> 📎 guide: docs/guides/tools-utils.md — "modifyFiltered / filter для иммутабельных модификаций коллекций в state"

- `shared_widget_module` — наполнение `:modules:widget:component_widgets` (dialogs / row widgets / empty state / fab); примитивы + callbacks API (без mate-зависимостей); удаление дубликатов из обоих screen-модулей.

> 📎 guide: docs/guides/ui-patterns.md — "Виджет никогда не принимает sendMessage; события через плоские callbacks"
>
> 📎 guide: docs/guides/code-style.md — "Не добавлять параметр на будущее; dead callback удалить"

- `per_template_architecture` — `TextWidget`, `ComponentBlock` wrapper, `ComponentByTemplate` resolver с exhaustive `when` — фундамент под будущие composite templates (MVP с одним TEXT template).

> 📎 guide: docs/guides/naming.md — "*Widget.kt для виджетов"

- `soft_deleted_removed_outcome` — разделение `BuiltInProtected` (system_key IS NOT NULL) vs `Removed` (removed_at IS NOT NULL): новый variant в `RenameOutcome`, `DeleteOutcome`, новый `EditOutcome`; Reducer ветки + snackbar «Компонент удалён» + close dialog. **Асимметрия с CreateOutcome:** Create не оперирует `existing type.id`, поэтому `Removed` там не нужен — soft-deleted name-коллизия идёт через `SameScopeCollision` / `CrossScopeCollision` (см. § Замысел задачи). [F004]

> 📎 guide: docs/guides/ui-patterns.md — "Snackbar / toast / vibration — one-shot side-effects, не часть data state; канон через UiHost"
>
> 📎 guide: docs/guides/state-modeling.md — "Sum types через sealed для взаимоисключающих outcomes; не плодить варианты которые на самом деле product (BuiltInProtected ⊥ Removed — разные оси)"

- `feature_log_tag` — shared `LogTags.COMPONENT_CONSTRUCTOR = "###ComponentConstructor###"`. **Локация (фиксированное решение, F014):** новый файл `:modules:core:logger/LogTags.kt` (shared LogTags object для feature-tag констант), параллельно с существующими per-module `LogTags.kt` в screen-модулях. Causa: feature-tag используется в **двух** screen-модулях (`components_manager` + `per_dictionary_components`) + в `:core:core-db-impl` (Migration_012_to_013) + в `app` (UseCaseImpl) — общая зависимость на `:modules:core:logger` уже есть у всех потребителей. Double-tag pattern: module-tag (`ComponentsManager` / `PerDictComponents`) для debug-логов + feature-tag (`###ComponentConstructor###`) для smoke-фильтрации.

> 📎 guide: docs/guides/logging.md — "###СЛОВО### — тройные решётки, БОЛЬШИЕ буквы; каждый модуль имеет LogTags.kt; использовать ТОЛЬКО константы"

- `migration_logging` — per-step counters в `Migration_012_to_013` (rows affected на каждый шаг) + `QuizConfigDao.updateComponentRefs` cascade + prefs reset. **Группировка фиксирована: 9 атомарных шагов (F020):**
    1. `renameComponentTypesRemoveDate` — rename `component_types.remove_date → removed_at` (rows affected).
    2. `addComponentTypesNewColumns` — ADD `is_multi` / `created_at` / `updated_at` (rows backfilled).
    3. `dropUniqueComponentTypesDictName` — DROP unique index `(dictionary_id, name)` (success/failure).
    4. `addComponentValuesNewColumns` — ADD `component_values.removed_at` (+ другие новые columns; rows count).
    5. `dropUniqueComponentValuesLexemeType` — DROP unique index `(lexeme_id, component_type_id)` (success/failure).
    6. `createComponentValuesLexemeIdIndex` — CREATE INDEX `(lexeme_id)` (success/failure).
    7. `consolidateLongTextTemplateKey` — `long_text → text` template consolidation (rows affected).
    8. `rewriteTextJson` — JSON rewrite text values (rows rewritten / skipped).
    9. `rewriteImageJson` — JSON rewrite image values (rows rewritten / skipped).

> 📎 guide: docs/guides/logging.md — "Запрещено android.util.Log напрямую; всё через LexemeLogger"
>
> 📎 guide: docs/guides/prefs-datastore.md — "Запись перед чтением: для ключей от которых зависит UI запись на SplashScreen; race condition обрабатывать через nullable Flow"

- `test_pass` — UseCase unit-тесты на новые ветки `EditOutcome` / `Removed` + API→domain mapping; Reducer-тесты на Edit-ветку (race / epoch / cardinality preview / mutual exclusion F138 / chip-staleness); тесты multi-dict scope flow.

> 📎 guide: docs/guides/testing-reducers.md — "Всегда проверять и стейт И эффекты в каждом тесте; testReduce / testScenario; assertSingleEffect"
>
> 📎 guide: docs/guides/testing-extensions.md — "Тесты расширений: основная функциональность + иммутабельность остальных полей + Given/When/Then"

## Затронутые файлы

### Domain (`:modules:domain:lexeme`)
- **NEW** `EditOutcome.kt` (рядом с `CreateOutcome.kt` — все CRUD outcomes в одном модуле). **Решение принято** (см. § Замысел задачи + F003): размещение зафиксировано, Q3 снят. [F003]
- **MODIFY** `RenameOutcome.kt` — добавить `data object Removed`.
- **MODIFY** `DeleteOutcome.kt` — добавить `data object Removed`.

> 📎 guide: docs/guides/state-modeling.md — "Dependency Rule моделей: Feature → Domain → Library; внутрь можно, наружу нельзя"
>
> 📎 guide: docs/guides/naming.md — "Domain enum/sealed — reuse из API, не дублировать"

### Data API (`core/core-db-api`)
- **MODIFY** `CoreDbApi.kt` — добавить `LexemeApi.editComponentType(typeId, name, template, isMulti): EditComponentOutcome` (template принимается — immutability check на UseCase уровне, см. § Замысел задачи + F017); добавить `Removed` variant в `RenameComponentOutcome` / `SoftDeleteComponentOutcome`.
- **MODIFY** `entity/ComponentOutcomeApiEntity.kt` — добавить sealed `EditComponentOutcome` **в существующий файл рядом с Create/Rename/SoftDelete outcomes** (file-locality convention, см. F001 — все API outcomes для component CRUD в одном файле). [F001]

  **Полный API-level перечень вариантов `EditComponentOutcome` (F015, F027 — 7 вариантов, parity с Create/Rename/SoftDelete API outcome pattern):**
    - `Success` — UPDATE прошёл, cascade quiz_configs выполнен (если name изменился).
    - `SameScopeCollision` — name уже занят в том же scope (dictionary_id + system_key IS NULL фильтр; removed_at IS NULL).
    - `CrossScopeCollision` — name занят в global / другом dict (concept policy phase 1).
    - `CardinalityDowngradeBlocked(impactedLexemeIds: List<Long>)` — downgrade `isMulti: true→false` заблокирован SELECT-ом (см. § Аспекты `cardinality_downgrade_guard`).
    - `TemplateImmutable` — попытка изменить template (UseCase-level check; на API уровне может возвращаться для defensive parity).
    - `BuiltInProtected` — `type.system_key IS NOT NULL` (нельзя редактировать встроенный).
    - `Removed` — `type.removed_at IS NOT NULL` (soft-deleted, нельзя редактировать; не путать с BuiltInProtected, см. F004).

  **Не входит в API (F027):**
    - `NameEmpty` — валидация на UseCaseImpl уровне (`trimmed.isBlank()` → domain `EditOutcome.NameEmpty` без обращения к API).
    - `Failure(cause: Throwable)` — try-catch на UseCaseImpl (mapping exception → domain `EditOutcome.Failure(cause)`).

> 📎 guide: docs/guides/naming.md — "API DTO data class → суффикс ApiEntity; API enum → без суффикса"
>
> 📎 guide: docs/guides/data-layer.md — "Template immutable: immutability check на UseCase уровне (business rule, не структурный invariant)"

### Data impl (`core/core-db-impl`)
- **MODIFY** `me/apomazkin/core_db_impl/CoreDbApiImpl.kt` (`LexemeApiImpl`) — **orchestration `editComponentType`** живёт здесь, рядом с `renameComponentType` (`CoreDbApiImpl.kt:524`). Включает: (a) `withTransaction` обёртку; (b) загрузку current type (NotFound → Failure); (c) `removed_at IS NOT NULL` check → `Removed`; (d) `system_key IS NOT NULL` check → `BuiltInProtected`; (e) name-collision check (SameScope / CrossScope) если name изменился; (f) cardinality downgrade SELECT при actual transition (см. § Аспекты `cardinality_downgrade_guard`); (g) UPDATE через DAO; (h) cascade `quiz_configs.component_refs` если name изменился (parity с `renameComponentType`); (i) feature-tag log на entry / exit с outcome. [F013]

> 📎 guide: docs/guides/data-layer.md — "Все DB-операции через withContext(Dispatchers.IO); orchestration в LexemeApiImpl (CoreDbApi имплементация), не в UseCase"
>
> 📎 guide: docs/guides/state-modeling.md — "Порядок sum-вариантов в защитной цепочке: Removed (soft-delete) проверяется до BuiltInProtected (system_key) — разные оси, обе должны блокировать update до бизнес-валидации"
>
> 📎 guide: docs/guides/logging.md — "Entry/exit логи с outcome через LexemeLogger + feature-tag константу; уровень DEBUG для smoke-фильтрации"

- **MODIFY** `room/dao/ComponentTypeDao.kt` (либо аналог) — новый `editComponentType` DAO метод (плоский UPDATE name / template / isMulti / updatedAt без оркестрации) + DAO method для cardinality downgrade SELECT.
- **MODIFY** `room/dao/QuizConfigDao.kt` — лог в `updateComponentRefs` (before/after refs count) — точечная правка, без сигнатуры.
- **MODIFY** `room/migrations/Migration_012_to_013.kt` — лог per-step (9 шагов по списку из § Аспекты `migration_logging` — F020).

> 📎 guide: docs/guides/testing-migrations.md — "Schema не меняется (M13 версия стабильна) → новые migration tests НЕ нужны; правки только в логировании per-step, существующий тест M12→M13 покрывает миграцию"

> 📎 guide: docs/guides/naming.md — "R-N-002: snake_case для имён колонок БД через @ColumnInfo"
>
> 📎 guide: docs/guides/data-layer.md — "Timestamps: updated_at обновляется в repository/DAO"

### Business (`:modules:screen:components_manager`)
- **MODIFY** `deps/ComponentsManagerUseCase.kt` — добавить `editComponent(typeId, name, template, isMulti): EditOutcome` + `flowDictionaries(): Flow<List<DictionaryApiEntity>>` (использует existing `DictionaryApiEntity` из `core-db-api`; UseCaseImpl делегирует на `dictionaryApi.flowDictionaryList()` без mapping — F026). [F017, F026]
- **MODIFY** `mate/State.kt` — `EditDialogState` (включая поле `template: ComponentTemplate` для UI control) + `availableDictionaries: List<DictionaryApiEntity>` + `impactedLexemes: List<Long>?` на CardinalityDowngradeBlocked preview. [F026]
- **MODIFY** `mate/Msg.kt` — Edit family (`Open/Close/Submit/EditNameChange/EditTemplateChange/EditMultiToggle/EditResult`) + `Msg.DictionariesLoaded`. [F017]
- **MODIFY** `mate/Reducer.kt` — Edit-ветка с epoch correlation + mutual-exclusion с Create/Rename/Delete (F138) + downgrade preview branch + dictionary-chip staleness filtering on `DictionariesLoaded`.
- **MODIFY** `mate/DatasourceEffect.kt` — `EditComponent(epoch, typeId, name, template, isMulti)` + (опц.) `SubscribeDictionaries` если subscribe-pattern. [F017]
- **MODIFY** `mate/DatasourceEffectHandler.kt` — handler ветка для EditComponent.
- **NEW** `mate/DictionariesFlowHandler.kt` — если subscribe-pattern выбран (parity с `AllUserDefinedTypesFlowHandler`).

> 📎 guide: docs/guides/effect-handlers.md — "Один FlowHandler = одна подписка (один job); первый emit может маркироваться отдельным Msg (Ready vs Updated)"
>
> 📎 guide: docs/guides/naming.md — "*EffectHandler / *FlowHandler конвенция файлов; имя класса совпадает с именем файла"

- **MODIFY** `LogTags.kt` — оставить module-tag, использовать `###ComponentConstructor###` параллельно (двойная ось).

> 📎 guide: docs/guides/data-layer.md — "UseCase интерфейс в feature модуле, реализация в app модуле"
>
> 📎 guide: docs/guides/state-and-extensions.md — "Иерархическая вложенность; nullable dialog states; явные поля на state"
>
> 📎 guide: docs/guides/messages.md — "Sealed interface Msg; внутренние UiMsg; data object для действий без данных, data class для параметров"
>
> 📎 guide: docs/guides/reducer-patterns.md — "Reducer-логика — inline chain через extension chain, не private методы"
>
> 📎 guide: docs/guides/effect-handlers.md — "Sealed interface для DatasourceEffect; consumer вызывается только при полезном msg"
>
> 📎 guide: docs/guides/effect-handlers.md — "FlowHandler subscribe/unsubscribe; collectLatest или collect; один FlowHandler = одна подписка"
>
> 📎 guide: docs/guides/logging.md — "Использовать ТОЛЬКО константы LogTags; НЕ хардкодить строки"

### Business (`:modules:screen:per_dictionary_components`)
- **MODIFY** `deps/PerDictionaryComponentsUseCase.kt` — добавить `editComponent(typeId, name, template, isMulti): EditOutcome` (без multi-dict picker — scope hardcoded current dict). [F017]
- **MODIFY** `mate/State.kt` / `mate/Msg.kt` / `mate/Reducer.kt` / `mate/DatasourceEffect.kt` / `mate/DatasourceEffectHandler.kt` — Edit family (зеркально Manager-экрану, минус scope picker; включая `EditTemplateChange`).

> 📎 guide: docs/guides/mate-framework.md — "Файловая структура фичи: logic/ (State.kt, Message.kt, Reducer.kt, DatasourceEffectHandler.kt) + deps/ (UseCase.kt) + ui/"

### Business impl (`app/src/main/java/.../di/module`)
- **MODIFY** `componentsmanager/ComponentsManagerUseCaseImpl.kt` — реализация `editComponent` (включая **template-immutability gate**: сравнить `template` параметра с current `type.template` → если mismatch вернуть `EditOutcome.TemplateImmutable` без обращения к data API) + `flowDictionaries` (через `dictionaryApi.flowDictionaryList()`) + mapping API `Removed` → domain `Removed` для rename / softDelete / edit; feature-tag в каждом методе. [F017]
- **MODIFY** `perdictionarycomponents/PerDictionaryComponentsUseCaseImpl.kt` — реализация `editComponent` (делегирует на shared CRUD; template-immutability gate тут же); feature-tag.
- **MODIFY** `componentsmanager/ComponentsManagerModule.kt` — DI для `flowDictionaries` deps (если нужно).
- **MODIFY** `perdictionarycomponents/PerDictionaryComponentsModule.kt` — параллельно.

> 📎 guide: docs/guides/data-layer.md — "Нормализация текстового ввода (trim) в UseCaseImpl — единая точка для всех write-методов"
>
> 📎 guide: docs/guides/dagger-di.md — "@Module + @Binds для UseCase: interface XxxModule { @Binds fun bindUseCase(impl: XxxUseCaseImpl): XxxUseCase }"

### UI / Widgets (`:modules:widget:component_widgets`)
- **NEW** `dialogs/CreateComponentDialog.kt` — примитивы + callbacks (name, isMulti, scope, availableDictionaries, nameError, isCreating, onNameChange, onMultiToggle, onScopeChange, onDictionaryToggle, onSubmit, onDismiss).
- **NEW** `dialogs/RenameComponentDialog.kt` — primitives + callbacks.
- **NEW** `dialogs/DeleteComponentConfirmDialog.kt` — primitives + callbacks.
- **NEW** `dialogs/EditComponentDialog.kt` — primitives + callbacks (name, template, isMulti, impactedLexemes preview, isEditing, onNameChange, onTemplateChange, onMultiToggle, onSubmit, onDismiss; включая downgrade preview).
- **NEW** `widgets/UserDefinedRowWidget.kt` (либо параметризованный `ComponentRowWidget.kt` + `PerDictRowWidget.kt`).
- **NEW** `widgets/ComponentsEmptyStateWidget.kt`, `widgets/CreateComponentFab.kt`, `widgets/ComponentTemplateLabel.kt`, `widgets/NameErrorLabel.kt`.
- **NEW** `templates/TextWidget.kt` — per-template composable.
- **NEW** `templates/ComponentBlock.kt` — `name + content` wrapper.
- **NEW** `templates/ComponentByTemplate.kt` — resolver с exhaustive `when` по `ComponentTemplate`.

> 📎 guide: docs/guides/ui-patterns.md — "Виджет никогда не принимает sendMessage; плоские callbacks (onClick, onValueChange, onConfirm)"
>
> 📎 guide: docs/guides/ui-patterns.md — "Не передавать лишних параметров для callback'ов: если параметр используется только чтобы построить Msg — он не нужен виджету"
>
> 📎 guide: docs/guides/code-style.md — "KDoc виджета НЕ описывает контекст использования; параметры без избыточных префиксов"
>
> 📎 guide: docs/guides/naming.md — "*Widget.kt для виджетов"
>
> 📎 guide: docs/guides/ui-primitives.md — "Виджет = иерархия layouts с atoms в слотах; слот именуется по семантике"

### UI cleanup (`:modules:screen:components_manager` / `per_dictionary_components`)
- **DELETE** `widget/CreateComponentDialog.kt`, `widget/RenameComponentDialog.kt`, `widget/DeleteComponentConfirmDialog.kt`, `widget/UserDefinedRowWidget.kt` (или `PerDictRowWidget.kt`), `widget/ComponentsEmptyStateWidget.kt`, `widget/CreateComponentFab.kt`, `widget/ComponentTemplateLabel.kt`, `widget/NameErrorLabel.kt` в обоих модулях (после переноса в shared).
- **MODIFY** `ComponentsManagerScreen.kt` / `PerDictionaryComponentsScreen.kt` — поменять import paths на `:modules:widget:component_widgets`; добавить Edit-диалог; в Manager — multi-dict scope chip-list.

> 📎 guide: docs/guides/ui-patterns.md — "Условные оверлеи: диалоги и bottom sheet по флагам стейта (if (state.X.show) {...})"

### Логгер (`:modules:core:logger`)
- **NEW** `:modules:core:logger/LogTags.kt` — shared object с константой `LogTags.COMPONENT_CONSTRUCTOR = "###ComponentConstructor###"`. Решение зафиксировано (F014): отдельный shared файл, потому что feature-tag используется в `components_manager` + `per_dictionary_components` + `core-db-impl` (Migration) + `app` (UseCaseImpl) — общая зависимость на `:modules:core:logger` уже есть. Per-module `LogTags.kt` в screen-модулях остаются как есть (двойная ось: module-tag для debug + feature-tag для smoke).

> 📎 guide: docs/guides/logging.md — "Новый модуль = создай LogTags.kt с константой; ###СЛОВО### формат"

### Spec
- **MODIFY** `docs/features-spec/component-constructor.md` — отражает Edit-flow / multi-dict picker / shared widget module / новые outcomes (`Removed` / `EditOutcome`) / cardinality downgrade / immutable template / feature-tag.

### Tests
- **NEW** `app/src/test/.../componentsmanager/ComponentsManagerUseCaseImplTest.kt` (extend):
  - `editComponent` ветки (Success / NameEmpty / SameScopeCollision / CrossScopeCollision / CardinalityDowngradeBlocked / TemplateImmutable / BuiltInProtected / Removed / Failure).
  - **Template immutability gate (F017):** `whenSubmitEditWithChangedTemplate_thenTemplateImmutable_andDataApiNotCalled` — UseCaseImpl возвращает `EditOutcome.TemplateImmutable` без вызова `lexemeApi.editComponentType`.
  - **API→domain mapping для Removed (F012):**
    - `whenRenameApiReturnsRemoved_thenDomainRenameOutcomeRemoved`
    - `whenSoftDeleteApiReturnsRemoved_thenDomainDeleteOutcomeRemoved`
    - `whenEditApiReturnsRemoved_thenDomainEditOutcomeRemoved` (parity для нового outcome).
  - **Cardinality downgrade SELECT precondition (F018, перенесено из ReducerTest по F022):** инвариант handler/UseCaseImpl-уровня — Reducer его проверить не может (только эмитит DatasourceEffect). Тесты используют spy на DAO downgrade SELECT (либо mock `lexemeApi.editComponentType` с verify-no-interactions на cardinality DAO method):
    - `whenEditUpgradesIsMulti_thenDowngradeSelectNotCalled` — Submit с `new.isMulti=true AND current.isMulti=false` → UseCase/orchestration НЕ вызывает downgrade SELECT (verify mock DAO method ни разу не вызван).
    - `whenEditOnlyName_thenDowngradeSelectNotCalled` — Submit с `new.isMulti=current.isMulti` (только name изменился) → downgrade SELECT НЕ запускается.
- **NEW** `app/src/test/.../perdictionarycomponents/PerDictionaryComponentsUseCaseImplTest.kt` (extend) — `editComponent` ветки + аналогичные mapping-тесты для Removed + template-immutability gate + аналогичные precondition тесты `whenEditUpgradesIsMulti_thenDowngradeSelectNotCalled` / `whenEditOnlyName_thenDowngradeSelectNotCalled` (F022).

> 📎 guide: docs/guides/reducer-patterns.md — "Reducer = чистая функция; сайд-эффекты живут в EffectHandler. Reducer тестируется без mock'ов — precondition «DAO method не вызывался» проверяется в UseCaseImpl-тестах со spy/mock на DAO"
>
> 📎 guide: docs/guides/code-style.md — "Минимализм API (YAGNI): не запускать DB-запрос если бизнес-правило его не требует; precondition guard на UseCase / handler уровне"
>
> 📎 guide: docs/guides/testing-extensions.md — "Тесты UseCase / sealed branch: основная функциональность + Given/When/Then; verify-no-interactions на DAO для precondition skip-веток"

- **NEW** `modules/screen/components_manager/src/test/.../mate/ComponentsManagerReducerTest.kt` (extend):
  - Edit-ветка: open / submit / result handling + epoch correlation + cardinality preview rendering edge-cases (UI state transitions — что хранится в state, что показывается).

> 📎 guide: docs/guides/reducer-patterns.md — "Reducer обязан логировать prevState / message / newState / effects; epoch correlation guard ветки"
>
> 📎 guide: docs/guides/testing-reducers.md — "Один тест-класс на группу сообщений; testScenario для многошаговых flow (open → submit → result)"

  - **Mutual exclusion F138 (F008)** — конкретные reducer-кейсы:
    - `whenCreateDialogOpen_thenOpenEditDialog_dropped`
    - `whenRenameInFlight_thenSubmitEdit_dropped`
    - `whenDeleteConfirmOpen_thenOpenEditDialog_dropped`
    - Симметричные обратные: `whenEditDialogOpen_thenOpenCreateDialog_dropped`, `whenEditDialogOpen_thenOpenRenameDialog_dropped`, `whenEditDialogOpen_thenOpenDeleteConfirm_dropped`, `whenEditInFlight_thenSubmitRename_dropped`.

> 📎 guide: docs/guides/state-modeling.md — "Sum types через sealed для взаимоисключающих диалогов; mutual exclusion = sum, не product (исключает невалидные комбинации)"
  - **Dictionary-chip staleness (F006):** `whenChipDictionaryRemovedOutOfBand_thenSelectionFiltered_andSubmitDisabledIfEmpty`.
  - **Edit-race-with-delete (F007):** `whenEditResultRemoved_thenDialogClosed_andRemovedSnackbarEmitted`.
  - **Rename/Delete close-on-Removed parity (F019):**
    - `whenRenameResultRemoved_thenDialogClosed_andRemovedSnackbarEmitted` — зеркало EditResult(Removed) для Rename-flow.
    - `whenDeleteResultRemoved_thenDialogClosed_andRemovedSnackbarEmitted` — зеркало EditResult(Removed) для Delete-flow.
  - **Cardinality preview rendering edge-cases (F023)** — UI state transitions при `EditResult.CardinalityDowngradeBlocked(impactedLexemeIds)`:
    - `whenImpactedSizeOneToThree_thenInlineFull_andDrillInHidden` — при `1 ≤ size ≤ 3` state содержит все impacted в inline preview, флаг drill-in кнопки = false (скрыта).
    - `whenImpactedSizeMoreThanThree_thenTop3Inline_andDrillInVisible` — при `size > 3` state содержит top-3 в inline preview, флаг drill-in кнопки = true (видна, открывает полный список).
  - **Edit Failure handling (F028)** — `whenEditResultFailure_thenDialogClosed_andGenericErrorSnackbarEmitted` — на `Msg.EditResult(Failure)` Reducer закрывает EditDialog (`editDialog = null`) и эмитит generic error snackbar effect (parity с Rename/Delete Failure handling; см. § Аспекты `edit_component`).
  - **DictionariesLoaded не мутирует EditDialogState (F030)** — `whenEditDialogOpen_thenDictionariesLoaded_doesNotMutateEditState` — инвариант: `Msg.DictionariesLoaded(updated)` при открытом EditDialog обновляет `availableDictionaries` и фильтрует только `createDialog.selectedDictionaryIds` (chip-staleness); поля `editDialog` (name / template / isMulti / impactedLexemes / isEditing) остаются неизменными. Регрессия если общая Reducer-ветка случайно применит filter к Edit.

> 📎 guide: docs/guides/state-and-extensions.md — "Explicit state flags must be explicit fields in state, not computed in composable"
>
> 📎 guide: docs/guides/testing-reducers.md — "Граничные случаи: пустые списки, NOT_IN_DB id, null значения; deterministic ordering для повторяемости тестов"

  **Note (F022):** Precondition «downgrade SELECT не запускается при non-transition» — НЕ reducer-уровень (Reducer только эмитит DatasourceEffect, проверить факт «SELECT не вызвался» невозможно из Reducer-теста). Тесты `whenEditUpgradesIsMulti_*` / `whenEditOnlyName_*` перенесены в UseCaseImplTest со spy на DAO. См. § Tests → UseCaseImplTest выше. [F025: typo fix]

> 📎 guide: docs/guides/code-style.md — "Минимализм API (YAGNI): не запускать DB-запрос если бизнес-правило его не требует; precondition guard на UseCase / handler уровне"
  - Multi-dict scope state transitions (`CreateScopeChange` + `CreateDictionaryToggle`).
- **NEW** `modules/screen/per_dictionary_components/src/test/.../mate/PerDictionaryComponentsReducerTest.kt` (extend) — Edit-ветка зеркально (без mutual exclusion с multi-dict scope, но включая edit_race_with_delete + Rename/Delete Removed parity + cardinality preview rendering edge-cases F023). Precondition negative cases — в UseCaseImplTest (F022), не здесь.
- Migration tests — НЕ нужны (M13 schema не меняется).

> 📎 guide: docs/guides/testing-reducers.md — "testReduce / testScenario / assertSingleEffect / assertNoEffects; всегда проверять state И эффекты"
>
> 📎 guide: docs/guides/naming.md — "Тесты — *Test.kt; метод теста — backtick-string"

## Релевантные спеки и гайды

- `docs/features-spec/component-constructor.md` — phase 1 spec (update target).
- `docs/features/IS481_component_constructor/concept/template_model.md` — Open Q9 (downgrade guard), Q10 (template immutable).
- `docs/features/IS481_component_constructor/concept/ui_placement.md` — Create-form требования (multi-dict scope picker).
- `docs/features/IS481_component_constructor/concept/typed_views.md` — Tier 2 widget module + per-template architecture.
- `docs/features/IS481_component_constructor/concept/deletion_concept.md` — soft-delete cascade (для `Removed` semantics).
- `docs/features/IS481_component_constructor/__plan_vs_reality.md` — полный список расхождений (источник 5 пунктов).
- `docs/features/IS481_component_constructor/business_contract_spec.md` — invariants `[shape]/[transition]/[correlation]` (F101/F124/F136/F138/F140) — Edit-ветка обязана им следовать.
- `docs/guides/mate-framework.md` — Reducer / Effect / FlowHandler patterns.
- `docs/guides/reducer-patterns.md` — epoch correlation, mutual exclusion (F138), guard ветки.
- `docs/guides/effect-handlers.md` — DatasourceEffect handler patterns.
- `docs/guides/state-modeling.md` + `docs/guides/state-and-extensions.md` — explicit flags (isEditing), nullable dialog states.
- `docs/guides/messages.md` — sealed Msg / UiMsg conventions.
- `docs/guides/testing-reducers.md` — Edit-ветка testing.
- `docs/guides/testing-extensions.md` — UseCase / sealed branch testing.
- `docs/guides/data-layer.md` — Room DAO patterns, transactions, cascade JSON updates.
- `docs/guides/prefs-datastore.md` — prefs reset best-effort (для логирования reset failures).
- `docs/guides/ui-primitives.md` + `docs/guides/ui-patterns.md` — primitives + callbacks dialog API.
- `docs/guides/naming.md` + `docs/guides/code-style.md` — naming для новых файлов и sealed branches.
- `docs/guides/dagger-di.md` — DI деривация (если нужен новый dep flow для dictionaries).
- `docs/guides/logging.md` — feature-tag pattern.
- `docs/features-spec/logger.md` — module-tag vs feature-tag convention.
- `docs/guides/testing-migrations.md` — НЕ нужен (миграция не меняется).
- `docs/guides/navigation.md` — НЕ затронут.
- `docs/guides/prefs-datastore.md` — побочно (только логи в reset).

## Sub-flow для запуска

| Sub-flow | Запускать? | Обоснование |
|---|---|---|
| Infrastructure | да | `LogTags.COMPONENT_CONSTRUCTOR` константа в shared `:modules:core:logger/LogTags.kt` (NEW файл, F014); DI-связки для `flowDictionaries` в `ComponentsManagerModule`; (опц.) compose-tooling dep + ревизия существующих deps в `:modules:widget:component_widgets/build.gradle.kts` (build файл уже содержит android{} + base deps — не «наполнение пустого», а тонкая правка). [F002] |
| Business | да | Новый `EditOutcome` sealed, `Removed` variant в Rename/Delete outcomes, новые UseCase методы (`editComponent(typeId, name, template, isMulti)`, `flowDictionaries`), Reducer Edit-ветка (6 Msg families включая EditTemplateChange) + mutual exclusion F138 + chip-staleness filter, DatasourceEffect Edit + handler ветка, template-immutability gate в UseCaseImpl. Самый объёмный слой phase 2. [F017] |
| UI | да | Multi-dict scope picker (chip-list), Edit-диалог, downgrade preview UI (top-3 с deterministic sort), вынос 8+ widgets в shared module (примитивы + callbacks), per-template resolver (`TextWidget` / `ComponentBlock` / `ComponentByTemplate`), import-path рефакторинг в обоих screen-модулях. |
| Data | да | `editComponentType` SQL + orchestration в `LexemeApiImpl` (`CoreDbApiImpl.kt:524`-area, F013): collision / cascade quiz_configs / cardinality downgrade SELECT с precondition `new.isMulti=false AND current.isMulti=true` (F018) + `ORDER BY updated_at DESC, lexeme_id ASC LIMIT 3` для preview / soft-deleted check / withTransaction; `Removed` variant на API outcome (`removed_at IS NOT NULL` check); полный API-перечень `EditComponentOutcome` (7 вариантов, F015 + F027); логи в `Migration_012_to_013` per-step 9 шагов (F020) + `QuizConfigDao.updateComponentRefs` + prefs reset. **Без новой миграции M13→M14.** |

## Open questions

1. **Создание подмодуля `:modules:widget:component_widgets/dialogs` vs flat structure?**  
   *Best-guess:* flat package `me.apomazkin.component_widgets.dialogs` / `.widgets` / `.templates` внутри одного gradle-модуля (как сейчас задано в build.gradle.kts namespace `me.apomazkin.component_widgets`). Альтернатива — три отдельных gradle-модуля (`:modules:widget:component_dialogs` / `:component_rows` / `:component_templates`) — over-engineering для MVP, отложить до композитных templates.

> 📎 guide: docs/guides/naming.md — "Пакеты: me.apomazkin.<module>.ui / .deps / .logic / .entity"

2. **Миграция M13→M14 — действительно НЕ нужна?**  
   *Best-guess:* НЕ нужна. Все изменения phase 2 — на уровне SQL-логики (`editComponentType` — новый UPDATE с downgrade-check SELECT) и Kotlin-маппинга (`removed_at IS NOT NULL → Removed`). Schema (columns / indices / constraints) стабильна. Если data-sub-flow найдёт необходимость новой column / index (например `last_edited_by` или history-table) — поднять M14 bump. **Альтернатива:** превентивно поднять M14 ради future-readiness — нарушает YAGNI, не делать.

> 📎 guide: docs/guides/code-style.md — "Минимализм API (YAGNI): не добавлять параметр / поле на будущее"

3. **Shared dialogs API: примитивы + callbacks vs mate-state pattern?**  
   *Best-guess:* примитивы + callbacks (как явно сказано в task.md § «Acceptance» п.3). Дает full control screen-модулю над state lifecycle и тестируемость shared-диалогов изолированно. Альтернатива — передавать целиком `CreateDialogState` — coupling shared widget на screen-specific state shape, нарушает Dependency Rule. Сохранить как гард в design.

> 📎 guide: docs/guides/ui-patterns.md — "Виджет никогда не принимает sendMessage; события через плоские callbacks; не передавать лишних параметров"

4. **Source of truth для `availableDictionaries` — push (FlowHandler subscribe) vs pull (LoadDictionaries on Open)?**  
   *Best-guess:* push через новый `DictionariesFlowHandler` (parity с `AllUserDefinedTypesFlowHandler`) — отражает live-обновления при add/delete dictionary out-of-band и автоматически триггерит `dictionary_chip_staleness` filtering в Reducer. Альтернатива — pull на `OpenCreateDialog` — проще, но stale если user создаст/удалит dictionary в другом tab и вернётся. Дёшево сделать push — выбрать его.

> 📎 guide: docs/guides/effect-handlers.md — "Реактивное обновление данных через FlowHandler: если экран должен автоматически обновляться при изменении данных — используй MateFlowHandler с подпиской на Room Flow"

5. **Cardinality downgrade preview UI: inline (top-3 lexemes список) vs drill-in bottom-sheet?**  
   *Best-guess:* inline top-3 + «Показать все» drill-in (или bottom-sheet) — как описано в task.md acceptance. Альтернатива — только error snackbar без preview — UX-беднее, не показывает что чинить. Делать как в task.md. Сортировка top-3 фиксирована в § Аспекты `cardinality_downgrade_guard` (deterministic `ORDER BY updated_at DESC, lexeme_id ASC`).

> 📎 guide: docs/guides/ui-primitives.md — "Виджет = иерархия layouts с atoms в слотах; column/row + явные именованные слоты вместо тегов"
>
> 📎 guide: docs/guides/ui-patterns.md — "Условные оверлеи: диалоги и bottom sheet по флагам стейта (if (state.X.show) {...})"

6. **Cascade `quiz_configs.component_refs` на rename внутри `editComponent` — переиспользовать существующий cascade-helper из `renameComponentType` SQL?**  
   *Best-guess:* да, переиспользовать (DRY); если в `Migration_012_to_013` / DAO существует utility — extract в общий helper / private function. Альтернатива — дублировать SQL — penalty на maintenance. Решит data-sub-flow design.

> 📎 guide: docs/guides/data-layer.md — "Три слоя маппинга через extension-функции; общая helper-функция для cascade JSON updates"

7. **Multi-dict scope picker UX: `Scope.PerDictionaries(emptyList())` submit-кнопка disabled vs показ error «Выберите хотя бы один словарь»?**  
   *Best-guess:* submit disabled (preventive UX) — как в task.md примере «либо `CreateOutcome.NameEmpty`-like ошибка»; visible affordance дешевле. Альтернатива — позволить submit и показать error post-factum — UX хуже. UI-sub-flow финализирует. Edge-case staleness описан в § Аспекты `dictionary_chip_staleness`.

> 📎 guide: docs/guides/state-and-extensions.md — "Computed properties для derived полей (canSubmit из name.isNotEmpty() && !isLoading); composable не вычисляет"

## context_output
- infra_touched: true
- business_touched: true
- ui_touched: true
- data_touched: true
- needs_tests: true
- needs_migration_tests: false
- feature_has_ui_contract: true
- spec_filename: component-constructor.md

## log_messages

- iter 5 closed 5 approved findings: F025 (typo fix `whenEditUpgradesIsMulli_*` → `whenEditUpgradesIsMulti_*` в Note F022 cross-reference), F026 (тип `availableDictionaries` зафиксирован как existing `DictionaryApiEntity` из `core-db-api`; UseCase сигнатура `flowDictionaries(): Flow<List<DictionaryApiEntity>>`; UseCaseImpl делегирует на `dictionaryApi.flowDictionaryList()` без mapping — все упоминания `DictionaryEntry` в State / UseCase заменены).
- iter 5 closed F027 (API-level `EditComponentOutcome` приведён к existing pattern: 7 вариантов вместо 9 — убраны `NameEmpty` (валидация в UseCaseImpl) и `Failure(cause)` (try-catch в UseCaseImpl → domain Failure); явный note о том что не входит в API).
- iter 5 closed F028 + F030: § Аспекты `edit_component` фиксирует UI-поведение на `EditOutcome.Failure` (close dialog + generic snackbar, parity с Rename/Delete); в § Tests Reducer добавлены `whenEditResultFailure_thenDialogClosed_andGenericErrorSnackbarEmitted` (F028) и `whenEditDialogOpen_thenDictionariesLoaded_doesNotMutateEditState` (F030, инвариант: DictionariesLoaded не мутирует EditDialogState).
- Состояние: 25 approved findings закрыто (iter 1-5: F001-F004, F006-F008, F010, F012-F015, F017-F020, F022-F023, F025-F028, F030), 7 rejected (F005, F009, F011, F016, F021, F024, F029). Open questions без изменений (7 пунктов).

_model: claude-opus-4-7[1m]_
