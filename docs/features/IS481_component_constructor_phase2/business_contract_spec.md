<!-- META: spec_filename: component-constructor.md -->

# Component Constructor (phase 2)

## Бизнес-описание

Component Constructor расширяет модель словаря: помимо built-in компонентов (Translation, Definition и т.п.) пользователь сам определяет компоненты для лексем — задавая имя, шаблон (template) и cardinality (`is_multi` — один или несколько values на лексему). Компоненты живут в одной из двух scope: **Global** (`dictionary_id IS NULL`) либо **Per-dictionary** (привязка к одному или нескольким словарям). Конструктор доступен из двух точек входа: общий менеджер (`SettingsTab → ComponentsManagerScreen`) с aggregated view всех user-defined компонентов и per-dictionary view (`DictionaryAppBar → молоток → PerDictionaryComponentsScreen`) с фильтром по словарю.

Phase 2 добавляет четыре business-возможности: (1) **Edit** existing user-defined компонента с template-immutability и cardinality-downgrade guard (`isMulti: true→false` блокируется если есть лексемы с count>1); (2) **multi-dict scope picker** в Create-диалоге Manager-экрана с reactive подпиской на список словарей и live-фильтрацией stale selections при out-of-band удалении; (3) **Removed-semantics** — отдельная ветка outcome для soft-deleted типов (`removed_at IS NOT NULL`) в Rename / Delete / новом Edit, отличная от `BuiltInProtected`; (4) **shared widget module** — вынос дублированных диалогов и row-виджетов из двух screen-модулей в `:modules:widget:component_widgets` через примитивы + callbacks с фундаментом per-template architecture.

Имя компонента уникально в рамках своего scope **и одновременно cross-scope**: global "Foo" исключает per-dict "Foo" в любом словаре и наоборот. Soft-delete остаётся не уничтожающим данные: cascade обновление `quiz_configs.component_refs` атомарно в транзакции; для Edit cascade на rename — parity с existing rename-flow. Template после релиза immutable: Edit может изменить `name` и `isMulti`, но попытка изменить `template` отбивается на UseCase-уровне без обращения к data API.

---

## User Stories

- **Как пользователь**, я хочу из настроек видеть все мои пользовательские компоненты из всех словарей сразу — чтобы понимать что у меня есть и где это применяется.
- **Как пользователь**, я хочу создать новый компонент с указанием имени, шаблона, cardinality и scope (global / несколько выбранных словарей) — чтобы расширить модель лексемы под мои нужды.
- **Как пользователь**, я хочу при создании компонента в Manager-экране выбрать **несколько** словарей одним диалогом — чтобы не повторять одно и то же создание для каждого словаря отдельно.
- **Как пользователь**, я хочу что бы при создании компонента из per-dictionary view scope преднастраивался на текущий словарь — чтобы не задумываться о scope в общем случае.
- **Как пользователь**, я хочу из appbar'а словаря быстро открыть список компонентов именно этого словаря — чтобы не искать нужный среди всех моих компонентов.
- **Как пользователь**, я хочу **отредактировать** имя и cardinality (single/multi) существующего пользовательского компонента — чтобы исправить ошибку без потери данных.
- **Как пользователь**, я хочу что бы попытка переключить компонент с multi на single при наличии лексем с несколькими values была заблокирована с превью затронутых лексем — чтобы я мог сначала почистить данные или передумать.
- **Как пользователь**, я хочу что бы при превью я видел inline top-3 затронутых лексем и кнопку «Показать все» если их больше — чтобы быстро оценить масштаб не открывая отдельный экран.
- **Как пользователь**, я хочу видеть **до подтверждения удаления** сколько values скроется, в каких словарях, какие quiz-конфиги это затронет — чтобы принять информированное решение.
- **Как пользователь**, я хочу получать понятное сообщение «Компонент удалён» когда я пытаюсь переименовать / удалить / отредактировать компонент который был soft-deleted в другой сессии — чтобы понять что именно произошло (не путать с «встроенный нельзя трогать»).
- **Как пользователь**, я хочу получать понятное сообщение об ошибке при коллизии имени (same-scope vs cross-scope) — чтобы понимать что нужно поменять.
- **Как пользователь**, я хочу что бы chip-selection словарей в Create-диалоге автоматически очищался от только что удалённых словарей — чтобы я не отправил submit с битыми ссылками.
- **Как пользователь**, я хочу что бы кнопка submit блокировалась пока операция в полёте — чтобы случайным двойным тапом не создать два одинаковых компонента.
- **Как пользователь**, я хочу что бы после удаления компонента quiz продолжал работать корректно — чтобы конфиги не ссылались на несуществующий тип.

---

## State

### Package partitioning

Domain-shared types живут в `:modules:domain:lexeme` (package `me.apomazkin.lexeme`):

- `Scope` (sealed interface) — `Global` / `PerDictionaries(ids: List<Long>)`.
- `NameError` (sealed) — `Empty` / `SameScopeCollision` / `CrossScopeCollision`.
- `CreateOutcome` / `RenameOutcome` / `DeleteOutcome` / `EditOutcome` (sealed).
- `UserDefinedTypesSnapshot`, `ComponentUsage`, `DeletionImpact`, `AffectedQuizConfig`, `PerDictionarySnapshot` (data class).

Screen-specific state — в `me.apomazkin.components_manager.logic` / `me.apomazkin.per_dictionary_components.logic`.

### `ComponentsManagerScreenState`

Aggregated state для глобального менеджера компонентов.

```kotlin
data class ComponentsManagerScreenState(
    // ===== Loaded data =====
    val userDefinedTypes: List<UserDefinedRow>? = null,
    val availableDictionaries: List<DictionaryApiEntity> = emptyList(),

    // ===== UI flags (explicit) =====
    val isLoading: Boolean = false,
    val isCreating: Boolean = false,
    val isRenaming: Boolean = false,
    val isDeleting: Boolean = false,
    val isEditing: Boolean = false,

    // ===== Dialogs =====
    val createDialog: CreateDialogState? = null,
    val renameDialog: RenameDialogState? = null,
    val deleteConfirm: DeleteConfirmState? = null,
    val editDialog: EditDialogState? = null,

    // ===== Snackbar =====
    val snackbar: SnackbarState? = null,
)
```

#### Per-field (phase 2 additions)

| Поле | Что | Почему |
|---|---|---|
| `availableDictionaries` | Список словарей для multi-dict scope picker | Source-of-truth снаружи диалога; live-обновляется через `DictionariesFlowHandler`. Тип элемента — existing `DictionaryApiEntity` из `core-db-api` (без promoted domain entity). |
| `isEditing` | Submit Edit в полёте | Блокирует submit-кнопку в EditDialog, защищает от двойного тапа. |
| `editDialog` | Per-dialog state (visible iff `!= null`) | Простая модель видимости — null/non-null. |

#### `CreateDialogState` (phase 2 extension)

```kotlin
data class CreateDialogState(
    val name: String = "",
    val template: ComponentTemplate = ComponentTemplate.TEXT,
    val isMulti: Boolean = false,
    val scope: Scope = Scope.Global,
    val nameError: NameError? = null,
    val selectedDictionaryIds: Set<Long> = emptySet(),
)
```

`selectedDictionaryIds: Set<Long>` хранится в dialog-state (только selection); полный список словарей в outer `availableDictionaries`. `Set` — для index intersection при chip-staleness фильтрации. При построении effect для submit с `scope = PerDictionaries` reducer формирует `Scope.PerDictionaries(selectedDictionaryIds.toList())`.

#### Computed: `canSubmit`

```kotlin
val CreateDialogState.canSubmit: Boolean
    get() = name.trim().isNotEmpty() && when (scope) {
        Scope.Global -> true
        is Scope.PerDictionaries -> selectedDictionaryIds.isNotEmpty()
    }
```

#### `EditDialogState` (NEW)

```kotlin
data class EditDialogState(
    val typeId: ComponentTypeId,
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

`originalName / originalTemplate / originalIsMulti` — snapshot для diff на submit (template-immutability check на UseCaseImpl). `name / template / isMulti` — current input. `template` остаётся в UI state (control остаётся видим), но любая попытка submit'нуть его изменённым отбивается на UseCase.

#### `ImpactedLexemesPreview` (NEW)

Explicit sealed-flag для cardinality downgrade preview:

```kotlin
sealed interface ImpactedLexemesPreview {
    val impactedLexemeIds: List<Long>

    /** 1 ≤ size ≤ 3 — inline preview всех; drill-in кнопка скрыта. */
    data class InlineOnly(override val impactedLexemeIds: List<Long>) : ImpactedLexemesPreview

    /** size > 3 — top-3 в `inlineIds`, full в `impactedLexemeIds`; drill-in видна. */
    data class InlineWithDrillIn(
        override val impactedLexemeIds: List<Long>,
        val inlineIds: List<Long>,
    ) : ImpactedLexemesPreview
}
```

`inlineIds` хранится explicitly (top-3 по deterministic sort с data-уровня — `ORDER BY component_values.updated_at DESC, lexeme_id ASC`); reducer не пересортирует. `size == 0` — не моделируется как ветка (downgrade проходит → `EditOutcome.Success`, preview не показывается).

#### `EditNameError` (NEW)

```kotlin
sealed interface EditNameError {
    data object NameEmpty : EditNameError
    data object SameScopeCollision : EditNameError
    data object CrossScopeCollision : EditNameError
}
```

`CardinalityDowngradeBlocked` / `TemplateImmutable` / `BuiltInProtected` / `Removed` / `Failure` НЕ через `nameError` — обрабатываются top-level UI reactions (snackbar + close dialog либо preview-ветка).

#### Инварианты (phase 2 extension)

- `[shape]` Одновременно открыт **не более одного** диалога: `createDialog ⊕ renameDialog ⊕ deleteConfirm ⊕ editDialog`. Любой `Open*Dialog` Msg закрывает остальные.
- `[shape]` `isEditing == true → editDialog != null`.
- `[shape]` Одновременно in-flight **не более одной** write-операции (`isCreating ⊕ isRenaming ⊕ isDeleting ⊕ isEditing`).
- `[transition]` `OpenEditDialog` сбрасывает `isEditing=false` (старый submit-индикатор не висит на новом диалоге); закрывает три existing dialogs и обнуляет их in-flight флаги.
- `[transition]` `Msg.DictionariesLoaded(updated)` фильтрует `createDialog.selectedDictionaryIds ∩ updated.ids`; поля `editDialog` НЕ мутируются.

---

### `PerDictionaryComponentsScreenState`

Phase 2 расширение:

```kotlin
data class PerDictionaryComponentsScreenState(
    // ... existing fields (dictionaryId, dictionaryName, items, isLoading,
    //                     isCreating, isRenaming, isDeleting,
    //                     createDialog, renameDialog, deleteConfirm, snackbar)

    val editDialog: EditDialogState? = null,
    val isEditing: Boolean = false,
)
```

`EditDialogState` / `ImpactedLexemesPreview` / `EditNameError` — структурно идентичны Manager-варианту; дублируются в screen-модуле PerDict (parity с existing `CreateDialogState` / `RenameDialogState` / `DeleteConfirmState`, которые тоже дублируются между двумя screen-модулями).

`availableDictionaries` в PerDict **отсутствует** — multi-dict scope picker не применим (scope hardcoded к текущему словарю). `CreateDialogState` в PerDict **не расширяется** полем `selectedDictionaryIds`.

---

## UI Messages

### `Msg` для `ComponentsManagerScreen` (phase 2 extensions)

```kotlin
sealed interface Msg {
    // ... existing Create / Rename / Delete / Lifecycle / Snackbar / Navigation families

    // ===== Edit family (NEW) =====
    data class OpenEditDialog(val typeId: ComponentTypeId) : Msg
    data object CloseEditDialog : Msg
    data class EditNameChange(val value: String) : Msg
    data class EditTemplateChange(val template: ComponentTemplate) : Msg
    data class EditMultiToggle(val isMulti: Boolean) : Msg
    data object SubmitEdit : Msg
    data class EditResult(val epochId: Long, val outcome: EditOutcome) : Msg

    // ===== Multi-dict scope picker (NEW) =====
    data class CreateDictionaryToggle(val dictionaryId: Long) : Msg
    data class DictionariesLoaded(val dictionaries: List<DictionaryApiEntity>) : Msg
}
```

Note: `OpenCreateDialog` и `CreateScopeChange(scope: Scope)` — existing (baseline); phase 2 делает их живыми в Manager (multi-dict UI начинает дёргать `CreateScopeChange` для переключения Global / PerDictionaries).

### `Msg` для `PerDictionaryComponentsScreen` (phase 2 extensions)

```kotlin
sealed interface Msg {
    // ... existing families

    data class OpenEditDialog(val typeId: ComponentTypeId) : Msg
    data object CloseEditDialog : Msg
    data class EditNameChange(val value: String) : Msg
    data class EditTemplateChange(val template: ComponentTemplate) : Msg
    data class EditMultiToggle(val isMulti: Boolean) : Msg
    data object SubmitEdit : Msg
    data class EditResult(val epochId: Long, val outcome: EditOutcome) : Msg
}
```

`DictionariesLoaded` / `CreateDictionaryToggle` отсутствуют (multi-dict scope не применим к PerDict).

### Per-Msg reducer reaction (phase 2 additions)

| Msg | State change | Effect |
|---|---|---|
| `OpenEditDialog(typeId)` | `editDialog = EditDialogState(typeId, originalName, originalTemplate, originalIsMulti, name=originalName, template=originalTemplate, isMulti=originalIsMulti)`; обнуляются `createDialog / renameDialog / deleteConfirm` и `isCreating / isRenaming / isDeleting`; `isEditing=false` | `∅` |
| `EditNameChange(v)` | `editDialog?.copy(name=v, nameError=null)` | `∅` |
| `EditTemplateChange(t)` | `editDialog?.copy(template=t)` (UI control; immutability check on submit) | `∅` |
| `EditMultiToggle(b)` | `editDialog?.copy(isMulti=b, impactedLexemesPreview=null)` | `∅` |
| `SubmitEdit` (guard: `!isEditing`) | `isEditing=true; epochId++` | `DatasourceEffect.EditComponent(epochId, typeId, name, template, isMulti)` |
| `EditResult(epoch, Success(type))` (current epoch) | `isEditing=false, editDialog=null, snackbar=Snackbar("Updated")` | `∅` |
| `EditResult(epoch, NameEmpty)` | `isEditing=false, editDialog?.copy(nameError=EditNameError.NameEmpty)` | `∅` |
| `EditResult(epoch, SameScopeCollision)` | `isEditing=false, editDialog?.copy(nameError=EditNameError.SameScopeCollision)` | `∅` |
| `EditResult(epoch, CrossScopeCollision)` | `isEditing=false, editDialog?.copy(nameError=EditNameError.CrossScopeCollision)` | `∅` |
| `EditResult(epoch, CardinalityDowngradeBlocked(ids))` | `isEditing=false, editDialog?.copy(impactedLexemesPreview = if (ids.size <= 3) InlineOnly(ids) else InlineWithDrillIn(ids, inlineIds=ids.take(3)))` | `∅` |
| `EditResult(epoch, TemplateImmutable)` | `isEditing=false, editDialog=null, snackbar=Snackbar(<template-immutable text>)` | `∅` |
| `EditResult(epoch, BuiltInProtected)` | `isEditing=false, editDialog=null, snackbar=Snackbar(<built-in text>)` | `∅` |
| `EditResult(epoch, Removed)` | `isEditing=false, editDialog=null, snackbar=Snackbar("Компонент удалён")` | `∅` |
| `EditResult(epoch, Failure(cause))` | `isEditing=false, editDialog=null, snackbar=Snackbar(failureLabel(cause))` | `∅` |
| `EditResult` (stale epoch) | без изменений | `∅` |
| `CloseEditDialog` | `editDialog=null, isEditing=false` | `∅` |
| `CreateDictionaryToggle(id)` | `createDialog?.copy(selectedDictionaryIds = if (id in current) current - id else current + id)` | `∅` |
| `DictionariesLoaded(list)` | `availableDictionaries = list`; `createDialog?.copy(selectedDictionaryIds = current ∩ list.ids)` (фильтрация stale); `editDialog` НЕ мутируется | `∅` |
| `RenameResult(epoch, Removed)` | `isRenaming=false, renameDialog=null, snackbar=Snackbar("Компонент удалён")` | `∅` |
| `DeleteResult(epoch, Removed)` | `isDeleting=false, deleteConfirm=null, snackbar=Snackbar("Компонент удалён")` | `∅` |

### Guard'ы (phase 2 additions)

- `SubmitEdit` обрабатывается только при `isEditing == false`.
- `EditNameChange` сбрасывает `nameError = null`.
- `EditMultiToggle` сбрасывает `impactedLexemesPreview = null` (toggling back на multi=true очищает stale preview).
- `*Result` с stale `epochId` — ignored (parity с existing).

---

## IO

### Effects

#### `ComponentsManagerDatasourceEffect` (phase 2 additions)

```kotlin
sealed interface ComponentsManagerDatasourceEffect : Effect {
    // ... existing: CreateComponent, RenameComponent, LoadImpact,
    //               SoftDeleteComponent, LoadAllUserDefinedTypes

    data class EditComponent(
        val epochId: Long,
        val typeId: ComponentTypeId,
        val name: String,
        val template: ComponentTemplate,
        val isMulti: Boolean,
    ) : ComponentsManagerDatasourceEffect

    /** Re-subscribe trigger для DictionariesFlowHandler (parity с LoadAllUserDefinedTypes). */
    data object SubscribeDictionaries : ComponentsManagerDatasourceEffect
}
```

#### `PerDictionaryComponentsDatasourceEffect` (phase 2 additions)

```kotlin
sealed interface PerDictionaryComponentsDatasourceEffect : Effect {
    // ... existing

    data class EditComponent(
        val epochId: Long,
        val typeId: ComponentTypeId,
        val name: String,
        val template: ComponentTemplate,
        val isMulti: Boolean,
    ) : PerDictionaryComponentsDatasourceEffect
}
```

(`SubscribeDictionaries` отсутствует в PerDict — нет multi-dict picker.)

#### Reducer mapping для datasource (phase 2 additions)

| Effect | UseCase call | Msg back |
|---|---|---|
| `EditComponent` | `useCase.editComponent(typeId, name, template, isMulti)` → `EditOutcome` | `Msg.EditResult(epochId, outcome)` |
| `SubscribeDictionaries` | (handler: cancel + re-subscribe `flowDictionaries()`) | (никакого Msg back; flow начнёт emit'ить `Msg.DictionariesLoaded`) |

Handler-level invariant prevails: `catch (e: Throwable) { if (e is CancellationException) throw e; ... }`. Для `editComponent` exception → `Msg.EditResult(epoch, EditOutcome.Failure(cause))`.

### Subscribers

#### `AllUserDefinedTypesFlowHandler` (existing — без изменений)

Подписывается на `useCase.flowAllUserDefinedTypes(): Flow<UserDefinedTypesSnapshot>`; emit `Msg.TypesLoaded` / `Msg.TypesLoadFailed`.

#### `DictionariesFlowHandler` (NEW — ComponentsManagerScreen)

Подписывается на `useCase.flowDictionaries(): Flow<List<DictionaryApiEntity>>`; emit:
- `Msg.DictionariesLoaded(list)` — на каждый emit Flow.
- При ошибке в collect — emit `Msg.DictionariesLoaded(emptyList())` (chip-list скрывается, scope picker degrade'ит к Global only).

Триггерится автоматически при init Mate. Re-subscribe — через `DatasourceEffect.SubscribeDictionaries` (parity с `LoadAllUserDefinedTypes` retry-trigger).

**PerDict — без нового FlowHandler:** `PerDictionaryComponentsScreen` не показывает multi-dict picker.

#### `ComponentsForDictionaryFlowHandler` (existing — без изменений)

---

## UseCase

### Domain — `:modules:domain:lexeme`

#### `EditOutcome` (NEW)

```kotlin
sealed interface EditOutcome {
    /** UPDATE прошёл; cascade quiz_configs.component_refs выполнен если name изменился. */
    data class Success(val updated: ComponentType) : EditOutcome

    /** Валидация на UseCaseImpl: trim().isBlank(). */
    data object NameEmpty : EditOutcome

    /** Name занят в том же scope (dictionary_id + system_key IS NULL + removed_at IS NULL). */
    data object SameScopeCollision : EditOutcome

    /** Name занят в global / другом dict (concept policy phase 1). */
    data object CrossScopeCollision : EditOutcome

    /**
     * Downgrade `isMulti: true → false` заблокирован — есть лексемы с count > 1.
     * `impactedLexemeIds` — полный список ids в deterministic sort
     * (`ORDER BY component_values.updated_at DESC, lexeme_id ASC` на data-уровне).
     * Reducer делит на InlineOnly (size ≤ 3) либо InlineWithDrillIn (size > 3, inlineIds=take(3)).
     */
    data class CardinalityDowngradeBlocked(val impactedLexemeIds: List<Long>) : EditOutcome

    /** Попытка изменить template — UseCaseImpl возвращает без обращения к data API. */
    data object TemplateImmutable : EditOutcome

    /** type.system_key IS NOT NULL — нельзя редактировать встроенный. */
    data object BuiltInProtected : EditOutcome

    /** type.removed_at IS NOT NULL — soft-deleted (асимметрия с CreateOutcome). */
    data object Removed : EditOutcome

    /** Exception на data layer (try-catch на UseCaseImpl). */
    data class Failure(val cause: Throwable) : EditOutcome
}
```

#### `RenameOutcome` (phase 2 extension)

```kotlin
sealed interface RenameOutcome {
    // ... existing: Success(ComponentType), NameEmpty, SameScopeCollision,
    //               CrossScopeCollision, BuiltInProtected, Failure(cause)

    /** type.removed_at IS NOT NULL — soft-deleted; не путать с BuiltInProtected. */
    data object Removed : RenameOutcome
}
```

#### `DeleteOutcome` (phase 2 extension)

```kotlin
sealed interface DeleteOutcome {
    // ... existing: Success(DeletionImpact), BuiltInProtected, Failure(cause)

    /** type.removed_at IS NOT NULL — повторный soft-delete. */
    data object Removed : DeleteOutcome
}
```

#### `CreateOutcome` — без изменений

`Removed` не добавляется (асимметрия): Create не оперирует existing `type.id`; soft-deleted name-коллизия покрывается `SameScopeCollision` / `CrossScopeCollision` (existing `removed_at IS NULL` фильтр).

### API — `core/core-db-api`

#### `EditComponentOutcome` (NEW — в `entity/ComponentOutcomeApiEntity.kt`)

```kotlin
sealed interface EditComponentOutcome {
    data class Success(val type: ComponentTypeApiEntity) : EditComponentOutcome
    data object SameScopeCollision : EditComponentOutcome
    data object CrossScopeCollision : EditComponentOutcome
    data class CardinalityDowngradeBlocked(val impactedLexemeIds: List<Long>) : EditComponentOutcome
    /** Defensive parity — основная проверка на UseCase, API возвращает defense-in-depth. */
    data object TemplateImmutable : EditComponentOutcome
    data object BuiltInProtected : EditComponentOutcome
    data object Removed : EditComponentOutcome
}
```

**НЕ входит в API** (валидация / try-catch на UseCaseImpl):
- `NameEmpty` — `trimmed.isBlank()` → domain `EditOutcome.NameEmpty` без обращения к API.
- `Failure(cause)` — try-catch → domain `EditOutcome.Failure(cause)`.

#### `RenameComponentOutcome` (phase 2 extension)

Добавить `data object Removed : RenameComponentOutcome`.

#### `SoftDeleteComponentOutcome` (phase 2 extension)

Добавить `data object Removed : SoftDeleteComponentOutcome`.

#### `LexemeApi` (phase 2 extension — `CoreDbApi.kt`)

```kotlin
interface LexemeApi {
    // ... existing methods

    /**
     * Edit user-defined component_type — UPDATE name / template / isMulti.
     *
     * Template принимается параметром — immutability check на UseCase уровне.
     * Cascade quiz_configs.component_refs выполняется если name изменился.
     * Cardinality downgrade SELECT запускается ТОЛЬКО при `isMulti=false AND current.isMulti=true`.
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

### `ComponentsManagerUseCase` (phase 2 extensions)

```kotlin
interface ComponentsManagerUseCase {
    // ... existing 5 methods (flowAllUserDefinedTypes, createUserDefinedComponent,
    //                         renameComponent, previewDeletionImpact, softDeleteComponent)

    /**
     * Edit existing user-defined component_type — name / template / isMulti.
     *
     * Business rules (UseCaseImpl-level):
     *  - name.trim().isBlank() → EditOutcome.NameEmpty (без обращения к data API).
     *  - template != current.template → EditOutcome.TemplateImmutable (без обращения к data API).
     *  - exception (CancellationException re-throw) → EditOutcome.Failure(cause).
     *
     * API-level (LexemeApi.editComponentType):
     *  - removed_at IS NOT NULL → EditOutcome.Removed.
     *  - system_key IS NOT NULL → EditOutcome.BuiltInProtected.
     *  - same/cross-scope collision → EditOutcome.SameScopeCollision / CrossScopeCollision.
     *  - cardinality downgrade (isMulti true→false при impacted lexemes) → CardinalityDowngradeBlocked(ids).
     *  - success → EditOutcome.Success (cascade quiz_configs.component_refs если name изменился).
     */
    suspend fun editComponent(
        typeId: ComponentTypeId,
        name: String,
        template: ComponentTemplate,
        isMulti: Boolean,
    ): EditOutcome

    /**
     * Reactive subscription на список словарей (для multi-dict scope picker в Create-диалоге).
     * Делегирует на dictionaryApi.flowDictionaryList() без mapping.
     */
    fun flowDictionaries(): Flow<List<DictionaryApiEntity>>
}
```

### `PerDictionaryComponentsUseCase` (phase 2 extensions)

```kotlin
interface PerDictionaryComponentsUseCase {
    // ... existing 5 CRUD methods

    /** Те же business rules что в ComponentsManagerUseCase.editComponent. */
    suspend fun editComponent(
        typeId: ComponentTypeId,
        name: String,
        template: ComponentTemplate,
        isMulti: Boolean,
    ): EditOutcome
}
```

`flowDictionaries` отсутствует (нет multi-dict picker в PerDict).

---

## Тестовые сценарии

### Edit — happy path (rename only)

- **Предусловие:** существует user-defined type T с name "Notes", `isMulti=true`; имени "Annotations" нет.
- **Действие:** `OpenEditDialog(T.id)` → `EditNameChange("Annotations")` → `SubmitEdit`.
- **Ожидание:** `isEditing=true`, dispatch `DatasourceEffect.EditComponent(epoch, T.id, "Annotations", TEXT, true)`. После `EditResult(epoch, Success(type with name="Annotations"))`: `isEditing=false`, `editDialog=null`, `snackbar=Snackbar("Updated")`. Cascade UPDATE `quiz_configs.component_refs` выполнен на data-уровне (name изменился).

### Edit — cardinality downgrade blocked (size ≤ 3)

- **Предусловие:** type T (`isMulti=true`) имеет 2 лексемы с count>1; user открыл EditDialog.
- **Действие:** `EditMultiToggle(false)` → `SubmitEdit`.
- **Ожидание:** `EditResult(epoch, CardinalityDowngradeBlocked(impactedLexemeIds=[L1, L2]))` → `editDialog.impactedLexemesPreview = InlineOnly([L1, L2])`, dialog остаётся открытым, drill-in кнопка скрыта.

### Edit — cardinality downgrade blocked (size > 3)

- **Предусловие:** type T (`isMulti=true`) имеет 7 лексем с count>1.
- **Действие:** `EditMultiToggle(false)` → `SubmitEdit`.
- **Ожидание:** `EditResult(epoch, CardinalityDowngradeBlocked(impactedLexemeIds=[L1..L7]))` → `editDialog.impactedLexemesPreview = InlineWithDrillIn(impactedLexemeIds=[L1..L7], inlineIds=[L1, L2, L3])`, drill-in кнопка видна.

### Edit — template immutability gate

- **Предусловие:** type T с template=TEXT.
- **Действие:** `EditTemplateChange(IMAGE)` → `SubmitEdit`.
- **Ожидание:** UseCaseImpl сравнивает new.template vs current.template, возвращает `EditOutcome.TemplateImmutable` БЕЗ вызова `lexemeApi.editComponentType`. `EditResult(epoch, TemplateImmutable)` → `editDialog=null, snackbar=Snackbar(<template-immutable text>)`.

### Edit — race with soft-delete (Removed)

- **Предусловие:** EditDialog открыт для type T; параллельно (другой process / cascade) T получает `removed_at = now()`.
- **Действие:** `SubmitEdit`.
- **Ожидание:** API возвращает `EditComponentOutcome.Removed` → UseCaseImpl mapping → `EditOutcome.Removed`. `EditResult(epoch, Removed)` → `editDialog=null, snackbar=Snackbar("Компонент удалён")`. Список (через `flowAllUserDefinedTypes`) автоматически перерендерится без removed item.

### Edit — same-scope collision

- **Предусловие:** уже существует active per-dict "Annotations" в том же словаре.
- **Действие:** Edit `T(name="Notes")` → `EditNameChange("Annotations")` → `SubmitEdit`.
- **Ожидание:** `EditResult(epoch, SameScopeCollision)` → `editDialog.nameError=EditNameError.SameScopeCollision`, диалог НЕ закрывается.

### Edit — built-in protected

- **Предусловие:** type T — built-in (`systemKey != null`).
- **Действие:** `SubmitEdit`.
- **Ожидание:** `EditResult(epoch, BuiltInProtected)` → `editDialog=null, snackbar=Snackbar(<built-in text>)`.

### Edit — Failure handling

- **Предусловие:** Datasource handler ловит exception при `editComponent`.
- **Действие:** `SubmitEdit`.
- **Ожидание:** `EditResult(epoch, Failure(cause))` → `editDialog=null, snackbar=Snackbar(failureLabel(cause))` (parity с Rename/Delete Failure handling).

### Edit — stale result (epoch mismatch)

- **Предусловие:** `SubmitEdit` (epoch=1) → dialog closed → reopened → новый `SubmitEdit` (epoch=2).
- **Действие:** late `EditResult(epoch=1, Success(...))` приходит после epoch=2 dispatched.
- **Ожидание:** state не меняется (stale epoch dropped).

### Edit — double-tap guard

- **Предусловие:** `isEditing=true`.
- **Действие:** второй `SubmitEdit`.
- **Ожидание:** state не меняется, второй effect не dispatch'ится.

### Create — multi-dict scope happy path

- **Предусловие:** Manager-экран, диалог закрыт; `availableDictionaries=[D1, D2, D3]`.
- **Действие:** `OpenCreateDialog` → `CreateNameChange("Tags")` → `CreateScopeChange(PerDictionaries(emptyList()))` → `CreateDictionaryToggle(D1.id)` → `CreateDictionaryToggle(D2.id)` → `SubmitCreate`.
- **Ожидание:** dispatch `DatasourceEffect.CreateComponent(epoch, "Tags", TEXT, false, PerDictionaries([D1.id, D2.id]))`. После `Success(created=[t1, t2])`: `snackbar=Snackbar("Created 2")`.

### Create — submit disabled при пустом PerDictionaries selection

- **Предусловие:** `createDialog` с `scope=PerDictionaries`, `selectedDictionaryIds=emptySet()`, `name="Tags"`.
- **Действие:** computed `canSubmit`.
- **Ожидание:** `canSubmit == false` (submit-кнопка disabled).

### Multi-dict — chip staleness filtering

- **Предусловие:** `createDialog.selectedDictionaryIds=[D1.id, D2.id]`; параллельно D1 удалён → приходит `DictionariesLoaded(updated=[D2, D3])`.
- **Действие:** `Msg.DictionariesLoaded(updated)`.
- **Ожидание:** `availableDictionaries=[D2, D3]`, `createDialog.selectedDictionaryIds=[D2.id]` (stale D1 отфильтрован). Если selection опустеет → `canSubmit=false`.

### DictionariesLoaded не мутирует EditDialogState (инвариант)

- **Предусловие:** `editDialog != null` (с current name/template/isMulti/impactedLexemesPreview).
- **Действие:** `Msg.DictionariesLoaded(updated)`.
- **Ожидание:** `availableDictionaries` обновлён; `createDialog.selectedDictionaryIds` фильтруется; поля `editDialog` (name/template/isMulti/impactedLexemesPreview/isEditing) **не меняются**.

### Mutual exclusion (Open*Dialog F138)

- **Предусловие:** `renameDialog != null`.
- **Действие:** `OpenEditDialog(T.id)`.
- **Ожидание:** `editDialog != null && renameDialog == null && createDialog == null && deleteConfirm == null && isCreating == isRenaming == isDeleting == false`.

### Rename — Removed parity

- **Предусловие:** RenameDialog открыт; type был soft-deleted параллельно.
- **Действие:** `SubmitRename`.
- **Ожидание:** `RenameResult(epoch, Removed)` → `renameDialog=null, snackbar=Snackbar("Компонент удалён")`.

### Delete — Removed parity

- **Предусловие:** DeleteConfirm открыт; type был soft-deleted параллельно.
- **Действие:** `ConfirmDelete`.
- **Ожидание:** `DeleteResult(epoch, Removed)` → `deleteConfirm=null, snackbar=Snackbar("Компонент удалён")`.

### Cardinality downgrade SELECT precondition (UseCaseImpl-level)

- **Предусловие:** type T (`current.isMulti=true`), Edit с `new.isMulti=true` (upgrade либо unchanged).
- **Действие:** `useCase.editComponent(T.id, name, TEXT, isMulti=true)`.
- **Ожидание:** orchestration НЕ вызывает cardinality downgrade SELECT (verify на DAO method ни разу не вызван). Аналогично для edit only name (isMulti unchanged).

---

_model: claude-opus-4-7[1m]_
