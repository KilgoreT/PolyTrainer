# Business contract: IS481 component_constructor phase 2

Контракт описывает **расширения** существующих business-артефактов phase 1 (см. `business_walkthrough.md` § 1 для baseline). Edit-ветка зеркалит Rename-ветку; multi-dict scope picker расширяет existing `CreateDialogState.scope` живой реакцией на UI; `Removed` variant добавляется в Rename/Delete/Edit outcomes (Create — асимметрично без `Removed`, см. `02_scope.md` § Замысел).

Скоуп файла: `:modules:domain:lexeme`, `core/core-db-api` (API outcome), `:modules:screen:components_manager` (Manager), `:modules:screen:per_dictionary_components` (PerDict), `app/.../di/module/{componentsmanager,perdictionarycomponents}/*UseCaseImpl.kt`. Логика data-orchestration в `LexemeApiImpl` — `data_contract` (отдельный sub-flow), здесь только API сигнатура.

---

## State

### `:modules:screen:components_manager` — `mate/State.kt`

#### Расширение `ComponentsManagerScreenState`

Добавляются **три** новых поля (положение — параллельно existing `createDialog / renameDialog / deleteConfirm`):

```kotlin
data class ComponentsManagerScreenState(
    // ... existing fields (rows, isLoading, createDialog, renameDialog, deleteConfirm,
    //                     isCreating, isRenaming, isDeleting, nextEpoch, errorState)

    // NEW phase 2:
    val editDialog: EditDialogState? = null,
    val isEditing: Boolean = false,
    val availableDictionaries: List<DictionaryApiEntity> = emptyList(),
)
```

**KDoc invariant (расширение existing `[shape]` invariant):**
> `createDialog / renameDialog / deleteConfirm / editDialog`: одновременно открыт не более одного диалога (enforced в Reducer через mutual-exclusion в Open*Dialog ветках — F138).
> `isCreating / isRenaming / isDeleting / isEditing`: одновременно in-flight не более одной write-operation (F140).

#### `CreateDialogState` (MODIFY existing)

Расширение existing record (`State.kt:60-67`) — добавляются два поля для multi-dict scope picker:

```kotlin
data class CreateDialogState(
    val epochId: Long,
    val name: String = "",
    val template: ComponentTemplate = ComponentTemplate.TEXT,
    val isMulti: Boolean = false,
    val scope: Scope = Scope.Global,             // existing — теперь live (см. Msg.CreateScopeChange)
    val nameError: CreateNameError? = null,      // existing
    // NEW phase 2 — multi-select chip-list state:
    val selectedDictionaryIds: Set<Long> = emptySet(),
)
```

**Decision rationale (см. `02_scope.md` § Open Q7 best-guess + § Аспекты `dictionary_chip_staleness`):**
- `availableDictionaries` живёт в outer `ComponentsManagerScreenState` (а не в `CreateDialogState`) — Source-of-truth снаружи диалога; dialog содержит только **selection** (`selectedDictionaryIds: Set<Long>`). При `DictionariesLoaded(updated)` Reducer фильтрует `selectedDictionaryIds ∩ updated.ids` — выкидывает stale id из selection (см. `Msg.DictionariesLoaded` ниже).
- Тип элемента `availableDictionaries` — existing `DictionaryApiEntity` (из `core-db-api`), без promoted domain entity (F026 решение — UseCaseImpl делегирует на `dictionaryApi.flowDictionaryList()` без mapping).
- `selectedDictionaryIds: Set<Long>` (не `List`) — index intersection через Set (`02_scope.md` § Аспекты `dictionary_chip_staleness` цитирует guide state-modeling.md «State как БД: index intersection через Set»).

**Computed: `canSubmit` (extension val, не stored)** — `02_scope.md` § Open Q7 best-guess: submit-кнопка disabled если `name.isBlank() || (scope is PerDictionaries && selectedDictionaryIds.isEmpty())`.

```kotlin
internal val CreateDialogState.canSubmit: Boolean
    get() = name.trim().isNotEmpty() && when (scope) {
        Scope.Global -> true
        is Scope.PerDictionaries -> selectedDictionaryIds.isNotEmpty()
    }
```

#### `EditDialogState` (NEW)

```kotlin
data class EditDialogState(
    val epochId: Long,
    val typeId: ComponentTypeId,
    val originalName: String,                    // snapshot для diff (template-immutable check на UseCase)
    val originalTemplate: ComponentTemplate,
    val originalIsMulti: Boolean,
    val name: String,                            // current input
    val template: ComponentTemplate,             // UI state (контролируется UI; immutability — на UseCase)
    val isMulti: Boolean,                        // current toggle
    val nameError: EditNameError? = null,        // mirror CreateNameError
    val impactedLexemesPreview: ImpactedLexemesPreview? = null,
)
```

**`ImpactedLexemesPreview`** — explicit state-flag для cardinality downgrade preview (F023 edge-cases):

```kotlin
sealed interface ImpactedLexemesPreview {
    val impactedLexemeIds: List<Long>            // full list (для drill-in)

    /** 1 ≤ size ≤ 3 — inline-preview всех; drill-in кнопка скрыта (F023). */
    data class InlineOnly(override val impactedLexemeIds: List<Long>) : ImpactedLexemesPreview

    /** size > 3 — top-3 в `inlineIds`, full в `impactedLexemeIds`; drill-in видна (F023). */
    data class InlineWithDrillIn(
        override val impactedLexemeIds: List<Long>,
        val inlineIds: List<Long>,               // первые 3 из `impactedLexemeIds` (deterministic sort — § Аспекты)
    ) : ImpactedLexemesPreview
}
```

**Decision rationale:**
- `size == 0` — `EditOutcome.Success` (downgrade проходит); preview не рендерится. Не моделируем как ветку sealed.
- Explicit sealed (а не computed) — соответствует MEMORY правилу «Explicit state flags must be explicit fields in state, not computed in composable» (см. `02_scope.md` ссылка на guide state-and-extensions.md).
- `inlineIds: List<Long>` хранится явно — детерминизм сортировки top-3 фиксирован в § Аспекты `cardinality_downgrade_guard` (`ORDER BY updated_at DESC, lexeme_id ASC`), Reducer не пересортирует.

**`EditNameError`** — mirror existing `CreateNameError` / `RenameNameError`:

```kotlin
sealed interface EditNameError {
    data object NameEmpty : EditNameError
    data object SameScopeCollision : EditNameError
    data object CrossScopeCollision : EditNameError
}
```

(`CardinalityDowngradeBlocked` / `TemplateImmutable` / `BuiltInProtected` / `Removed` / `Failure` — НЕ через `nameError`, через top-level UI reactions: snackbar + close dialog либо preview ветка.)

---

### `:modules:screen:per_dictionary_components` — `mate/State.kt`

#### Расширение `PerDictionaryComponentsState`

Добавляются **два** новых поля (без `availableDictionaries` — multi-dict picker отсутствует в PerDict):

```kotlin
data class PerDictionaryComponentsState(
    // ... existing fields (dictionaryId, rows, isLoading, createDialog, renameDialog,
    //                     deleteConfirm, isCreating, isRenaming, isDeleting, nextEpoch, errorState)

    // NEW phase 2:
    val editDialog: EditDialogState? = null,
    val isEditing: Boolean = false,
)
```

`EditDialogState` / `ImpactedLexemesPreview` / `EditNameError` — структурно идентичны Manager-варианту; **дублируются в модуле PerDict** (parity с existing `CreateDialogState` / `RenameDialogState` / etc., которые также дублируются между двумя screen-модулями — см. baseline `business_walkthrough.md` § 1). Shared widget-уровень (UI composables в `:modules:widget:component_widgets`) — другой sub-flow, business-state остаётся per-module.

`CreateDialogState` в PerDict **не расширяется** new полями (multi-dict picker отсутствует — `02_scope.md` § Затронутые файлы PerDict).

---

## Msg

### `:modules:screen:components_manager` — `mate/Msg.kt`

#### Edit family (NEW — 8 case'ов)

```kotlin
sealed interface ComponentsManagerMsg : Msg {
    // ... existing Create/Rename/Delete/UiMsg families

    // NEW phase 2 — Edit family (mirror Rename + template control + cardinality state):
    data class OpenEditDialog(val typeId: ComponentTypeId) : ComponentsManagerMsg
    data object CloseEditDialog : ComponentsManagerMsg
    data class EditNameChange(val name: String) : ComponentsManagerMsg
    data class EditTemplateChange(val template: ComponentTemplate) : ComponentsManagerMsg
    data class EditMultiToggle(val isMulti: Boolean) : ComponentsManagerMsg
    data object SubmitEdit : ComponentsManagerMsg
    data class EditResult(val epochId: Long, val outcome: EditOutcome) : ComponentsManagerMsg

    // NEW phase 2 — Multi-dict scope picker (CreateScopeChange — existing, see baseline):
    data class CreateDictionaryToggle(val dictionaryId: Long) : ComponentsManagerMsg
    data class DictionariesLoaded(val dictionaries: List<DictionaryApiEntity>) : ComponentsManagerMsg
}
```

**Notes:**
- `OpenCreateDialog` / `CreateScopeChange(scope: Scope)` — **existing** Msg (baseline § 1, `Msg.kt:26-37`). Phase 2 делает их **живыми** в Manager — реакция Reducer уже зашита, но multi-dict UI ранее не дёргал. `02_scope.md` task hint указывает «(`Msg.OpenCreateDialog` / `Msg.CreateScopeChange(scope)` — ранее dead-Msg `CreateScopeChange` теперь живой)».
- `CreateDictionaryToggle(dictionaryId)` — multi-select chip toggle; Reducer добавляет/удаляет id из `createDialog.selectedDictionaryIds`. `02_scope.md` § Открытые вопросы Q7 best-guess фиксирует это поведение.
- `DictionariesLoaded(list)` — emit'ится новым `DictionariesFlowHandler` (push pattern, см. § Аспекты `dictionary_chip_staleness` Open Q4 решение «push через новый FlowHandler»). Reducer обновляет `availableDictionaries = list` + фильтрует `createDialog.selectedDictionaryIds ∩ list.ids` (chip-staleness F006); **EditDialogState не мутируется** (F030 invariant — `whenEditDialogOpen_thenDictionariesLoaded_doesNotMutateEditState`).
- `EditResult.outcome: EditOutcome` — domain sealed (см. § Domain ниже), не API-уровень.
- `SubmitEdit` — без параметров (значения берутся из `state.editDialog`); mirror `SubmitCreate` / `SubmitRename` baseline.

#### Mutual exclusion F138 — что нужно добавить в Open\*Dialog ветках

Все **четыре** Open\*Dialog ветки (`OpenCreateDialog` / `OpenRenameDialog` / `OpenDeleteConfirm` / `OpenEditDialog`) должны в reset-блоке обнулять остальные три dialog-state поля + сбрасывать остальные три `is*ing` флага. То есть baseline три ветки (existing) обогащаются `editDialog = null, isEditing = false`, а новая `OpenEditDialog` повторяет pattern + закрывает три existing dialogs + обнуляет три existing in-flight флага. Конкретные reducer-кейсы — `02_scope.md` § Tests F008.

### `:modules:screen:per_dictionary_components` — `mate/Msg.kt`

#### Edit family (NEW — 7 case'ов, без dictionary-toggle / loaded)

```kotlin
sealed interface PerDictionaryComponentsMsg : Msg {
    // ... existing Create/Rename/Delete/UiMsg families

    // NEW phase 2 — Edit family (зеркало Manager, минус multi-dict scope):
    data class OpenEditDialog(val typeId: ComponentTypeId) : PerDictionaryComponentsMsg
    data object CloseEditDialog : PerDictionaryComponentsMsg
    data class EditNameChange(val name: String) : PerDictionaryComponentsMsg
    data class EditTemplateChange(val template: ComponentTemplate) : PerDictionaryComponentsMsg
    data class EditMultiToggle(val isMulti: Boolean) : PerDictionaryComponentsMsg
    data object SubmitEdit : PerDictionaryComponentsMsg
    data class EditResult(val epochId: Long, val outcome: EditOutcome) : PerDictionaryComponentsMsg
}
```

`DictionariesLoaded` / `CreateDictionaryToggle` — **отсутствуют** (multi-dict scope не относится к PerDict).

---

## Effect/IO

### DatasourceEffect

#### `:modules:screen:components_manager` — `mate/DatasourceEffect.kt`

```kotlin
sealed interface DatasourceEffect : Effect {
    // ... existing: CreateComponent, RenameComponent, LoadImpact, SoftDeleteComponent,
    //               LoadAllUserDefinedTypes

    // NEW phase 2:
    data class EditComponent(
        val epochId: Long,
        val typeId: ComponentTypeId,
        val name: String,
        val template: ComponentTemplate,         // принимается параметром (immutability — на UseCaseImpl, F017)
        val isMulti: Boolean,
    ) : DatasourceEffect

    /**
     * Re-subscribe trigger для DictionariesFlowHandler (parity с LoadAllUserDefinedTypes / F163).
     * Emit'ится при error в подписке либо при manual refresh.
     */
    data object SubscribeDictionaries : DatasourceEffect
}
```

**Decision rationale:**
- `EditComponent` несёт `epochId` — F124/F136 correlation (parity с CreateComponent / RenameComponent / SoftDeleteComponent baseline).
- `EditComponent` несёт `template` — UseCaseImpl сравнивает с current type.template; если mismatch → `EditOutcome.TemplateImmutable` без data API. Это **business rule** на UseCaseImpl, не invariant data-layer (`02_scope.md` § Замысел задачи F017).
- `SubscribeDictionaries` — опциональный re-subscribe trigger (см. F163 pattern существующий для `LoadAllUserDefinedTypes`). Может быть emit'ен Reducer на первичную инициализацию либо после restoreState. Если не нужен в MVP — можно опустить (FlowHandler автоподписывается через `subscribe()`).

#### `:modules:screen:per_dictionary_components` — `mate/DatasourceEffect.kt`

```kotlin
sealed interface DatasourceEffect : Effect {
    // ... existing

    // NEW phase 2 (mirror Manager, без SubscribeDictionaries):
    data class EditComponent(
        val epochId: Long,
        val typeId: ComponentTypeId,
        val name: String,
        val template: ComponentTemplate,
        val isMulti: Boolean,
    ) : DatasourceEffect
}
```

### DatasourceEffectHandler — расширение

Обе `mate/DatasourceEffectHandler.kt` (Manager + PerDict) получают новый branch в exhaustive `when (effect) { ... }` (pattern из baseline `DatasourceEffectHandler.kt:42-109`):

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
```

В Manager handler — также branch `DatasourceEffect.SubscribeDictionaries -> dictionariesFlowHandler.runEffect(...)` (если выбран F163 pattern с явным re-subscribe trigger; см. baseline `AllUserDefinedTypesFlowHandler.runEffect` line 33-40).

Catch-блок (line 91-105) расширяется аналогично: при exception в `editComponent` → `Msg.EditResult(epochId, EditOutcome.Failure(e))`.

### FlowHandler — NEW

#### `:modules:screen:components_manager/.../mate/DictionariesFlowHandler.kt` (NEW)

Template — `AllUserDefinedTypesFlowHandler` (baseline § 4, `AllUserDefinedTypesFlowHandler.kt:22-59`):

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
                    // нет TypesLoadFailed-эквивалента в Msg для dictionaries в MVP;
                    // best-guess: эмитим пустой список (см. Open Q ниже)
                    send(Msg.DictionariesLoaded(emptyList()))
                }
                .collectLatest { list ->
                    send(Msg.DictionariesLoaded(list))
                }
        }
    }

    override fun runEffect(effect: Effect, consumer: (Msg) -> Unit) {
        // SubscribeDictionaries (опц.) → unsubscribe + subscribe
        if (effect is DatasourceEffect.SubscribeDictionaries) {
            val s = scope ?: return
            val snd = send ?: return
            unsubscribe()
            subscribe(s, snd)
        }
    }
}
```

**Decision rationale (Open Q4 best-guess в `02_scope.md`):** push через FlowHandler (parity с `AllUserDefinedTypesFlowHandler`) — реактивно обновляет `availableDictionaries` при add/delete dictionary out-of-band, автоматически триггерит chip-staleness фильтрацию (F006). Альтернатива (pull on OpenCreateDialog) — отвергнута (stale data risk).

**Open для дизайна Q (не блокирует контракт, фиксируется в plan):** обработка error в `.catch` ветке — emit пустого списка vs новый `Msg.DictionariesLoadFailed(e)` ветка. MVP best-guess — пустой список (chip-list скроется, scope picker degrade'ит к Global only). Параллель с `TypesLoadFailed` baseline (`AllUserDefinedTypesFlowHandler.kt:51`) подсказывает использовать typed-failure Msg для парности — но для MVP overhead избыточен.

#### PerDict — БЕЗ нового FlowHandler

`PerDictionaryComponentsScreen` не показывает multi-dict picker; `DictionariesFlowHandler` не нужен.

---

## UseCase

### `:modules:screen:components_manager/.../deps/ComponentsManagerUseCase.kt`

**Расширение existing interface** (baseline `ComponentsManagerUseCase.kt:20-65` — `flowAllUserDefinedTypes` / `createUserDefinedComponent` / `renameComponent` / `previewDeletionImpact` / `softDeleteComponent`):

```kotlin
interface ComponentsManagerUseCase {
    // ... existing 5 methods

    // NEW phase 2:

    /**
     * Edit existing user-defined component_type — name / template / isMulti.
     *
     * Business rules (UseCaseImpl-level):
     * - name.trim().isBlank() → EditOutcome.NameEmpty (без обращения к data API).
     * - template != current.template → EditOutcome.TemplateImmutable (без обращения к data API; F017).
     * - exception (включая CancellationException re-throw) → EditOutcome.Failure(cause).
     *
     * API-level (LexemeApi.editComponentType):
     * - removed_at IS NOT NULL → EditOutcome.Removed (F012).
     * - system_key IS NOT NULL → EditOutcome.BuiltInProtected.
     * - same-scope name collision (removed_at IS NULL filter) → EditOutcome.SameScopeCollision.
     * - cross-scope name collision → EditOutcome.CrossScopeCollision.
     * - cardinality downgrade (isMulti true→false при impacted lexemes) → EditOutcome.CardinalityDowngradeBlocked(ids).
     * - success → EditOutcome.Success (cascade quiz_configs.component_refs если name изменился).
     */
    suspend fun editComponent(
        typeId: ComponentTypeId,
        name: String,
        template: ComponentTemplate,
        isMulti: Boolean,
    ): EditOutcome

    /**
     * Reactive subscription на список словарей (для multi-dict scope picker в Create-диалоге).
     * Делегирует на dictionaryApi.flowDictionaryList() без mapping (F026).
     */
    fun flowDictionaries(): Flow<List<DictionaryApiEntity>>
}
```

**Notes:**
- `editComponent` сигнатура: `(typeId, name, template, isMulti) → EditOutcome`. **`Scope` параметра нет** — phase 2 не позволяет менять scope (F017 решение; scope editing — отдельная фича вне scope).
- **`typeId: ComponentTypeId`** в UseCase / Msg / Effect / State (typed inline class — convention проекта, type-safety vs голый Long). На data API boundary (`LexemeApi.editComponentType(typeId: Long, ...)`) UseCaseImpl делает `typeId.id` для извлечения raw Long — parity с rename/softDelete API.
- `flowDictionaries()` возвращает `List<DictionaryApiEntity>` (existing API entity), не promoted domain entity (F026). Импорт `me.apomazkin.core_db_api.entity.DictionaryApiEntity` в Manager-модуле — новая зависимость на `:core:core-db-api` (уже есть transitive через `LexemeApi`).

### `:modules:screen:per_dictionary_components/.../deps/PerDictionaryComponentsUseCase.kt`

**Расширение existing interface** (baseline `PerDictionaryComponentsUseCase.kt:22-46`):

```kotlin
interface PerDictionaryComponentsUseCase {
    // ... existing 6 methods (flowComponentsForDictionary + 5 shared CRUD)

    // NEW phase 2 (без flowDictionaries — нет multi-dict picker в PerDict):
    suspend fun editComponent(
        typeId: ComponentTypeId,
        name: String,
        template: ComponentTemplate,
        isMulti: Boolean,
    ): EditOutcome
}
```

**UseCaseImpl delegation pattern** (`02_scope.md` § Затронутые файлы PerDict — UseCaseImpl делегирует на `sharedCrud: ComponentsManagerUseCase`, baseline `PerDictionaryComponentsUseCaseImpl.kt:29-65`):

`editComponent` либо делегирует на `sharedCrud.editComponent(...)` (DRY — единая реализация template-immutability gate + API→domain mapping живёт в Manager UseCaseImpl), либо имеет собственную реализацию с тем же gate. Решение оставлено за business sub-flow design шагом — оба варианта валидны.

### Domain — `:modules:domain:lexeme`

#### `EditOutcome` (NEW — `EditOutcome.kt` рядом с `CreateOutcome.kt` / `RenameOutcome.kt`)

**9 вариантов на domain level** (F015 + F027):

```kotlin
sealed interface EditOutcome {
    /** UPDATE прошёл; cascade quiz_configs.component_refs выполнен если name изменился. */
    data class Success(val updated: ComponentType) : EditOutcome  // parity с RenameOutcome.Success(ComponentType) — F-BCR1 finalized after review iter 1

    /** Валидация на UseCaseImpl: trim().isBlank(). */
    data object NameEmpty : EditOutcome

    /** Name занят в том же scope (dictionary_id + system_key IS NULL + removed_at IS NULL). */
    data object SameScopeCollision : EditOutcome

    /** Name занят в global / другом dict (concept policy phase 1). */
    data object CrossScopeCollision : EditOutcome

    /**
     * Downgrade `isMulti: true → false` заблокирован — есть лексемы с count > 1.
     * `impactedLexemeIds` — **полный** список ids в порядке сортировки (F-BCR2 finalized after review iter 1).
     * API/Data слой возвращает full list (без LIMIT в SQL — нужен общий size для UI «Показать все»).
     * Сортировка: `ORDER BY component_values.updated_at DESC, lexeme_id ASC` (deterministic, F018) — выполняется на data-уровне в SELECT.
     * Reducer (при `Msg.EditResult(CardinalityDowngradeBlocked)`) делит:
     *   - top-3 (`take(3)`) → `ImpactedLexemesPreview.InlineOnly(inlineIds)` если size ≤ 3, иначе `InlineWithDrillIn(inlineIds = take(3), fullIds = all)`.
     *   - split-logic — в Reducer (ui-state mapping), не в API/UseCase.
     */
    data class CardinalityDowngradeBlocked(val impactedLexemeIds: List<Long>) : EditOutcome

    /**
     * Попытка изменить template — запрещено по `template_model.md § Open Q10`.
     * UseCaseImpl возвращает без обращения к data API (F017).
     */
    data object TemplateImmutable : EditOutcome

    /** type.system_key IS NOT NULL — нельзя редактировать встроенный. */
    data object BuiltInProtected : EditOutcome

    /** type.removed_at IS NOT NULL — soft-deleted; асимметрия F004 vs Create. */
    data object Removed : EditOutcome

    /** Exception на data layer (try-catch на UseCaseImpl). */
    data class Failure(val cause: Throwable) : EditOutcome
}
```

#### `RenameOutcome` (MODIFY — `RenameOutcome.kt:9-16`)

Добавить **один** variant:

```kotlin
sealed interface RenameOutcome {
    // ... existing: Success(ComponentType), NameEmpty, SameScopeCollision,
    //               CrossScopeCollision, BuiltInProtected, Failure(cause)

    /** type.removed_at IS NOT NULL — soft-deleted; не путать с BuiltInProtected (F004). */
    data object Removed : RenameOutcome
}
```

#### `DeleteOutcome` (MODIFY — `DeleteOutcome.kt:9-13`)

Добавить **один** variant:

```kotlin
sealed interface DeleteOutcome {
    // ... existing: Success(DeletionImpact), BuiltInProtected, Failure(cause)

    /** type.removed_at IS NOT NULL — повторный soft-delete; не путать с BuiltInProtected (F004). */
    data object Removed : DeleteOutcome
}
```

#### `CreateOutcome` — без изменений

`Removed` НЕ добавляется — Create не оперирует existing `type.id`; soft-deleted name-коллизия покрывается `SameScopeCollision` / `CrossScopeCollision` (existing `removed_at IS NULL` фильтр на уникальности). См. `02_scope.md` § Замысел задачи + § Аспекты `soft_deleted_removed_outcome` (F004).

### API — `core/core-db-api/.../entity/ComponentOutcomeApiEntity.kt`

#### `EditComponentOutcome` (NEW — в существующем файле, F001)

**7 вариантов** (parity с Create/Rename/SoftDelete API outcome pattern; F015 + F027):

```kotlin
sealed interface EditComponentOutcome {
    data class Success(val type: ComponentTypeApiEntity) : EditComponentOutcome
    data object SameScopeCollision : EditComponentOutcome
    data object CrossScopeCollision : EditComponentOutcome
    data class CardinalityDowngradeBlocked(val impactedLexemeIds: List<Long>) : EditComponentOutcome
    /** Defensive parity — основная проверка на UseCase, но API возвращает defense-in-depth. */
    data object TemplateImmutable : EditComponentOutcome
    data object BuiltInProtected : EditComponentOutcome
    data object Removed : EditComponentOutcome
}
```

**НЕ входит в API (F027):**
- `NameEmpty` — валидация на UseCaseImpl (`trimmed.isBlank()` → domain `EditOutcome.NameEmpty`).
- `Failure(cause)` — try-catch на UseCaseImpl (`catch (Exception)` → domain `EditOutcome.Failure(cause)`).

#### `RenameComponentOutcome` (MODIFY)

Добавить `data object Removed : RenameComponentOutcome`.

#### `SoftDeleteComponentOutcome` (MODIFY)

Добавить `data object Removed : SoftDeleteComponentOutcome`.

### LexemeApi — `core/core-db-api/.../CoreDbApi.kt`

Добавить новый метод:

```kotlin
interface LexemeApi {
    // ... existing methods

    /**
     * Edit user-defined component_type — UPDATE name / template / isMulti.
     *
     * Template принимается параметром — immutability check на UseCase уровне (F017).
     * Cascade quiz_configs.component_refs выполняется если name изменился (parity с renameComponentType).
     * Cardinality downgrade SELECT запускается ТОЛЬКО при `isMulti=false AND current.isMulti=true` (F018).
     *
     * Outcome ветки см. EditComponentOutcome.
     */
    suspend fun editComponentType(
        typeId: Long,
        name: String,
        template: ComponentTemplate,
        isMulti: Boolean,
    ): EditComponentOutcome
}
```

(Implementation в `CoreDbApiImpl.LexemeApiImpl.editComponentType` — orchestration см. `02_scope.md` § Затронутые файлы Data impl; деталь — data sub-flow.)

---

## Соответствие scope.aspects

Таблица — пробежка по каждому aspect из `02_scope.md § Аспекты`:

| Aspect | Покрытие | Где в контракте |
|---|---|---|
| `edit_component` | покрыт | `Msg.OpenEditDialog/CloseEditDialog/EditNameChange/EditTemplateChange/EditMultiToggle/SubmitEdit/EditResult` + `EditDialogState` + `DatasourceEffect.EditComponent` + `UseCase.editComponent(typeId, name, template, isMulti): EditOutcome` + `EditOutcome` (9 вариантов) + `EditComponentOutcome` (7 вариантов API). Template-immutability gate — UseCaseImpl-level (`02_scope.md` § Затронутые файлы Business impl). UI Failure handling (F028 — close dialog + generic snackbar) — Reducer-уровень, фиксируется в § Reducer (UI sub-flow), здесь только Msg/Effect/UseCase. |
| `edit_race_with_delete` | покрыт | `EditOutcome.Removed` ветка (domain) + `EditComponentOutcome.Removed` (API) + mapping `removed_at IS NOT NULL → Removed` в UseCaseImpl (`02_scope.md` § Business impl + § Аспекты). F007 reducer-кейс `whenEditResultRemoved_thenDialogClosed_andRemovedSnackbarEmitted` — § Tests Reducer. |
| `cardinality_downgrade_guard` | покрыт | `EditOutcome.CardinalityDowngradeBlocked(impactedLexemeIds: List<Long>)` + `EditDialogState.impactedLexemesPreview: ImpactedLexemesPreview?` (sealed Inline-only / InlineWithDrillIn — F023 edge-cases) + UseCase описание ветки. Precondition skip-веток (F018: `whenEditUpgradesIsMulti_*` / `whenEditOnlyName_*`) — UseCaseImpl tests (F022), не reducer-уровень. Detailed sort `ORDER BY updated_at DESC, lexeme_id ASC LIMIT 3` — data sub-flow (SQL detail). |
| `multi_dict_scope_picker` | покрыт | Расширение `CreateDialogState` (`selectedDictionaryIds: Set<Long>`) + `availableDictionaries: List<DictionaryApiEntity>` на outer state + `Msg.CreateScopeChange` (existing, теперь живой) + `Msg.CreateDictionaryToggle(dictionaryId)` (NEW) + `Msg.DictionariesLoaded(list)` (NEW) + `UseCase.flowDictionaries(): Flow<List<DictionaryApiEntity>>` + `DictionariesFlowHandler` (NEW, parity с `AllUserDefinedTypesFlowHandler`). Submit с 0 selected → `canSubmit: Boolean` extension val возвращает false → UI disabled (Open Q7 best-guess). |
| `dictionary_chip_staleness` | покрыт | `Msg.DictionariesLoaded` Reducer-ветка фильтрует `state.createDialog?.selectedDictionaryIds ∩ updated.ids`; `canSubmit` computed возвращает false при пустом selection в PerDictionaries scope (F006). F030 invariant — `Msg.DictionariesLoaded` НЕ мутирует `editDialog` поля; зафиксирован в § Msg + reducer-тестах (`whenEditDialogOpen_thenDictionariesLoaded_doesNotMutateEditState`, § Tests). |
| `shared_widget_module` | НЕ ПРИМЕНИМО | UI sub-flow (`02_scope.md` § Sub-flow для запуска — UI отдельный). Business-контракт описывает state/msg/effect/usecase; вынос composables в `:modules:widget:component_widgets` — UI design. |
| `per_template_architecture` | НЕ ПРИМЕНИМО | UI sub-flow (composable resolver `ComponentByTemplate`, `TextWidget`, `ComponentBlock` — UI level). Domain enum `ComponentTemplate` уже существует (baseline `ComponentTemplate.kt`) — не расширяется. |
| `soft_deleted_removed_outcome` | покрыт | `EditOutcome.Removed` / `RenameOutcome.Removed` (NEW) / `DeleteOutcome.Removed` (NEW); API уровень — `EditComponentOutcome.Removed` / `RenameComponentOutcome.Removed` / `SoftDeleteComponentOutcome.Removed` (все три API outcomes расширены). Асимметрия `CreateOutcome` без `Removed` — зафиксирована в § Domain (F004). UseCaseImpl mapping `removed_at IS NOT NULL → Removed` — описан в KDoc `editComponent`. Reducer ветки snackbar «Компонент удалён» + close dialog — UI sub-flow (§ Reducer), но Msg `EditResult/RenameResult/DeleteResult` сигнатуры уже несут `outcome` с новым variant. |
| `feature_log_tag` | НЕ ПРИМЕНИМО | Infra sub-flow (`02_scope.md` § Sub-flow для запуска — infra отдельный, `LogTags.COMPONENT_CONSTRUCTOR` константа в shared `:modules:core:logger` уже создана, см. `business_walkthrough.md` § Дополнительные факты). Использование тэга в UseCaseImpl — feature-tag pass, не часть state/msg/effect/usecase контракта. |
| `migration_logging` | НЕ ПРИМЕНИМО | Data sub-flow / infra (логи в `Migration_012_to_013.kt` + `QuizConfigDao.updateComponentRefs` per-step counters). Не business-контракт. |
| `test_pass` | покрыт частично (контракт-уровень) | Business-контракт обеспечивает testability: все `EditOutcome` ветки enumerable; epoch correlation parity (F124/F136) inherited from Rename pattern; F138 mutual exclusion enumerable через 4×3 Open\*Dialog кейсов. Конкретные test method names — § Tests в `02_scope.md` (UseCaseImplTest + ReducerTest). |

---

_model: claude-opus-4-7[1m]_
