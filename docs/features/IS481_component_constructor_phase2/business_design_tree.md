# Business design tree: IS481 component_constructor phase 2

Граф файлов **бизнес-слоя** для phase 2 IS481. UI-узлы вынесены в `## UI dependencies` (декларация для ui sub-flow). Каждый файл verified через `Read`/`ls`.

Маркеры: `[+]` — новый файл; `[~]` — модификация существующего.

---

## Граф (YAML)

```yaml
- id: 0
  file: modules/domain/lexeme/src/main/java/me/apomazkin/lexeme/EditOutcome.kt
  marker: "+"
  layer: domain
  deps: []
  what: "NEW sealed interface EditOutcome (9 variants): Success(ComponentType), NameEmpty, SameScopeCollision, CrossScopeCollision, CardinalityDowngradeBlocked(impactedLexemeIds), TemplateImmutable, BuiltInProtected, Removed, Failure(cause)."

- id: 1
  file: modules/domain/lexeme/src/main/java/me/apomazkin/lexeme/RenameOutcome.kt
  marker: "~"
  layer: domain
  deps: []
  what: "Add `data object Removed : RenameOutcome` (F004 — soft-deleted асимметрия с BuiltInProtected)."

- id: 2
  file: modules/domain/lexeme/src/main/java/me/apomazkin/lexeme/DeleteOutcome.kt
  marker: "~"
  layer: domain
  deps: []
  what: "Add `data object Removed : DeleteOutcome` (повторный soft-delete уже-removed type)."

- id: 3
  file: core/core-db-api/src/main/java/me/apomazkin/core_db_api/entity/ComponentOutcomeApiEntity.kt
  marker: "~"
  layer: api
  deps: []
  what: "Add `sealed interface EditComponentOutcome` (7 variants — без NameEmpty/Failure, они на UseCaseImpl); add `data object Removed` в RenameComponentOutcome и SoftDeleteComponentOutcome."

- id: 4
  file: core/core-db-api/src/main/java/me/apomazkin/core_db_api/CoreDbApi.kt
  marker: "~"
  layer: api
  deps: [3]
  what: "Add `suspend fun editComponentType(typeId, name, template, isMulti): EditComponentOutcome` в `interface LexemeApi`. KDoc фиксирует Removed/BuiltInProtected/Collision/CardinalityDowngradeBlocked/Success ветки + cascade quiz_configs.component_refs на rename."

- id: 5
  file: modules/screen/components_manager/src/main/java/me/apomazkin/components_manager/deps/ComponentsManagerUseCase.kt
  marker: "~"
  layer: deps
  deps: [0]
  what: "Add `suspend fun editComponent(typeId, name, template, isMulti): EditOutcome` (template принимается параметром, immutability — на impl); add `fun flowDictionaries(): Flow<List<DictionaryApiEntity>>` (для multi-dict picker)."

- id: 6
  file: modules/screen/per_dictionary_components/src/main/java/me/apomazkin/per_dictionary_components/deps/PerDictionaryComponentsUseCase.kt
  marker: "~"
  layer: deps
  deps: [0]
  what: "Add `suspend fun editComponent(typeId, name, template, isMulti): EditOutcome`. `flowDictionaries` НЕ добавляется — multi-dict picker отсутствует."

- id: 7
  file: modules/screen/components_manager/src/main/java/me/apomazkin/components_manager/mate/State.kt
  marker: "~"
  layer: mate-manager
  deps: [0]
  what: "Add three fields: `editDialog: EditDialogState? = null`, `isEditing: Boolean = false`, `availableDictionaries: List<DictionaryApiEntity> = emptyList()`. Add EditDialogState data class (epochId, typeId, original*, current name/template/isMulti, nameError, impactedLexemesPreview). Add sealed `ImpactedLexemesPreview { InlineOnly, InlineWithDrillIn }`. Add `EditNameError { NameEmpty, SameScopeCollision, CrossScopeCollision }`. Extend `CreateDialogState` с `selectedDictionaryIds: Set<Long>`. Add `internal val CreateDialogState.canSubmit: Boolean` extension. Update `[shape]` KDoc invariant (4-way mutual exclusion dialogs + 4-way in-flight)."

- id: 8
  file: modules/screen/components_manager/src/main/java/me/apomazkin/components_manager/mate/Msg.kt
  marker: "~"
  layer: mate-manager
  deps: [0]
  what: "Add Edit family (8 cases): OpenEditDialog(typeId), CloseEditDialog, EditNameChange(name), EditTemplateChange(template), EditMultiToggle(isMulti), SubmitEdit, EditResult(epochId, outcome: EditOutcome). Add multi-dict picker Msg: CreateDictionaryToggle(dictionaryId), DictionariesLoaded(dictionaries)."

- id: 9
  file: modules/screen/components_manager/src/main/java/me/apomazkin/components_manager/mate/Reducer.kt
  marker: "~"
  layer: mate-manager
  deps: [7, 8]
  what: "Add 7 Edit-family Reducer ветвей (OpenEditDialog с F138 mutual-exclusion закрытия 3 existing dialogs + epoch bump; EditNameChange/EditTemplateChange/EditMultiToggle с copy; SubmitEdit guard !isEditing + emit DatasourceEffect.EditComponent; EditResult per-variant 9 sub-cases mapping outcome → state/snackbar + stale-epoch check F136). Add 4 existing Open*Dialog ветви — добавить `editDialog=null, isEditing=false` в reset. Add CreateDictionaryToggle (toggle set). Add DictionariesLoaded ветвь: `availableDictionaries=list` + `createDialog?.copy(selectedDictionaryIds = current ∩ list.ids)` chip-staleness filter + invariant editDialog НЕ мутируется (F030). Add RenameResult/DeleteResult Removed-варианты (snackbar 'Компонент удалён' + close dialog)."

- id: 10
  file: modules/screen/components_manager/src/main/java/me/apomazkin/components_manager/mate/DatasourceEffect.kt
  marker: "~"
  layer: mate-manager
  deps: [0]
  what: "Add `data class EditComponent(epochId, typeId, name, template, isMulti) : DatasourceEffect`. Add `data object SubscribeDictionaries : DatasourceEffect` (parity с F163 re-subscribe trigger для DictionariesFlowHandler)."

- id: 11
  file: modules/screen/components_manager/src/main/java/me/apomazkin/components_manager/mate/DatasourceEffectHandler.kt
  marker: "~"
  layer: mate-manager
  deps: [5, 10, 12]  # F-BDT1 closed: depends на DictionariesFlowHandler (#12) — constructor inject
  what: "Add branch `is DatasourceEffect.EditComponent → Msg.EditResult(epochId, useCase.editComponent(typeId, name, template, isMulti))`. Add branch `DatasourceEffect.SubscribeDictionaries → dictionariesFlowHandler.runEffect(...)`. Расширить catch-блок (parity line 91-105 baseline): EditComponent exception → `Msg.EditResult(epoch, EditOutcome.Failure(cause))`. Inject `dictionariesFlowHandler: DictionariesFlowHandler` в constructor."

- id: 12
  file: modules/screen/components_manager/src/main/java/me/apomazkin/components_manager/mate/DictionariesFlowHandler.kt
  marker: "+"
  layer: mate-manager
  deps: [5]
  what: "NEW FlowHandler (template = AllUserDefinedTypesFlowHandler). `subscribe(scope, send)` подписывается на `useCase.flowDictionaries()`, `.catch` emits `Msg.DictionariesLoaded(emptyList())`, `.collectLatest { send(Msg.DictionariesLoaded(list)) }`. `runEffect` обрабатывает `SubscribeDictionaries → unsubscribe + subscribe`. Constructor: `@Inject (useCase: ComponentsManagerUseCase, logger: LexemeLogger)`."

- id: 13
  file: modules/screen/per_dictionary_components/src/main/java/me/apomazkin/per_dictionary_components/mate/State.kt
  marker: "~"
  layer: mate-perdict
  deps: [0]
  what: "Add `editDialog: EditDialogState? = null`, `isEditing: Boolean = false`. Дублируется EditDialogState/ImpactedLexemesPreview/EditNameError (parity с дублированием CreateDialogState/RenameDialogState между двумя screen-модулями). `CreateDialogState` НЕ расширяется (multi-dict picker отсутствует). Update `[shape]` KDoc invariant."

- id: 14
  file: modules/screen/per_dictionary_components/src/main/java/me/apomazkin/per_dictionary_components/mate/Msg.kt
  marker: "~"
  layer: mate-perdict
  deps: [0]
  what: "Add Edit family (7 cases): OpenEditDialog, CloseEditDialog, EditNameChange, EditTemplateChange, EditMultiToggle, SubmitEdit, EditResult. CreateDictionaryToggle/DictionariesLoaded ОТСУТСТВУЮТ."

- id: 15
  file: modules/screen/per_dictionary_components/src/main/java/me/apomazkin/per_dictionary_components/mate/Reducer.kt
  marker: "~"
  layer: mate-perdict
  deps: [13, 14]
  what: "Зеркало #9 (Manager Reducer) минус multi-dict ветви: 7 Edit-ветвей + расширение 3 existing Open*Dialog ветвей `editDialog=null, isEditing=false` + RenameResult/DeleteResult Removed-варианты."

- id: 16
  file: modules/screen/per_dictionary_components/src/main/java/me/apomazkin/per_dictionary_components/mate/DatasourceEffect.kt
  marker: "~"
  layer: mate-perdict
  deps: [0]
  what: "Add `data class EditComponent(epochId, typeId, name, template, isMulti) : DatasourceEffect`. SubscribeDictionaries ОТСУТСТВУЕТ."

- id: 17
  file: modules/screen/per_dictionary_components/src/main/java/me/apomazkin/per_dictionary_components/mate/DatasourceEffectHandler.kt
  marker: "~"
  layer: mate-perdict
  deps: [6, 16]
  what: "Add branch `is DatasourceEffect.EditComponent → Msg.EditResult(epochId, useCase.editComponent(...))`. Catch-блок расширяется: EditComponent → `EditOutcome.Failure(cause)`."

- id: 18
  file: app/src/main/java/me/apomazkin/polytrainer/di/module/componentsmanager/ComponentsManagerUseCaseImpl.kt
  marker: "~"
  layer: app-impl
  deps: [4, 5]
  what: "Implement `editComponent(typeId, name, template, isMulti)` (pattern parity с renameComponent line 83-101): trim+isBlank→NameEmpty; load current type через componentTypeDao для template-immutability gate (template != current.template → TemplateImmutable БЕЗ обращения к API); try `lexemeApi.editComponentType(...)` → exhaustive `when (api)` 7 веток → domain mapping (Success/SameScopeCollision/CrossScopeCollision/CardinalityDowngradeBlocked/TemplateImmutable/BuiltInProtected/Removed); catch (CancellationException → throw, Exception → logger.e + EditOutcome.Failure(e)). Implement `flowDictionaries() = dictionaryApi.flowDictionaryList()` (delegate без mapping, F026). Inject `dictionaryApi: CoreDbApi.DictionaryApi` в constructor (новая зависимость)."

- id: 19
  file: app/src/main/java/me/apomazkin/polytrainer/di/module/perdictionarycomponents/PerDictionaryComponentsUseCaseImpl.kt
  marker: "~"
  layer: app-impl
  deps: [6, 18]
  what: "Implement `editComponent(...) = sharedCrud.editComponent(...)` (DRY-delegation pattern parity с rename/delete delegations на sharedCrud baseline line 46-64)."

- id: 20
  file: modules/screen/components_manager/src/test/java/me/apomazkin/components_manager/mate/ComponentsManagerReducerTest.kt
  marker: "~"
  layer: tests
  deps: [9]
  what: "Add Edit-family reducer tests (parity Rename baseline ~25 tests): Open/Close/NameChange/TemplateChange/MultiToggle/Submit/Result per outcome (9 веток) + F138 mutual-exclusion (OpenEditDialog closes other 3 + reverse) + F140 in-flight reset + F136 stale-epoch + F030 `whenEditDialogOpen_thenDictionariesLoaded_doesNotMutateEditState` + F007 `whenEditResultRemoved_thenDialogClosed_andRemovedSnackbarEmitted` + chip-staleness `whenDictionariesLoaded_thenStaleSelectionsFiltered` + RenameResult/DeleteResult Removed-ветки."

- id: 21
  file: modules/screen/per_dictionary_components/src/test/java/me/apomazkin/per_dictionary_components/mate/PerDictionaryComponentsReducerTest.kt
  marker: "~"
  layer: tests
  deps: [15]
  what: "Зеркало #20 минус multi-dict тесты: Edit-family + F138 4-way mutual-exclusion + Removed-ветки в Rename/Delete/Edit Result."

- id: 22
  file: app/src/test/java/me/apomazkin/polytrainer/di/module/componentsmanager/ComponentsManagerUseCaseImplTest.kt
  marker: "~"
  layer: tests
  deps: [18]
  what: "Add editComponent tests: F022 trim+blank→NameEmpty без API call; F017 template-immutability gate (verify lexemeApi.editComponentType НЕ вызван при template mismatch); F018 cardinality downgrade SELECT precondition (verify НЕ вызван при isMulti unchanged либо upgrade) — это unit-level через mocked API; happy path mapping всех 7 API outcomes → domain; CancellationException re-throw; Exception → Failure(cause); flowDictionaries delegates на dictionaryApi.flowDictionaryList() без transform."

- id: 23
  file: app/src/test/java/me/apomazkin/polytrainer/di/module/perdictionarycomponents/PerDictionaryComponentsUseCaseImplTest.kt
  marker: "~"
  layer: tests
  deps: [19]
  what: "Add `editComponent_delegatesToSharedCrud` test (parity rename/delete delegate tests baseline). flowDictionaries — нет (отсутствует в PerDict interface)."
```

---

## Детали изменений

### #0 — `modules/domain/lexeme/src/main/java/me/apomazkin/lexeme/EditOutcome.kt` [+]

NEW файл рядом с `CreateOutcome.kt` / `RenameOutcome.kt` / `DeleteOutcome.kt`. 9 sealed variants:

```kotlin
sealed interface EditOutcome {
    data class Success(val updated: ComponentType) : EditOutcome
    data object NameEmpty : EditOutcome
    data object SameScopeCollision : EditOutcome
    data object CrossScopeCollision : EditOutcome
    data class CardinalityDowngradeBlocked(val impactedLexemeIds: List<Long>) : EditOutcome
    data object TemplateImmutable : EditOutcome
    data object BuiltInProtected : EditOutcome
    data object Removed : EditOutcome
    data class Failure(val cause: Throwable) : EditOutcome
}
```

Контракт-источник: `business_contract.md § Domain EditOutcome`, `business_contract_spec.md § EditOutcome (NEW)`.

### #1 — `modules/domain/lexeme/.../RenameOutcome.kt` [~]

Add ровно один variant в существующий sealed interface (baseline `RenameOutcome.kt:9-16`):

```kotlin
data object Removed : RenameOutcome
```

Семантика: type.removed_at IS NOT NULL — отличается от BuiltInProtected (F004).

### #2 — `modules/domain/lexeme/.../DeleteOutcome.kt` [~]

Add ровно один variant (baseline `DeleteOutcome.kt:9-13`):

```kotlin
data object Removed : DeleteOutcome
```

### #3 — `core/core-db-api/.../entity/ComponentOutcomeApiEntity.kt` [~]

Add NEW sealed interface `EditComponentOutcome` (7 variants) + `data object Removed` в существующих `RenameComponentOutcome` и `SoftDeleteComponentOutcome`:

```kotlin
sealed interface EditComponentOutcome {
    data class Success(val type: ComponentTypeApiEntity) : EditComponentOutcome
    data object SameScopeCollision : EditComponentOutcome
    data object CrossScopeCollision : EditComponentOutcome
    data class CardinalityDowngradeBlocked(val impactedLexemeIds: List<Long>) : EditComponentOutcome
    data object TemplateImmutable : EditComponentOutcome
    data object BuiltInProtected : EditComponentOutcome
    data object Removed : EditComponentOutcome
}
```

NameEmpty / Failure отсутствуют (валидация и try-catch — на UseCaseImpl, F027).

### #4 — `core/core-db-api/.../CoreDbApi.kt` [~]

Add метод в `interface LexemeApi`:

```kotlin
suspend fun editComponentType(
    typeId: Long,
    name: String,
    template: ComponentTemplate,
    isMulti: Boolean,
): EditComponentOutcome
```

KDoc: cascade quiz_configs.component_refs если name изменился; cardinality downgrade SELECT только при `isMulti=false AND current.isMulti=true` (F018); template-immutability — defensive на API, основная проверка на UseCase.

### #5 — `.../components_manager/deps/ComponentsManagerUseCase.kt` [~]

Add два метода в existing interface (baseline 5 методов, `ComponentsManagerUseCase.kt:20-65`):

```kotlin
suspend fun editComponent(
    typeId: Long,
    name: String,
    template: ComponentTemplate,
    isMulti: Boolean,
): EditOutcome

fun flowDictionaries(): Flow<List<DictionaryApiEntity>>
```

KDoc фиксирует UseCaseImpl-level правила (NameEmpty/TemplateImmutable/Failure) vs API-level (Removed/BuiltIn/Collision/Cardinality/Success).

### #6 — `.../per_dictionary_components/deps/PerDictionaryComponentsUseCase.kt` [~]

Add только `editComponent` (тот же сигнатура). `flowDictionaries` отсутствует — multi-dict picker не применим к PerDict.

### #7 — `.../components_manager/mate/State.kt` [~]

Три новых поля в `ComponentsManagerState`:

```kotlin
val editDialog: EditDialogState? = null,
val isEditing: Boolean = false,
val availableDictionaries: List<DictionaryApiEntity> = emptyList(),
```

Add data class `EditDialogState`:

```kotlin
data class EditDialogState(
    val epochId: Long,
    val typeId: Long,
    val originalName: String,
    val originalTemplate: ComponentTemplate,
    val originalIsMulti: Boolean,
    val name: String,
    val template: ComponentTemplate,
    val isMulti: Boolean,
    val nameError: EditNameError? = null,
    val impactedLexemesPreview: ImpactedLexemesPreview? = null,
)
```

Add sealed `ImpactedLexemesPreview { InlineOnly(ids), InlineWithDrillIn(ids, inlineIds) }`.

Add sealed `EditNameError { NameEmpty, SameScopeCollision, CrossScopeCollision }`.

Extend `CreateDialogState` (baseline line 60-67) — add:

```kotlin
val selectedDictionaryIds: Set<Long> = emptySet(),
```

Add extension val:

```kotlin
internal val CreateDialogState.canSubmit: Boolean
    get() = name.trim().isNotEmpty() && when (scope) {
        Scope.Global -> true
        is Scope.PerDictionaries -> selectedDictionaryIds.isNotEmpty()
    }
```

Update `[shape]` KDoc: dialog mutual-exclusion и in-flight расширяются 4-way (editDialog/isEditing включены).

### #8 — `.../components_manager/mate/Msg.kt` [~]

Add Edit family (8 cases) в существующий `ComponentsManagerMsg` (baseline line 18-79):

```kotlin
data class OpenEditDialog(val typeId: Long) : ComponentsManagerMsg
data object CloseEditDialog : ComponentsManagerMsg
data class EditNameChange(val name: String) : ComponentsManagerMsg
data class EditTemplateChange(val template: ComponentTemplate) : ComponentsManagerMsg
data class EditMultiToggle(val isMulti: Boolean) : ComponentsManagerMsg
data object SubmitEdit : ComponentsManagerMsg
data class EditResult(val epochId: Long, val outcome: EditOutcome) : ComponentsManagerMsg
```

Add multi-dict picker Msg:

```kotlin
data class CreateDictionaryToggle(val dictionaryId: Long) : ComponentsManagerMsg
data class DictionariesLoaded(val dictionaries: List<DictionaryApiEntity>) : ComponentsManagerMsg
```

Note: `OpenCreateDialog` / `CreateScopeChange` уже существуют (line 26-37) — phase 2 делает их живыми, не добавляет.

### #9 — `.../components_manager/mate/Reducer.kt` [~]

**Edit family — 7 новых reducer-ветвей:**

- `OpenEditDialog(typeId)`: epoch bump (`state.nextEpoch + 1`); init `EditDialogState` со snapshot из row; **F138 4-way mutual-exclusion** — обнулить `createDialog=null, renameDialog=null, deleteConfirm=null, isCreating=false, isRenaming=false, isDeleting=false, isEditing=false`.
- `CloseEditDialog`: `editDialog=null, isEditing=false`.
- `EditNameChange(v)`: `editDialog?.copy(name=v, nameError=null)`.
- `EditTemplateChange(t)`: `editDialog?.copy(template=t)` (UI control; immutability gate на submit).
- `EditMultiToggle(b)`: `editDialog?.copy(isMulti=b, impactedLexemesPreview=null)`.
- `SubmitEdit` (guard `!isEditing && editDialog != null && name.trim().isNotBlank()`): `isEditing=true` + emit `DatasourceEffect.EditComponent(epochId, typeId, name, template, isMulti)`.
- `EditResult(epoch, outcome)`: stale-epoch check (F136 parity); per-variant 9 sub-cases:
  - `Success(type)` → `isEditing=false, editDialog=null, snackbar=Snackbar("Updated")`.
  - `NameEmpty/SameScopeCollision/CrossScopeCollision` → `isEditing=false, editDialog?.copy(nameError=...)` — dialog НЕ закрывается.
  - `CardinalityDowngradeBlocked(ids)` → `isEditing=false, editDialog?.copy(impactedLexemesPreview = if (ids.size <= 3) InlineOnly(ids) else InlineWithDrillIn(ids, ids.take(3)))`.
  - `TemplateImmutable/BuiltInProtected/Removed/Failure` → `isEditing=false, editDialog=null, snackbar=Snackbar(<text>)`.
  - F101 race close-during-flight fallback (parity baseline `:127-137`): `if (dlg == null) snackbar fallback`.

**Расширение 3 existing Open\*Dialog ветвей** (baseline `:50-63` Create, `:172-194` Rename, `:285-311` Delete) — добавить `editDialog=null, isEditing=false` в reset-блок.

**Multi-dict scope picker:**
- `CreateDictionaryToggle(id)`: `createDialog?.copy(selectedDictionaryIds = if (id in current) current - id else current + id)`.
- `DictionariesLoaded(list)`: `availableDictionaries=list`; `createDialog?.copy(selectedDictionaryIds = current ∩ list.ids)` chip-staleness filter; **invariant F030** — `editDialog` НЕ мутируется.

**RenameResult/DeleteResult Removed-варианты:**
- `RenameResult(epoch, Removed)` → `isRenaming=false, renameDialog=null, snackbar=Snackbar("Компонент удалён")`.
- `DeleteResult(epoch, Removed)` → `isDeleting=false, deleteConfirm=null, snackbar=Snackbar("Компонент удалён")`.

### #10 — `.../components_manager/mate/DatasourceEffect.kt` [~]

Add в existing sealed interface (baseline line 18-48):

```kotlin
data class EditComponent(
    val epochId: Long,
    val typeId: Long,
    val name: String,
    val template: ComponentTemplate,
    val isMulti: Boolean,
) : DatasourceEffect

data object SubscribeDictionaries : DatasourceEffect
```

### #11 — `.../components_manager/mate/DatasourceEffectHandler.kt` [~]

В existing exhaustive `when (effect)` (baseline `:42-109`) add:

```kotlin
is DatasourceEffect.EditComponent -> Msg.EditResult(
    epochId = effect.epochId,
    outcome = useCase.editComponent(
        typeId = effect.typeId,
        name = effect.name,
        template = effect.template,
        isMulti = effect.isMulti,
    ),
)

DatasourceEffect.SubscribeDictionaries -> dictionariesFlowHandler.runEffect(effect, consumer)
```

Catch-блок (baseline `:91-105`) расширить: `is DatasourceEffect.EditComponent → Msg.EditResult(epochId, EditOutcome.Failure(cause))`.

Constructor: inject `private val dictionariesFlowHandler: DictionariesFlowHandler`.

### #12 — `.../components_manager/mate/DictionariesFlowHandler.kt` [+]

NEW класс (template = `AllUserDefinedTypesFlowHandler.kt:22-59`):

```kotlin
class DictionariesFlowHandler @Inject constructor(
    private val useCase: ComponentsManagerUseCase,
    private val logger: LexemeLogger,
) : MateFlowHandler<Msg, Effect> {

    override var job: Job? = null
    private var scope: CoroutineScope? = null
    private var send: ((Msg) -> Unit)? = null

    override fun subscribe(scope: CoroutineScope, send: (Msg) -> Unit) {
        this.scope = scope
        this.send = send
        job?.cancel()
        job = scope.launch {
            useCase.flowDictionaries()
                .catch { e ->
                    logger.e(LogTags.COMPONENTS_MANAGER, "flowDictionaries failed", e)
                    send(Msg.DictionariesLoaded(emptyList()))
                }
                .collectLatest { list -> send(Msg.DictionariesLoaded(list)) }
        }
    }

    override fun runEffect(effect: Effect, consumer: (Msg) -> Unit) {
        if (effect is DatasourceEffect.SubscribeDictionaries) {
            val s = scope ?: return
            val snd = send ?: return
            unsubscribe()
            subscribe(s, snd)
        }
    }
}
```

### #13 — `.../per_dictionary_components/mate/State.kt` [~]

Add два поля в `PerDictionaryComponentsState`:

```kotlin
val editDialog: EditDialogState? = null,
val isEditing: Boolean = false,
```

`EditDialogState` / `ImpactedLexemesPreview` / `EditNameError` дублируются (parity с дублированием `CreateDialogState` / `RenameDialogState` между двумя screen-модулями — см. baseline). Shared widget extraction — UI sub-flow.

`availableDictionaries` ОТСУТСТВУЕТ — multi-dict picker не применим в PerDict. `CreateDialogState` НЕ расширяется `selectedDictionaryIds`.

Update `[shape]` KDoc invariant (4-way mutual exclusion + 4-way in-flight).

### #14 — `.../per_dictionary_components/mate/Msg.kt` [~]

Add Edit family (7 cases — без CreateDictionaryToggle/DictionariesLoaded):

```kotlin
data class OpenEditDialog(val typeId: Long) : PerDictionaryComponentsMsg
data object CloseEditDialog : PerDictionaryComponentsMsg
data class EditNameChange(val name: String) : PerDictionaryComponentsMsg
data class EditTemplateChange(val template: ComponentTemplate) : PerDictionaryComponentsMsg
data class EditMultiToggle(val isMulti: Boolean) : PerDictionaryComponentsMsg
data object SubmitEdit : PerDictionaryComponentsMsg
data class EditResult(val epochId: Long, val outcome: EditOutcome) : PerDictionaryComponentsMsg
```

### #15 — `.../per_dictionary_components/mate/Reducer.kt` [~]

Зеркало #9 минус multi-dict ветви:
- 7 Edit-family ветвей (тот же pattern + F138/F140/F136/F030 inv).
- Расширение 3 existing Open\*Dialog (baseline `:59-75` Create, `:178-199` Rename, `:286-311` Delete) — add `editDialog=null, isEditing=false`.
- RenameResult/DeleteResult Removed-варианты.
- `CreateDictionaryToggle` / `DictionariesLoaded` ОТСУТСТВУЮТ.

### #16 — `.../per_dictionary_components/mate/DatasourceEffect.kt` [~]

Add только `EditComponent` (без `SubscribeDictionaries`).

### #17 — `.../per_dictionary_components/mate/DatasourceEffectHandler.kt` [~]

Add branch `is DatasourceEffect.EditComponent → Msg.EditResult(epochId, useCase.editComponent(...))`. Catch-блок расширяется аналогично #11 (без SubscribeDictionaries).

### #18 — `app/.../di/module/componentsmanager/ComponentsManagerUseCaseImpl.kt` [~]

**Implement `editComponent` (pattern parity baseline `renameComponent` line 83-101):**

```kotlin
override suspend fun editComponent(
    typeId: Long,
    name: String,
    template: ComponentTemplate,
    isMulti: Boolean,
): EditOutcome {
    val trimmed = name.trim()
    if (trimmed.isBlank()) return EditOutcome.NameEmpty

    return try {
        // Template-immutability gate (F017) — current type lookup
        val current = lexemeApi.getComponentTypeById(typeId)  // или эквивалент существующего lookup
        if (current != null && current.template != template) {
            return EditOutcome.TemplateImmutable
        }

        when (val r = lexemeApi.editComponentType(typeId, trimmed, template, isMulti)) {
            is EditComponentOutcome.Success -> EditOutcome.Success(r.type.toDomain())
            EditComponentOutcome.SameScopeCollision -> EditOutcome.SameScopeCollision
            EditComponentOutcome.CrossScopeCollision -> EditOutcome.CrossScopeCollision
            is EditComponentOutcome.CardinalityDowngradeBlocked ->
                EditOutcome.CardinalityDowngradeBlocked(r.impactedLexemeIds)
            EditComponentOutcome.TemplateImmutable -> EditOutcome.TemplateImmutable
            EditComponentOutcome.BuiltInProtected -> EditOutcome.BuiltInProtected
            EditComponentOutcome.Removed -> EditOutcome.Removed
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        logger.e(LogTags.COMPONENTS_MANAGER, "editComponent failed", e)
        EditOutcome.Failure(e)
    }
}
```

Note: точный API для lookup current type (для template-immutability gate) уточняется в data sub-flow — может быть отдельный `lexemeApi.getComponentTypeById(typeId)` либо immutability полностью на API-уровне (defensive). Контракт допускает оба варианта (см. `business_contract.md § Open Q`).

**Implement `flowDictionaries`:**

```kotlin
override fun flowDictionaries(): Flow<List<DictionaryApiEntity>> =
    dictionaryApi.flowDictionaryList()
```

**Inject `dictionaryApi: CoreDbApi.DictionaryApi`** в constructor (новая зависимость, см. walkthrough §2: existing pattern в `SplashUseCaseImpl`, `DictionaryAppBarUseCaseImpl`, `DictionaryUseCaseImpl`).

### #19 — `app/.../di/module/perdictionarycomponents/PerDictionaryComponentsUseCaseImpl.kt` [~]

DRY-delegation pattern (parity baseline `:46-64`):

```kotlin
override suspend fun editComponent(
    typeId: Long,
    name: String,
    template: ComponentTemplate,
    isMulti: Boolean,
): EditOutcome = sharedCrud.editComponent(typeId, name, template, isMulti)
```

### #20 — `.../components_manager/src/test/.../ComponentsManagerReducerTest.kt` [~]

Add Edit-family reducer tests (~25 кейсов, parity с Rename baseline `:513-531` mutual-exclusion + per-outcome ветки):
- `Open/Close/NameChange/TemplateChange/MultiToggle/Submit` — happy path state транзишены.
- 9 EditResult outcome веток (Success, NameEmpty, SameScope, CrossScope, CardinalityDowngrade {size≤3, size>3}, TemplateImmutable, BuiltInProtected, Removed, Failure).
- F138 mutual-exclusion: `OpenEditDialog closes other 3 dialogs (F138)` + 3 reverse кейса (`OpenCreate/Rename/DeleteConfirm closes editDialog`).
- F140 in-flight reset: `OpenEditDialog resets isCreating/isRenaming/isDeleting` + reverse.
- F136 stale-epoch: `EditResult ignored when epochId stale`.
- F030 invariant: `whenEditDialogOpen_thenDictionariesLoaded_doesNotMutateEditState`.
- F007 Removed: `whenEditResultRemoved_thenDialogClosed_andRemovedSnackbarEmitted` + аналог для Rename/Delete.
- Chip-staleness: `whenDictionariesLoaded_thenStaleSelectionsFiltered`, `whenCreateDictionaryToggle_thenSetUpdated`.
- F139 double-tap: `SubmitEdit while isEditing=true → no state change`.
- F101 race close-during-flight: `EditResult arrives after CloseEditDialog → snackbar fallback`.

### #21 — `.../per_dictionary_components/src/test/.../PerDictionaryComponentsReducerTest.kt` [~]

Зеркало #20 минус multi-dict тесты: Edit-family + F138 4-way + F140 4-way + F136 + F101 + Removed для Rename/Delete/Edit.

### #22 — `app/.../test/componentsmanager/ComponentsManagerUseCaseImplTest.kt` [~]

Add editComponent tests:
- `editComponent_emptyName_returnsNameEmpty_withoutApiCall` (F022 — verify api НЕ вызван).
- `editComponent_templateMismatch_returnsTemplateImmutable_withoutApiCall` (F017 — verify api НЕ вызван при template mismatch).
- `editComponent_apiSuccess_returnsDomainSuccess` + mapping для каждой API outcome ветки (7 case).
- `editComponent_apiCardinalityDowngradeBlocked_returnsDomainWithIds`.
- `editComponent_apiRemoved_returnsDomainRemoved`.
- `editComponent_cancellationException_rethrows`.
- `editComponent_genericException_returnsFailure_andLogs`.
- F018 precondition (на UseCaseImpl-level через mocked API): `editComponent_isMultiUnchanged_doesNotTriggerCardinalitySelect` (verify через mock что upstream API call параметры корректны — фактический skip cardinality SELECT — это data-layer test; здесь только что параметры переданы).
- `flowDictionaries_delegatesToDictionaryApi` — verify `dictionaryApi.flowDictionaryList()` вызван, transform нет.

### #23 — `app/.../test/perdictionarycomponents/PerDictionaryComponentsUseCaseImplTest.kt` [~]

Add `editComponent_delegatesToSharedCrud` test (parity с baseline rename/delete delegate tests).

---

## UI dependencies

Декларация для UI sub-flow — НЕ часть business design tree, но должна быть учтена при оркестрации фаз.

- **`CreateComponentDialog`** (`@Composable` widget) — расширить scope picker chip-list для multi-select словарей (управляется `Msg.CreateDictionaryToggle` + `state.availableDictionaries` + `state.createDialog.selectedDictionaryIds`). Submit-кнопка disable'ится через `canSubmit` extension val.
- **`EditComponentDialog`** [NEW] — новый `@Composable` widget для редактирования (поля: name input, template picker disabled либо readonly, isMulti toggle, cardinality downgrade preview block). Принимает `EditDialogState` + callbacks для Edit-family Msg.
- **`DeleteComponentConfirmDialog`** — extract в shared widget module (`:modules:widget:component_widgets`) — UI sub-flow.
- **`RenameComponentDialog`** — extract в shared widget module.
- **`UserDefinedRowWidget`** — добавить Edit action button (callback `onEdit(typeId)` → `Msg.OpenEditDialog(typeId)`).
- **`CardinalityDowngradePreviewWidget`** [NEW] — inline preview top-3 + drill-in кнопка (рендерит `ImpactedLexemesPreview.InlineOnly` vs `InlineWithDrillIn`).
- **`ComponentsManagerScreen`** — поменять import paths на shared widget module; mount `EditComponentDialog` (visible iff `state.editDialog != null`); подключить multi-dict scope picker chip-list к `state.availableDictionaries`.
- **`PerDictionaryComponentsScreen`** — analogous: import paths shared widget module; mount EditDialog (БЕЗ scope picker).
- **Shared widget module `:modules:widget:component_widgets`** [NEW] — создание модуля, build.gradle, package + миграция дублированных composables. Отдельный design tree для UI sub-flow.
- **Strings (`core-resources`)** — `edit_component_title`, `edit_component_submit_label`, `edit_template_immutable_message`, `edit_built_in_protected_message`, `component_removed_message`, `cardinality_downgrade_blocked_inline_label`, `cardinality_downgrade_blocked_drill_in_label` (или эквиваленты).

---

## log_messages

- iter 1: business design tree составлен — 24 узла (3 domain + 2 api + 11 mate + 2 deps + 2 app-impl + 4 tests), DAG verified против `business_contract.md` + `business_contract_spec.md` + `business_walkthrough.md`; UI узлы вынесены в `## UI dependencies` (10 деклараций); все file: пути verified через ls/Read.
- decision: точный API lookup для template-immutability gate в #18 (отдельный `lexemeApi.getComponentTypeById` vs полностью defensive на API) оставлен открытым — допускается data sub-flow design.
- decision: вынос shared widget module (`:modules:widget:component_widgets`) и extract composables — полностью в UI sub-flow scope (не входит в business design tree).

_model: claude-opus-4-7[1m]_
