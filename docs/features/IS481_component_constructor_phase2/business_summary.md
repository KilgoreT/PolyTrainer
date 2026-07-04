---
status: done
---

# Summary — business

Business sub-flow для IS481 component_constructor phase 2 пройден до конца: `walkthrough` → `contract` → `contract_review` (1 inline-fix) → `contract_spec` → `design_tree` (1 inline-fix) → `test` (75 TDD-skeleton tests) → `implement` (22 узла; `assembleDebug` + `testDebugUnitTest` PASS) → `publish_spec`. Все 19 implementation-узлов реализованы; UI-узлы переданы в ui sub-flow как декларация зависимостей (не реализуются здесь).

## Что сделано

### Domain (`:modules:domain:lexeme`)

- `EditOutcome.kt` [+] — **NEW** sealed interface с 9 вариантами: `Success(updated: ComponentType)` / `NameEmpty` / `SameScopeCollision` / `CrossScopeCollision` / `CardinalityDowngradeBlocked(impactedLexemeIds: List<Long>)` / `TemplateImmutable` / `BuiltInProtected` / `Removed` / `Failure(cause)`. `Success` зеркалит `RenameOutcome.Success(ComponentType)` (F-BCR1 финализировано в contract_review).
- `RenameOutcome.kt` [~] — добавлен `data object Removed` (F004 — асимметрия с `BuiltInProtected`; soft-deleted ловится отдельной веткой, не collapsed в BuiltInProtected).
- `DeleteOutcome.kt` [~] — добавлен `data object Removed` (повторный soft-delete уже-removed type).
- `CreateOutcome` — **не трогаем** (асимметрия: Create не оперирует existing `type.id`; soft-deleted name-коллизия покрывается existing Same/CrossScopeCollision).

### API (`core/core-db-api`)

- `entity/ComponentOutcomeApiEntity.kt` [~] — **NEW** `sealed interface EditComponentOutcome` (7 вариантов; **без** `NameEmpty` и `Failure` — валидация и try-catch на UseCaseImpl, F027). Добавлен `data object Removed` в `RenameComponentOutcome` и `SoftDeleteComponentOutcome`.
- `CoreDbApi.kt` [~] — `LexemeApi` получил `suspend fun editComponentType(typeId: Long, name: String, template: ComponentTemplate, isMulti: Boolean): EditComponentOutcome`. KDoc фиксирует: cascade `quiz_configs.component_refs` на rename, cardinality downgrade SELECT только при `isMulti=false AND current.isMulti=true` (F018), template-immutability — defensive на API + основной gate на UseCaseImpl.

### Data impl (вне business design tree, но необходим для компиляции/прохождения tests)

- `core/core-db-impl/.../CoreDbApiImpl.kt` (`LexemeApiImpl.editComponentType`) — минимальная рабочая реализация: lookup current type → Removed/BuiltInProtected/TemplateImmutable checks → two-prong collision SELECT только при name change → cardinality downgrade conservative approximation (`existing.isMulti && !isMulti && countActiveByTypeId > 0 && dictIds.isNotEmpty()` → blocked с пустым ids list) → UPDATE + cascade quiz_configs если name изменился. Точный per-lexeme `HAVING count(*) > 1` SELECT с deterministic `ORDER BY component_values.updated_at DESC, lexeme_id ASC` — задача data sub-flow.

### UseCase interfaces (`:modules:screen:components_manager` + `:modules:screen:per_dictionary_components`)

- `ComponentsManagerUseCase.kt` [~] — добавлено `suspend fun editComponent(typeId: ComponentTypeId, name: String, template, isMulti): EditOutcome` + `fun flowDictionaries(): Flow<List<DictionaryApiEntity>>`. KDoc разделяет UseCaseImpl-level rules (NameEmpty/TemplateImmutable/Failure) и API-level outcomes (Removed/BuiltIn/Collision/Cardinality/Success).
- `PerDictionaryComponentsUseCase.kt` [~] — только `editComponent(...)` с тем же signature; `flowDictionaries` отсутствует (multi-dict picker не применим в PerDict).
- `:modules:screen:components_manager/build.gradle.kts` — добавлена dep на `:core:core-db-api` (для прямого импорта `DictionaryApiEntity`).

### Manager mate (Reducer / State / Msg / Effect / Handler / FlowHandler)

- **`State.kt`** [~] — три новых поля: `editDialog: EditDialogState? = null`, `isEditing: Boolean = false`, `availableDictionaries: List<DictionaryApiEntity> = emptyList()`. `CreateDialogState` расширен `selectedDictionaryIds: Set<Long> = emptySet()` (index intersection через Set). Добавлены sealed `EditNameError { NameEmpty, SameScopeCollision, CrossScopeCollision }` и `ImpactedLexemesPreview { InlineOnly(ids), InlineWithDrillIn(ids, inlineIds) }`. `canSubmit: Boolean` extension val (Global → не пустой trim'нутый name; PerDictionaries → дополнительно `selectedDictionaryIds.isNotEmpty()`). `[shape]` KDoc invariant расширен до 4-way mutual-exclusion dialogs + 4-way in-flight.
- **`Msg.kt`** [~] — Edit family (7 case): `OpenEditDialog(typeId)/CloseEditDialog/EditNameChange/EditTemplateChange/EditMultiToggle/SubmitEdit/EditResult(epochId, outcome)`. Multi-dict picker: `CreateDictionaryToggle(dictionaryId)/DictionariesLoaded(dictionaries)`.
- **`Reducer.kt`** [~] — 7 Edit-веток с F138 4-way mutual-exclusion в `OpenEditDialog` (закрывает три existing + сбрасывает `isCreating/isRenaming/isDeleting`); SubmitEdit guard `!isEditing && editDialog != null && name.trim().isNotBlank()` + epoch bump + emit `DatasourceEffect.EditComponent`; EditResult per-variant 9 sub-cases с F101 race close-during-flight fallback + F136 stale-epoch check. Все три existing Open*Dialog ветки получили `editDialog=null, isEditing=false` в reset. Multi-dict: `CreateDictionaryToggle` (toggle Set ± id), `DictionariesLoaded` (`availableDictionaries=list` + chip-staleness filter `selectedDictionaryIds ∩ list.ids`; **editDialog НЕ мутируется** — F030 invariant). `CreateScopeChange(Global)` дополнительно очищает `selectedDictionaryIds = emptySet()` (test-driven). `RenameResult/DeleteResult` получили `Removed` ветку (snackbar "Компонент удалён" + close dialog).
- **`DatasourceEffect.kt`** [~] — `EditComponent(epochId, typeId: ComponentTypeId, name, template, isMulti)` + `data object SubscribeDictionaries`.
- **`DatasourceEffectHandler.kt`** [~] — `is EditComponent → Msg.EditResult(epochId, useCase.editComponent(...))`; `SubscribeDictionaries → dictionariesFlowHandler.runEffect(...)`. Constructor inject `dictionariesFlowHandler: DictionariesFlowHandler`. Catch-блок расширен: `EditComponent` exception → `EditOutcome.Failure(cause)`.
- **`DictionariesFlowHandler.kt`** [+] — **NEW** (template = `AllUserDefinedTypesFlowHandler`). Subscribe на `useCase.flowDictionaries()`, `.catch { emit Msg.DictionariesLoaded(emptyList()) }`, `.collectLatest { send(Msg.DictionariesLoaded(list)) }`. `runEffect(SubscribeDictionaries) → unsubscribe + subscribe` (F163 re-subscribe parity).

### PerDict mate (зеркало Manager минус multi-dict)

- `State.kt` [~] — `editDialog`, `isEditing` + дублированные `EditDialogState/EditNameError/ImpactedLexemesPreview` (parity с дублированием Create/Rename/Delete dialog states между screen-модулями; shared widget extraction — задача UI sub-flow).
- `Msg.kt` [~] — Edit family 7 case (без `CreateDictionaryToggle/DictionariesLoaded`).
- `Reducer.kt` [~] — зеркало Manager Reducer минус multi-dict ветки.
- `DatasourceEffect.kt` [~] — только `EditComponent`.
- `DatasourceEffectHandler.kt` [~] — `is EditComponent` branch + catch расширение (без SubscribeDictionaries).

### UseCaseImpl (app/.../di/module)

- **`ComponentsManagerUseCaseImpl.kt`** [~] — Constructor получил `dictionaryApi: CoreDbApi.DictionaryApi` (новая зависимость, паттерн from `SplashUseCaseImpl/DictionaryAppBarUseCaseImpl/DictionaryUseCaseImpl`). `editComponent` реализован по pattern parity с `renameComponent` (baseline `:83-101`): `trim+isBlank → NameEmpty` (без вызова API); try → exhaustive `when` на 7 API outcomes → 7 domain outcomes (Success: `r.type.toDomain()`; passthrough Same/CrossScope/TemplateImmutable/BuiltInProtected/Removed; CardinalityDowngradeBlocked несёт ids); catch (CancellationException → throw, Exception → `logger.e + Failure(e)`). `flowDictionaries() = dictionaryApi.flowDictionaryList()` (F026 — pure delegate без mapping). Также расширены existing `renameComponent` (RenameComponentOutcome.Removed → RenameOutcome.Removed) и `softDeleteComponent` (SoftDeleteComponentOutcome.Removed → DeleteOutcome.Removed) — для exhaustive `when`.
- **`PerDictionaryComponentsUseCaseImpl.kt`** [~] — `editComponent(...) = sharedCrud.editComponent(...)` (DRY-delegation pattern parity с baseline rename/delete delegations).

### Tests (TDD-skeleton — добавлены ДО implementation, всё прошло после implement)

- `ComponentsManagerReducerTest.kt` — **+32 tests**: Edit family (Open/Close/changes/Submit/9 outcome веток) + F138 4-way mutual-exclusion (4 forward + 3 reverse) + F140 in-flight reset + F136 stale-epoch + F030 editDialog-invariant + F006 chip-staleness + F007 Removed parity (Rename/Delete/Edit) + F101 race close-during-flight + F139 double-tap guard.
- `PerDictionaryComponentsReducerTest.kt` — **+24 tests**: зеркало минус multi-dict.
- `ComponentsManagerUseCaseImplTest.kt` — **+15 tests**: editComponent (12: validation + 7 API→domain mapping + Cancellation re-throw + Exception → Failure + trim whitespace), Rename/Delete Removed mapping (2), `flowDictionaries_delegatesToDictionaryApi` (1).
- `PerDictionaryComponentsUseCaseImplTest.kt` — **+4 tests**: `editComponent_delegatesToSharedCrud` + 3 outcome-specific delegate tests.

Build: `./scripts/cc-build.sh testDebugUnitTest` + `assembleDebug` — **PASS**.

### Spec (`docs/features-spec/component-constructor.md`)

- Расширен phase 1 baseline (~1359 строк) → 1470 строк итого. Phase 2 добавления интегрированы inline в каждый раздел (Бизнес-описание / User Stories / State / UI Messages / UI Layout / IO / UseCase / Тестовые сценарии). Применены 10 corrections из implement стадии (см. `business_publish_spec.md § Корректировки`).

## Ключевые решения

1. **`typeId: ComponentTypeId` на screen/UseCase уровне, `Long` только на data API** — parity с `renameComponent` baseline. `LexemeApi.editComponentType(typeId: Long, ...)` — единственное место с raw Long.
2. **`EditOutcome.Failure` Reducer закрывает editDialog** (асимметрия с `RenameOutcome.Failure` который dialog НЕ закрывает) — test-driven; единая UX для terminal failures на Edit. `CreateScopeChange(Scope.Global)` очищает `selectedDictionaryIds = emptySet()` — test-driven; не было в design_tree.
3. **Template-immutability gate на UseCaseImpl** (F017) — `if (current.template != template) return EditOutcome.TemplateImmutable` ДО вызова `lexemeApi.editComponentType(...)`. API имеет `TemplateImmutable` defensive parity, но основная проверка наверху — экономит data round-trip.
4. **Cardinality downgrade conservative implementation в data layer** — `existing.isMulti && !isMulti && countActiveByTypeId > 0 && dictIds.isNotEmpty()` → `Blocked(emptyList())`. Точный per-lexeme SELECT (`HAVING count(*) > 1` + `ORDER BY updated_at DESC, lexeme_id ASC LIMIT 3`) — backlog data sub-flow. UseCaseImpl mock-tests прошли — реальная корректность ids остаётся за data impl.
5. **`Msg.DictionariesLoaded` chip-staleness filter с editDialog invariant** (F030) — фильтрует `createDialog.selectedDictionaryIds ∩ list.ids` при out-of-band удалении словаря, но **никогда не мутирует editDialog поля**. Зафиксировано в Reducer + dedicated test `whenEditDialogOpen_thenDictionariesLoaded_doesNotMutateEditState`.
6. **`F-BCR1` `EditOutcome.Success(updated: ComponentType)`** (contract_review iter 1) — parity с `RenameOutcome.Success(ComponentType)`. Изначальный draft использовал `Success(typeId: ComponentTypeId)`; финализировано на полный entity для consistency.
7. **`F-BCR2` `CardinalityDowngradeBlocked.impactedLexemeIds: List<Long>` — full list без LIMIT в SQL** (contract_review iter 1) — нужен общий size для UI «Показать все» drill-in. Top-3 split — на Reducer уровне (`InlineOnly` vs `InlineWithDrillIn`), не на data/API. Deterministic sort фиксирован в data sub-flow (`ORDER BY component_values.updated_at DESC, lexeme_id ASC`).
8. **`F-BDT1` `DatasourceEffectHandler` depends на `DictionariesFlowHandler` через constructor inject** (design_tree iter 1) — единственная точка координации re-subscribe trigger; FlowHandler не самоинициализируется через Mate init.
9. **PerDict без `flowDictionaries` / без `DictionariesFlowHandler` / без `SubscribeDictionaries` Effect** — scope hardcoded к `dictionaryId`, multi-dict picker неприменим. PerDict `CreateDialogState` НЕ расширяется `selectedDictionaryIds`. Дублирование `EditDialogState/EditNameError/ImpactedLexemesPreview` между двумя screen-модулями — parity с existing дублированием (shared widget extraction — UI sub-flow).
10. **PerDict UseCaseImpl делегирует на `sharedCrud: ComponentsManagerUseCase`** (DRY) — единая реализация template-immutability gate + API→domain mapping живёт в Manager UseCaseImpl. Решение оставлено в design_tree открытым (оба варианта валидны), implement выбрал delegation.
11. **`SubscribeDictionaries` Effect — опциональный re-subscribe trigger** (F163 parity) — emit'ится при ошибке либо manual refresh. В MVP `DictionariesFlowHandler` автоподписывается через `subscribe()` при init Mate; Effect остаётся для retry-pattern parity.
12. **Spec consolidated — один файл `component-constructor.md`** (НЕ отдельный `component-constructor-phase2.md`). Phase 2 интегрирован inline с пометками «(phase 2)» — следуем правилу «не плодить спеки с разными именами для одной фичи».

## UI dependencies handed off to ui sub-flow

Декларация из `business_design_tree.md § UI dependencies` — это **НЕ часть business design tree**, но ui sub-flow обязан реализовать:

- **`CreateComponentDialog`** [~] — расширить scope picker chip-list для multi-select словарей (управляется `Msg.CreateDictionaryToggle` + `state.availableDictionaries` + `state.createDialog.selectedDictionaryIds`). Submit-кнопка disable'ится через `canSubmit` extension val.
- **`EditComponentDialog`** [+ NEW] — новый `@Composable` widget: name input, template picker (disabled либо readonly per UI design), isMulti toggle, cardinality downgrade preview block. Принимает `EditDialogState` + callbacks для всей Edit-family Msg.
- **`CardinalityDowngradePreviewWidget`** [+ NEW] — inline preview top-3 + drill-in кнопка; рендерит ветки `ImpactedLexemesPreview.InlineOnly` (drill-in скрыта) vs `InlineWithDrillIn` (видна).
- **`UserDefinedRowWidget`** [~] — добавить Edit action button (callback `onEdit(typeId)` → `Msg.OpenEditDialog(typeId)`).
- **`ComponentsManagerScreen`** [~] — mount `EditComponentDialog` (visible iff `state.editDialog != null`); подключить multi-dict scope picker chip-list к `state.availableDictionaries`.
- **`PerDictionaryComponentsScreen`** [~] — analogous: mount `EditComponentDialog` (БЕЗ scope picker).
- **`RenameComponentDialog`** [~] и **`DeleteComponentConfirmDialog`** [~] — extract в shared widget module (`:modules:widget:component_widgets`).
- **Shared widget module `:modules:widget:component_widgets`** [+ NEW] — создание модуля, build.gradle, package + миграция дублированных composables из обоих screen-модулей. Отдельный design tree для UI sub-flow.
- **Strings (`core-resources`)** — `edit_component_title`, `edit_component_submit_label`, `edit_template_immutable_message`, `edit_built_in_protected_message`, `component_removed_message`, `cardinality_downgrade_blocked_inline_label`, `cardinality_downgrade_blocked_drill_in_label` (имена rough — финальные за UI sub-flow).
- **Per-template architecture** (composable resolver `ComponentByTemplate`, `TextWidget`, `ComponentBlock`) — фундамент закладывается в UI sub-flow; domain `ComponentTemplate` enum уже существует, не расширяется.

## Артефакты

### Business sub-flow артефакты (7 .md файлов)

- `docs/features/IS481_component_constructor_phase2/business_walkthrough.md` — факт-чек реального кода baseline в 5 областях phase 2.
- `docs/features/IS481_component_constructor_phase2/business_contract.md` — business contract (state/msg/effect/usecase/domain/api расширения).
- `docs/features/IS481_component_constructor_phase2/business_contract_spec.md` — финальный contract spec после `contract_review` (1 inline-fix: `F-BCR1` Success(ComponentType), `F-BCR2` full list для CardinalityDowngradeBlocked).
- `docs/features/IS481_component_constructor_phase2/business_design_tree.md` — DAG из 24 узлов (3 domain + 2 api + 11 mate + 2 deps + 2 app-impl + 4 tests) + UI dependencies (10 деклараций).
- `docs/features/IS481_component_constructor_phase2/business_test.md` — 75 TDD-skeleton tests добавлены в 4 файла.
- `docs/features/IS481_component_constructor_phase2/business_implement.md` — implement отчёт (22 узла реализованы, build+test PASS).
- `docs/features/IS481_component_constructor_phase2/business_publish_spec.md` — финальная spec публикация (`docs/features-spec/component-constructor.md` 1470 строк, 10 corrections от implement).

### Реализованные source файлы (22 узла)

**Domain (3):** `modules/domain/lexeme/src/main/java/me/apomazkin/lexeme/EditOutcome.kt` [+], `RenameOutcome.kt` [~], `DeleteOutcome.kt` [~].

**API (2):** `core/core-db-api/src/main/java/me/apomazkin/core_db_api/entity/ComponentOutcomeApiEntity.kt` [~], `core/core-db-api/src/main/java/me/apomazkin/core_db_api/CoreDbApi.kt` [~].

**Data impl (1, вне design tree):** `core/core-db-impl/src/main/java/me/apomazkin/core_db_impl/CoreDbApiImpl.kt` [~] — `LexemeApiImpl.editComponentType`.

**UseCase deps (2):** `modules/screen/components_manager/src/main/java/me/apomazkin/components_manager/deps/ComponentsManagerUseCase.kt` [~], `modules/screen/per_dictionary_components/src/main/java/me/apomazkin/per_dictionary_components/deps/PerDictionaryComponentsUseCase.kt` [~]. Также `:modules:screen:components_manager/build.gradle.kts` — dep на `:core:core-db-api`.

**Manager mate (6):** `modules/screen/components_manager/src/main/java/me/apomazkin/components_manager/mate/{State.kt, Msg.kt, Reducer.kt, DatasourceEffect.kt, DatasourceEffectHandler.kt}` [~] + `DictionariesFlowHandler.kt` [+].

**PerDict mate (5):** `modules/screen/per_dictionary_components/src/main/java/me/apomazkin/per_dictionary_components/mate/{State.kt, Msg.kt, Reducer.kt, DatasourceEffect.kt, DatasourceEffectHandler.kt}` [~].

**App impl (2):** `app/src/main/java/me/apomazkin/polytrainer/di/module/componentsmanager/ComponentsManagerUseCaseImpl.kt` [~], `app/src/main/java/me/apomazkin/polytrainer/di/module/perdictionarycomponents/PerDictionaryComponentsUseCaseImpl.kt` [~].

### Test файлы (4)

- `modules/screen/components_manager/src/test/java/me/apomazkin/components_manager/mate/ComponentsManagerReducerTest.kt` [~] — +32 tests.
- `modules/screen/per_dictionary_components/src/test/java/me/apomazkin/per_dictionary_components/mate/PerDictionaryComponentsReducerTest.kt` [~] — +24 tests.
- `app/src/test/java/me/apomazkin/polytrainer/di/module/componentsmanager/ComponentsManagerUseCaseImplTest.kt` [~] — +15 tests.
- `app/src/test/java/me/apomazkin/polytrainer/di/module/perdictionarycomponents/PerDictionaryComponentsUseCaseImplTest.kt` [~] — +4 tests.

Также fix existing phase 1 test: `DatasourceEffectHandlerTest` (Manager) — добавлен mock `dictionariesFlowHandler` в constructor.

### Spec

- `docs/features-spec/component-constructor.md` [~] — 1470 строк (phase 1 baseline + phase 2 inline).

_model: claude-opus-4-7[1m]_
