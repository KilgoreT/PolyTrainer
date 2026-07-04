---
status: done
---

# Summary — business sub-flow (IS481 component_constructor)

Sub-flow business завершён: M13 переход в `:modules:domain:lexeme`, расширение `CoreDbApi.LexemeApi` (5 BREAKING сигнатур + 6 NEW методов), 2 UseCase impl с прямой/делегирующей реализацией, две Mate-обвязки для `ComponentsManagerScreen` и `PerDictionaryComponentsScreen` (logic-only, без Composables), миграция 6 business call-sites M12→M13 и mechanical rebind 11 test files. Build broken для `:app:testDebugUnitTest` (by design — ждёт data_implement для DAO/мapper/migration в `:core:core-db-impl`).

## Что сделано

### Tier 0-1 — Domain types (`:modules:domain:lexeme`)

Созданы (новые файлы):
- `Primitive.kt` — sealed `Text(String) / Image(uri) / Color(hex)`.
- `PrimitiveType.kt` — `enum { TEXT, IMAGE, COLOR }`.
- `Field.kt` — `data class Field(name, type: PrimitiveType)`.
- `TemplateValues.kt` — sealed; MVP `TextValues(Primitive.Text)`, `ImageValues(Primitive.Image)`.
- `Scope.kt` — sealed `Global / PerDictionaries(ids: List<Long>)`.
- `NameError.kt` — sealed `Empty / TooLong / SameScopeCollision / CrossScopeCollision`.
- `AffectedQuizConfig.kt`, `DeletionImpact.kt`, `ComponentUsage.kt`, `UserDefinedTypesSnapshot.kt`, `PerDictionarySnapshot.kt`.
- `CreateOutcome.kt` (sealed: `Success(List<ComponentType>) / NameEmpty / NameTooLong / SameScopeCollision / CrossScopeCollision / Failure(cause)`).
- `RenameOutcome.kt` (sealed: `Success(type) / NameEmpty / SameScopeCollision / CrossScopeCollision / BuiltInProtected / Failure`).
- `DeleteOutcome.kt` (sealed: `Success(impact) / BuiltInProtected / Failure`).

Модифицированы:
- `ComponentTemplate.kt` — drop `LONG_TEXT` (M13 consolidation), `fromKey` → nullable (fail-soft), новый computed `fields: List<Field>`.
- `ComponentType.kt` — добавлены `isMulti: Boolean`, `createdAt: Date`, `updatedAt: Date`; `removeDate → removedAt`.
- `ComponentValue.kt` — rebind `data: ComponentValueData → TemplateValues`.
- `Lexeme.kt` — `@Deprecated` text refresh (`Use TextValues via components`).

Удаление `ComponentValueData.kt` перенесено в `data_design_tree` как финальный узел всего IS481 DAG (data-side ещё ссылается).

### Tier 2 — Data-API contract (`:core:core-db-api`)

Созданы:
- `entity/DictionaryTypesSnapshot.kt` — data-side shape для `flowUserDefinedTypesForDictionary`.
- `entity/UserDefinedTypesUsageSnapshot.kt` — data-side shape для aggregated view.
- `entity/ComponentOutcomeApiEntity.kt` — `CreateComponentOutcome / RenameComponentOutcome / SoftDeleteComponentOutcome` (мапятся на domain outcomes).

Модифицированы:
- `entity/ComponentTypeApiEntity.kt` — synchronized с domain `ComponentType` (`isMulti/createdAt/updatedAt/removedAt`).
- `entity/ComponentValueApiEntity.kt` — `data: ComponentValueData → TemplateValues` + добавлены `createdAt/updatedAt/removedAt`.
- `CoreDbApi.kt` (`LexemeApi`):
  - **BREAKING (5 сигнатур M12→M13):** `addLexemeWithBuiltInComponent`, `addLexemeWithUserDefinedComponent`, `addLexemeWithComponents`, `addComponentValue`, `updateComponentValue` — все `data` параметры теперь `TemplateValues`.
  - **NEW (6 методов):**
    - `fun flowAllUserDefinedTypesWithUsage(): Flow<UserDefinedTypesUsageSnapshot>`
    - `fun flowUserDefinedTypesForDictionary(dictionaryId: Long): Flow<DictionaryTypesSnapshot>`
    - `suspend fun createUserDefinedComponent(name, template, isMulti, scope): CreateComponentOutcome`
    - `suspend fun renameComponentType(typeId, newName): RenameComponentOutcome`
    - `suspend fun previewDeletionImpact(typeId): DeletionImpact?`
    - `suspend fun softDeleteComponentType(typeId): SoftDeleteComponentOutcome`

### Tier 3-4 — UseCase interfaces + impls

- `modules/screen/components_manager/.../deps/ComponentsManagerUseCase.kt` — placeholder → 5 методов (`flowAllUserDefinedTypes / createUserDefinedComponent / renameComponent / previewDeletionImpact / softDeleteComponent`).
- `modules/screen/per_dictionary_components/.../deps/PerDictionaryComponentsUseCase.kt` — placeholder → 5 методов (read = `flowComponentsForDictionary(dictId)`, write = parity).
- `app/.../di/module/componentsmanager/ComponentsManagerUseCaseImpl.kt` — реальная реализация: domain mapping, `name.trim()`, blank → `NameEmpty`, try/catch → `Failure(cause)` + LexemeLogger, `resetQuizPickerPrefsBestEffort()` после soft-delete (F049/F103 best-effort, try/catch wrap, warning log при failure).
- `app/.../di/module/perdictionarycomponents/PerDictionaryComponentsUseCaseImpl.kt` — read собственный (`flowComponentsForDictionary`), write делегируется в `ComponentsManagerUseCaseImpl` через ctor (DI composition, не наследование).

LogTags:
- `modules/screen/components_manager/.../LogTags.kt` — `COMPONENTS_MANAGER = "ComponentsManager"`.
- `modules/screen/per_dictionary_components/.../LogTags.kt` — `PER_DICT_COMPONENTS = "PerDictComponents"`.

### Tier 5 — ComponentsManagerScreen Mate (logic-only, no Composables)

Созданы в `modules/screen/components_manager/.../mate/`:
- `State.kt` — `ComponentsManagerScreenState(userDefinedTypes, isLoading, isCreating, isRenaming, isDeleting, createDialog, renameDialog, deleteConfirm, snackbar)` + `UserDefinedRow` + `CreateDialogState / RenameDialogState / DeleteConfirmState` + `SnackbarState(text)` + computed `isEmpty` + extension `UserDefinedTypesSnapshot.toRows()`.
- `Msg.kt` — sealed Lifecycle/Create/Rename/Delete/Navigation/UiMsg ветки + `DismissSnackbar`; `*Result` несут `epochId: Long` (F136), `ImpactPreviewLoaded/Failed` correlated `typeId` (F124).
- `UiEffect.kt` + `UiEffectHandler.kt` — `Snackbar(text)` → `UiMsg.Snackbar(text)` (без `show`, F128).
- `DatasourceEffect.kt` + `DatasourceEffectHandler.kt` — IO mapper, `Dispatchers.IO`, catch `Throwable` → typed `Failure` + LexemeLogger; `CancellationException` re-throw invariant (F125).
- `AllUserDefinedTypesFlowHandler.kt` — `MateFlowHandler`, на init Mate подписывается на `useCase.flowAllUserDefinedTypes()` через `subscribe(scope, send)`.
- `NavigationEffect.kt` + `NavigationEffectHandler.kt` — `@AssistedInject(navigator)`; `onScreenEffect` no-op (base `Back` уже обработан super).
- `Reducer.kt` — `ComponentsManagerReducer` (exhaustive `when` по 20+ веткам); guard'ы `isCreating/isRenaming/isDeleting` против double-tap; `Open*Dialog` закрывает остальные диалоги (F138); `OpenCreateDialog` сбрасывает `isCreating=false` (F140); race-conditions «closed dialog during in-flight» → snackbar fallback; preview correlation by `typeId`.

Модифицирован:
- `ComponentsManagerViewModel.kt` — placeholder → `Mate(...)` с `effectHandlerSet = {datasource, flow, ui, nav(navigator)}` через `@AssistedInject`; `onCleared() = stateHolder.dispose()`.
- `modules/screen/components_manager/build.gradle.kts` — +`:modules:core:logger`, testLibs.

### Tier 6 — PerDictionaryComponentsScreen Mate (зеркально Tier 5)

Созданы в `modules/screen/per_dictionary_components/.../mate/`:
- `State.kt` — `PerDictionaryComponentsScreenState(dictionaryId, dictionaryName, items, ...flags..., dialogs, snackbar)` + `PerDictRow(typeId, name, template, isMulti, isGlobal, valueCount)` + extension `PerDictionarySnapshot.toPerDictRows(dictionaryId)`.
- `Msg.kt` — `ItemsLoaded(snapshot) / ItemsLoadFailed(cause)` parity с CM + Create/Rename/Delete/Navigation ветки c epochId/typeId correlation.
- `UiEffect.kt`, `UiEffectHandler.kt`, `DatasourceEffect.kt`, `DatasourceEffectHandler.kt` — зеркально CM.
- `ComponentsForDictionaryFlowHandler.kt` — `@AssistedInject(dictionaryId: Long)` + `@AssistedFactory`; подписка на `useCase.flowComponentsForDictionary(dictionaryId)` через `subscribe(scope, send)` (assisted-инжект через ViewModel factory вместо отдельного SubscribeForDictionary Effect).
- `NavigationEffect.kt` + `NavigationEffectHandler.kt`.
- `Reducer.kt` — `PerDictionaryComponentsReducer`; key отличие: `OpenCreateDialog` инициализирует `scope = Scope.PerDictionaries(listOf(state.dictionaryId))` (preselect текущий словарь).

Модифицирован:
- `PerDictionaryComponentsViewModel.kt` — `@AssistedInject(dictionaryId, navigator)` + factory pattern для FlowHandler/NavigationEffectHandler.

### Tier 7 — Migration call-sites M12 → M13 (business-side)

- `app/.../mapper/LexemeMapper.kt` (#52) — `ComponentValueData.TextValue` casts → `TextValues`; `ComponentTypeApiEntity.toDomain()` пробрасывает `isMulti/createdAt/updatedAt/removedAt`.
- `app/.../di/module/wordCard/WordCardUseCaseImpl.kt` (#49) — 4× call-sites + 5× signatures.
- `modules/screen/wordcard/.../deps/WordCardUseCase.kt` (#50) — 5 interface methods rebind.
- `modules/screen/wordcard/.../mate/DatasourceEffectHandler.kt` (#51) — 1× call-site.
- `modules/screen/quiz/chat/.../quiz/QuizGameImpl.kt` (#53) — 2× casts; `LongTextValue` fallback dropped (F046).
- `modules/domain/lexeme/.../Lexeme.kt` (#54) — `@Deprecated` text refresh.

F117 cleanup: local `internal fun toDomain()` extensions в обоих UseCase impl удалены — canonical import из `me.apomazkin.polytrainer.mapper`.

### Тесты (~146 тестов across 8 файлов)

Созданы (по `business_test.md`, с корректировкой counts iter 4):
- `app/.../ComponentsManagerUseCaseImplTest.kt` — 25 тестов (CRUD outcome mapping, NameEmpty guard, prefs cleanup, orphan prefs reset throws).
- `app/.../PerDictionaryComponentsUseCaseImplTest.kt` — 6 тестов (flow mapping + delegation sanity).
- `modules/screen/components_manager/.../mate/ComponentsManagerReducerTest.kt` — 68 тестов (exhaustive по 20+ Msg веткам, guard'ы, race-conditions, overwrite policy, helper `toRows()`).
- `modules/screen/components_manager/.../mate/DatasourceEffectHandlerTest.kt` — 14 тестов (4 Effect × happy/error + CancellationException re-throw по всем 4 веткам, F125/F146).
- `modules/screen/components_manager/.../mate/AllUserDefinedTypesFlowHandlerTest.kt` — 4 теста.
- `modules/screen/per_dictionary_components/.../mate/PerDictionaryComponentsReducerTest.kt` — 62 теста (зеркально CM + scope preselect + `toPerDictRows`).
- `modules/screen/per_dictionary_components/.../mate/DatasourceEffectHandlerTest.kt` — 14 тестов.
- `modules/screen/per_dictionary_components/.../mate/ComponentsForDictionaryFlowHandlerTest.kt` — 4 теста.

Mechanical rebind 11 existing test files (F099 — обнаружено 11, не 6 как ожидалось):
- `WordCardUseCaseImplTest.kt`, `LexemeMapperTest.kt`, `wordcard/mate/DatasourceEffectHandlerTest.kt`, `WordLoadedTest.kt`, `QuizGameImplTest.kt`, `QuizGameImplFetchDataTest.kt`, `ChatReducerTest.kt`, `quiz/chat/logic/DatasourceEffectHandlerTest.kt`, `QuizPickerFlowHandlerTest.kt`, `LexemeBuiltInExtTest.kt`, `ComponentTypeRefExtTest.kt`. Pattern: `ComponentValueData.TextValue(s) → TextValues(Primitive.Text(s))` + `createdAt/updatedAt/removedAt` на api-entity helpers.

### Build/Test состояние

- `:modules:domain:lexeme:test` — **PASS**.
- `:modules:screen:components_manager:testDebugUnitTest` — **PASS** (69 тестов).
- `:modules:screen:per_dictionary_components:testDebugUnitTest` — **PASS** (77 тестов).
- `:modules:screen:wordcard:testDebugUnitTest` — **PASS** (after migration).
- `:modules:screen:quiz:chat:testDebugUnitTest` — **PASS**.
- `:app:testDebugUnitTest` — **не запускался** by design (`:core:core-db-impl` ждёт data_implement для новых LexemeApi методов; `ComponentsManagerUseCaseImplTest` / `PerDictionaryComponentsUseCaseImplTest` находятся в `:app` source set и не достижимы пока chain сломан).

## Ключевые решения

1. **Placeholder ViewModel → real Mate wiring.** Infra-фаза создала placeholder `ViewModel` и `UseCase` interface (`infra_design_tree` id 5/6/23/25); business-фаза заполнила реальной Mate-сборкой через `@AssistedInject(navigator)` + factory pattern; `effectHandlerSet = {datasource, flow, ui, nav}` + `onCleared() = dispose()`.

2. **Epoch pattern для race conditions (F136).** `*Result` Msg (CreateResult/RenameResult/DeleteResult) и соответствующие `DatasourceEffect` несут `epochId: Long`. Reducer dropит stale results (epoch mismatch ⇒ dialog был перенесён/закрыт между submit и result). Дополняется correlation by `typeId` для `ImpactPreviewLoaded/Failed` (F124).

3. **F142 shared util — `:modules:core:tools/ThrowableExt.kt`.** `failureLabel(throwable)` для UI snackbar text вынесена в shared core util (вместо дублирования inline на screen-модулях).

4. **F099 mechanical migration в 11 test files.** Pattern-replacement обнаружил 11 файлов (vs 6 ожидаемых в business_test.md spec). Cause: `createdAt/updatedAt/removedAt` rename в `ComponentType / ComponentTypeApiEntity` затронул больше helper-функций чем шли по grep `ComponentValueData`. Discovery — compile-time gate сработал correctly.

5. **UseCase impl locations.** `app/.../di/module/...` по existing convention с 11 другими UseCaseImpl, не `modules/screen/.../deps/`.

6. **PerDictionary delegation через DI composition.** `PerDictionaryComponentsUseCaseImpl` принимает `ComponentsManagerUseCaseImpl` через ctor, делегирует все write-методы; read — собственный со scoped query. Cross-module наследование нежелательно, shared CRUD impl ok через DI.

7. **Pass split (5 passes).** Conductor нарушил `execute_step_once` псевдокод runner.md (multiple sub-agent passes внутри одной execute фазы вместо одного agent_execute) — зафиксировано в `docs/FlowBacklog.md → IS481cc-F7`. С iter 3 protocol corrected — каждый pass = одна iteration с review между.

## Что вне scope этого sub-flow

- **data_implement** (отдельный sub-flow): implementation новых `LexemeApi` методов в `:core:core-db-impl` (`CoreDbApiImpl`, `SeedBuiltIns`, новый `Migration_012_to_013`, DAO/JSON mappers), plus delete `ComponentValueData.kt` (domain узел #6 перенесён в `data_design_tree` как финальный узел всего IS481 DAG — символ ещё ссылается с data-стороны). Без data_implement `:core:core-db-impl` и `:app:testDebugUnitTest` остаются broken — by design.

- **ui_implement** (отдельный sub-flow): создание Composables — `ComponentsManagerScreen`, `PerDictionaryComponentsScreen`, `UserDefinedRowWidget`, `PerDictRowWidget`, `CreateComponentDialog`, `RenameComponentDialog`, `DeleteConfirmDialog`, `ComponentManageWidget` (settings-row), `HammerIconButton` (DictionaryAppBar entry), `TemplatePreview` widgets. UI-зависимости задекларированы в `business_design_tree.md` § Часть 3 «UI dependencies», но ни одного узла графа не на UI-стороне.

- **Edit-операции и cardinality downgrade** (`is_multi=true→false`) — design не покрывает edit-flow. Перенесено в `docs/Backlog.md → IS481 phase 2` и `docs/FlowBacklog.md → IS481cc-F6` (process gap).

- **`RenameOutcome.NameTooLong`** — out-of-scope текущего contract (только `CreateOutcome.NameTooLong` реализован; rename TODO в impl).

## Артефакты

- Бизнес-контракт: `/Users/kilg/AndroidStudioProjects/PolyTrainer/docs/features/IS481_component_constructor/business_contract_spec.md`
- DAG-граф: `/Users/kilg/AndroidStudioProjects/PolyTrainer/docs/features/IS481_component_constructor/business_design_tree.md`
- Test spec: `/Users/kilg/AndroidStudioProjects/PolyTrainer/docs/features/IS481_component_constructor/business_test.md`
- Implementation log: `/Users/kilg/AndroidStudioProjects/PolyTrainer/docs/features/IS481_component_constructor/business_implement.md` (+ `business_implement_pass1.md` … `business_implement_pass5.md`)
- Implement review: `/Users/kilg/AndroidStudioProjects/PolyTrainer/docs/features/IS481_component_constructor/business_implement_review.md`
- Publish spec: `/Users/kilg/AndroidStudioProjects/PolyTrainer/docs/features/IS481_component_constructor/business_publish_spec.md`
- Published feature spec: `/Users/kilg/AndroidStudioProjects/PolyTrainer/docs/features-spec/component-constructor.md`
