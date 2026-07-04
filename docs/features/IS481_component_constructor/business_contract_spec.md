<!-- META: spec_filename: component-constructor.md -->

# Component Constructor — конструктор пользовательских компонентов

Конструктор user-defined компонентов словаря: CRUD (create / rename / soft-delete) с двумя независимыми точками входа — глобальный `ComponentsManagerScreen` из `SettingsTab` и per-dictionary `PerDictionaryComponentsScreen`, открываемый из `DictionaryAppBar` по icon-button «молоток».

---

## Бизнес-описание

Component Constructor расширяет модель словаря: помимо built-in компонентов (Translation, Definition и т.п.) пользователь сам определяет компоненты для лексем — задавая имя, шаблон (template) и cardinality (`is_multi` — один или несколько values на лексему). Компоненты живут в одной из двух scope:

- **Global** (`dictionary_id IS NULL`) — компонент применим ко всем словарям.
- **Per-dictionary** — компонент привязан к конкретному словарю (или к нескольким — создание N rows из одного диалога).

Конструктор доступен из двух точек входа: **общий менеджер** (`SettingsTab → ComponentsManagerScreen`) показывает aggregated view всех user-defined компонентов из всех словарей с usage badge, **per-dictionary view** (`DictionaryAppBar → молоток → PerDictionaryComponentsScreen`) фильтрует только компоненты применимые к открытому словарю (global + own per-dict). Built-in компоненты в обоих экранах **не показываются** — это территория конструктора пользовательских компонентов.

Soft-delete не уничтожает данные: `component_types.removed_at` ставит timestamp, существующие `component_values` остаются в БД, но скрываются на чтении (фильтр `WHERE removed_at IS NULL` / JOIN на parent). Перед удалением показывается preview impact: сколько values скрываются, в каких словарях, какие quiz-конфиги затронуты, какие prefs сбросятся. Cascade-эффекты атомарны: soft-delete component_type → cleanup `quiz_configs.component_refs` (одна транзакция) → reset `quiz_picker_dict_<id>` prefs (UseCase composition, prefs живут в DataStore вне Room).

Имя компонента уникально в рамках своего scope **и одновременно cross-scope**: global "Foo" исключает per-dict "Foo" в любом словаре и наоборот (инвариант `userdefined_identity_invariant`). Уникальность enforce'ится в UseCase через two-prong SELECT перед INSERT (UNIQUE-индекс из БД убран ради поддержки пересоздания имени после soft-delete).

---

## User Stories

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

---

## State

### Package partitioning

Domain-shared types живут в `:modules:domain:lexeme` (package `me.apomazkin.lexeme`) — обоснование F079:

- `Scope` (sealed interface)
- `NameError` (sealed) — используется в State обоих экранов
- `CreateOutcome` / `RenameOutcome` / `DeleteOutcome` (sealed)
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

    // ===== UI flags (explicit) =====
    val isLoading: Boolean = false,                        // initial load / refresh
    val isCreating: Boolean = false,                       // submit in flight
    val isRenaming: Boolean = false,                       // submit in flight
    val isDeleting: Boolean = false,                       // soft-delete in flight

    // ===== Dialogs =====
    val createDialog: CreateDialogState? = null,
    val renameDialog: RenameDialogState? = null,
    val deleteConfirm: DeleteConfirmState? = null,
)
```

#### Per-field

| Поле | Что | Почему |
|---|---|---|
| `userDefinedTypes` | Список aggregated row'ов user-defined типов из всех словарей | `null` = ещё не загружено (initial), `emptyList` = загружено и пусто (показ empty state). Built-in в список не входят. |
| `isLoading` | Initial load / refresh in flight | Explicit флаг, не выводится из `userDefinedTypes == null` (после первой загрузки `null` уже не возникнет, нужен отдельный сигнал на refresh). |
| `isCreating` / `isRenaming` / `isDeleting` | Submit в полёте | Блокирует submit-кнопку в соответствующем диалоге, защищает от двойного тапа. |
| `createDialog` / `renameDialog` / `deleteConfirm` | Per-dialog state (visible iff `!= null`) | Простая модель видимости диалога — null/non-null. Один диалог одновременно (см. инварианты). |

#### Shared domain types (`:modules:domain:lexeme`)

```kotlin
package me.apomazkin.lexeme

sealed interface Scope {
    data object Global : Scope                                  // dictionaryId IS NULL
    data class PerDictionaries(val ids: List<Long>) : Scope     // одна или несколько привязок
}

sealed interface NameError {
    data object Empty : NameError                               // name.isBlank()
    data object TooLong : NameError                             // длиной свыше лимита (UI-policy)
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
    val isMulti: Boolean,
    val scope: Scope,                       // Global / PerDictionaries(list)
    val usageCount: Int,                    // суммарно активных values по словарям
    val dictionaryNames: List<String>,      // в каких словарях виден (badge)
)

data class CreateDialogState(
    val name: String = "",
    val template: ComponentTemplate = ComponentTemplate.TEXT,   // MVP: только TEXT
    val isMulti: Boolean = false,
    val scope: Scope = Scope.Global,                            // дефолт на aggregated view
    val nameError: NameError? = null,
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
```

#### Computed properties

```kotlin
val ComponentsManagerScreenState.isEmpty: Boolean
    get() = userDefinedTypes?.isEmpty() == true && !isLoading
```

#### Инварианты

- `[shape]` Одновременно открыт **не более одного** диалога: `createDialog != null` ⊕ `renameDialog != null` ⊕ `deleteConfirm != null`.
- `[shape]` `isCreating == true` → `createDialog != null` (submit подразумевает открытый диалог).
- `[shape]` `isRenaming == true` → `renameDialog != null`.
- `[shape]` `isDeleting == true` → `deleteConfirm != null`.
- `[shape]` `deleteConfirm?.impact == null && deleteConfirm?.isLoadingImpact == false` — preview ещё не запросили (отдельное состояние от «грузится»).
- `[transition]` после `Msg.ConfirmDelete` reducer выставляет `isDeleting=true` и dispatch `SoftDeleteComponent` ровно один раз; повторный `ConfirmDelete` при `isDeleting=true` игнорируется.

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
    val dictionaryId: Long,                                    // навигационный параметр (через @Assisted)
    val dictionaryName: String? = null,                        // для header

    // ===== Loaded data =====
    val items: List<PerDictRow>? = null,                       // null = ещё не загружено

    // ===== UI flags (explicit) =====
    val isLoading: Boolean = false,
    val isCreating: Boolean = false,
    val isRenaming: Boolean = false,
    val isDeleting: Boolean = false,

    // ===== Dialogs =====
    val createDialog: CreateDialogState? = null,
    val renameDialog: RenameDialogState? = null,
    val deleteConfirm: DeleteConfirmState? = null,
)

/**
 * Per-dictionary view row. `scope` упрощён до Boolean: компонент либо global
 * (виден везде), либо local (привязан именно к этому словарю).
 */
data class PerDictRow(
    val typeId: ComponentTypeId,
    val name: String,
    val template: ComponentTemplate,
    val isMulti: Boolean,
    val isGlobal: Boolean,           // dictionaryId IS NULL
    val valueCount: Int,             // активные values в данном словаре
)
```

`CreateDialogState`, `RenameDialogState`, `DeleteConfirmState`, `NameError` — переиспользуются из `components_manager.logic` (или дублируются точечно — конкретное решение на business_design_tree; контракт от выбора не зависит).

**Отличие диалога создания** на per-dict: дефолт `scope = Scope.PerDictionaries(listOf(dictionaryId))` (открытие из контекста словаря — логично прибиндить к нему). Пользователь может переключить на Global; per-dict список preselect'ен текущим словарём.

Инварианты `[shape]` те же что у `ComponentsManagerScreenState`.

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
import me.apomazkin.lexeme.DeletionImpact
import me.apomazkin.lexeme.UserDefinedTypesSnapshot

sealed interface Msg {

    // ===== Lifecycle / data =====
    /** Результат подписки на flowAllUserDefinedTypes (или initial load). */
    data class TypesLoaded(val snapshot: UserDefinedTypesSnapshot) : Msg
    data class TypesLoadFailed(val cause: Throwable) : Msg

    // ===== Create dialog =====
    data object OpenCreateDialog : Msg
    data object CloseCreateDialog : Msg
    data class CreateNameChange(val value: String) : Msg
    data class CreateTemplateChange(val template: ComponentTemplate) : Msg
    data class CreateMultiToggle(val isMulti: Boolean) : Msg
    data class CreateScopeChange(val scope: Scope) : Msg
    data object SubmitCreate : Msg
    data class CreateResult(val outcome: CreateOutcome) : Msg

    // ===== Rename dialog =====
    data class OpenRenameDialog(val typeId: ComponentTypeId) : Msg
    data object CloseRenameDialog : Msg
    data class RenameTextChange(val value: String) : Msg
    data object SubmitRename : Msg
    data class RenameResult(val outcome: RenameOutcome) : Msg

    // ===== Delete dialog =====
    data class OpenDeleteConfirm(val typeId: ComponentTypeId) : Msg
    data object CloseDeleteConfirm : Msg
    data class ImpactPreviewLoaded(val impact: DeletionImpact) : Msg
    data class ImpactPreviewFailed(val cause: Throwable) : Msg
    data object ConfirmDelete : Msg
    data class DeleteResult(val outcome: DeleteOutcome) : Msg

    // ===== Navigation =====
    data object RequestBack : Msg

    // ===== No-op =====
    data object Empty : Msg
}

/**
 * UI feedback (snackbar). Top-level (parity с existing convention — см. wordcard,
 * quiz-chat и т.д.); вынесен из `Msg` ради consistency.
 */
sealed interface UiMsg : Msg {
    data class Snackbar(val text: String, val show: Boolean) : UiMsg
}
```

### Категории Msg

- **Lifecycle / data** — `TypesLoaded`, `TypesLoadFailed` — приходят из flow-handler'а.
- **Dialog open/close + field edit** — `Open*Dialog` / `Close*Dialog` / `*Change` / `*Toggle` — управление UI state без IO.
- **Submit** — `SubmitCreate`, `SubmitRename`, `ConfirmDelete` — триггерят datasource effect.
- **Result** — `CreateResult`, `RenameResult`, `DeleteResult`, `ImpactPreviewLoaded/Failed` — приходят из datasource handler'а с typed outcome.
- **Navigation** — `RequestBack`.
- **UiMsg** — внутренние UI feedback (snackbar text).

### Per-Msg reducer reaction (выборочно)

| Msg | State change | Effect |
|---|---|---|
| `TypesLoaded(snapshot)` | `userDefinedTypes = snapshot.toRows(), isLoading=false` | `∅` |
| `OpenCreateDialog` | `createDialog = CreateDialogState()` | `∅` |
| `CreateNameChange(v)` | `createDialog?.copy(name=v, nameError=null)` | `∅` |
| `SubmitCreate` | `isCreating=true` | `DatasourceEffect.CreateComponent(...)` |
| `CreateResult(Success(types))` | `isCreating=false, createDialog=null` | `UiEffect.Snackbar("Created ${types.size}")` |
| `CreateResult(SameScopeCollision)` | `isCreating=false, createDialog.copy(nameError=NameError.SameScopeCollision)` | `∅` |
| `CreateResult(CrossScopeCollision)` | `isCreating=false, createDialog.copy(nameError=NameError.CrossScopeCollision)` | `∅` |
| `OpenDeleteConfirm(id)` | `deleteConfirm = DeleteConfirmState(id, name, isLoadingImpact=true)` | `DatasourceEffect.LoadImpact(id)` |
| `ImpactPreviewLoaded(i)` | `deleteConfirm?.copy(impact=i, isLoadingImpact=false)` | `∅` |
| `ConfirmDelete` | `isDeleting=true` (guard `isDeleting==false`) | `DatasourceEffect.SoftDeleteComponent(id)` |
| `DeleteResult(Success(impact))` | `isDeleting=false, deleteConfirm=null` | `UiEffect.Snackbar("N values hidden")` |
| `RequestBack` | без изменений | `NavigationEffect.Back` |

### Msg для `PerDictionaryComponentsScreen`

Зеркальный sealed `Msg` — те же `Open*Dialog` / `*Change` / `Submit*` / `*Result` события. Отличие в lifecycle-сообщении: `ItemsLoaded(snapshot: PerDictionarySnapshot)` вместо `TypesLoaded`.

### Guard'ы

- `SubmitCreate` обрабатывается только при `isCreating == false`.
- `SubmitRename` — только при `isRenaming == false`.
- `ConfirmDelete` — только при `isDeleting == false`.
- `Create*Change` / `Rename*Change` сбрасывают `nameError = null` при изменении поля (immediate clear hint).

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

    // Note: initial subscribe реализован как `init`-trigger в `MateFlowHandler.subscribe(scope, send)`
    // (assisted FlowHandler — `AllUserDefinedTypesFlowHandler` подписывается при init Mate).
    // Отдельный `SubscribeAll` Effect не нужен — handler автоматически стартует на старте экрана.

    data class CreateComponent(
        val name: String,
        val template: ComponentTemplate,
        val isMulti: Boolean,
        val scope: Scope,
    ) : ComponentsManagerDatasourceEffect

    data class RenameComponent(
        val typeId: ComponentTypeId,
        val newName: String,
    ) : ComponentsManagerDatasourceEffect

    data class LoadImpact(val typeId: ComponentTypeId) : ComponentsManagerDatasourceEffect

    data class SoftDeleteComponent(val typeId: ComponentTypeId) : ComponentsManagerDatasourceEffect
}
```

**Reducer mapping для datasource:**

| Effect | UseCase call | Msg back |
|---|---|---|
| `CreateComponent` | `useCase.createUserDefinedComponent(name, template, isMulti, scope)` → `CreateOutcome` | `Msg.CreateResult(outcome)` |
| `RenameComponent` | `useCase.renameComponent(typeId, newName)` → `RenameOutcome` | `Msg.RenameResult(outcome)` |
| `LoadImpact` | `useCase.previewDeletionImpact(typeId)` → `DeletionImpact?` | `Msg.ImpactPreviewLoaded(it)` или `Msg.ImpactPreviewFailed(e)` |
| `SoftDeleteComponent` | `useCase.softDeleteComponent(typeId)` → `DeleteOutcome` | `Msg.DeleteResult(outcome)` |

#### `ComponentsManagerUiEffect`

```kotlin
sealed interface ComponentsManagerUiEffect : Effect {
    data class Snackbar(val text: String) : ComponentsManagerUiEffect
}
```

#### `ComponentsManagerNavigationEffect`

```kotlin
sealed interface ComponentsManagerNavigationEffect : NavigationEffect {
    // Только Back (наследуется из общей NavigationEffect-иерархии);
    // drill-in в per-dict вид не предусмотрен — экраны независимы.
}
```

#### `PerDictionaryComponentsScreen` — те же три категории

Отличие в datasource:
- Initial subscribe реализован как `init`-trigger в `MateFlowHandler.subscribe(scope, send)` (assisted FlowHandler — `ComponentsForDictionaryFlowHandler` подписывается при init Mate с `dictionaryId`). Отдельный `SubscribeForDictionary` Effect не нужен — handler автоматически стартует на старте экрана (parity с CM, см. § Subscribers).
- `CreateComponent(...)` / `RenameComponent(...)` / `LoadImpact(...)` / `SoftDeleteComponent(...)` — те же signature; `dictionaryId` приходит из `state.dictionaryId` при `CreateComponent` (preselect scope).

### Subscribers

#### `AllUserDefinedTypesFlowHandler` (ComponentsManagerScreen)

Подписывается на `useCase.flowAllUserDefinedTypes(): Flow<UserDefinedTypesSnapshot>`, emit'ит:
- `Msg.TypesLoaded(snapshot)` — на каждый emit Flow;
- `Msg.TypesLoadFailed(cause)` — на ошибку collect.

Триггерится автоматически при init Mate (assisted FlowHandler — `subscribe(scope, send)` стартует collect без явного Effect; см. `business_design_tree` #34).

#### `ComponentsForDictionaryFlowHandler` (PerDictionaryComponentsScreen)

Подписывается на `useCase.flowComponentsForDictionary(dictionaryId): Flow<PerDictionarySnapshot>`, emit'ит:
- `Msg.ItemsLoaded(snapshot)`;
- `Msg.ItemsLoadFailed(cause)`.

Триггерится при инициализации Mate с `dictionaryId`.

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
    // composite templates — добавляются в будущих фичах
    ;

    val fields: List<Field> get() = when (this) {
        TEXT -> listOf(Field("value", PrimitiveType.TEXT))
        IMAGE -> listOf(Field("value", PrimitiveType.IMAGE))
    }

    companion object {
        /**
         * Fail-soft парсинг: unknown key → null + caller логирует в Crashlytics.
         */
        fun fromKey(key: String): ComponentTemplate? = entries.firstOrNull { it.key == key }
    }
}
```

#### `TemplateValues` (sealed)

```kotlin
sealed interface TemplateValues

data class TextValues(
    val value: Primitive.Text,
) : TemplateValues

data class ImageValues(
    val value: Primitive.Image,
) : TemplateValues
```

MVP — только `TextValues`; `ImageValues` показан как пример как масштабируется. Composite values (`QuoteWithSourceValues` и т.п.) — будущие фичи.

#### `ComponentType`

```kotlin
data class ComponentType(
    val id: ComponentTypeId,
    val systemKey: BuiltInComponent?,    // null для user-defined
    val dictionaryId: Long?,             // null для global
    val name: String?,
    val template: ComponentTemplate,
    val position: Int,
    val isMulti: Boolean = false,
    val createdAt: Date,
    val updatedAt: Date,
    val removedAt: Date? = null,
)
```

#### `DeletionImpact`

```kotlin
/**
 * Preview каскадного soft-delete user-defined component_type.
 *
 * `affectedQuizConfigs` — список (dictId, quizMode), у которых в `component_refs`
 *  есть ref на удаляемый тип; будет вычищен в одной транзакции с soft-delete.
 *
 * `affectedPrefs` — список dictId у которых `quiz_picker_dict_<id>` pref
 *  ссылается на удаляемый ref; будет сброшен after soft-delete.
 */
data class DeletionImpact(
    val valueCount: Int,                                    // hidden component_values
    val dictionariesWithValues: List<Long>,                 // dictIds где values скроются
    val affectedQuizConfigs: List<AffectedQuizConfig>,
    val affectedPrefs: List<Long>,                          // dictIds где quiz_picker pref сбросится
)

data class AffectedQuizConfig(
    val dictionaryId: Long,
    val quizMode: String,    // quizMode хранится как String (data layer convention); enum может появиться в будущем
)
```

#### `ComponentUsage`

```kotlin
/** Aggregated usage data для отрисовки rows без N+1 запросов. */
data class ComponentUsage(
    val valueCountByType: Map<ComponentTypeId, Int>,
    val dictionaryIdsByType: Map<ComponentTypeId, Set<Long>>,
    val dictionaryNames: Map<Long, String>,
)
```

#### `UserDefinedTypesSnapshot`

```kotlin
/** Snapshot для aggregated view. Dedicated data class устраняет неоднозначность `.first/.second`. */
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

#### `CreateOutcome` / `RenameOutcome` / `DeleteOutcome`

```kotlin
sealed interface CreateOutcome {
    /**
     * Создано N rows. Для `Scope.Global` или `Scope.PerDictionaries(listOf(x))` — list length = 1;
     * для `Scope.PerDictionaries(listOf(d1, d2, ...))` — list length = N (по одному row per dictionary).
     */
    data class Success(val created: List<ComponentType>) : CreateOutcome
    data object NameEmpty : CreateOutcome
    /** Same-scope коллизия (active row с тем же именем уже есть). */
    data object SameScopeCollision : CreateOutcome
    /** Cross-scope коллизия (global ⊥ per-dict invariant). */
    data object CrossScopeCollision : CreateOutcome
    /** Любая другая ошибка (IO / SQL / etc) — UI показывает generic error snackbar. */
    data class Failure(val cause: Throwable) : CreateOutcome
}

sealed interface RenameOutcome {
    data class Success(val type: ComponentType) : RenameOutcome
    data object NameEmpty : RenameOutcome
    data object SameScopeCollision : RenameOutcome
    data object CrossScopeCollision : RenameOutcome
    /** Попытка переименовать built-in запрещена. */
    data object BuiltInProtected : RenameOutcome
    data class Failure(val cause: Throwable) : RenameOutcome
}

sealed interface DeleteOutcome {
    data class Success(val impact: DeletionImpact) : DeleteOutcome  // для snackbar
    data object BuiltInProtected : DeleteOutcome
    data class Failure(val cause: Throwable) : DeleteOutcome
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
import me.apomazkin.lexeme.DeletionImpact
import me.apomazkin.lexeme.UserDefinedTypesSnapshot

interface ComponentsManagerUseCase {

    /**
     * Реактивная подписка на все user-defined component_types (built-in исключены)
     * + aggregated usage по всем словарям. Один snapshot — без N+1 запросов из reducer.
     *
     * Subscribed by: `AllUserDefinedTypesFlowHandler`.
     */
    fun flowAllUserDefinedTypes(): Flow<UserDefinedTypesSnapshot>

    /**
     * Создать user-defined component_type. Внутри:
     *  1) валидация имени (non-blank);
     *  2) two-prong SELECT (invariant `userdefined_identity_invariant`):
     *     - same-scope active row с тем же именем (per-dict либо global);
     *     - cross-scope (global ⊥ per-dict) inverse check;
     *  3) INSERT row(s) — для `Scope.PerDictionaries(N)` создаётся N rows.
     *
     * Triggered by: `ComponentsManagerDatasourceEffect.CreateComponent`.
     * @return [CreateOutcome.Success] с list созданных entities (length = N для multi-scope) либо typed error.
     */
    suspend fun createUserDefinedComponent(
        name: String,
        template: ComponentTemplate,
        isMulti: Boolean,
        scope: Scope,
    ): CreateOutcome

    /**
     * Переименовать user-defined component_type. Cascade UPDATE на
     * `quiz_configs.component_refs` в одной транзакции.
     * Built-in protected на SQL-уровне (`WHERE system_key IS NULL`).
     *
     * Triggered by: `ComponentsManagerDatasourceEffect.RenameComponent`.
     */
    suspend fun renameComponent(
        typeId: ComponentTypeId,
        newName: String,
    ): RenameOutcome

    /**
     * Preview каскада soft-delete: valueCount + dictionariesWithValues +
     * affectedQuizConfigs + affectedPrefs. Read-only; не изменяет БД.
     * UI вызывает перед confirm-dialog'ом.
     *
     * Triggered by: `ComponentsManagerDatasourceEffect.LoadImpact`.
     */
    suspend fun previewDeletionImpact(typeId: ComponentTypeId): DeletionImpact?

    /**
     * Soft-delete user-defined component_type + atomic cleanup:
     *  1) UPDATE component_types SET removed_at = now() WHERE id = ? AND system_key IS NULL;
     *  2) Cleanup `quiz_configs.component_refs` — убрать ref на этот type;
     *  3) Cleanup `quiz_picker_dict_<id>` prefs которые ссылаются на этот ref.
     *
     * `component_values` НЕ trogаются — фильтр по `parent.removed_at IS NULL`
     * через JOIN на чтении.
     *
     * Triggered by: `ComponentsManagerDatasourceEffect.SoftDeleteComponent`.
     */
    suspend fun softDeleteComponent(typeId: ComponentTypeId): DeleteOutcome
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
import me.apomazkin.lexeme.DeletionImpact
import me.apomazkin.lexeme.PerDictionarySnapshot

interface PerDictionaryComponentsUseCase {

    /**
     * Подписка на active user-defined component_types применимых к словарю
     * (global `dictionaryId IS NULL` + per-dict `dictionaryId = :dictId`).
     * Built-in исключены. Также — valueCount внутри словаря.
     *
     * Subscribed by: `ComponentsForDictionaryFlowHandler`.
     */
    fun flowComponentsForDictionary(dictionaryId: Long): Flow<PerDictionarySnapshot>

    /** Те же CRUD методы, что в `ComponentsManagerUseCase`. Domain-семантика общая. */
    suspend fun createUserDefinedComponent(
        name: String,
        template: ComponentTemplate,
        isMulti: Boolean,
        scope: Scope,
    ): CreateOutcome

    suspend fun renameComponent(typeId: ComponentTypeId, newName: String): RenameOutcome

    suspend fun previewDeletionImpact(typeId: ComponentTypeId): DeletionImpact?

    suspend fun softDeleteComponent(typeId: ComponentTypeId): DeleteOutcome
}
```

---

## Тестовые сценарии

### Create — happy path

- **Предусловие:** state без активных диалогов, существует словарь D1; user-defined типов с именем "Notes" нет.
- **Действие:** `OpenCreateDialog` → `CreateNameChange("Notes")` → `CreateTemplateChange(TEXT)` → `CreateScopeChange(Scope.PerDictionaries(listOf(D1.id)))` → `SubmitCreate`.
- **Ожидание:** `isCreating=true`, dispatch `DatasourceEffect.CreateComponent("Notes", TEXT, false, PerDictionaries([D1.id]))`. После `CreateResult(Success(created=[type]))`: `isCreating=false`, `createDialog=null`, UI snackbar "Created 1".

### Create — same-scope collision

- **Предусловие:** уже существует active user-defined per-dict "Notes" (`dictionary_id=D1, removed_at IS NULL`).
- **Действие:** Open + Fill + Submit с тем же именем + scope `PerDictionaries([D1.id])`.
- **Ожидание:** `CreateResult(SameScopeCollision)` → `isCreating=false`, `createDialog.nameError=NameError.SameScopeCollision`, диалог НЕ закрывается.

### Create — cross-scope collision (invariant)

- **Предусловие:** существует active **global** "Notes" (`dictionary_id IS NULL, removed_at IS NULL`).
- **Действие:** Submit per-dict "Notes" в любом словаре.
- **Ожидание:** `CreateResult(CrossScopeCollision)` → `nameError=NameError.CrossScopeCollision`.

### Create — recreate after soft-delete

- **Предусловие:** существует soft-deleted user-defined "Notes" (`removed_at IS NOT NULL`); активной "Notes" нет.
- **Действие:** Submit "Notes" в том же scope.
- **Ожидание:** `Success(created=[...])` — пересоздание разрешено, поскольку UNIQUE-индекс из БД убран а UseCase фильтрует `WHERE removed_at IS NULL`.

### Rename — happy path

- **Предусловие:** существует user-defined type T с name "Notes"; в одном из quiz_configs есть ref `user:"Notes"`.
- **Действие:** `OpenRenameDialog(T.id)` → `RenameTextChange("Annotations")` → `SubmitRename`.
- **Ожидание:** `RenameResult(Success(type with name="Annotations"))`; в БД — атомарный UPDATE component_types + cascade UPDATE quiz_configs.component_refs (`user:"Notes"` → `user:"Annotations"`). `renameDialog=null`.

### Rename — built-in protected

- **Предусловие:** type T — built-in (`systemKey != null`).
- **Действие:** `SubmitRename`.
- **Ожидание:** `RenameResult(BuiltInProtected)`; UI snackbar с error message; диалог закрывается.

### Delete — preview + confirm

- **Предусловие:** type T (`isMulti=true`) имеет 23 active values в 2 словарях; в 1 quiz_config есть ref; 1 dict pref ссылается на ref.
- **Действие:** `OpenDeleteConfirm(T.id)` → жди `ImpactPreviewLoaded(impact)` → `ConfirmDelete`.
- **Ожидание:** preview: `valueCount=23, dictionariesWithValues=[D1, D2], affectedQuizConfigs=[(D1, mode)], affectedPrefs=[D1]`. После `ConfirmDelete`: `DeleteResult(Success(impact))`, snackbar "23 values hidden".

### Delete — double-tap guard

- **Предусловие:** `isDeleting=true` (первый ConfirmDelete уже в полёте).
- **Действие:** второй `ConfirmDelete`.
- **Ожидание:** state не меняется, второй effect не dispatch'ится.

### Per-dictionary — create with preselect scope

- **Предусловие:** `PerDictionaryComponentsScreenState(dictionaryId=D1, ...)`, диалог закрыт.
- **Действие:** `OpenCreateDialog`.
- **Ожидание:** `createDialog = CreateDialogState(scope = Scope.PerDictionaries(listOf(D1)))` — текущий словарь preselect'ен.

### Per-dictionary — global components visible

- **Предусловие:** существует active global "Tags" (`dictionary_id IS NULL`); открыт `PerDictionaryComponentsScreen(D1)`.
- **Действие:** Initial load → `ItemsLoaded(snapshot)`.
- **Ожидание:** `items` содержит row для "Tags" с `isGlobal=true`.

---

## История ревью

### iter 1 (2026-06-16) — заархивирован

Verdict: changes_requested. 4 findings:
- F078 (critical): QuizMode unresolved type → String
- F079 (minor): types в screen-package → moved to `:modules:domain:lexeme`
- F080 (minor): CreateOutcome.NameTaken(scope) dead → removed
- F081 (minor): NameError.ScopeCollision dead → removed; reducer maps discrete cases

### iter 2 (2026-06-16): 4 findings fixed → spec готов

### iter 4 sync (2026-06-16): F086 + F088 sync from business_design_tree review

- F086: UiMsg переписан с вложенного Msg.UiMsg на top-level UiMsg : Msg (parity с DT и existing convention).
- F088: SubscribeAll удалён из ComponentsManagerDatasourceEffect sealed (init подписка через assisted FlowHandler).

### iter 5 sync (2026-06-16): F091 sync from business_design_tree review

- SubscribeForDictionary удалён из PerDictionaryComponentsDatasourceEffect (parity с CM после F088).
