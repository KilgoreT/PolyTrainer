# business_test.md — IS481 component_constructor

## Решение: тесты нужны

**Обоснование:** Business слой содержит реальную логику, подлежащую unit-test'у:

- **UseCase impls** (#27 `ComponentsManagerUseCaseImpl`, #28 `PerDictionaryComponentsUseCaseImpl`) — CRUD operations с pre-validation (NameEmpty), маппингом data-layer outcome → domain outcome, обёрткой `try/catch → Failure(cause)`, side-effect `resetQuizPickerPrefsFor()` после soft-delete. Все ветки exhaustive `when` — unit-testable через mock `CoreDbApi.LexemeApi`, `DictionaryApi`, `PrefsProvider`.
- **Reducer impls** (#37 `ComponentsManagerReducer`, #47 `PerDictionaryComponentsReducer`) — pure TEA state-machine с 20+ `Msg` ветками. Каждая ветка (`OpenCreateDialog`, `SubmitCreate`, `CreateResult.*`, `OpenRenameDialog`, `RenameResult.*`, `OpenDeleteConfirm`, `ConfirmDelete`, `DeleteResult.*`, navigation, no-op) — изолируемый input → (state', effects') mapping. Tested через `MateTestHelper.testReduce`.
- **FlowHandler + EffectHandler** (#33, #34, #43, #44) — IO mapper'ы Effect ↔ UseCase. Тестируем error paths, exception → typed Failure outcome, dispatch правильного `Msg`. Pattern существует (`QuizPickerFlowHandlerTest`, `DatasourceEffectHandlerTest`).

**Инварианты особого внимания** (из business_contract `[shape]` / `[transition]`):
- Одновременно открыт не более одного диалога (после Submit*Confirm — все диалоги закрываются).
- `isCreating/Renaming/Deleting=true` → dialog != null (submit подразумевает открытый диалог).
- `ConfirmDelete` пока `isDeleting=true` — игнорируется (защита двойного тапа).
- `SubmitCreate/SubmitRename` пока `isCreating/isRenaming=true` — игнорируется.

---

## Тестовые спеки

### 1. ComponentsManagerUseCaseImplTest (#27)

**Файл:** `app/src/test/java/me/apomazkin/polytrainer/di/module/componentsmanager/ComponentsManagerUseCaseImplTest.kt`

**Convention:** mockk + `runTest`, по аналогии с `WordCardUseCaseImplTest`.

**Mocks (constructor):**
- `lexemeApi: CoreDbApi.LexemeApi` — mockk
- `dictionaryApi: CoreDbApi.DictionaryApi` — mockk (relaxed)
- `prefsProvider: PrefsProvider` — mockk (relaxed)
- `logger: LexemeLogger` — mockk (relaxed)

#### 1.1 `flowAllUserDefinedTypes` — Flow маппинг (3 теста)

- `flow emits empty snapshot → domain UserDefinedTypesSnapshot with empty types`
- `flow emits snapshot with types → maps to domain via toDomain + ComponentTypeId wrapping`
- `valueCountByType / dictionaryIdsByType keys wrapped Long → ComponentTypeId`

Verify: `lexemeApi.flowAllUserDefinedTypesWithUsage()` called; domain snapshot — точно тот же shape что и data-layer api snapshot, но с `ComponentTypeId` keys.

#### 1.2 `createUserDefinedComponent` — CRUD (6 тестов)

| Test | Input | LexemeApi mock | Expected outcome |
|---|---|---|---|
| `name blank → NameEmpty without api call` | `name=""`, scope=Global | not called | `CreateOutcome.NameEmpty` |
| `success returns mapped list` | valid input | `CreateComponentOutcome.Success(listOf(apiEntity))` | `CreateOutcome.Success(listOf(domainType))` |
| `success multi-dict returns N rows` | `scope=PerDictionaries([1,2,3])` | `Success(listOf(entity1, entity2, entity3))` | `Success(created.size == 3)` |
| `same-scope collision passes through` | — | `SameScopeCollision` | `CreateOutcome.SameScopeCollision` |
| `cross-scope collision passes through` | — | `CrossScopeCollision` | `CreateOutcome.CrossScopeCollision` |
| `api throws → Failure(cause) + logged` | — | throws `SQLException` | `Failure(cause is SQLException)` + `logger.e` called |

Also assert: `name` передаётся через `.trim()` в lexemeApi (whitespace handling).

#### 1.3 `renameComponent` (5 тестов)

| Test | Input | LexemeApi mock | Expected outcome |
|---|---|---|---|
| `name blank → NameEmpty без api call` | `newName=""` | not called | `RenameOutcome.NameEmpty` |
| `success maps entity → domain` | valid | `RenameComponentOutcome.Success(entity)` | `RenameOutcome.Success(domainType)` |
| `same-scope collision passes through` | — | `SameScopeCollision` | `RenameOutcome.SameScopeCollision` |
| `cross-scope collision passes through` | — | `CrossScopeCollision` | `RenameOutcome.CrossScopeCollision` |
| `built-in protected passes through` | — | `BuiltInProtected` | `RenameOutcome.BuiltInProtected` |
| `api throws → Failure + logged` | — | throws | `RenameOutcome.Failure(cause)` |

#### 1.4 `previewDeletionImpact` (3 теста)

- `success returns impact` — `lexemeApi.previewDeletionImpact()` returns `DeletionImpact(...)` → forwarded as-is.
- `null returns null` — null pass-through.
- `api throws → returns null + logged` — exception swallowed, returns `null`, `logger.e` called.

#### 1.5 `softDeleteComponent` (4 теста)

- `success with no affected prefs → DeleteOutcome.Success(impact), no prefs writes`
  - impact.affectedPrefs = empty → `prefsProvider.setStringByRawKey` never called.
- `success with affected prefs → resets each pref via quizPickerPrefKey`
  - impact.affectedPrefs = `[1L, 5L]` → 2 calls `setStringByRawKey(quizPickerPrefKey(1L), null)` и `(quizPickerPrefKey(5L), null)`.
- `built-in protected → DeleteOutcome.BuiltInProtected, no prefs writes`
- `api throws → Failure(cause) + logged`

##### 1.5.6 orphan prefs reset throws

- `given DB soft-delete success + prefsProvider.setStringByRawKey throws, when softDeleteComponent invoked, then either: (a) Returns DeleteOutcome.Success (prefs reset wrapped in try/catch best-effort, logged but not propagated), OR (b) Returns DeleteOutcome.PartialSuccess (new variant; DB committed, prefs left stale). Рекомендация для implement: (a) — best-effort prefs reset с warning log.`

#### Total: ~21 теста

---

### 2. PerDictionaryComponentsUseCaseImplTest (#28)

**Файл:** `app/src/test/java/me/apomazkin/polytrainer/di/module/perdictionarycomponents/PerDictionaryComponentsUseCaseImplTest.kt`

**Mocks:**
- `lexemeApi: CoreDbApi.LexemeApi` — mockk
- `sharedCrud: ComponentsManagerUseCaseImpl` — mockk (write-методы делегируются)

#### 2.1 `flowComponentsForDictionary` (2 теста)

- `flow emits PerDictionarySnapshot → domain mapping (dictionaryId, dictionaryName, types, valueCountByType wrapped to ComponentTypeId)`
- `empty snapshot → empty types list`

#### 2.2 Delegation sanity-check (4 теста)

Each write-method вызывает соответствующий `sharedCrud.*` ровно один раз с теми же аргументами и возвращает результат как-есть.

- `createUserDefinedComponent delegates to sharedCrud`
- `renameComponent delegates`
- `previewDeletionImpact delegates`
- `softDeleteComponent delegates`

Coverage rationale: implementation тривиальное `=` делегирование, тесты — гарантия что delegation не подменён ложным no-op (regression protection).

#### Total: ~6 тестов

---

### 3. ComponentsManagerReducerTest (#37)

**Файл:** `modules/screen/components_manager/src/test/java/me/apomazkin/components_manager/logic/ComponentsManagerReducerTest.kt`

**Convention:** `MateReducer.testReduce(state, msg)` + `result.state()` + `result.effects()` + `assertEffects` / `assertNoEffects` (как в `ChatReducerTest`).

**Helpers (private):**
```kotlin
private val reducer = ComponentsManagerReducer()
private fun row(id: Long = 1L, name: String = "Definition", scope: Scope = Scope.Global) = UserDefinedRow(...)
private fun stateWithRows(vararg rows: UserDefinedRow) = ComponentsManagerScreenState(userDefinedTypes = rows.toList(), isLoading = false)
private fun stateWithCreateDialog(name: String = "", isCreating: Boolean = false, nameError: NameError? = null) = ...
private fun stateWithRenameDialog(typeId: Long = 1L, originalName: String = "X", editedName: String = "X", isRenaming: Boolean = false) = ...
private fun stateWithDeleteConfirm(typeId: Long = 1L, name: String = "X", impact: DeletionImpact? = null, isLoadingImpact: Boolean = false, isDeleting: Boolean = false) = ...
```

#### 3.1 Lifecycle / data branches (3 теста)

- `TypesLoaded → state.userDefinedTypes = snapshot.toRows(), isLoading=false, no effects`
- `TypesLoaded with empty snapshot → state.isEmpty == true after`
- `TypesLoadFailed → isLoading=false, emits UiEffect.Snackbar(msg)`

#### 3.2 Create dialog branches (15 тестов)

| Msg | Pre-state | Expected post-state | Expected effects |
|---|---|---|---|
| `OpenCreateDialog` | `createDialog = null` | `createDialog = CreateDialogState() default` | `∅` |
| `OpenCreateDialog` (когда уже открыт) | `createDialog = …` | overwrite — допустимо | `∅` |
| `CloseCreateDialog` | `createDialog != null` | `createDialog = null` | `∅` |
| `CreateNameChange("x")` | dialog open | `dialog.name="x", nameError=null` | `∅` |
| `CreateNameChange clears previous nameError` | `nameError=Empty` | `nameError=null` | `∅` |
| `CreateNameChange when no dialog` | `createDialog=null` | unchanged (no-op via `?.copy`) | `∅` |
| `CreateTemplateChange(IMAGE)` | dialog open | `dialog.template=IMAGE` | `∅` |
| `CreateMultiToggle(true)` | dialog open | `dialog.isMulti=true` | `∅` |
| `CreateScopeChange(PerDictionaries([1]))` | dialog open | `dialog.scope = PerDictionaries([1])` | `∅` |
| `SubmitCreate happy path` | `dialog.name="X"`, `isCreating=false` | `isCreating=true` | `{CreateComponent(name="X", template=TEXT, isMulti=false, scope=Global)}` |
| `SubmitCreate blank name → NameError.Empty` | `dialog.name=""`, `isCreating=false` | `dialog.nameError=NameError.Empty`, `isCreating=false` | `∅` |
| `SubmitCreate when isCreating=true` (guard) | `isCreating=true` | unchanged | `∅` (no double effect) |
| `SubmitCreate when no dialog` (guard) | `createDialog=null` | unchanged | `∅` |
| `CreateResult.Success(created=[type1,type2])` | `isCreating=true` | `isCreating=false, createDialog=null` | `{UiEffect.Snackbar("Created 2")}` |
| `CreateResult.NameEmpty` | dialog open, `isCreating=true` | `isCreating=false, dialog.nameError=NameError.Empty` | `∅` |
| `CreateResult.SameScopeCollision` | dialog open | `isCreating=false, dialog.nameError=NameError.SameScopeCollision` | `∅` |
| `CreateResult.CrossScopeCollision` | dialog open | `isCreating=false, dialog.nameError=NameError.CrossScopeCollision` | `∅` |
| `CreateResult.Failure(e)` | dialog open | `isCreating=false`, dialog не закрывается (или согласно reducer) | `{UiEffect.Snackbar("Failed: …")}` |

**Race-condition (close-during-flight):**

- `given state.createDialog=null + state.isCreating=true (dialog closed during in-flight create), when Msg.CreateResult(SameScopeCollision), then state.isCreating=false, state.createDialog stays null, effects = setOf(UiEffect.Snackbar(...))`. Snackbar fallback для error display когда dialog уже закрыт.

**Overwrite policy (OpenCreateDialog поверх открытого):**

- `given state.createDialog != null (already open), when Msg.OpenCreateDialog, then state.createDialog reset to default empty state: name="", template=Translation, scope=Global, nameError=null, isCreating=false`. (overwrite policy = always reset).

#### 3.3 Rename dialog branches (10 тестов)

| Msg | Pre-state | Expected |
|---|---|---|
| `OpenRenameDialog(id)` row found | rows = [row(id, "X")] | `renameDialog = RenameDialogState(id, "X", "X")`, no effect |
| `OpenRenameDialog(id)` row missing | rows = [] | unchanged (guard) |
| `CloseRenameDialog` | dialog open | `renameDialog=null` |
| `RenameTextChange("Y")` clears error | `nameError=Empty` | `editedName="Y", nameError=null` |
| `SubmitRename` happy | `editedName="Y"` non-blank, `isRenaming=false` | `isRenaming=true`, emits `RenameComponent(typeId,"Y")` |
| `SubmitRename` blank → Empty | `editedName=""` | `dialog.nameError=Empty` |
| `SubmitRename when isRenaming=true` (guard) | — | unchanged, no effect |
| `RenameResult.Success` | — | `isRenaming=false, renameDialog=null`, emits `Snackbar("Renamed")` |
| `RenameResult.NameEmpty/SameScopeCollision/CrossScopeCollision` | — | `nameError` set соответствующий |
| `RenameResult.BuiltInProtected` | — | `renameDialog=null`, emits `Snackbar("Built-in protected")` |
| `RenameResult.Failure` | — | `isRenaming=false`, emits Snackbar |

**Race-condition (close-during-flight):**

- `given state.renameDialog=null + state.isRenaming=true (dialog closed during in-flight rename), when Msg.RenameResult(SameScopeCollision), then state.isRenaming=false, state.renameDialog stays null, effects = setOf(UiEffect.Snackbar(...))`. Snackbar fallback для error display когда dialog уже закрыт.

**Overwrite policy (OpenRenameDialog поверх открытого) — если применимо:**

- `given state.renameDialog != null (already open), when Msg.OpenRenameDialog(otherId), then state.renameDialog reset to fresh dialog state for new row` (overwrite policy = always reset для актуального row).

#### 3.4 Delete confirm branches (10 тестов)

| Msg | Pre-state | Expected |
|---|---|---|
| `OpenDeleteConfirm(id)` row found | rows = [row(id, "X")] | `deleteConfirm = DeleteConfirmState(id,"X", impact=null, isLoadingImpact=true)`, emits `LoadImpact(id)` |
| `OpenDeleteConfirm` row missing | rows = [] | unchanged (guard), no effect |
| `CloseDeleteConfirm` | dialog open | `deleteConfirm=null` |
| `ImpactPreviewLoaded(impact)` | `isLoadingImpact=true` | `deleteConfirm.impact=impact, isLoadingImpact=false`, no effect |
| `ImpactPreviewFailed(cause)` | `isLoadingImpact=true` | `isLoadingImpact=false`, emits `Snackbar("Failed to load impact")` |
| `ConfirmDelete` happy | `deleteConfirm != null`, `isDeleting=false` | `isDeleting=true`, emits `SoftDeleteComponent(id)` ровно один effect |
| `ConfirmDelete when isDeleting=true` (idempotency guard) | `isDeleting=true` | unchanged, `∅` (защита от двойного тапа — [transition] инвариант) |
| `ConfirmDelete when no dialog` (guard) | `deleteConfirm=null` | unchanged |
| `DeleteResult.Success(impact)` | `isDeleting=true` | `isDeleting=false, deleteConfirm=null`, emits `Snackbar("${impact.valueCount} values hidden")` |
| `DeleteResult.BuiltInProtected` | — | `isDeleting=false, deleteConfirm=null`, emits `Snackbar("Built-in protected")` |
| `DeleteResult.Failure` | — | `isDeleting=false`, emits `Snackbar("Failed: …")` |

**Race-condition (close-during-flight):**

- `given state.deleteConfirm=null + state.isDeleting=true (dialog closed during in-flight delete), when Msg.DeleteResult(Failure(e)), then state.isDeleting=false, state.deleteConfirm stays null, effects = setOf(UiEffect.Snackbar(...))`. Snackbar fallback для error display когда dialog уже закрыт.

**ConfirmDelete guard на isLoadingImpact:**

- `given deleteConfirm with isLoadingImpact=true, when Msg.ConfirmDelete, then NO DatasourceEffect.SoftDelete emitted (guard); state unchanged`. (Это указывает на invariant в reducer impl: ConfirmDelete пропускается если impact ещё грузится.)

#### 3.5 Navigation / no-op (3 теста)

- `RequestBack → emits NavigationEffect.Back, state unchanged`
- `Empty → state unchanged, no effects`
- `UiMsg.Snackbar → state unchanged, no effects` (sealed UiMsg branch)

#### 3.6 Helper `UserDefinedTypesSnapshot.toRows()` (3 теста — отдельный test class либо в этом)

- `empty types → empty rows`
- `built-in исключены не должны попадать` (если фильтрация в маппере — иначе верифицируется на data layer)
- `Scope inference — `dictionaryId == null → Scope.Global`, иначе `PerDictionaries(listOf(dictionaryId))``
- `usageCount = usage.valueCountByType[typeId] ?: 0` для несуществующего ID
- `dictionaryNames lookup` через `usage.dictionaryIdsByType` + `usage.dictionaryNames`

#### Total: ~44 тестa

---

### 4. PerDictionaryComponentsReducerTest (#47)

**Файл:** `modules/screen/per_dictionary_components/src/test/java/me/apomazkin/per_dictionary_components/logic/PerDictionaryComponentsReducerTest.kt`

**Зеркально #37 с отличиями:**

#### 4.1 Lifecycle (3 теста)

- `ItemsLoaded(snapshot) → state.items = snapshot.toPerDictRows(dictionaryId), state.dictionaryName = snapshot.dictionaryName, isLoading=false`
- `ItemsLoaded empty → state.isEmpty == true`
- `ItemsLoadFailed → isLoading=false, emits Snackbar`

#### 4.2 Create dialog (специфика scope preselect)

- **`OpenCreateDialog`** — отличие от #37: `dialog.scope = Scope.PerDictionaries(listOf(state.dictionaryId))` (preselect текущий словарь — см. `business_design_tree.md` #47).
- остальные create-ветки — зеркально #37 (~15 тестов).

#### 4.3 Rename / Delete — зеркально #37 (~10 + ~10 тестов).

#### 4.4 Helper `PerDictionarySnapshot.toPerDictRows(dictionaryId)` (3 теста)

- `empty types → empty rows`
- `isGlobal correctly inferred: dictionaryId==null → true, else false`
- `valueCount lookup: present / fallback 0`

#### Total: ~44 теста

---

### 5. ComponentsManagerDatasourceEffectHandlerTest (#33)

**Файл:** `modules/screen/components_manager/src/test/java/me/apomazkin/components_manager/logic/DatasourceEffectHandlerTest.kt`

**Mocks:**
- `useCase: ComponentsManagerUseCase` — mockk либо FakeUseCase (см. соображения mockk + value class в `DatasourceEffectHandlerTest` quizchat).
- `logger: LexemeLogger` — mockk (relaxed).

**Helper:**
```kotlin
private suspend fun runEffect(effect: DatasourceEffect): Msg {
    var captured: Msg = Msg.Empty
    handler.runEffect(effect) { captured = it }
    return captured
}
```

**Тесты по веткам Effect:**

#### 5.1 `CreateComponent` (3 теста)

- `useCase returns Success → emits Msg.CreateResult(Success)`
- `useCase returns SameScopeCollision → emits Msg.CreateResult(SameScopeCollision)`
- `useCase throws → emits Msg.CreateResult(Failure(cause)) + logged` (catch блок в handler #33).

#### 5.2 `RenameComponent` (2 теста)

- `useCase returns Success → emits Msg.RenameResult(Success)`
- `useCase throws → emits Msg.RenameResult(Failure(cause))`

#### 5.3 `LoadImpact` (3 теста)

- `useCase returns impact → emits Msg.ImpactPreviewLoaded(impact)`
- `useCase returns null → emits Msg.ImpactPreviewFailed(IllegalStateException)`
- `useCase throws → emits Msg.ImpactPreviewFailed(cause)`

#### 5.4 `SoftDeleteComponent` (2 теста)

- `useCase returns Success(impact) → emits Msg.DeleteResult(Success(impact))`
- `useCase throws → emits Msg.DeleteResult(Failure(cause))`

#### Total: ~10 тестов

---

### 6. ComponentsManagerFlowHandlerTest (#34)

**Файл:** `modules/screen/components_manager/src/test/java/me/apomazkin/components_manager/logic/AllUserDefinedTypesFlowHandlerTest.kt`

**Convention:** `MutableSharedFlow + runTest + advanceUntilIdle` (по образу `QuizPickerFlowHandlerTest`).

#### 6.1 Subscribe + emit (4 теста)

- `subscribe emits initial Msg.TypesLoaded на первом snapshot`
- `subscribe re-emits Msg.TypesLoaded на каждом новом snapshot`
- `flow throws → emits Msg.TypesLoadFailed(cause) + logged`
- `unsubscribe cancels job — no further emissions после cancel`

#### Total: ~4 теста

---

### 7. PerDictionaryComponentsDatasourceEffectHandlerTest (#43)

**Файл:** `modules/screen/per_dictionary_components/src/test/java/me/apomazkin/per_dictionary_components/logic/DatasourceEffectHandlerTest.kt`

Зеркально #5 (~10 тестов).

---

### 8. ComponentsForDictionaryFlowHandlerTest (#44)

**Файл:** `modules/screen/per_dictionary_components/src/test/java/me/apomazkin/per_dictionary_components/logic/ComponentsForDictionaryFlowHandlerTest.kt`

**Convention:** assisted-inject c `dictionaryId: Long` (см. design tree финальная форма #44). Конструируем напрямую: `ComponentsForDictionaryFlowHandler(dictionaryId=1L, useCase=mock, logger=mock)`.

#### 8.1 Subscribe + dictionaryId (4 теста)

- `subscribe collects useCase.flowComponentsForDictionary(dictId) и emit Msg.ItemsLoaded`
- `flow re-emit → Msg.ItemsLoaded второй раз`
- `flow throws → Msg.ItemsLoadFailed + logged`
- `subscribe передаёт правильный dictionaryId в useCase` (verify).

#### Total: ~4 теста

---

## Существующие тесты — миграция M12→M13

Tier 7 migration call-sites затрагивают существующие тесты. Они **обязаны** быть обновлены синхронно с domain rewrite.

**Файлы и счётчик `ComponentValueData` references:**

| Файл | Refs |
|---|---|
| `modules/screen/wordcard/src/test/.../WordCardUseCaseImplTest.kt` | 15 |
| `modules/domain/lexeme/src/test/.../LexemeMapperTest.kt` | 14 |
| `modules/screen/wordcard/src/test/.../mate/DatasourceEffectHandlerTest.kt` | 12 |
| `modules/screen/quizchat/src/test/.../QuizGameImplTest.kt` | 3 |
| `modules/screen/quizchat/src/test/.../QuizGameImplFetchDataTest.kt` | 3 |
| `modules/domain/lexeme/src/test/.../LexemeBuiltInExtTest.kt` | 3 |

**Pattern replacement (mechanical):**
- `ComponentValueData.TextValue(s)` → `TemplateValues.TextValues(Primitive.Text(s))`
- imports: `me.apomazkin.lexeme.ComponentValueData` → `me.apomazkin.lexeme.TemplateValues; me.apomazkin.lexeme.Primitive`

После replacement тесты должны компилироваться и проходить без изменения assert'ов.

---

## Не покрываем (обоснование)

- **cardinality downgrade (`is_multi=true→false`) + edit component** — design (business_contract + business_design_tree) не покрывает edit-операции. Перенесено в backlog как продолжение фичи (см. `docs/Backlog.md` → IS481 phase 2 + `docs/FlowBacklog.md → IS481cc-F6` process gap). Тесты на edit / downgrade добавятся в следующей итерации.

- **UseCase interfaces** (#25 `ComponentsManagerUseCase`, #26 `PerDictionaryComponentsUseCase`) — interface не testable; behaviour покрыт impl-тестами.
- **Sealed data classes** (`Scope`, `NameError`, `CreateOutcome`, `RenameOutcome`, `DeleteOutcome`, `UserDefinedTypesSnapshot`, `PerDictionarySnapshot`, `ComponentUsage`, `DeletionImpact`, `AffectedQuizConfig`, `Primitive`, `Field`, `TemplateValues`, `TextValues`, `ImageValues`) — value objects, runtime behavior нет. Compile-time exhaustive check на `when` enforce'ит coverage `[guide: state-modeling.md § 4 ADT]`.
- **`State / Msg / Effect`** sealed-структуры — shape проверяется через Reducer тесты (через `result.state()` и `result.effects()`).
- **`LogTags`** (#55 `me.apomazkin.components_manager.LogTags`, #56 `me.apomazkin.per_dictionary_components.LogTags`) — constants, runtime behavior нет.
- **`UiEffectHandler`** (#31, #41) — тривиальное mapping `UiEffect.Snackbar → UiMsg.Snackbar`. Coverage через Reducer (которая dispatch'ит `UiEffect.Snackbar` после `CreateResult.Success`) + manual smoke.
- **`NavigationEffect / NavigationEffectHandler`** (#35, #36, #45, #46) — `Back` уже обработан super-классом `MateNavigationEffectHandler`, экраны не имеют собственных переходов. Coverage через Reducer (`RequestBack → NavigationEffect.Back`).
- **`ViewModel`** (#38, #48) — assisted-factory glue + `Mate` сборка. Behavior покрывается Reducer + EffectHandler тестами; integration smoke — manual.
- **Computed selectors** `isEmpty` (Tier 5 / 6) — однострочный getter, неявно verified через Reducer тесты сравнивающие `state.isEmpty` после `TypesLoaded` / `ItemsLoaded`.
- **Mate framework wiring** — pre-existing tested (`modules/core/mate`).
- **DI binding modules** (`ComponentsManagerModule`, `PerDictionaryComponentsModule`) — infra-phase placeholder, Dagger compile-time verified.
- **Migration call-sites M12 → M13** (#49-#54, Tier 7) — compile-time gates после удаления `ComponentValueData.kt`. Тесты обновляются point-wise если уже существовали для соответствующих UseCase (`WordCardUseCaseImplTest`, `QuizChatUseCaseImplTest` и т.д.) — это уже покрыто `WordCardUseCaseImplTest` и аналогами; rebind tests на `TemplateValues` — на business_implement при компиляции.

---

## Тестовые файлы (создаются на business_implement)

| # | Файл | Tests |
|---|---|---|
| 1 | `app/src/test/java/me/apomazkin/polytrainer/di/module/componentsmanager/ComponentsManagerUseCaseImplTest.kt` | ~21 |
| 2 | `app/src/test/java/me/apomazkin/polytrainer/di/module/perdictionarycomponents/PerDictionaryComponentsUseCaseImplTest.kt` | ~6 |
| 3 | `modules/screen/components_manager/src/test/java/me/apomazkin/components_manager/logic/ComponentsManagerReducerTest.kt` | ~44 |
| 4 | `modules/screen/per_dictionary_components/src/test/java/me/apomazkin/per_dictionary_components/logic/PerDictionaryComponentsReducerTest.kt` | ~44 |
| 5 | `modules/screen/components_manager/src/test/java/me/apomazkin/components_manager/logic/DatasourceEffectHandlerTest.kt` | ~10 |
| 6 | `modules/screen/components_manager/src/test/java/me/apomazkin/components_manager/logic/AllUserDefinedTypesFlowHandlerTest.kt` | ~4 |
| 7 | `modules/screen/per_dictionary_components/src/test/java/me/apomazkin/per_dictionary_components/logic/DatasourceEffectHandlerTest.kt` | ~10 |
| 8 | `modules/screen/per_dictionary_components/src/test/java/me/apomazkin/per_dictionary_components/logic/ComponentsForDictionaryFlowHandlerTest.kt` | ~4 |

**Total:** ~143 теста (8 файлов).

---

## Конвенции

- **Framework:** JUnit 4 + mockk + kotlinx.coroutines.test (`runTest`, `advanceUntilIdle`).
- **Reducer:** `MateReducer.testReduce(state, msg)` + `result.state()` / `result.effects()` / `assertEffects` / `assertNoEffects` / `assertHasEffect` (см. `MateTestHelper`).
- **FlowHandler:** `MutableSharedFlow<T>` + collect emissions в `mutableListOf<Msg>` + `advanceUntilIdle`.
- **EffectHandler:** прямой вызов `handler.runEffect(effect) { captured = it }`.
- **UseCase impl:** mock `CoreDbApi.LexemeApi` + `DictionaryApi` + `PrefsProvider` + assert outcomes / verify side effects.
- **mockk + value class warning:** `ComponentTypeRef` (value class) и `ComponentTypeId` (data class wrapper) — потенциальные камни преткновения в mockk capture. Если воспроизведётся "null packRef" — fallback на FakeUseCase (см. `quiz.chat.logic.DatasourceEffectHandlerTest`).
- **Naming:** `\`<Msg/Effect> <expected behaviour>\`` (backtick names, по образу `ChatReducerTest`).
- **Helper factories** — приватные top-of-class `fun row()`, `fun stateWith…()`, `fun apiEntity()` чтобы не дублировать full constructor.

---

## Принципы

- Спека, не реализация. Реализация — на business_implement.
- Все ветки `when` в Reducer — обязаны иметь хотя бы один тест (exhaustive coverage по `Msg` branches).
- Инварианты `[shape]` / `[transition]` из business_contract — выделены отдельными guard-тестами.
- Имена методов следуют convention существующих тестов проекта (`ChatReducerTest`, `WordCardUseCaseImplTest`, `QuizPickerFlowHandlerTest`).

DONE — business_test.md created.

---

## История ревью

### iter 1 (2026-06-16): qa 10 findings → inquisitor 6 approved + 4 rejected

- F099 (critical): plan миграции existing тестов — добавлена секция «Существующие тесты — миграция M12→M13».
- F100 (critical → backlog): cardinality downgrade out-of-scope → § Не покрываем + Backlog/FlowBacklog.
- F101 (critical): race-condition Close-during-flight — 3 сценария добавлены.
- F102 (critical): ConfirmDelete guard на isLoadingImpact — тест добавлен.
- F103 (critical): orphan prefs reset throws — тест 1.5.6 добавлен с (a)/(b) альтернативой.
- F106 (minor): overwrite dialog reset — invariant зафиксирован.
- F104/F105/F107/F108: rejected (стилистика / out-of-scope).

### iter 2 (2026-06-16): 6 findings fixed.
