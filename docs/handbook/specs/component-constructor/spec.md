# Component Constructor — конструктор пользовательских компонентов

Конструктор user-defined компонентов словаря.

**Phase 1** (released): CRUD (create / rename / soft-delete) с двумя независимыми точками входа — глобальный `ComponentsManagerScreen` из `SettingsTab` и per-dictionary `PerDictionaryComponentsScreen`, открываемый из `DictionaryAppBar` по icon-button «молоток».

**Phase 2** (IS481 phase 2): Edit family (rename + cardinality toggle) с template-immutability gate и cardinality-downgrade guard; multi-dict scope picker в Create-диалоге Manager-экрана; Removed-semantics ветка в Rename / Delete / Edit; reactive подписка на dictionaries с stale-filter для chip-selection.

---

## Бизнес-описание

Component Constructor расширяет модель словаря: помимо built-in компонентов (Translation, Definition и т.п.) пользователь сам определяет компоненты для лексем — задавая имя, шаблон (template) и cardinality (`is_multi` — один или несколько values на лексему). Компоненты живут в одной из двух scope:

- **Global** (`dictionary_id IS NULL`) — компонент применим ко всем словарям.
- **Per-dictionary** — компонент привязан к конкретному словарю (или к нескольким — создание N rows из одного диалога).

Конструктор доступен из двух точек входа: **общий менеджер** (`SettingsTab → ComponentsManagerScreen`) показывает aggregated view всех user-defined компонентов из всех словарей с usage badge, **per-dictionary view** (`DictionaryAppBar → молоток → PerDictionaryComponentsScreen`) фильтрует только компоненты применимые к открытому словарю (global + own per-dict). Built-in компоненты в обоих экранах **не показываются** — это территория конструктора пользовательских компонентов.

Soft-delete не уничтожает данные: `component_types.removed_at` ставит timestamp, существующие `component_values` остаются в БД, но скрываются на чтении (фильтр `WHERE removed_at IS NULL` / JOIN на parent). Перед удалением показывается preview impact: сколько values скрываются, в каких словарях, какие quiz-конфиги затронуты, какие prefs сбросятся. Cascade-эффекты атомарны: soft-delete component_type → cleanup `quiz_configs.component_refs` (одна транзакция) → reset `quiz_picker_dict_<id>` prefs (UseCase composition, prefs живут в DataStore вне Room).

Имя компонента уникально в рамках своего scope **и одновременно cross-scope**: global "Foo" исключает per-dict "Foo" в любом словаре и наоборот (инвариант `userdefined_identity_invariant`). Уникальность enforce'ится в UseCase через two-prong SELECT перед INSERT (UNIQUE-индекс из БД убран ради поддержки пересоздания имени после soft-delete). Имя — произвольной длины, лимита нет.

**Phase 2 расширения**:

- **Edit** existing user-defined компонента с template-immutability (template после релиза менять нельзя — отбивается на UseCase-уровне без обращения к data API) и cardinality-downgrade guard (`isMultiple: true → false` блокируется если есть лексемы с count>1; preview top-3 затронутых лексем inline + drill-in кнопка если > 3).
- **Multi-dict scope picker** в Create-диалоге Manager-экрана: reactive подписка на список словарей, chip-selection нескольких словарей одним диалогом, live-фильтрация stale selections при out-of-band удалении словаря.
- **Removed-semantics** — отдельная ветка outcome для soft-deleted типов (`removed_at IS NOT NULL`) в Rename / Delete / новом Edit, отличная от `BuiltInProtected`. Сообщение «Компонент удалён» — race с параллельным soft-delete не путается с попыткой редактировать built-in.
- Cascade на rename name — parity с existing rename-flow (UPDATE `quiz_configs.component_refs` в одной транзакции).

---

## User Stories

### Phase 1

- **Как пользователь**, я хочу из настроек видеть все мои пользовательские компоненты из всех словарей сразу — чтобы понимать что у меня есть и где это применяется.
- **Как пользователь**, я хочу создать новый компонент с указанием имени, шаблона (text / image / …), cardinality (single / multi) и scope (global / выбранные словари) — чтобы расширить модель лексемы под мои нужды.
- **Как пользователь**, я хочу из appbar'а словаря быстро открыть список компонентов именно этого словаря — чтобы не искать нужный среди всех моих компонентов.
- **Как пользователь**, я хочу при создании компонента из per-dictionary view получить scope преднастроенным на текущий словарь — чтобы не задумываться о scope в общем случае.
- **Как пользователь**, я хочу переименовать пользовательский компонент — чтобы исправить ошибку в имени без потери данных.
- **Как пользователь**, я хочу видеть **до подтверждения удаления** сколько values скроется, в каких словарях, какие quiz-конфиги это затронет — чтобы принять информированное решение.
- **Как пользователь**, я хочу что бы удаление было обратимым на data-уровне (soft-delete) — без явного UI recovery в этой фиче, но без потери данных.
- **Как пользователь**, я хочу получать понятное сообщение об ошибке при коллизии имени (same-scope vs cross-scope) — чтобы понимать что нужно поменять.
- **Как пользователь**, я хочу что бы после удаления компонента quiz продолжал работать корректно — чтобы конфиги не ссылались на несуществующий тип, а выбор компонента в picker'е словаря не воскрешал удалённый ref.
- **Как пользователь**, я хочу что бы кнопка submit блокировалась пока операция в полёте — чтобы случайным двойным тапом не создать два одинаковых компонента.

### Phase 2

- **Как пользователь**, я хочу при создании компонента в Manager-экране выбрать **несколько** словарей одним диалогом — чтобы не повторять одно и то же создание для каждого словаря отдельно.
- **Как пользователь**, я хочу **отредактировать** имя и cardinality (single/multi) существующего пользовательского компонента — чтобы исправить ошибку без потери данных.
- **Как пользователь**, я хочу что бы попытка переключить компонент с multi на single при наличии лексем с несколькими values была заблокирована с превью затронутых лексем — чтобы я мог сначала почистить данные или передумать.
- **Как пользователь**, я хочу что бы при превью я видел inline top-3 затронутых лексем и кнопку «Показать все» если их больше — чтобы быстро оценить масштаб не открывая отдельный экран.
- **Как пользователь**, я хочу получать понятное сообщение «Компонент удалён» когда я пытаюсь переименовать / удалить / отредактировать компонент который был soft-deleted в другой сессии — чтобы понять что именно произошло (не путать с «встроенный нельзя трогать»).
- **Как пользователь**, я хочу что бы chip-selection словарей в Create-диалоге автоматически очищался от только что удалённых словарей — чтобы я не отправил submit с битыми ссылками.

---

## State

### Package partitioning

Domain-shared types живут в `:modules:domain:lexeme` (package `me.apomazkin.lexeme`):

- `Scope` (sealed interface)
- `NameError` (sealed) — используется в State обоих экранов для Create/Rename
- `CreateOutcome` / `RenameOutcome` / `DeleteOutcome` / `EditOutcome` (sealed)
- `UserDefinedTypesSnapshot` (data class)
- `ComponentUsage` (data class)
- `DeletionImpact` (data class)
- `AffectedQuizConfig` (data class)
- `PerDictionarySnapshot` (data class)

Screen-specific (State / Msg / Effect / UiMsg / Reducer / FlowHandler) — в соответствующих screen-packages:
- `me.apomazkin.components_manager.logic` для `ComponentsManagerScreen`
- `me.apomazkin.per_dictionary_components.logic` для `PerDictionaryComponentsScreen`

### `ComponentsManagerScreenState`

Aggregated state для глобального менеджера компонентов.

```kotlin
package me.apomazkin.components_manager.logic

import me.apomazkin.lexeme.ComponentTemplate
import me.apomazkin.lexeme.ComponentTypeId
import me.apomazkin.lexeme.Scope
import me.apomazkin.lexeme.NameError
import me.apomazkin.lexeme.DeletionImpact

data class ComponentsManagerScreenState(
    // ===== Loaded data =====
    val userDefinedTypes: List<UserDefinedRow>? = null,    // null = ещё не загружено
    val availableDictionaries: List<DictionaryApiEntity> = emptyList(),  // phase 2

    // ===== UI flags (explicit) =====
    val isLoading: Boolean = false,                        // initial load / refresh
    val isCreating: Boolean = false,                       // submit in flight
    val isRenaming: Boolean = false,                       // submit in flight
    val isDeleting: Boolean = false,                       // soft-delete in flight
    val isEditing: Boolean = false,                        // phase 2: edit submit in flight

    // ===== Dialogs =====
    val createDialog: CreateDialogState? = null,
    val renameDialog: RenameDialogState? = null,
    val deleteConfirm: DeleteConfirmState? = null,
    val editDialog: EditDialogState? = null,               // phase 2

    // ===== Snackbar (single source of truth для UI feedback) =====
    val snackbarState: SnackbarState? = null,
)
```

#### Per-field

| Поле | Что | Почему |
|---|---|---|
| `userDefinedTypes` | Список aggregated row'ов user-defined типов из всех словарей | `null` = ещё не загружено (initial), `emptyList` = загружено и пусто (показ empty state). Built-in в список не входят. |
| `availableDictionaries` (phase 2) | Список словарей для multi-dict scope picker | Source-of-truth снаружи диалога; live-обновляется через `DictionariesFlowHandler`. Тип элемента — existing `DictionaryApiEntity` из `core-db-api`. |
| `isLoading` | Initial load / refresh in flight | Explicit флаг, не выводится из `userDefinedTypes == null`. |
| `isCreating` / `isRenaming` / `isDeleting` / `isEditing` | Submit в полёте | Блокирует submit-кнопку в соответствующем диалоге, защищает от двойного тапа. |
| `createDialog` / `renameDialog` / `deleteConfirm` / `editDialog` | Per-dialog state (visible iff `!= null`) | Простая модель видимости диалога — null/non-null. Один диалог одновременно (см. инварианты). |
| `snackbarState` | Текущий snackbar (текст) | Reducer выставляет на success/error результаты; UI consumes и сбрасывает через `DismissSnackbar`. |

#### Shared domain types (`:modules:domain:lexeme`)

```kotlin
package me.apomazkin.lexeme

sealed interface Scope {
    data object Global : Scope                                  // dictionaryId IS NULL
    data class PerDictionaries(val ids: List<Long>) : Scope     // одна или несколько привязок
}

sealed interface NameError {
    data object Empty : NameError                               // name.isBlank()
    data object SameScopeCollision : NameError                  // active rows в том же scope
    data object CrossScopeCollision : NameError                 // global ⊥ per-dict invariant
}
```

#### Nested state (screen-package)

```kotlin
package me.apomazkin.components_manager.logic

import me.apomazkin.lexeme.ComponentTemplate
import me.apomazkin.lexeme.ComponentTypeId
import me.apomazkin.lexeme.Scope
import me.apomazkin.lexeme.NameError
import me.apomazkin.lexeme.DeletionImpact

/**
 * Aggregated UI row для одного user-defined component_type из любого словаря.
 * Built-in компоненты в этот список НЕ попадают.
 */
data class UserDefinedRow(
    val typeId: ComponentTypeId,
    val name: String,
    val template: ComponentTemplate,
    val isMultiple: Boolean,
    val scope: Scope,                       // Global / PerDictionaries(list)
    val usageCount: Int,                    // суммарно активных values по словарям
    val dictionaryNames: List<String>,      // в каких словарях виден (badge)
)

data class CreateDialogState(
    val name: String = "",
    val template: ComponentTemplate = ComponentTemplate.TEXT,   // MVP: только TEXT
    val isMultiple: Boolean = false,
    val scope: Scope = Scope.Global,                            // дефолт на aggregated view
    val nameError: NameError? = null,
    val selectedDictionaryIds: Set<Long> = emptySet(),          // phase 2: multi-dict picker
)

data class RenameDialogState(
    val typeId: ComponentTypeId,
    val originalName: String,
    val editedName: String,
    val nameError: NameError? = null,
)

data class DeleteConfirmState(
    val typeId: ComponentTypeId,
    val name: String,
    val impact: DeletionImpact? = null,    // null пока preview грузится / не запрошен
    val isLoadingImpact: Boolean = false,
)

data class SnackbarState(
    val text: String,
)
```

#### Computed properties

```kotlin
val ComponentsManagerScreenState.isEmpty: Boolean
    get() = userDefinedTypes?.isEmpty() == true && !isLoading

val CreateDialogState.canSubmit: Boolean
    get() = name.trim().isNotEmpty() && when (scope) {
        Scope.Global -> true
        is Scope.PerDictionaries -> selectedDictionaryIds.isNotEmpty()
    }
```

#### Инварианты

- `[shape]` Одновременно открыт **не более одного** диалога: `createDialog ⊕ renameDialog ⊕ deleteConfirm ⊕ editDialog`. Enforced в reducer'е: любой `Open*Dialog` Msg закрывает остальные.
- `[shape]` Одновременно in-flight **не более одной** write-операции: `isCreating ⊕ isRenaming ⊕ isDeleting ⊕ isEditing`.
- `[shape]` `isCreating == true` → `createDialog != null` (submit подразумевает открытый диалог). Аналогично для `isRenaming` / `isDeleting` / `isEditing`.
- `[shape]` `deleteConfirm?.impact == null && deleteConfirm?.isLoadingImpact == false` — preview ещё не запросили (отдельное состояние от «грузится»).
- `[transition]` после `Msg.ConfirmDelete` reducer выставляет `isDeleting=true` и dispatch `SoftDeleteComponent` ровно один раз; повторный `ConfirmDelete` при `isDeleting=true` игнорируется.
- `[transition]` `OpenCreateDialog` / `OpenRenameDialog` / `OpenDeleteConfirm` / `OpenEditDialog` сбрасывают соответствующие in-flight флаги до `false` (старый submit-индикатор не должен висеть на новом диалоге); закрывают остальные диалоги.
- `[transition]` `Msg.DictionariesLoaded(updated)` фильтрует `createDialog.selectedDictionaryIds ∩ updated.ids`; поля `editDialog` НЕ мутируются.

#### `EditDialogState` (phase 2)

```kotlin
data class EditDialogState(
    val typeId: ComponentTypeId,
    val originalName: String,
    val originalTemplate: ComponentTemplate,
    val originalIsMultiple: Boolean,
    val name: String,
    val template: ComponentTemplate,
    val isMultiple: Boolean,
    val nameError: EditNameError? = null,
    val impactedLexemesPreview: ImpactedLexemesPreview? = null,
    val epochId: Long = 0L,                  // correlation id для submit Edit
)
```

`originalName / originalTemplate / originalIsMultiple` — snapshot для diff на submit (template-immutability check на UseCaseImpl). `name / template / isMultiple` — current input. `template` остаётся в UI state (control остаётся видим), но любая попытка submit'нуть его изменённым отбивается на UseCase без обращения к data API.

`epochId` живёт внутри `EditDialogState` (а не в outer `ComponentsManagerScreenState`) — каждый submit ↑ epoch, `EditResult(epoch, …)` с устаревшим epoch игнорируется reducer'ом. (Эта схема — parity с `Create` / `Rename` / `Delete` epoch correlation, см. existing reducer.)

#### `ImpactedLexemesPreview` (phase 2)

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

`inlineIds` хранится explicitly (top-3 по deterministic sort с data-уровня — `ORDER BY component_values.updated_at DESC, lexeme_id ASC`); reducer не пересортирует. `size == 0` не моделируется как ветка (downgrade проходит → `EditOutcome.Success`, preview не показывается).

#### `EditNameError` (phase 2)

```kotlin
sealed interface EditNameError {
    data object NameEmpty : EditNameError
    data object SameScopeCollision : EditNameError
    data object CrossScopeCollision : EditNameError
}
```

`CardinalityDowngradeBlocked` / `TemplateImmutable` / `BuiltInProtected` / `Removed` / `Failure` НЕ через `nameError` — обрабатываются top-level UI reactions (snackbar + close dialog либо preview-ветка).

---

### `PerDictionaryComponentsScreenState`

Scoped state — фильтр по `dictionaryId`. Видны user-defined компоненты применимые к словарю: global (`dictionaryId IS NULL`) + per-dict с `dictionaryId == this.id`.

```kotlin
package me.apomazkin.per_dictionary_components.logic

import me.apomazkin.lexeme.ComponentTemplate
import me.apomazkin.lexeme.ComponentTypeId
import me.apomazkin.lexeme.Scope
import me.apomazkin.lexeme.NameError
import me.apomazkin.lexeme.DeletionImpact

data class PerDictionaryComponentsScreenState(
    // ===== Init context =====
    val dictionaryId: Long,
    val dictionaryName: String? = null,

    // ===== Loaded data =====
    val items: List<PerDictRow>? = null,

    // ===== UI flags (explicit) =====
    val isLoading: Boolean = false,
    val isCreating: Boolean = false,
    val isRenaming: Boolean = false,
    val isDeleting: Boolean = false,
    val isEditing: Boolean = false,                       // phase 2

    // ===== Dialogs =====
    val createDialog: CreateDialogState? = null,
    val renameDialog: RenameDialogState? = null,
    val deleteConfirm: DeleteConfirmState? = null,
    val editDialog: EditDialogState? = null,              // phase 2

    // ===== Snackbar =====
    val snackbarState: SnackbarState? = null,
)

/**
 * Per-dictionary view row. `scope` упрощён до Boolean.
 */
data class PerDictRow(
    val typeId: ComponentTypeId,
    val name: String,
    val template: ComponentTemplate,
    val isMultiple: Boolean,
    val isGlobal: Boolean,           // dictionaryId IS NULL
    val valueCount: Int,             // активные values в данном словаре
)
```

`CreateDialogState` / `RenameDialogState` / `DeleteConfirmState` / `EditDialogState` / `ImpactedLexemesPreview` / `EditNameError` / `SnackbarState` / `NameError` — переиспользуются из `components_manager.logic` (или дублируются точечно — конкретное решение на design level).

**Отличия PerDict от Manager**:

- Дефолт `CreateDialogState.scope = Scope.PerDictionaries(listOf(dictionaryId))` (открытие из контекста словаря — логично прибиндить к нему). Пользователь может переключить на Global.
- `availableDictionaries` **отсутствует** — multi-dict scope picker не применим (scope hardcoded к текущему словарю). `CreateDialogState` в PerDict **не расширяется** полем `selectedDictionaryIds` (либо игнорирует его).

Инварианты `[shape]` те же, что у `ComponentsManagerScreenState`.

---

## UI Messages

### `Msg` для `ComponentsManagerScreen`

```kotlin
package me.apomazkin.components_manager.logic

import me.apomazkin.lexeme.ComponentTemplate
import me.apomazkin.lexeme.ComponentTypeId
import me.apomazkin.lexeme.Scope
import me.apomazkin.lexeme.CreateOutcome
import me.apomazkin.lexeme.RenameOutcome
import me.apomazkin.lexeme.DeleteOutcome
import me.apomazkin.lexeme.EditOutcome
import me.apomazkin.lexeme.DeletionImpact
import me.apomazkin.lexeme.UserDefinedTypesSnapshot

sealed interface Msg {

    // ===== Lifecycle / data =====
    data class TypesLoaded(val snapshot: UserDefinedTypesSnapshot) : Msg
    data class TypesLoadFailed(val cause: Throwable) : Msg

    // ===== Create dialog =====
    data object OpenCreateDialog : Msg
    data object CloseCreateDialog : Msg
    data class CreateNameChange(val value: String) : Msg
    data class CreateTemplateChange(val template: ComponentTemplate) : Msg
    data class CreateMultiToggle(val isMultiple: Boolean) : Msg
    data class CreateScopeChange(val scope: Scope) : Msg
    data object SubmitCreate : Msg
    data class CreateResult(val epochId: Long, val outcome: CreateOutcome) : Msg

    // ===== Rename dialog =====
    data class OpenRenameDialog(val typeId: ComponentTypeId) : Msg
    data object CloseRenameDialog : Msg
    data class RenameTextChange(val value: String) : Msg
    data object SubmitRename : Msg
    data class RenameResult(val epochId: Long, val outcome: RenameOutcome) : Msg

    // ===== Delete dialog =====
    data class OpenDeleteConfirm(val typeId: ComponentTypeId) : Msg
    data object CloseDeleteConfirm : Msg
    data class ImpactPreviewLoaded(val typeId: ComponentTypeId, val impact: DeletionImpact) : Msg
    data class ImpactPreviewFailed(val typeId: ComponentTypeId, val cause: Throwable?) : Msg
    data object ConfirmDelete : Msg
    data class DeleteResult(val epochId: Long, val outcome: DeleteOutcome) : Msg

    // ===== Edit dialog (phase 2) =====
    data class OpenEditDialog(val typeId: ComponentTypeId) : Msg
    data object CloseEditDialog : Msg
    data class EditNameChange(val value: String) : Msg
    data class EditTemplateChange(val template: ComponentTemplate) : Msg
    data class EditMultiToggle(val isMultiple: Boolean) : Msg
    data object SubmitEdit : Msg
    data class EditResult(val epochId: Long, val outcome: EditOutcome) : Msg

    // ===== Multi-dict scope picker (phase 2) =====
    data class CreateDictionaryToggle(val dictionaryId: Long) : Msg
    data class DictionariesLoaded(val dictionaries: List<DictionaryApiEntity>) : Msg

    // ===== Snackbar =====
    data object DismissSnackbar : Msg

    // ===== Navigation =====
    data object RequestBack : Msg

    // ===== Error retry =====
    data object OnRetryClick : Msg

    // ===== No-op =====
    data object Empty : Msg
}

/**
 * UI feedback (snackbar). Top-level `UiMsg : Msg` (parity с existing convention).
 */
sealed interface UiMsg : Msg {
    data class Snackbar(val text: String) : UiMsg
}
```

### Категории Msg

- **Lifecycle / data** — `TypesLoaded`, `TypesLoadFailed` — приходят из flow-handler'а.
- **Dialog open/close + field edit** — `Open*Dialog` / `Close*Dialog` / `*Change` / `*Toggle` — управление UI state без IO.
- **Submit** — `SubmitCreate`, `SubmitRename`, `ConfirmDelete`, `SubmitEdit` — триггерят datasource effect.
- **Result** — `CreateResult`, `RenameResult`, `DeleteResult`, `EditResult` с `epochId` correlation, `ImpactPreviewLoaded/Failed` с `typeId` correlation; stale (epoch ≠ current / typeId ≠ open dialog) игнорируются reducer'ом.
- **Multi-dict** — `CreateDictionaryToggle`, `DictionariesLoaded` — управление chip-selection в Create-диалоге Manager-экрана.
- **Snackbar** — `DismissSnackbar` сбрасывает `state.snackbarState = null` после consume в UI.
- **Navigation** — `RequestBack`.
- **UiMsg** — внутренний UI feedback (snackbar text без флага видимости).

### Per-Msg reducer reaction (выборочно)

| Msg | State change | Effect |
|---|---|---|
| `TypesLoaded(snapshot)` | `userDefinedTypes = snapshot.toRows(), isLoading=false` | `∅` |
| `OpenCreateDialog` | `createDialog = CreateDialogState()`; остальные dialogs `null`; in-flight flags = false | `∅` |
| `CreateNameChange(v)` | `createDialog?.copy(name=v, nameError=null)` | `∅` |
| `CreateScopeChange(Global)` | `createDialog?.copy(scope=Global, selectedDictionaryIds=emptySet())` (phase 2: очистка selection при switch) | `∅` |
| `CreateScopeChange(PerDictionaries(_))` | `createDialog?.copy(scope=PerDictionaries(...))` | `∅` |
| `SubmitCreate` | `isCreating=true; epochId++` | `DatasourceEffect.CreateComponent(epochId, name, template, isMultiple, scope)` |
| `CreateResult(epoch, Success(types))` (current epoch) | `isCreating=false, createDialog=null, snackbarState=Snackbar("Created ${types.size}")` | `∅` |
| `CreateResult(epoch, SameScopeCollision)` | `isCreating=false, createDialog.copy(nameError=NameError.SameScopeCollision)` | `∅` |
| `CreateResult(epoch, CrossScopeCollision)` | `isCreating=false, createDialog.copy(nameError=NameError.CrossScopeCollision)` | `∅` |
| `CreateResult` (stale epoch) | без изменений | `∅` |
| `OpenDeleteConfirm(id)` | `deleteConfirm = DeleteConfirmState(id, name, isLoadingImpact=true)`; остальные dialogs `null` | `DatasourceEffect.LoadImpact(id)` |
| `ImpactPreviewLoaded(typeId, i)` (typeId == open) | `deleteConfirm?.copy(impact=i, isLoadingImpact=false)` | `∅` |
| `ImpactPreviewLoaded` (stale typeId) | без изменений | `∅` |
| `ImpactPreviewFailed(typeId, cause)` (typeId == open) | `deleteConfirm?.copy(isLoadingImpact=false); snackbarState = Snackbar(failureLabel(cause))` | `∅` |
| `ImpactPreviewFailed` (dialog closed) | silent | `∅` |
| `ConfirmDelete` | `isDeleting=true; epochId++` | `DatasourceEffect.SoftDeleteComponent(epochId, id)` |
| `DeleteResult(epoch, Success(impact))` | `isDeleting=false, deleteConfirm=null, snackbarState=Snackbar("${impact.valueCount} values hidden")` | `∅` |
| `DeleteResult(epoch, Removed)` | `isDeleting=false, deleteConfirm=null, snackbarState=Snackbar("Компонент удалён")` (phase 2) | `∅` |
| `RenameResult(epoch, Removed)` | `isRenaming=false, renameDialog=null, snackbarState=Snackbar("Компонент удалён")` (phase 2) | `∅` |
| `OpenEditDialog(typeId)` (phase 2) | `editDialog = EditDialogState(typeId, originalName, originalTemplate, originalIsMultiple, name=originalName, template=originalTemplate, isMultiple=originalIsMultiple)`; остальные dialogs `null`; in-flight flags = false | `∅` |
| `EditNameChange(v)` | `editDialog?.copy(name=v, nameError=null)` | `∅` |
| `EditTemplateChange(t)` | `editDialog?.copy(template=t)` (UI control; immutability check on submit) | `∅` |
| `EditMultiToggle(b)` | `editDialog?.copy(isMultiple=b, impactedLexemesPreview=null)` | `∅` |
| `SubmitEdit` (guard: `!isEditing`) | `isEditing=true; editDialog.epochId++` | `DatasourceEffect.EditComponent(epochId, typeId, name, template, isMultiple)` |
| `EditResult(epoch, Success(type))` | `isEditing=false, editDialog=null, snackbarState=Snackbar("Updated")` | `∅` |
| `EditResult(epoch, NameEmpty)` | `isEditing=false, editDialog?.copy(nameError=EditNameError.NameEmpty)` | `∅` |
| `EditResult(epoch, SameScopeCollision)` | `isEditing=false, editDialog?.copy(nameError=EditNameError.SameScopeCollision)` | `∅` |
| `EditResult(epoch, CrossScopeCollision)` | `isEditing=false, editDialog?.copy(nameError=EditNameError.CrossScopeCollision)` | `∅` |
| `EditResult(epoch, CardinalityDowngradeBlocked(ids))` | `isEditing=false, editDialog?.copy(impactedLexemesPreview = if (ids.size <= 3) InlineOnly(ids) else InlineWithDrillIn(ids, inlineIds=ids.take(3)))` | `∅` |
| `EditResult(epoch, TemplateImmutable)` | `isEditing=false, editDialog=null, snackbarState=Snackbar(<template-immutable text>)` | `∅` |
| `EditResult(epoch, BuiltInProtected)` | `isEditing=false, editDialog=null, snackbarState=Snackbar(<built-in text>)` | `∅` |
| `EditResult(epoch, Removed)` | `isEditing=false, editDialog=null, snackbarState=Snackbar("Компонент удалён")` | `∅` |
| `EditResult(epoch, Failure(cause))` | `isEditing=false, editDialog=null, snackbarState=Snackbar(failureLabel(cause))` | `∅` |
| `EditResult` (stale epoch) | без изменений | `∅` |
| `CloseEditDialog` | `editDialog=null, isEditing=false` | `∅` |
| `CreateDictionaryToggle(id)` | `createDialog?.copy(selectedDictionaryIds = if (id in current) current - id else current + id)` | `∅` |
| `DictionariesLoaded(list)` | `availableDictionaries = list`; `createDialog?.copy(selectedDictionaryIds = current ∩ list.ids)`; `editDialog` НЕ мутируется | `∅` |
| `DismissSnackbar` | `snackbarState = null` | `∅` |
| `OnRetryClick` | `userDefinedTypes=null → isLoading=true` | emit `DatasourceEffect.LoadAllUserDefinedTypes` |
| `RequestBack` | без изменений | `NavigationEffect.Back` |

### Guard'ы

- `SubmitCreate` обрабатывается только при `isCreating == false`.
- `SubmitRename` — только при `isRenaming == false`.
- `ConfirmDelete` — только при `isDeleting == false`.
- `SubmitEdit` — только при `isEditing == false`.
- `Create*Change` / `Rename*Change` / `EditNameChange` сбрасывают `nameError = null`.
- `EditMultiToggle` сбрасывает `impactedLexemesPreview = null`.
- `*Result` с stale `epochId` — ignored.
- `ImpactPreviewLoaded/Failed` с typeId ≠ open dialog typeId — ignored.

### Msg для `PerDictionaryComponentsScreen`

Зеркальный sealed `Msg` — те же события Create / Rename / Delete / Edit. Отличие в lifecycle-сообщении: `ItemsLoaded(snapshot: PerDictionarySnapshot)` / `ItemsLoadFailed(cause: Throwable)` вместо `TypesLoaded` / `TypesLoadFailed`.

`CreateDictionaryToggle` / `DictionariesLoaded` **отсутствуют** (multi-dict scope не применим к PerDict).

---

## UI Layout

> Финальный snapshot phase 2. Базируется на `docs/features/IS481_component_constructor_phase2/ui_layout.md` (UI design) с корректировками от ui_implement (см. блок «Корректировки от implement» ниже). Phase 1 поправки (F158 multi-dict в Manager, F161 i18n value_count, F162 LexemeRadioRow, F163 ErrorStateWidget) интегрированы по умолчанию.

### Легенда

- **⚙️** — системный Material3 / Compose (Scaffold, Column, FlowRow, FilterChip, AssistChip, LazyColumn, Checkbox).
- **❇️** — новый кастомный виджет (введён в этой фиче).
- **🔄** — кастомный, меняется в этой фиче.
- **📌** — кастомный, не меняется в этой фиче.
- **ℹ️** — обычная пояснительная заметка.

Phase 2 не использует Figma (Case A — feature_has_figma=false).

---

### Карта экрана

#### Экран 1 — `ComponentsManagerScreen` (drill-in из Settings)

```
⚙️ Scaffold
├─ ⚙️ TopAppBar                                  title=R.string.components_manager_title, navigation=back-arrow
├─ ↘️ snackbarHost                               state-driven, ∀ state.snackbarState != null
└─ ⚙️ Box (content)                              padding=paddings, fillMaxSize
   ├─ ∀ state.isLoading && state.userDefinedTypes == null:
   │  └─ ⚙️ CircularProgressIndicator            align=Center
   ├─ ∀ !state.isLoading && state.userDefinedTypes == null:
   │  └─ 📌 <ErrorStateWidget>                   message=R.string.components_manager_load_failed, retryLabel=R.string.components_error_retry, onRetry→Msg.OnRetryClick
   ├─ ∀ state.isEmpty:
   │  └─ 🔄 <ComponentsEmptyStateWidget>         centered, with create-CTA (extracted → :modules:widget:component_widgets)
   ├─ ∀ state.userDefinedTypes != null && !state.isEmpty:
   │  └─ ⚙️ LazyColumn (× N rows)                contentPadding=h:16 v:8  spacing=8
   │     └─ 🔄 <UserDefinedRowWidget>            (плоский API: typeId, name, template, isMultiple, isGlobal, usageCount, dictionaryNames)  onEdit→Msg.OpenEditDialog  onDelete→Msg.OpenDeleteConfirm
   ↘️ FAB-slot                                   pos=BottomEnd, padding=16
   └─ 🔄 <CreateComponentFab>                    visible=always, onClick→Msg.OpenCreateDialog
   ↘️ Dialog-overlay slots                       ∀ соответствующий dialog != null
   ├─ 🔄 <CreateComponentDialog>                 ∀ createDialog != null    (Manager variant — hostVariant=Manager, scope_slot виден)
   ├─ 🔄 <RenameComponentDialog>                 ∀ renameDialog != null
   ├─ 🔄 <DeleteComponentConfirmDialog>          ∀ deleteConfirm != null
   └─ ❇️ <EditComponentDialog>                   ∀ editDialog != null                            (phase 2 NEW)
```

#### Экран 2 — `PerDictionaryComponentsScreen` (drill-in по «молоток» из `DictionaryAppBar`)

```
⚙️ Scaffold
├─ ⚙️ TopAppBar                                  title=state.dictionaryName ?: R.string.per_dict_components_title, navigation=back-arrow
├─ ↘️ snackbarHost                               state-driven
└─ ⚙️ Box (content)                              padding=paddings, fillMaxSize
   ├─ ∀ state.isLoading && state.items == null:
   │  └─ ⚙️ CircularProgressIndicator            align=Center
   ├─ ∀ !state.isLoading && state.items == null:
   │  └─ 📌 <ErrorStateWidget>                   message=R.string.components_per_dict_load_failed
   ├─ ∀ state.isEmpty:
   │  └─ 🔄 <ComponentsEmptyStateWidget>         variant=per-dict (headlineRes/bodyRes — переключаются хостом)
   ├─ ∀ state.items != null && !state.isEmpty:
   │  └─ ⚙️ LazyColumn (× N rows)
   │     └─ 🔄 <PerDictRowWidget>                (плоский API: typeId, name, template, isMultiple, isGlobal, valueCount)  onEdit→Msg.OpenEditDialog  onDelete→Msg.OpenDeleteConfirm
   ↘️ FAB-slot                                   pos=BottomEnd, padding=16
   └─ 🔄 <CreateComponentFab>                    onClick→Msg.OpenCreateDialog
   ↘️ Dialog-overlay slots
   ├─ 🔄 <CreateComponentDialog>                 hostVariant=PerDict (scope_slot скрыт, scope hardcoded reducer-side)
   ├─ 🔄 <RenameComponentDialog>
   ├─ 🔄 <DeleteComponentConfirmDialog>
   └─ ❇️ <EditComponentDialog>                   ∀ editDialog != null                            (phase 2 NEW)
```

#### Точка касания 3 — `SettingsTabScreen` (новый entry-row)

```
⚙️ Scaffold (existing)
└─ ⚙️ LazyColumn                                 contentPadding=h:16  spacing=8
   └─ item: 📌 <SettingsSectionWidget> (existing)
      ├─ 📌 <LangManageWidget>                   (existing)
      ├─ ❇️ <ComponentsManageWidget>             onClick → Msg.OpenComponentsManager
      ├─ 📌 <ExportDataWidget>                   (existing)
      └─ 📌 <ImportDataWidget>                   (existing)
```

#### Точка касания 4 — `DictionaryAppBar` (новый icon «молоток»)

```
⚙️ TopAppBar (existing, shared 3 tabs)
├─ title: 📌 <AppBarTitleWidget>                 (existing)
└─ actions:
   ∀ state.isLoading: ⚙️ CircularProgressIndicator
   ∀ !state.isLoading:
   ├─ ∀ state.currentDict != null:
   │  └─ ❇️ <ComponentsToolsIconButton>          iconRes=ic_hammer, onClick→Msg.OpenPerDictionaryComponents(currentDict.id)
   └─ 📌 <DictDropDownWidget>                    (existing)
```

#### Subscreen — `EditComponentDialog` (phase 2 NEW) — детализация поверх `LexemeDialog`

```
❇️ <EditComponentDialog>                        container=📌 <LexemeDialog>
└─ ⚙️ Column                                     padding=24  spacing=16
   ├─ title_slot:    ⚙️ Text                     source=R.string.components_edit_dialog_title
   ├─ name_slot:     ⚙️ Column                   spacing=4
   │                   ├─ ⚙️ Text                source=R.string.components_edit_field_name
   │                   ├─ 📌 <LexemeTextFieldWidget>  value=editDialog.name, onValueChange=onNameChange
   │                   └─ ∀ nameErrorRes != null:
   │                      └─ ⚙️ Text             source=nameErrorRes  color=error
   ├─ template_slot: ⚙️ Column                   spacing=4
   │                   ├─ ⚙️ Text                source=R.string.components_edit_field_template
   │                   └─ ∀ ComponentTemplate.entries:
   │                      └─ 📌 <LexemeRadioRow> selected=(t==editDialog.template), onClick=onTemplateSelect(t)  (clickable — immutability gate в UseCase)
   ├─ multi_slot:    ⚙️ Row                      spacing=8
   │                   ├─ ⚙️ Checkbox            checked=editDialog.isMultiple, onCheckedChange=onMultiToggle
   │                   └─ ⚙️ Text                source=R.string.components_edit_field_is_multi
   ├─ preview_slot:  ∀ previewInlineIds != null:
   │                   └─ ❇️ <CardinalityDowngradePreviewWidget>  inlineIds, totalCount, showAllVisible, lexemeLabel, onShowAll
   └─ actions_slot:  ⚙️ Row                      spacing=12
                       ├─ 📌 <CancelButtonWidget>     onClick=onDismiss
                       └─ 📌 <PrimaryFullButtonWidget> enabled=canSubmit, onClick=onSubmit
```

---

### Анализ виджетов (ключевые)

Все 8 phase 1 виджетов вынесены в shared module `:modules:widget:component_widgets` (3 dialogs + 2 row widgets + 2 helpers + EmptyState + FAB + 2 extensions). Phase 2 добавляет 6 новых артефактов в тот же module (1 dialog + 1 preview + 1 resolver + 1 block-wrapper + 1 per-template TEXT + reuse phase 1 extensions). `EditNameError → labelRes` mapping остался **host-local** (private extension в каждом screen) — `EditNameError` живёт в двух разных package'ах mate-state'ов и в shared widget не выносится.

#### 🔄 `<UserDefinedRowWidget>` (changed — extract + onEdit semantic + плоский API)

```
   • structure:
       row spacing=12  padding=h:16 v:12  container=Surface  shape=rounded-12
         leading_slot: icon  variant=IconBoxed  iconRes=ic_components  size=24
         content_slot:
           column spacing=4  weight=1
             title_slot: text  source=name  style=LexemeStyle.BodyL
             meta_slot:
               row spacing=8
                 template_chip:    chip  variant=AssistChip  label=template.labelRes
                 cardinality_chip: chip  variant=AssistChip  label=R.string.components_chip_multi|single
                 global_chip:      chip  variant=AssistChip  label=R.string.components_chip_global  visible=∀ isGlobal
             usage_text: text  source="usageCount · {dict1, dict2}"  style=LexemeStyle.BodyS  color=gray
         trailing_slot:   icon  variant=IconBoxed  iconRes=ic_edit   size=44  onClick=onEdit
         trailing_slot_2: icon  variant=IconBoxed  iconRes=ic_trash  size=44  onClick=onDelete
   • params (плоский API — без mate UserDefinedRow):
       – typeId: ComponentTypeId
       – name: String
       – template: ComponentTemplate
       – isMultiple: Boolean
       – isGlobal: Boolean
       – usageCount: Int
       – dictionaryNames: List<String>
       – onEdit: (ComponentTypeId) -> Unit
       – onDelete: (ComponentTypeId) -> Unit
   • callbacks:
       – onEdit → Msg.OpenEditDialog(typeId)        (phase 2: now opens Edit, not Rename)
       – onDelete → Msg.OpenDeleteConfirm(typeId)
   • notes:
       ℹ️ Извлечён в `:modules:widget:component_widgets`. Hosts (Manager screen) раскладывают mate `UserDefinedRow` → плоские примитивы на mount-site (Dependency Rule: shared widget не знает screen-specific row type).
```

#### 🔄 `<PerDictRowWidget>` (changed — extract + onEdit semantic + плоский API)

```
   • structure: (parity с UserDefinedRow, но global_chip — в title row)
       row spacing=12  padding=h:16 v:12  container=Surface  shape=rounded-12
         leading_slot: icon  variant=IconBoxed  iconRes=ic_components
         content_slot:
           column spacing=4  weight=1
             title_slot:
               row spacing=8
                 name_text:   text  source=name  style=LexemeStyle.BodyL
                 global_chip: chip  variant=AssistChip  label=R.string.components_chip_global  visible=∀ isGlobal
             meta_slot:
               row spacing=8
                 template_chip:    chip  variant=AssistChip  label=template.labelRes
                 cardinality_chip: chip  variant=AssistChip  label=R.string.components_chip_multi|single
             usage_text: text  source=R.string.per_dict_row_value_count (formatted, F161)  arg=valueCount  style=LexemeStyle.BodyS  color=gray
         trailing_slot:   icon  variant=IconBoxed  iconRes=ic_edit   size=44  onClick=onEdit
         trailing_slot_2: icon  variant=IconBoxed  iconRes=ic_trash  size=44  onClick=onDelete
   • params (плоский API):
       – typeId, name, template, isMultiple, isGlobal, valueCount, onEdit, onDelete
   • callbacks: onEdit → Msg.OpenEditDialog(typeId); onDelete → Msg.OpenDeleteConfirm(typeId)
   • notes:
       ℹ️ Извлечён в shared module. Отличие от UserDefinedRow: global_chip в title row + usage_text использует formatted i18n строку.
```

#### 🔄 `<CreateComponentDialog>` (changed — extract + phase 2 multi-dict picker + плоский API)

```
   • structure:
       column spacing=16  padding=24  container=LexemeDialog
         title_slot: text  source=R.string.components_create_dialog_title
         name_slot:
           column spacing=4
             label: text  source=R.string.components_create_field_name
             input: input  variant=LexemeTextFieldWidget  value=name
             error: text  source=nameError.labelRes  visible=∀ nameError != null
         template_slot:
           column spacing=4
             label: text  source=R.string.components_create_field_template
             options:
               column spacing=0
                 ∀ ComponentTemplate.entries:
                   option_row: button  variant=LexemeRadioRow  isSelected=(t == template)
         multi_slot:
           row spacing=8
             checkbox: button  variant=M3-Checkbox  checked=isMultiple
             label: text  source=R.string.components_create_field_is_multi
         scope_slot (∀ hostVariant == Manager):
           column spacing=8
             label: text  source=R.string.components_create_field_scope
             scope_radio_global:   button  variant=LexemeRadioRow  isSelected=(scope is Global)        onClick=Msg.CreateScopeChange(Global)
             scope_radio_per_dict: button  variant=LexemeRadioRow  isSelected=(scope is PerDictionaries) onClick=Msg.CreateScopeChange(PerDictionaries(emptyList()))
             ∀ scope is PerDictionaries:
               chip_group:
                 ⚙️ FlowRow spacing=8
                   ∀ dict in availableDictionaries:
                     chip: button  variant=FilterChip  selected=(dict.id in selectedDictionaryIds)  label=dict.name  onClick=Msg.CreateDictionaryToggle(dict.id)
         actions_slot:
           row spacing=12
             cancel_btn: button variant=CancelButtonWidget       onClick=onDismiss
             submit_btn: button variant=PrimaryFullButtonWidget  enabled=canSubmit  onClick=onSubmit
   • params (плоский API — без CreateDialogState coupling):
       – name: String
       – template: ComponentTemplate
       – isMultiple: Boolean
       – scope: Scope
       – nameError: NameError?
       – isSubmitting: Boolean
       – availableDictionaries: List<DictionaryRef>            (display-only DTO: id + name; маппится из DictionaryApiEntity.name на mount-site)
       – selectedDictionaryIds: Set<Long>
       – hostVariant: HostVariant                              (enum Manager | PerDict — управляет видимостью scope_slot; declared в том же файле что и dialog)
       – onNameChange / onTemplateSelect / onMultiToggle / onScopeChange / onDictionaryToggle / onSubmit / onDismiss
   • behavior:
       canSubmit = name.trim().isNotEmpty() && nameError == null && !isSubmitting
                  && (scope is Global || selectedDictionaryIds.isNotEmpty()).
       hostVariant=Manager → scope_slot виден; hostVariant=PerDict → scope_slot полностью скрыт (scope hardcoded reducer-side; PerDict host передаёт availableDictionaries=emptyList + selectedDictionaryIds=emptySet + no-op onScopeChange/onDictionaryToggle).
       isCreating → submit_btn disabled (защита от двойного тапа).
       M3 `ChipPicker` (existing single-select) не подходит → inline `FlowRow + FilterChip`.
   • notes:
       ℹ️ DictionaryRef и HostVariant declared в том же файле что и dialog (один dialog = один файл с display-only DTO'шками).
```

#### 🔄 `<RenameComponentDialog>` (changed — extract + плоский API)

```
   • structure:
       column spacing=16  padding=24  container=LexemeDialog
         title_slot: text  source=R.string.components_rename_dialog_title
         original_slot:
           row spacing=8
             label: text  source=R.string.components_rename_field_original
             value: text  source=originalName  style=LexemeStyle.BodyL
         input_slot:
           column spacing=4
             label: text  source=R.string.components_rename_field_new
             input: input  variant=LexemeTextFieldWidget  value=editedName
             error: text  source=nameError.labelRes  visible=∀ nameError != null
         actions_slot:
           row spacing=12
             cancel_btn: button variant=CancelButtonWidget       onClick=onDismiss
             submit_btn: button variant=PrimaryFullButtonWidget  enabled=canSubmit  onClick=onSubmit
   • params (плоский API):
       – originalName, editedName, nameError, isSubmitting, onNameChange, onSubmit, onDismiss
   • behavior:
       canSubmit = editedName.isNotBlank() && editedName != originalName && nameError == null && !isSubmitting.
   • notes:
       ℹ️ Render идентичен phase 1, переезд в shared module + API rewrite на плоские примитивы (без RenameDialogState coupling).
```

#### 🔄 `<DeleteComponentConfirmDialog>` (changed — extract + плоский API + lightweight DTO)

```
   • structure:
       column spacing=16  padding=24  container=LexemeDialog
         title_slot:  text  source=R.string.components_delete_dialog_title  arg=name
         impact_slot:
           ∀ isLoadingImpact:
             progress: ⚙️ CircularProgressIndicator  size=small
           ∀ impact != null:
             column spacing=4
               line_values:  text  source=R.string.components_delete_impact_values  arg=impact.valueCount   visible=∀ impact.valueCount > 0
               line_dicts:   text  source=R.string.components_delete_impact_dicts   arg=impact.dictCount    visible=∀ impact.dictCount > 0
               line_quiz:    text  source=R.string.components_delete_impact_quiz    arg=impact.quizCount    visible=∀ impact.quizCount > 0
               line_prefs:   text  source=R.string.components_delete_impact_prefs   arg=impact.prefsCount   visible=∀ impact.prefsCount > 0
           ∀ !isLoadingImpact && impact == null:
             text  source=R.string.components_delete_impact_unavailable
         actions_slot:
           row spacing=12
             cancel_btn:  button variant=CancelButtonWidget   onClick=onDismiss
             confirm_btn: button variant=AlarmButtonWidget    enabled=!isSubmitting  onClick=onConfirm
   • params (плоский API):
       – name: String
       – impact: DeletionImpactRef?         (lightweight display-only DTO: valueCount, dictCount, quizCount, prefsCount — declared в том же файле; избегаем import full DeletionImpact в shared widget — Dependency Rule)
       – isLoadingImpact: Boolean
       – isSubmitting: Boolean
       – onConfirm, onDismiss
   • behavior: 3-way conditional (loading | impact | unavailable). confirm_btn disabled when isSubmitting. Host маппит domain `DeletionImpact` → `DeletionImpactRef` (counts only) на mount-site.
   • notes:
       ℹ️ Render идентичен phase 1, переезд в shared module + API rewrite.
```

#### ❇️ `<EditComponentDialog>` (phase 2 NEW — composite form-dialog поверх LexemeDialog)

```
   • structure:
       column spacing=16  padding=24  container=LexemeDialog
         title_slot: text  source=R.string.components_edit_dialog_title  style=LexemeStyle.H6
         name_slot:
           column spacing=4
             label: text  source=R.string.components_edit_field_name  style=LexemeStyle.BodyS         (fallback: LabelM отсутствует в LexemeStyle)
             input: input  variant=LexemeTextFieldWidget  value=editDialog.name  onValueChange=onNameChange
             error: text  source=nameErrorRes  visible=∀ nameErrorRes != null  color=error
         template_slot:
           column spacing=4
             label: text  source=R.string.components_edit_field_template
             options: (radio group — UI control видим, но immutability gate на UseCase submit)
               ∀ ComponentTemplate.entries:
                 option_row: button  variant=LexemeRadioRow  isSelected=(t == editDialog.template)  onClick=onTemplateSelect(t)
         multi_slot:
           row spacing=8
             checkbox: button variant=M3-Checkbox checked=editDialog.isMultiple  onCheckedChange=onMultiToggle
             label: text source=R.string.components_edit_field_is_multi
         preview_slot (∀ previewInlineIds != null):
           ❇️ <CardinalityDowngradePreviewWidget>  inlineIds=previewInlineIds, totalCount=previewTotalCount, showAllVisible=previewShowAllVisible, lexemeLabel, onShowAll=onShowAllImpacted
         actions_slot:
           row spacing=12
             cancel_btn: button variant=CancelButtonWidget       onClick=onDismiss
             submit_btn: button variant=PrimaryFullButtonWidget  enabled=canSubmit  onClick=onSubmit
   • params (плоский API — без EditDialogState coupling):
       – name, template, isMultiple
       – originalName, originalTemplate, originalIsMultiple       (для dirty-check на canSubmit)
       – nameErrorRes: Int?                                    (host маппит EditNameError → StringRes через private extension)
       – previewInlineIds: List<Long>?                         (null = preview скрыт; non-null = блок виден)
       – previewTotalCount: Int                                (передаётся в drill-in label)
       – previewShowAllVisible: Boolean                        (показ drill-in кнопки)
       – lexemeLabel: (Long) -> String                         (host-supplied; resolves id → display label)
       – isSubmitting: Boolean                                 (= state.isEditing)
       – onNameChange / onTemplateSelect / onMultiToggle / onShowAllImpacted / onSubmit / onDismiss
   • behavior:
       canSubmit = name.trim().isNotBlank() && nameErrorRes == null && !isSubmitting
                  && (name != originalName || isMultiple != originalIsMultiple || template != originalTemplate).
       Template change на UI разрешён (radio clickable) — immutability гасится UseCase'ом (EditOutcome.TemplateImmutable → snackbar + close).
       previewInlineIds != null → preview_slot виден; диалог остаётся открытым (не закрывается на CardinalityDowngradeBlocked).
       onShowAllImpacted — в phase 2 **no-op** в обоих screen'ах (drill-in destination — backlog: bottom-sheet или отдельный screen).
   • notes:
       ℹ️ `EditNameError` mapping локален каждому screen (private `EditNameError.toLabelRes()` extension; не выносится в shared module ради избегания cross-package mate-coupling).
       ℹ️ `lexemeLabel` — placeholder резолвер `R.string.components_edit_lexeme_label` ("Lexeme #%1$d" / "Лексема №%1$d") через `context.getString(id, lexemeId)` (lambda вызывается из non-@Composable scope). Backlog: реальный label через UseCase query `getLexemesByIds`.
   • source: phase 2 — composite form-dialog mirrors phase 1 CreateComponentDialog с заменой fields на edit semantics.
```

#### ❇️ `<CardinalityDowngradePreviewWidget>` (phase 2 NEW)

```
   • structure:
       column padding=h:8 v:8  spacing=8  background=errorContainer  shape=rounded-12
         title_slot:  text  source=R.string.components_edit_cardinality_blocked_title  style=LexemeStyle.BodyLBold  (fallback: LabelL отсутствует)
         ∀ id in inlineIds:
           lexeme_row: text  source=lexemeLabel(id)  style=LexemeStyle.BodyM  color=onErrorContainer
         ∀ showAllVisible:
           drill_in_btn: ⚙️ TextButton (M3)  label=stringResource(R.string.components_edit_show_all, totalCount)  onClick=onShowAll
   • params (плоский API):
       – inlineIds: List<Long>                (top-3 ids; deterministic sort с data-уровня)
       – totalCount: Int                      (counter для drill-in label)
       – showAllVisible: Boolean              (true iff totalCount > inlineIds.size)
       – lexemeLabel: (Long) -> String        (host-supplied resolver)
       – onShowAll: () -> Unit                (visible iff showAllVisible)
   • behavior:
       InlineOnly (totalCount ≤ 3) → only inline rows; drill_in_btn скрыт.
       InlineWithDrillIn (totalCount > 3) → top-3 inline + drill_in_btn видна с count.
       Reducer (host) маппит `ImpactedLexemesPreview` sealed → 3 плоских параметра (inlineIds, totalCount, showAllVisible).
   • notes:
       ℹ️ Размещается inline внутри EditComponentDialog preview_slot.
       ℹ️ `PrimaryTextButtonWidget` не поддерживает StringRes с args → fallback на M3 `TextButton` + `stringResource(id, totalCount)`. Backlog: добавить overload PrimaryTextButtonWidget(title: Int, vararg formatArgs: Any).
   • source: проектное решение — UX requirement из concept ui_placement.md ("inline top-3 + drill-in если больше").
```

#### ❇️ `<ComponentByTemplate>` / `<ComponentBlock>` / `<TextWidget>` (phase 2 NEW — per-template architecture)

```
   • <ComponentByTemplate>: exhaustive `when (type.template)` resolver
       ∀ TEXT: ComponentBlock(type) { TextWidget(value=values.value.text, editable, onValueChange) }
       ∀ IMAGE: ComponentBlock(type) { /* пусто — backlog */ }
   • <ComponentBlock>: structural wrapper
       column spacing=4
         name_slot: text  source=type.name  style=LexemeStyle.BodyS  color=onSurfaceVariant   (fallback: LabelM отсутствует в LexemeStyle)
         content_slot: composite slot (lambda)
   • <TextWidget>: per-template Tier-2 composable
       ∀ !editable: text  source=value  style=LexemeStyle.BodyL
       ∀ editable:  input variant=LexemeTextFieldWidget  value=value  onValueChange=onValueChange
   • params (ComponentByTemplate):
       – type: ComponentType
       – values: TemplateValues       (sealed: TextValues | ImageValues)
       – editable: Boolean = false
       – onValueChange: (TemplateValues) -> Unit = {}
   • notes:
       ℹ️ Фундамент per-template architecture (concept typed_views.md Tier 2). MVP: только TEXT template имеет рендер; IMAGE — backlog.
       ℹ️ Зависит от `:modules:domain:lexeme` (ComponentTemplate, ComponentType, TemplateValues, Primitive).
   • source: проектное решение — concept typed_views.md.
```

#### 🔄 `<ComponentsEmptyStateWidget>` / `<CreateComponentFab>` (changed — extract в shared module)

```
   • <ComponentsEmptyStateWidget>:
       column padding=32  spacing=16  align=center
         icon_slot: icon iconRes=ic_components size=64
         headline_slot: text source=headlineRes style=LexemeStyle.H6
         body_slot: text source=bodyRes style=LexemeStyle.BodyL color=gray
         cta_slot: button variant=PrimaryFullButtonWidget titleRes=ctaRes onClick=onCreate
       Hosts передают разные headlineRes/bodyRes (Manager vs PerDict variants).
   • <CreateComponentFab>:
       thin wrapper над PrimaryLongFabWidget (iconRes=ic_add, titleRes=R.string.components_create_cta).
   • notes:
       ℹ️ Render 1-в-1 phase 1 — переезд в shared module без изменений.
```

#### 📌 `<ComponentTemplateLabel>` / `<NameErrorLabel>` (extract — top-level internal extensions)

```
   • notes:
       ℹ️ `internal fun ComponentTemplate.labelRes(): Int` + `internal fun NameError.labelRes(): Int` переехали в shared module как top-level internal extensions (не composables).
       ℹ️ `EditNameError.labelRes()` — НЕ выносится: живёт private в каждом screen ради избегания cross-package mate-coupling. Strings те же что для NameError (NameEmpty/SameScope/CrossScope).
```

#### 📌 Existing baseline primitives (unchanged)

`<LexemeDialog>`, `<LexemeRadioRow>` (F162), `<LexemeTextFieldWidget>`, `<CancelButtonWidget>`, `<PrimaryFullButtonWidget>`, `<AlarmButtonWidget>`, `<PrimaryLongFabWidget>`, `<PrimaryTextButtonWidget>`, `<IconBoxed>`, `<ErrorStateWidget>` (F163) — переиспользуются из `:modules:core:ui` без изменений.

---

### Новые виджеты (summary)

Phase 1 (released):
- `<UserDefinedRowWidget>`, `<PerDictRowWidget>`, `<ComponentsEmptyStateWidget>`, `<CreateComponentFab>`, `<CreateComponentDialog>`, `<RenameComponentDialog>`, `<DeleteComponentConfirmDialog>`, `<ComponentsManageWidget>`, `<ComponentsToolsIconButton>`, `LexemeRadioRow`, `ErrorStateWidget`.

Phase 2 (NEW):
- `<EditComponentDialog>` — name + template (gated) + isMultiple + impacted-lexemes preview.
- `<CardinalityDowngradePreviewWidget>` — inline top-3 + drill-in кнопка (через 3 плоских параметра).
- `<ComponentByTemplate>` / `<ComponentBlock>` / `<TextWidget>` — per-template architecture (Tier 2, concept typed_views.md).
- Multi-dict chip-group внутри `<CreateComponentDialog>` (FlowRow + FilterChip over `availableDictionaries`).

### Меняем (ключевое)

- `<UserDefinedRowWidget>` / `<PerDictRowWidget>` — `onEdit` callback теперь триггерит `Msg.OpenEditDialog` вместо `OpenRenameDialog` (Rename как отдельный flow остаётся в шторке backlog cleanup); переезд в shared module + плоский API.
- `<CreateComponentDialog>` — добавлены scope_slot + chip-group для Manager-варианта (hostVariant=Manager); API rewrite на плоские примитивы (с display-only `DictionaryRef` + `HostVariant` declared inline).
- `<RenameComponentDialog>` / `<DeleteComponentConfirmDialog>` — переезд в shared module + плоские примитивы (display-only `DeletionImpactRef` для Delete).
- `<ComponentsEmptyStateWidget>` / `<CreateComponentFab>` — переезд в shared module (рендер 1-в-1).
- `ComponentsManagerScreen` / `PerDictionaryComponentsScreen` — добавлен mount `<EditComponentDialog>`; все widget import paths переключены на `:modules:widget:component_widgets.{dialogs|widgets|templates}.*`; раскладка mate state на плоские примитивы выполнена на mount-site; private `EditNameError.toLabelRes()` в каждом screen.

### Удалено (с миграцией)

Все 16 файлов (8 widgets × 2 screen-модуля) удалены через `rm`; пустые `widget/` директории убраны через `rmdir`. Build PASS подтверждает отсутствие dangling references. Миграция в shared module 1:1 с переименованием API на плоские примитивы.

### Корректировки от implement

Финальный snapshot отличается от `ui_layout.md` (UI design) по следующим nontrivial решениям, принятым на implement-стадии:

1. **`lexemeLabel` placeholder** — `lexemeLabel: (Long) -> String` lambda вызывается из non-@Composable scope → host резолвит через `context.getString(R.string.components_edit_lexeme_label, id)` = "Lexeme #N" / "Лексема №N". Backlog: реальный label через UseCase query `getLexemesByIds`.
2. **`onShowAllImpacted` — no-op** в обоих screen'ах (drill-in destination — bottom-sheet или отдельный screen — оставлен в backlog).
3. **`DictionaryRef.name` field** — verified Read'ом из `core-db-api/.../DictionaryApiEntity.kt:8` (не `.title` как было в design tree). Mapping исправлен.
4. **`HostVariant` enum** (Manager | PerDict) — declared в том же файле что и `CreateComponentDialog` (один dialog = один файл с display-only DTO'шками). PerDict передаёт `availableDictionaries=emptyList + selectedDictionaryIds=emptySet + no-op callbacks`.
5. **Плоские примитивы для всех Tier 1/2 widgets** — `UserDefinedRowWidget`, `PerDictRowWidget`, `RenameComponentDialog`, `DeleteComponentConfirmDialog`, `CreateComponentDialog`, `EditComponentDialog` принимают плоские примитивы (не mate state-объекты). Dependency Rule: shared widget не coupled на screen-specific state shape.
6. **`DeletionImpactRef` display-only DTO** (valueCount, dictCount, quizCount, prefsCount) declared в файле `DeleteComponentConfirmDialog.kt` — host маппит full domain `DeletionImpact` → counts.
7. **`EditNameError.toLabelRes()` host-local** — private extension в каждом screen (не выносится в shared module). `EditNameError` живёт в двух разных package'ах mate-state'ов; strings те же что для `NameError`.
8. **`LexemeStyle.LabelM/LabelL` fallback** — `LexemeStyle` содержит H1-H6 + BodyXL/L/M/S (+ Bold), но не Label-семейство. Fallback: label → `BodyS`, label-large → `BodyLBold` (семантика сохранена).
9. **`PrimaryTextButtonWidget` без StringRes args** — drill-in label с `%1$d` placeholder использует fallback M3 `TextButton` + `stringResource(id, totalCount)`. Backlog: overload `PrimaryTextButtonWidget(title: Int, vararg formatArgs: Any)`.
10. **`ImpactedLexemesPreview` sealed → 3 плоских параметра на widget-уровне** — `inlineIds`, `totalCount`, `showAllVisible`. Reducer (host) маппит sealed в плоский набор на mount-site (`CardinalityDowngradePreviewWidget` не знает domain sealed).

### Иконки к импорту

Phase 2 не добавляет новых иконок. Используются existing phase 1:

- `ic_hammer.xml` — vector «молоток» для `ComponentsToolsIconButton`.
- `ic_components.xml` — vector «компоненты» для Settings entry, row leading icons, EmptyState.
- `ic_edit.xml` — trailing edit button в Row widgets (callback в phase 2 меняется на OpenEditDialog).
- `ic_trash.xml` — trailing delete button.
- `ic_add.xml` — FAB.

### Палитра

Phase 2 не вводит новых color tokens:

- `MaterialTheme.colorScheme.surface` / `.onSurface` — dialog containers + Row widgets.
- `MaterialTheme.colorScheme.errorContainer` / `.onErrorContainer` — `CardinalityDowngradePreviewWidget` background.
- `MaterialTheme.colorScheme.secondaryContainer` — FilterChip selected state.
- `MaterialTheme.colorScheme.error` — name error text.

### Новые strings (phase 2)

`core/core-resources/.../strings.xml`:
- `components_edit_dialog_title` — title EditDialog.
- `components_edit_field_name` / `components_edit_field_template` / `components_edit_field_is_multi` — labels.
- `components_edit_cardinality_blocked_title` — title preview block.
- `components_edit_show_all` ("Show all (%1$d)") — drill-in label с counter (formatted).
- `components_edit_lexeme_label` ("Lexeme #%1$d") — fallback resolver (placeholder; backlog: real label).
- `components_create_field_scope` / `components_create_scope_global` / `components_create_scope_per_dict` — scope picker labels (Manager variant).

---

## IO

### Effects

#### `ComponentsManagerDatasourceEffect`

```kotlin
package me.apomazkin.components_manager.logic

import me.apomazkin.lexeme.ComponentTemplate
import me.apomazkin.lexeme.ComponentTypeId
import me.apomazkin.lexeme.Scope

sealed interface ComponentsManagerDatasourceEffect : Effect {

    // Initial subscribe — `init`-trigger в `MateFlowHandler.subscribe(scope, send)`.

    data class CreateComponent(
        val epochId: Long,
        val name: String,
        val template: ComponentTemplate,
        val isMultiple: Boolean,
        val scope: Scope,
    ) : ComponentsManagerDatasourceEffect

    data class RenameComponent(
        val epochId: Long,
        val typeId: ComponentTypeId,
        val newName: String,
    ) : ComponentsManagerDatasourceEffect

    data class LoadImpact(val typeId: ComponentTypeId) : ComponentsManagerDatasourceEffect

    data class SoftDeleteComponent(
        val epochId: Long,
        val typeId: ComponentTypeId,
    ) : ComponentsManagerDatasourceEffect

    /** Phase 2: Edit existing user-defined component_type. */
    data class EditComponent(
        val epochId: Long,
        val typeId: ComponentTypeId,
        val name: String,
        val template: ComponentTemplate,
        val isMultiple: Boolean,
    ) : ComponentsManagerDatasourceEffect

    // F163: re-subscribe trigger для Retry flow.
    data object LoadAllUserDefinedTypes : ComponentsManagerDatasourceEffect

    /** Phase 2: re-subscribe trigger для DictionariesFlowHandler (parity с LoadAllUserDefinedTypes). */
    data object SubscribeDictionaries : ComponentsManagerDatasourceEffect
}
```

**Reducer mapping для datasource:**

| Effect | UseCase call | Msg back |
|---|---|---|
| `CreateComponent` | `useCase.createUserDefinedComponent(name, template, isMultiple, scope)` → `CreateOutcome` | `Msg.CreateResult(epochId, outcome)` |
| `RenameComponent` | `useCase.renameComponent(typeId, newName)` → `RenameOutcome` | `Msg.RenameResult(epochId, outcome)` |
| `LoadImpact` | `useCase.previewDeletionImpact(typeId)` → `DeletionImpact?` | non-null → `Msg.ImpactPreviewLoaded(typeId, impact)`; null → `Msg.ImpactPreviewFailed(typeId, cause=null)` |
| `SoftDeleteComponent` | `useCase.softDeleteComponent(typeId)` → `DeleteOutcome` | `Msg.DeleteResult(epochId, outcome)` |
| `EditComponent` (phase 2) | `useCase.editComponent(typeId, name, template, isMultiple)` → `EditOutcome` | `Msg.EditResult(epochId, outcome)` |
| `SubscribeDictionaries` (phase 2) | handler: cancel + re-subscribe `flowDictionaries()` | flow emits `Msg.DictionariesLoaded(list)` |

**Handler-level invariant:** все datasource handler'ы делают `catch (e: Throwable) { if (e is CancellationException) throw e; ... }` — `CancellationException` re-throw'ится ради корректной structured concurrency, остальные exceptions конвертятся в `Failure`-outcome / `ImpactPreviewFailed`. Для `editComponent` exception → `Msg.EditResult(epoch, EditOutcome.Failure(cause))`.

#### `ComponentsManagerUiEffect`

```kotlin
sealed interface ComponentsManagerUiEffect : Effect {
    data class Snackbar(val text: String) : ComponentsManagerUiEffect
}
```

#### `ComponentsManagerNavigationEffect`

```kotlin
sealed interface ComponentsManagerNavigationEffect : NavigationEffect {
    // Только Back.
}
```

#### `PerDictionaryComponentsScreen` — те же три категории

Отличие в datasource:
- Initial subscribe — `init`-trigger в `MateFlowHandler.subscribe(scope, send)` (`ComponentsForDictionaryFlowHandler` с `dictionaryId`). `LoadComponentsForDictionary` Effect для re-subscribe (F163).
- `CreateComponent` / `RenameComponent` / `LoadImpact` / `SoftDeleteComponent` / `EditComponent` (phase 2) — те же signature с `epochId`.
- `SubscribeDictionaries` **отсутствует** — нет multi-dict picker в PerDict.

### Subscribers

#### `AllUserDefinedTypesFlowHandler` (ComponentsManagerScreen)

Подписывается на `useCase.flowAllUserDefinedTypes(): Flow<UserDefinedTypesSnapshot>`, emit'ит:
- `Msg.TypesLoaded(snapshot)` — на каждый emit Flow;
- `Msg.TypesLoadFailed(cause)` — на ошибку collect.

Триггерится при init Mate. Re-subscribe через `DatasourceEffect.LoadAllUserDefinedTypes` (F163).

#### `DictionariesFlowHandler` (ComponentsManagerScreen, phase 2)

Подписывается на `useCase.flowDictionaries(): Flow<List<DictionaryApiEntity>>`, emit'ит:
- `Msg.DictionariesLoaded(list)` — на каждый emit Flow.
- При ошибке в collect — emit `Msg.DictionariesLoaded(emptyList())` (chip-list скрывается, scope picker degrade'ит к Global only).

Триггерится автоматически при init Mate. Re-subscribe — через `DatasourceEffect.SubscribeDictionaries`.

**PerDict — без нового FlowHandler:** `PerDictionaryComponentsScreen` не показывает multi-dict picker.

#### `ComponentsForDictionaryFlowHandler` (PerDictionaryComponentsScreen)

Подписывается на `useCase.flowComponentsForDictionary(dictionaryId): Flow<PerDictionarySnapshot>`, emit'ит:
- `Msg.ItemsLoaded(snapshot)`;
- `Msg.ItemsLoadFailed(cause)`.

### Shared утилиты

`failureLabel(cause: Throwable?): String` — общий helper в `:modules:core:tools` (`ThrowableExt.kt`) для перевода causes в snackbar-текст; переиспользуется обоими reducer'ами.

---

## UseCase

### Domain типы

Все типы ниже живут в `:modules:domain:lexeme` (package `me.apomazkin.lexeme`).

#### `Primitive` (sealed)

```kotlin
sealed interface Primitive {
    data class Text(val value: String) : Primitive
    data class Image(val uri: String) : Primitive
    data class Color(val hex: String) : Primitive
}
```

#### `Field` + `PrimitiveType`

```kotlin
data class Field(
    val name: String,
    val type: PrimitiveType,
)

enum class PrimitiveType { TEXT, IMAGE, COLOR }
```

#### `ComponentTemplate`

```kotlin
enum class ComponentTemplate(val key: String) {
    TEXT("text"),
    IMAGE("image"),
    ;

    val fields: List<Field> get() = when (this) {
        TEXT -> listOf(Field("value", PrimitiveType.TEXT))
        IMAGE -> listOf(Field("value", PrimitiveType.IMAGE))
    }

    companion object {
        /** Fail-soft парсинг: unknown key → null + caller логирует в Crashlytics. */
        fun fromKey(key: String): ComponentTemplate? = entries.firstOrNull { it.key == key }
    }
}
```

#### `TemplateValues` (sealed)

```kotlin
sealed interface TemplateValues

data class TextValues(val value: Primitive.Text) : TemplateValues
data class ImageValues(val value: Primitive.Image) : TemplateValues
```

#### `ComponentType`

```kotlin
data class ComponentType(
    val id: ComponentTypeId,
    val systemKey: BuiltInComponent?,    // null для user-defined
    val dictionaryId: Long?,             // null для global
    val name: String?,
    val template: ComponentTemplate,
    val position: Int,
    val isMultiple: Boolean = false,
    val createdAt: Date,
    val updatedAt: Date,
    val removedAt: Date? = null,
)
```

#### `DeletionImpact`

```kotlin
data class DeletionImpact(
    val valueCount: Int,
    val dictionariesWithValues: List<Long>,
    val affectedQuizConfigs: List<AffectedQuizConfig>,
    val affectedPrefs: List<Long>,
)

data class AffectedQuizConfig(
    val dictionaryId: Long,
    val quizMode: String,
)
```

#### `ComponentUsage`

```kotlin
data class ComponentUsage(
    val valueCountByType: Map<ComponentTypeId, Int>,
    val dictionaryIdsByType: Map<ComponentTypeId, Set<Long>>,
    val dictionaryNames: Map<Long, String>,
)
```

#### `UserDefinedTypesSnapshot`

```kotlin
data class UserDefinedTypesSnapshot(
    val types: List<ComponentType>,
    val usage: ComponentUsage,
)
```

#### `PerDictionarySnapshot`

```kotlin
data class PerDictionarySnapshot(
    val dictionaryId: Long,
    val dictionaryName: String,
    val types: List<ComponentType>,
    val valueCountByType: Map<ComponentTypeId, Int>,
)
```

#### `CreateOutcome` / `RenameOutcome` / `DeleteOutcome` / `EditOutcome`

```kotlin
sealed interface CreateOutcome {
    /**
     * Создано N rows. Для Scope.Global или Scope.PerDictionaries(listOf(x)) — list length = 1;
     * для Scope.PerDictionaries(listOf(d1, d2, ...)) — list length = N (по одному row per dictionary).
     */
    data class Success(val created: List<ComponentType>) : CreateOutcome
    data object NameEmpty : CreateOutcome
    data object SameScopeCollision : CreateOutcome
    data object CrossScopeCollision : CreateOutcome
    data class Failure(val cause: Throwable) : CreateOutcome
    // Note: Removed не моделируется — Create не оперирует existing id; soft-deleted name-коллизия
    // покрывается SameScopeCollision / CrossScopeCollision (фильтр removed_at IS NULL).
}

sealed interface RenameOutcome {
    data class Success(val type: ComponentType) : RenameOutcome
    data object NameEmpty : RenameOutcome
    data object SameScopeCollision : RenameOutcome
    data object CrossScopeCollision : RenameOutcome
    /** Попытка переименовать built-in запрещена. */
    data object BuiltInProtected : RenameOutcome
    /** Phase 2: type.removed_at IS NOT NULL — soft-deleted; не путать с BuiltInProtected. */
    data object Removed : RenameOutcome
    data class Failure(val cause: Throwable) : RenameOutcome
}

sealed interface DeleteOutcome {
    data class Success(val impact: DeletionImpact) : DeleteOutcome
    data object BuiltInProtected : DeleteOutcome
    /** Phase 2: type.removed_at IS NOT NULL — повторный soft-delete. */
    data object Removed : DeleteOutcome
    data class Failure(val cause: Throwable) : DeleteOutcome
}

/** Phase 2 — Edit existing user-defined component_type. */
sealed interface EditOutcome {
    /** UPDATE прошёл; cascade quiz_configs.component_refs выполнен если name изменился. */
    data class Success(val updated: ComponentType) : EditOutcome

    /** Валидация на UseCaseImpl: trim().isBlank(). */
    data object NameEmpty : EditOutcome

    /** Name занят в том же scope (dictionary_id + system_key IS NULL + removed_at IS NULL). */
    data object SameScopeCollision : EditOutcome

    /** Name занят в global / другом dict (cross-scope invariant). */
    data object CrossScopeCollision : EditOutcome

    /**
     * Downgrade isMultiple: true → false заблокирован — есть лексемы с count > 1.
     * impactedLexemeIds — полный список в deterministic sort
     * (ORDER BY component_values.updated_at DESC, lexeme_id ASC на data-уровне).
     * Reducer делит на InlineOnly (size ≤ 3) либо InlineWithDrillIn (size > 3, inlineIds=take(3)).
     */
    data class CardinalityDowngradeBlocked(val impactedLexemeIds: List<Long>) : EditOutcome

    /** Попытка изменить template — UseCaseImpl возвращает БЕЗ обращения к data API. */
    data object TemplateImmutable : EditOutcome

    /** type.systemKey IS NOT NULL — нельзя редактировать встроенный. */
    data object BuiltInProtected : EditOutcome

    /** type.removed_at IS NOT NULL — soft-deleted (асимметрия с CreateOutcome). */
    data object Removed : EditOutcome

    /** Exception на data layer (try-catch на UseCaseImpl). */
    data class Failure(val cause: Throwable) : EditOutcome
}
```

### `ComponentsManagerUseCase`

```kotlin
package me.apomazkin.components_manager.deps

import kotlinx.coroutines.flow.Flow
import me.apomazkin.lexeme.ComponentTemplate
import me.apomazkin.lexeme.ComponentTypeId
import me.apomazkin.lexeme.Scope
import me.apomazkin.lexeme.CreateOutcome
import me.apomazkin.lexeme.RenameOutcome
import me.apomazkin.lexeme.DeleteOutcome
import me.apomazkin.lexeme.EditOutcome
import me.apomazkin.lexeme.DeletionImpact
import me.apomazkin.lexeme.UserDefinedTypesSnapshot

interface ComponentsManagerUseCase {

    /**
     * Реактивная подписка на все user-defined component_types (built-in исключены)
     * + aggregated usage по всем словарям. Один snapshot — без N+1 запросов из reducer.
     *
     * Subscribed by: AllUserDefinedTypesFlowHandler.
     */
    fun flowAllUserDefinedTypes(): Flow<UserDefinedTypesSnapshot>

    /**
     * Создать user-defined component_type. Внутри:
     *  1) валидация имени (non-blank);
     *  2) two-prong SELECT (invariant userdefined_identity_invariant):
     *     - same-scope active row;
     *     - cross-scope (global ⊥ per-dict) inverse check;
     *  3) INSERT row(s) — для Scope.PerDictionaries(N) создаётся N rows.
     *
     * Triggered by: ComponentsManagerDatasourceEffect.CreateComponent.
     */
    suspend fun createUserDefinedComponent(
        name: String,
        template: ComponentTemplate,
        isMultiple: Boolean,
        scope: Scope,
    ): CreateOutcome

    /**
     * Переименовать user-defined component_type. Cascade UPDATE на quiz_configs.component_refs.
     * Built-in protected на SQL-уровне.
     *
     * Triggered by: ComponentsManagerDatasourceEffect.RenameComponent.
     */
    suspend fun renameComponent(
        typeId: ComponentTypeId,
        newName: String,
    ): RenameOutcome

    /**
     * Preview каскада soft-delete: valueCount + dictionariesWithValues +
     * affectedQuizConfigs + affectedPrefs. Read-only.
     *
     * Triggered by: ComponentsManagerDatasourceEffect.LoadImpact.
     */
    suspend fun previewDeletionImpact(typeId: ComponentTypeId): DeletionImpact?

    /**
     * Soft-delete user-defined component_type + atomic cleanup:
     *  1) UPDATE component_types SET removed_at = now() WHERE id = ? AND system_key IS NULL;
     *  2) Cleanup quiz_configs.component_refs;
     *  3) Cleanup quiz_picker_dict_<id> prefs (best-effort).
     *
     * Triggered by: ComponentsManagerDatasourceEffect.SoftDeleteComponent.
     */
    suspend fun softDeleteComponent(typeId: ComponentTypeId): DeleteOutcome

    /**
     * Phase 2 — Edit existing user-defined component_type: name / template / isMultiple.
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
     *  - cardinality downgrade (isMultiple true→false при impacted lexemes) → CardinalityDowngradeBlocked(ids).
     *  - success → EditOutcome.Success (cascade quiz_configs.component_refs если name изменился).
     */
    suspend fun editComponent(
        typeId: ComponentTypeId,
        name: String,
        template: ComponentTemplate,
        isMultiple: Boolean,
    ): EditOutcome

    /**
     * Phase 2 — Reactive subscription на список словарей (для multi-dict scope picker
     * в Create-диалоге Manager-экрана). Делегирует на dictionaryApi.flowDictionaryList().
     */
    fun flowDictionaries(): Flow<List<DictionaryApiEntity>>
}
```

### `PerDictionaryComponentsUseCase`

```kotlin
package me.apomazkin.per_dictionary_components.deps

import kotlinx.coroutines.flow.Flow
import me.apomazkin.lexeme.ComponentTemplate
import me.apomazkin.lexeme.ComponentTypeId
import me.apomazkin.lexeme.Scope
import me.apomazkin.lexeme.CreateOutcome
import me.apomazkin.lexeme.RenameOutcome
import me.apomazkin.lexeme.DeleteOutcome
import me.apomazkin.lexeme.EditOutcome
import me.apomazkin.lexeme.DeletionImpact
import me.apomazkin.lexeme.PerDictionarySnapshot

interface PerDictionaryComponentsUseCase {

    /**
     * Подписка на active user-defined component_types применимых к словарю
     * (global + per-dict с dictionaryId = :dictId).
     *
     * Subscribed by: ComponentsForDictionaryFlowHandler.
     */
    fun flowComponentsForDictionary(dictionaryId: Long): Flow<PerDictionarySnapshot>

    /** Те же CRUD методы, что в ComponentsManagerUseCase. */
    suspend fun createUserDefinedComponent(
        name: String,
        template: ComponentTemplate,
        isMultiple: Boolean,
        scope: Scope,
    ): CreateOutcome

    suspend fun renameComponent(typeId: ComponentTypeId, newName: String): RenameOutcome

    suspend fun previewDeletionImpact(typeId: ComponentTypeId): DeletionImpact?

    suspend fun softDeleteComponent(typeId: ComponentTypeId): DeleteOutcome

    /** Phase 2 — те же business rules что в ComponentsManagerUseCase.editComponent. */
    suspend fun editComponent(
        typeId: ComponentTypeId,
        name: String,
        template: ComponentTemplate,
        isMultiple: Boolean,
    ): EditOutcome

    // flowDictionaries отсутствует — нет multi-dict picker в PerDict.
}
```

### Data API — `core/core-db-api`

#### `EditComponentOutcome` (phase 2, в `entity/ComponentOutcomeApiEntity.kt`)

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

#### `RenameComponentOutcome` / `SoftDeleteComponentOutcome` (phase 2 extension)

Добавлен `data object Removed` в каждый.

#### `LexemeApi.editComponentType` (phase 2 extension в `CoreDbApi.kt`)

```kotlin
interface LexemeApi {
    // ... existing methods

    /**
     * Edit user-defined component_type — UPDATE name / template / isMultiple.
     *
     * Template принимается параметром — immutability check на UseCase уровне.
     * Cascade quiz_configs.component_refs выполняется если name изменился.
     * Cardinality downgrade SELECT запускается ТОЛЬКО при isMultiple=false AND current.isMultiple=true.
     *
     * Outcome ветки см. EditComponentOutcome.
     */
    suspend fun editComponentType(
        typeId: Long,
        name: String,
        template: ComponentTemplate,
        isMultiple: Boolean,
    ): EditComponentOutcome
}
```

---

## Тестовые сценарии

### Phase 1

#### Create — happy path

- **Предусловие:** state без активных диалогов, существует словарь D1; user-defined типов с именем "Notes" нет.
- **Действие:** `OpenCreateDialog` → `CreateNameChange("Notes")` → `CreateTemplateChange(TEXT)` → `CreateScopeChange(Scope.PerDictionaries(listOf(D1.id)))` → `SubmitCreate`.
- **Ожидание:** `isCreating=true`, dispatch `DatasourceEffect.CreateComponent(epoch, "Notes", TEXT, false, PerDictionaries([D1.id]))`. После `CreateResult(epoch, Success(created=[type]))`: `isCreating=false`, `createDialog=null`, `snackbarState = Snackbar("Created 1")`.

#### Create — same-scope collision

- **Предусловие:** уже существует active user-defined per-dict "Notes" (`dictionary_id=D1, removed_at IS NULL`).
- **Действие:** Open + Fill + Submit с тем же именем + scope `PerDictionaries([D1.id])`.
- **Ожидание:** `CreateResult(epoch, SameScopeCollision)` → `isCreating=false`, `createDialog.nameError=NameError.SameScopeCollision`, диалог НЕ закрывается.

#### Create — cross-scope collision (invariant)

- **Предусловие:** существует active **global** "Notes" (`dictionary_id IS NULL, removed_at IS NULL`).
- **Действие:** Submit per-dict "Notes" в любом словаре.
- **Ожидание:** `CreateResult(epoch, CrossScopeCollision)` → `nameError=NameError.CrossScopeCollision`.

#### Create — recreate after soft-delete

- **Предусловие:** существует soft-deleted user-defined "Notes" (`removed_at IS NOT NULL`); активной "Notes" нет.
- **Действие:** Submit "Notes" в том же scope.
- **Ожидание:** `Success(created=[...])` — пересоздание разрешено.

#### Create — stale result (epoch mismatch)

- **Предусловие:** `SubmitCreate` (epoch=1) → dialog closed → reopened → новый `SubmitCreate` (epoch=2).
- **Действие:** late `CreateResult(epoch=1, Success(...))` приходит после epoch=2 dispatched.
- **Ожидание:** state не меняется (stale epoch dropped).

#### Rename — happy path

- **Предусловие:** существует user-defined type T с name "Notes"; в одном из quiz_configs есть ref `user:"Notes"`.
- **Действие:** `OpenRenameDialog(T.id)` → `RenameTextChange("Annotations")` → `SubmitRename`.
- **Ожидание:** `RenameResult(epoch, Success(type with name="Annotations"))`; в БД — атомарный UPDATE component_types + cascade UPDATE quiz_configs.component_refs.

#### Rename — built-in protected

- **Предусловие:** type T — built-in (`systemKey != null`).
- **Действие:** `SubmitRename`.
- **Ожидание:** `RenameResult(epoch, BuiltInProtected)`; `snackbarState = Snackbar(<error>)`; диалог закрывается.

#### Delete — preview + confirm

- **Предусловие:** type T (`isMultiple=true`) имеет 23 active values в 2 словарях; в 1 quiz_config есть ref; 1 dict pref ссылается на ref.
- **Действие:** `OpenDeleteConfirm(T.id)` → жди `ImpactPreviewLoaded(T.id, impact)` → `ConfirmDelete`.
- **Ожидание:** preview: `valueCount=23, dictionariesWithValues=[D1, D2], affectedQuizConfigs=[(D1, mode)], affectedPrefs=[D1]`. После `ConfirmDelete`: `DeleteResult(epoch, Success(impact))`, `snackbarState = Snackbar("23 values hidden")`.

#### Delete — preview not-found

- **Предусловие:** UseCase возвращает `null` (type был soft-deleted параллельным actor'ом).
- **Действие:** `OpenDeleteConfirm(T.id)`.
- **Ожидание:** `Msg.ImpactPreviewFailed(T.id, cause=null)` → reducer закрывает loading-флаг + snackbar.

#### Delete — double-tap guard

- **Предусловие:** `isDeleting=true`.
- **Действие:** второй `ConfirmDelete`.
- **Ожидание:** state не меняется, второй effect не dispatch'ится.

#### CancellationException propagation

- **Предусловие:** Datasource handler выполняет UseCase call; coroutine cancelled извне.
- **Ожидание:** `CancellationException` re-throw'ится handler'ом (не конвертируется в `Failure`-Msg).

#### Per-dictionary — create with preselect scope

- **Предусловие:** `PerDictionaryComponentsScreenState(dictionaryId=D1, ...)`, диалог закрыт.
- **Действие:** `OpenCreateDialog`.
- **Ожидание:** `createDialog = CreateDialogState(scope = Scope.PerDictionaries(listOf(D1)))`.

#### Per-dictionary — global components visible

- **Предусловие:** существует active global "Tags"; открыт `PerDictionaryComponentsScreen(D1)`.
- **Действие:** Initial load → `ItemsLoaded(snapshot)`.
- **Ожидание:** `items` содержит row для "Tags" с `isGlobal=true`.

#### Open dialog closes other dialogs (mutual exclusion invariant)

- **Предусловие:** `renameDialog != null`.
- **Действие:** `OpenCreateDialog`.
- **Ожидание:** `createDialog != null && renameDialog == null && deleteConfirm == null && editDialog == null && in-flight flags == false`.

### Phase 2

#### Edit — happy path (rename only)

- **Предусловие:** существует user-defined type T с name "Notes", `isMultiple=true`; имени "Annotations" нет.
- **Действие:** `OpenEditDialog(T.id)` → `EditNameChange("Annotations")` → `SubmitEdit`.
- **Ожидание:** `isEditing=true`, dispatch `DatasourceEffect.EditComponent(epoch, T.id, "Annotations", TEXT, true)`. После `EditResult(epoch, Success(type with name="Annotations"))`: `isEditing=false`, `editDialog=null`, `snackbarState=Snackbar("Updated")`. Cascade UPDATE `quiz_configs.component_refs` выполнен на data-уровне (name изменился).

#### Edit — cardinality downgrade blocked (size ≤ 3)

- **Предусловие:** type T (`isMultiple=true`) имеет 2 лексемы с count>1; user открыл EditDialog.
- **Действие:** `EditMultiToggle(false)` → `SubmitEdit`.
- **Ожидание:** `EditResult(epoch, CardinalityDowngradeBlocked(impactedLexemeIds=[L1, L2]))` → `editDialog.impactedLexemesPreview = InlineOnly([L1, L2])`, dialog остаётся открытым, drill-in кнопка скрыта.

#### Edit — cardinality downgrade blocked (size > 3)

- **Предусловие:** type T (`isMultiple=true`) имеет 7 лексем с count>1.
- **Действие:** `EditMultiToggle(false)` → `SubmitEdit`.
- **Ожидание:** `EditResult(epoch, CardinalityDowngradeBlocked(impactedLexemeIds=[L1..L7]))` → `editDialog.impactedLexemesPreview = InlineWithDrillIn(impactedLexemeIds=[L1..L7], inlineIds=[L1, L2, L3])`, drill-in кнопка видна.

#### Edit — template immutability gate

- **Предусловие:** type T с template=TEXT.
- **Действие:** `EditTemplateChange(IMAGE)` → `SubmitEdit`.
- **Ожидание:** UseCaseImpl сравнивает new.template vs current.template, возвращает `EditOutcome.TemplateImmutable` БЕЗ вызова `lexemeApi.editComponentType`. `EditResult(epoch, TemplateImmutable)` → `editDialog=null, snackbarState=Snackbar(<template-immutable text>)`.

#### Edit — race with soft-delete (Removed)

- **Предусловие:** EditDialog открыт для type T; параллельно (другой process / cascade) T получает `removed_at = now()`.
- **Действие:** `SubmitEdit`.
- **Ожидание:** API возвращает `EditComponentOutcome.Removed` → UseCaseImpl mapping → `EditOutcome.Removed`. `EditResult(epoch, Removed)` → `editDialog=null, snackbarState=Snackbar("Компонент удалён")`.

#### Edit — same-scope collision

- **Предусловие:** уже существует active per-dict "Annotations" в том же словаре.
- **Действие:** Edit `T(name="Notes")` → `EditNameChange("Annotations")` → `SubmitEdit`.
- **Ожидание:** `EditResult(epoch, SameScopeCollision)` → `editDialog.nameError=EditNameError.SameScopeCollision`, диалог НЕ закрывается.

#### Edit — built-in protected

- **Предусловие:** type T — built-in (`systemKey != null`).
- **Действие:** `SubmitEdit`.
- **Ожидание:** `EditResult(epoch, BuiltInProtected)` → `editDialog=null, snackbarState=Snackbar(<built-in text>)`.

#### Edit — Failure handling

- **Предусловие:** Datasource handler ловит exception при `editComponent`.
- **Действие:** `SubmitEdit`.
- **Ожидание:** `EditResult(epoch, Failure(cause))` → `editDialog=null, snackbarState=Snackbar(failureLabel(cause))`.

#### Edit — stale result (epoch mismatch)

- **Предусловие:** `SubmitEdit` (epoch=1) → dialog closed → reopened → новый `SubmitEdit` (epoch=2).
- **Действие:** late `EditResult(epoch=1, Success(...))` приходит после epoch=2 dispatched.
- **Ожидание:** state не меняется (stale epoch dropped).

#### Edit — double-tap guard

- **Предусловие:** `isEditing=true`.
- **Действие:** второй `SubmitEdit`.
- **Ожидание:** state не меняется, второй effect не dispatch'ится.

#### Create — multi-dict scope happy path

- **Предусловие:** Manager-экран, диалог закрыт; `availableDictionaries=[D1, D2, D3]`.
- **Действие:** `OpenCreateDialog` → `CreateNameChange("Tags")` → `CreateScopeChange(PerDictionaries(emptyList()))` → `CreateDictionaryToggle(D1.id)` → `CreateDictionaryToggle(D2.id)` → `SubmitCreate`.
- **Ожидание:** dispatch `DatasourceEffect.CreateComponent(epoch, "Tags", TEXT, false, PerDictionaries([D1.id, D2.id]))`. После `Success(created=[t1, t2])`: `snackbarState=Snackbar("Created 2")`.

#### Create — submit disabled при пустом PerDictionaries selection

- **Предусловие:** `createDialog` с `scope=PerDictionaries`, `selectedDictionaryIds=emptySet()`, `name="Tags"`.
- **Действие:** computed `canSubmit`.
- **Ожидание:** `canSubmit == false` (submit-кнопка disabled).

#### Multi-dict — chip staleness filtering

- **Предусловие:** `createDialog.selectedDictionaryIds=[D1.id, D2.id]`; параллельно D1 удалён → приходит `DictionariesLoaded(updated=[D2, D3])`.
- **Действие:** `Msg.DictionariesLoaded(updated)`.
- **Ожидание:** `availableDictionaries=[D2, D3]`, `createDialog.selectedDictionaryIds=[D2.id]` (stale D1 отфильтрован). Если selection опустеет → `canSubmit=false`.

#### DictionariesLoaded не мутирует EditDialogState (инвариант)

- **Предусловие:** `editDialog != null` (с current name/template/isMultiple/impactedLexemesPreview).
- **Действие:** `Msg.DictionariesLoaded(updated)`.
- **Ожидание:** `availableDictionaries` обновлён; `createDialog.selectedDictionaryIds` фильтруется; поля `editDialog` (name/template/isMultiple/impactedLexemesPreview/isEditing) **не меняются**.

#### Mutual exclusion (Open*Dialog 4-way)

- **Предусловие:** `renameDialog != null`.
- **Действие:** `OpenEditDialog(T.id)`.
- **Ожидание:** `editDialog != null && renameDialog == null && createDialog == null && deleteConfirm == null && isCreating == isRenaming == isDeleting == false`.

#### Rename — Removed parity

- **Предусловие:** RenameDialog открыт; type был soft-deleted параллельно.
- **Действие:** `SubmitRename`.
- **Ожидание:** `RenameResult(epoch, Removed)` → `renameDialog=null, snackbarState=Snackbar("Компонент удалён")`.

#### Delete — Removed parity

- **Предусловие:** DeleteConfirm открыт; type был soft-deleted параллельно.
- **Действие:** `ConfirmDelete`.
- **Ожидание:** `DeleteResult(epoch, Removed)` → `deleteConfirm=null, snackbarState=Snackbar("Компонент удалён")`.

#### Cardinality downgrade SELECT precondition (UseCaseImpl-level)

- **Предусловие:** type T (`current.isMultiple=true`), Edit с `new.isMultiple=true` (upgrade либо unchanged).
- **Действие:** `useCase.editComponent(T.id, name, TEXT, isMultiple=true)`.
- **Ожидание:** orchestration НЕ вызывает cardinality downgrade SELECT (verify на DAO method ни разу не вызван). Аналогично для edit only name (isMultiple unchanged).
