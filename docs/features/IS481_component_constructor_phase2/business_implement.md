# business_implement

## Узлы реализованы

### Domain (#0-2)
- **#0** `modules/domain/lexeme/.../EditOutcome.kt` [+] — новый sealed interface, 9 вариантов (Success/NameEmpty/SameScopeCollision/CrossScopeCollision/CardinalityDowngradeBlocked/TemplateImmutable/BuiltInProtected/Removed/Failure).
- **#1** `RenameOutcome.kt` [~] — добавлен `data object Removed`.
- **#2** `DeleteOutcome.kt` [~] — добавлен `data object Removed`.

### API (#3-4)
- **#3** `core/core-db-api/.../entity/ComponentOutcomeApiEntity.kt` [~] — добавлен `sealed interface EditComponentOutcome` (7 вариантов: Success/SameScopeCollision/CrossScopeCollision/CardinalityDowngradeBlocked/TemplateImmutable/BuiltInProtected/Removed) + `Removed` в Rename/SoftDelete API outcomes.
- **#4** `core/core-db-api/.../CoreDbApi.kt` [~] — добавлен `suspend fun editComponentType(typeId: Long, name: String, template, isMulti): EditComponentOutcome` в `LexemeApi` с подробным KDoc.

### Data impl (вне design tree, но необходимо для компиляции)
- `core/core-db-impl/.../CoreDbApiImpl.kt` [~] — добавлена реализация `editComponentType` в `LexemeApiImpl`: lookup current type → Removed/BuiltInProtected/TemplateImmutable checks → collision two-prong SELECT (только при name change) → cardinality downgrade approximation (true→false с непустым impacted dictionariesByType list) → UPDATE + cascade `quiz_configs.component_refs` если name изменился. Это минимальная рабочая реализация; полная data sub-flow доработка (per-lexeme cardinality SELECT с deterministic ORDER BY) — backlog data sub-flow.

### UseCase deps (#5-6)
- **#5** `modules/screen/components_manager/.../deps/ComponentsManagerUseCase.kt` [~] — добавлены `editComponent(typeId, name, template, isMulti): EditOutcome` + `flowDictionaries(): Flow<List<DictionaryApiEntity>>` с KDoc'ом контракта. Module build.gradle.kts получил dep на `:core:core-db-api`.
- **#6** `modules/screen/per_dictionary_components/.../deps/PerDictionaryComponentsUseCase.kt` [~] — добавлен `editComponent(...)` (без flowDictionaries).

### Manager mate (#7-12)
- **#7** `State.kt` [~] — добавлены `editDialog: EditDialogState? = null`, `isEditing: Boolean = false`, `availableDictionaries: List<DictionaryApiEntity> = emptyList()`; `CreateDialogState` расширен `selectedDictionaryIds: Set<Long> = emptySet()`; добавлены `EditDialogState`, `EditNameError` (sealed: NameEmpty/SameScopeCollision/CrossScopeCollision), `ImpactedLexemesPreview` (sealed: InlineOnly/InlineWithDrillIn); `canSubmit` extension val (Global → true / PerDictionaries → требует non-empty selection). KDoc invariant 4-way mutual exclusion + F030 invariant зафиксирован.
- **#8** `Msg.kt` [~] — добавлены Edit family (7 cases): OpenEditDialog(typeId)/CloseEditDialog/EditNameChange/EditTemplateChange/EditMultiToggle/SubmitEdit/EditResult + multi-dict Msg: CreateDictionaryToggle(dictionaryId)/DictionariesLoaded(list).
- **#9** `Reducer.kt` [~] — реализованы 7 Edit-family веток (Open с F138 4-way mutual-exclusion + epoch bump; NameChange/TemplateChange/MultiToggle с copy + preview clear на MultiToggle; SubmitEdit с guards `!isEditing` + blank-trim + emit DatasourceEffect.EditComponent; EditResult per-variant 9 sub-cases mapping outcome → state/snackbar + F101 race fallback + F136 stale-epoch); CreateDictionaryToggle (toggle Set ± id); DictionariesLoaded ветка: `availableDictionaries=list` + chip-staleness `selectedDictionaryIds ∩ list.ids` filter; **F030 invariant** — editDialog НЕ мутируется; CreateScopeChange расширена очисткой `selectedDictionaryIds` при switch на Global; OpenCreate/Rename/DeleteConfirm Reducer ветви получили `editDialog=null, isEditing=false` в reset-блок; RenameResult/DeleteResult получили `Removed` ветку.
- **#10** `DatasourceEffect.kt` [~] — добавлены `data class EditComponent(epochId, typeId: ComponentTypeId, name, template, isMulti)` + `data object SubscribeDictionaries`.
- **#11** `DatasourceEffectHandler.kt` [~] — добавлен `is DatasourceEffect.EditComponent → Msg.EditResult(epochId, useCase.editComponent(...))` branch; `DatasourceEffect.SubscribeDictionaries → dictionariesFlowHandler.runEffect(...)` (delegates). Constructor inject `private val dictionariesFlowHandler: DictionariesFlowHandler`. Catch-блок расширен `is EditComponent → EditOutcome.Failure(e)`.
- **#12** `DictionariesFlowHandler.kt` [+] — NEW FlowHandler (template = AllUserDefinedTypesFlowHandler). `subscribe(scope, send)` подписывается на `useCase.flowDictionaries()`, error в `.catch` → emit `Msg.DictionariesLoaded(emptyList())` (MVP best-guess), `.collectLatest { send(Msg.DictionariesLoaded(list)) }`. `runEffect(SubscribeDictionaries) → unsubscribe + subscribe`.

### PerDict mate (#13-17)
- **#13** `State.kt` [~] — добавлены `editDialog`, `isEditing`; `EditDialogState`/`EditNameError`/`ImpactedLexemesPreview` дублируются (parity).
- **#14** `Msg.kt` [~] — Edit family 7 cases (без CreateDictionaryToggle/DictionariesLoaded).
- **#15** `Reducer.kt` [~] — зеркало #9 минус multi-dict ветви: 7 Edit-ветвей + OpenCreate/Rename/DeleteConfirm reset `editDialog/isEditing` + RenameResult/DeleteResult `Removed` ветки.
- **#16** `DatasourceEffect.kt` [~] — `EditComponent` (без SubscribeDictionaries).
- **#17** `DatasourceEffectHandler.kt` [~] — `is EditComponent` branch + catch расширение.

### UseCaseImpl (#18-19)
- **#18** `app/.../componentsmanager/ComponentsManagerUseCaseImpl.kt` [~] — Constructor получил `dictionaryApi: CoreDbApi.DictionaryApi` (новая зависимость). Реализован `editComponent(typeId, name, template, isMulti)`: trim+isBlank → NameEmpty; try-catch wrapper; маппинг 7 API outcomes → 7 domain outcomes (Success → Success(toDomain) / Same/CrossScope passthrough / CardinalityDowngradeBlocked(ids) / TemplateImmutable / BuiltInProtected / Removed); CancellationException re-throws; Exception → Failure(e) + logger.e. Реализован `flowDictionaries() = dictionaryApi.flowDictionaryList()` (delegate без mapping, F026). Также добавлены Removed ветки в `renameComponent` (RenameComponentOutcome.Removed → RenameOutcome.Removed) и `softDeleteComponent` (SoftDeleteComponentOutcome.Removed → DeleteOutcome.Removed) — для exhaustive `when`.
- **#19** `app/.../perdictionarycomponents/PerDictionaryComponentsUseCaseImpl.kt` [~] — `editComponent(...) = sharedCrud.editComponent(...)` DRY-delegation pattern.

## Тесты

Запущены: **pass**
Запускалась команда: `./scripts/cc-build.sh testDebugUnitTest`

Все 75 phase 2 tests из 4 файлов проходят:
- `ComponentsManagerReducerTest` — 32 phase 2 tests (Edit family + F138 4-way + F140 + F136 + F030 + chip-staleness + Rename/Delete Removed parity + F101 race).
- `PerDictionaryComponentsReducerTest` — 24 phase 2 tests (зеркало минус multi-dict).
- `ComponentsManagerUseCaseImplTest` — 15 phase 2 tests (editComponent — 12, Rename/Delete Removed mapping — 2, flowDictionaries delegation — 1).
- `PerDictionaryComponentsUseCaseImplTest` — 4 phase 2 tests (editComponent_delegatesToSharedCrud + Removed/TemplateImmutable/CardinalityDowngradeBlocked).

Также прошёл assembleDebug — production build OK.

## Нетривиальные решения

1. **`DatasourceEffect.EditComponent.typeId: ComponentTypeId`** (не `Long` из contract design_tree #10). Tests требовали `ComponentTypeId(42L)` в effect param. UseCase signature тоже `ComponentTypeId`; only data API `editComponentType` принимает raw `Long` (parity с `renameComponentType`).
2. **`SubmitEdit` использует `dlg.name` без trim** в DatasourceEffect.EditComponent (UseCaseImpl делает trim сам). Tests подтверждают: `name = "Updated"` (без пробелов) → effect `name = "Updated"`.
3. **`CreateScopeChange(Scope.Global)` очищает `selectedDictionaryIds`** (тест `whenCreateScopeChange_resetsSelectedDictionaryIds_onGlobalSwitch`). Это поведение НЕ было в design_tree, но требуется тестами.
4. **`EditOutcome.Failure` в Reducer всегда закрывает editDialog** (`whenEditResultFailure_thenSnackbar_andDialogClosed`). Это отличается от `RenameOutcome.Failure` (которая dialog НЕ закрывает) — асимметрия по требованию тестов.
5. **Data API `editComponentType` имеет минимальную рабочую реализацию** в `CoreDbApiImpl.LexemeApiImpl`: cardinality downgrade — conservative approximation (если `existing.isMulti && !isMulti && countActiveByTypeId > 0 && dictIds.isNotEmpty()` → blocked с empty ids list). Точный per-lexeme cardinality SELECT (`HAVING count(*) > 1` + deterministic ORDER) — задача data sub-flow; здесь главное чтобы UseCaseImpl tests с mocked API проходили + production build не падал.
6. **`DatasourceEffectHandlerTest`** (existing, phase 1) был обновлён — добавлен mock `dictionariesFlowHandler` в constructor.
7. **`build.gradle.kts` для `:modules:screen:components_manager`** получил dep на `:core:core-db-api` (transient через `:core:core-db-api` не было — `DictionaryApiEntity` теперь напрямую импортируется).

## log_messages

- iter 1: 22 узла реализованы (3 domain + 2 api + 1 data impl + 2 deps + 11 mate + 2 useCaseImpl + 1 existing test fix). Build passes (assembleDebug + testDebugUnitTest). 75 phase 2 tests pass.
- decision: data impl `editComponentType` cardinality downgrade — approximation, не точная impl (полная — задача data sub-flow). UseCaseImpl tests с mocked API не зависят от data impl корректности → проходят.
- decision: `CreateScopeChange(Global)` очистка `selectedDictionaryIds` добавлена в Reducer (test-driven; не было в design_tree).
- decision: `EditOutcome.Failure` Reducer закрывает editDialog (test-driven асимметрия с Rename/Create Failure).

_model: claude-opus-4-7[1m]_
