# Business contract: IS481 component_constructor

Контракт двух новых экранов конструктора компонентов (global `ComponentsManagerScreen`, scoped `PerDictionaryComponentsScreen`) + UseCase API + domain rewrite (`TemplateValues`).

Контракт описывает **target state ПОСЛЕ M13** (`is_multi`, `removed_at`, `TemplateValues`, `Primitive`/`Field`/`PrimitiveType`). M12 → M13 переход — в `concept/template_model.md` § Миграция.

Базовые конвенции UseCase извлечены из `WordCardUseCaseImpl` (walkthrough § 9):
- `suspend fun` для CRUD; `Flow<...>` только для реактивных подписок.
- Возврат `T?` для простых ошибок (логирование внутри impl через `LexemeLogger`).
- Sealed `Result` иерархия только для типизированных ветвлений (cascade vs partial).
- `try { ... } catch (e: Exception) { logger.e(...); null }` — пэттерн wrap внутри impl.

State моделирование — `[guide: state-modeling.md → §10-11, §16]`: feature-level state, simple `data: T? = null` + `isLoading: Boolean`, инварианты помечены `[shape]` / `[transition]`.

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

### `ComponentsManagerScreen` (aggregated CRUD)

```kotlin
package me.apomazkin.components_manager.logic

import me.apomazkin.lexeme.ComponentTemplate
import me.apomazkin.lexeme.ComponentTypeId
import me.apomazkin.lexeme.Scope
import me.apomazkin.lexeme.NameError
import me.apomazkin.lexeme.DeletionImpact
import me.apomazkin.dictionary.Dictionary // если нужен

data class ComponentsManagerScreenState(
    // ===== Loaded data =====
    val userDefinedTypes: List<UserDefinedRow>? = null,    // null = ещё не загружено

    // ===== UI flags (explicit) =====
    val isLoading: Boolean = false,                        // initial load / refresh
    val isCreating: Boolean = false,                       // submit in flight
    val isRenaming: Boolean = false,                       // submit in flight
    val isDeleting: Boolean = false,                       // soft-delete in flight

    // ===== Dialog: create =====
    val createDialog: CreateDialogState? = null,

    // ===== Dialog: rename =====
    val renameDialog: RenameDialogState? = null,

    // ===== Dialog: delete confirm =====
    val deleteConfirm: DeleteConfirmState? = null,
)
```

#### Shared domain types (`:modules:domain:lexeme`)

```kotlin
package me.apomazkin.lexeme

sealed interface Scope {
    data object Global : Scope                                  // dictionaryId IS NULL
    data class PerDictionaries(val ids: List<Long>) : Scope     // одна или несколько строк per scope
}

sealed interface NameError {
    data object Empty : NameError                               // name.isBlank()
    data object TooLong : NameError                             // длиной свыше лимита (резерв; UI-policy)
    data object SameScopeCollision : NameError                  // active rows в том же scope
    data object CrossScopeCollision : NameError                 // global ⊥ per-dict invariant
}
```

#### Nested state классы (screen-package)

```kotlin
package me.apomazkin.components_manager.logic

import me.apomazkin.lexeme.ComponentTemplate
import me.apomazkin.lexeme.ComponentTypeId
import me.apomazkin.lexeme.Scope
import me.apomazkin.lexeme.NameError
import me.apomazkin.lexeme.DeletionImpact

/**
 * Aggregated UI row для одного user-defined component_type из любого словаря.
 * Built-in компоненты в этот список НЕ попадают (ui_placement.md § Общий view).
 */
data class UserDefinedRow(
    val typeId: ComponentTypeId,
    val name: String,
    val template: ComponentTemplate,
    val isMulti: Boolean,
    val scope: Scope,                       // Global / PerDictionaries(list)
    val usageCount: Int,                    // суммарно values по словарям
    val dictionaryNames: List<String>,      // в каких словарях видны (UI badge)
)

data class CreateDialogState(
    val name: String = "",
    val template: ComponentTemplate = ComponentTemplate.TEXT,   // MVP: только TEXT
    val isMulti: Boolean = false,
    val scope: Scope = Scope.Global,                            // дефолт по ui_placement.md
    val nameError: NameError? = null,                           // показывает в UI
)

data class RenameDialogState(
    val typeId: ComponentTypeId,
    val originalName: String,        // domain model (не правится)
    val editedName: String,          // [guide: state-modeling.md § 14 Редактируемые поля]
    val nameError: NameError? = null,
)

data class DeleteConfirmState(
    val typeId: ComponentTypeId,
    val name: String,
    val impact: DeletionImpact? = null,    // null пока preview грузится
    val isLoadingImpact: Boolean = false,
)
```

#### Computed properties (selectors) `[guide: state-modeling.md § 12]`

```kotlin
val ComponentsManagerScreenState.isEmpty: Boolean
    get() = userDefinedTypes?.isEmpty() == true && !isLoading
```

#### Инварианты State

- `[shape]` `createDialog != null` ⊕ `renameDialog != null` ⊕ `deleteConfirm != null` — одновременно открыт не более одного диалога. Если ломается — UI неоднозначен.
- `[shape]` `isCreating == true` → `createDialog != null` (submit в полёте подразумевает что диалог открыт).
- `[shape]` `isRenaming == true` → `renameDialog != null`.
- `[shape]` `isDeleting == true` → `deleteConfirm != null`.
- `[shape]` `deleteConfirm?.impact == null && deleteConfirm?.isLoadingImpact == false` означает что preview ещё не запросили (initial state диалога) — отдельно от «грузится».

---

### `PerDictionaryComponentsScreen` (scoped CRUD)

Per-dictionary view — тот же CRUD сюжет что и aggregated, но **фильтр по `dictionaryId`** (`ui_placement.md` § Per-dictionary view). Видны только user-defined компоненты которые применимы к этому словарю — global (`dictionaryId IS NULL`) **И** per-dict с `dictionaryId == this.id`.

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
    val dictionaryName: String? = null,                        // для header / breadcrumb

    // ===== Loaded data =====
    val items: List<PerDictRow>? = null,                       // null = ещё не загружено

    // ===== UI flags (explicit) =====
    val isLoading: Boolean = false,
    val isCreating: Boolean = false,
    val isRenaming: Boolean = false,
    val isDeleting: Boolean = false,

    // ===== Dialogs (re-using shared shapes) =====
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
    val valueCount: Int,             // values в данном словаре
)
```

`CreateDialogState`, `RenameDialogState`, `DeleteConfirmState`, `NameError` — переиспользуются из `components_manager.logic` (либо дублируются точечно — финальное решение на business_design_tree; контракт не зависит).

Отличие диалога создания на per-dict: дефолт `scope = Scope.PerDictionaries(listOf(dictionaryId))` (открытие из контекста словаря — логично прибиндить именно к нему). Юзер может переключить на Global, но имя «PerDictionaries(...)` чек-боксы предзаполнены текущим словарём.

Те же инварианты `[shape]` что и для `ComponentsManagerScreenState`.

---

## Msg

### `ComponentsManagerScreen`

```kotlin
package me.apomazkin.components_manager.logic

import me.apomazkin.lexeme.ComponentTemplate
import me.apomazkin.lexeme.ComponentType
import me.apomazkin.lexeme.ComponentTypeId
import me.apomazkin.lexeme.Scope
import me.apomazkin.lexeme.CreateOutcome
import me.apomazkin.lexeme.RenameOutcome
import me.apomazkin.lexeme.DeleteOutcome
import me.apomazkin.lexeme.DeletionImpact
import me.apomazkin.lexeme.UserDefinedTypesSnapshot

sealed interface Msg {

    // ===== Lifecycle / data =====
    /** Результат подписки `FlowAllUserDefinedTypes` (или initial load). */
    data class TypesLoaded(val snapshot: UserDefinedTypesSnapshot) : Msg

    /** Сигнал что подписка вернула пустой emit / ошибка — `[guide: state-modeling.md § 9 Loadable]`. */
    data class TypesLoadFailed(val cause: Throwable) : Msg

    // ===== Create dialog =====
    data object OpenCreateDialog : Msg
    data object CloseCreateDialog : Msg
    data class CreateNameChange(val value: String) : Msg
    data class CreateTemplateChange(val template: ComponentTemplate) : Msg
    data class CreateMultiToggle(val isMulti: Boolean) : Msg
    data class CreateScopeChange(val scope: Scope) : Msg
    data object SubmitCreate : Msg
    /** Результат UseCase: created OK или typed error. */
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
    /** Результат предзагрузки impact preview перед confirm. */
    data class ImpactPreviewLoaded(val impact: DeletionImpact) : Msg
    data class ImpactPreviewFailed(val cause: Throwable) : Msg
    data object ConfirmDelete : Msg
    data class DeleteResult(val outcome: DeleteOutcome) : Msg

    // ===== Navigation =====
    data object RequestBack : Msg

    // ===== No-op =====
    data object Empty : Msg

    /** UI-feedback (snackbar / toast) — см. `[guide: messages.md § Внутренние сообщения]`. */
    sealed interface UiMsg : Msg {
        data class Snackbar(val text: String, val show: Boolean) : UiMsg
    }
}
```

### `PerDictionaryComponentsScreen`

Аналогичный sealed `Msg` с теми же `Open*Dialog` / `Submit*` / `*Result` событиями. Отличие — init: `data class ItemsLoaded(val snapshot: PerDictionarySnapshot) : Msg`. Подробно — на business_implement, набор reducer-веток зеркальный.

### Reducer-реакции (выборочно, для проверки связности State ↔ Msg ↔ Effect)

| Msg | Изменение state | Effect |
|---|---|---|
| `OpenCreateDialog` | `createDialog = CreateDialogState()` | `∅` |
| `CreateNameChange(v)` | `createDialog?.copy(name=v, nameError=null)` | `∅` |
| `SubmitCreate` | `isCreating=true` | `DatasourceEffect.CreateComponent(dialog.snapshot)` |
| `CreateResult(Success(types))` | `isCreating=false, createDialog=null` | `UiEffect.Snackbar("Created ${types.size}")` |
| `CreateResult(SameScopeCollision)` | `isCreating=false, createDialog.copy(nameError=NameError.SameScopeCollision)` | `∅` |
| `CreateResult(CrossScopeCollision)` | `isCreating=false, createDialog.copy(nameError=NameError.CrossScopeCollision)` | `∅` |
| `OpenDeleteConfirm(id)` | `deleteConfirm = DeleteConfirmState(id, name, isLoadingImpact=true)` | `DatasourceEffect.LoadImpact(id)` |
| `ImpactPreviewLoaded(i)` | `deleteConfirm?.copy(impact=i, isLoadingImpact=false)` | `∅` |
| `ConfirmDelete` | `isDeleting=true` | `DatasourceEffect.SoftDeleteComponent(id)` |
| `DeleteResult(Success)` | `isDeleting=false, deleteConfirm=null` | `UiEffect.Snackbar("Deleted")` |
| `RequestBack` | без изменений | `NavigationEffect.Back` `[guide: messages.md § Навигационные сообщения]` |
| `TypesLoaded(snapshot)` | `userDefinedTypes = snapshot.toRows(), isLoading=false` | `∅` |

`[transition]` инвариант: после `Msg.ConfirmDelete` reducer обязан выставить `isDeleting=true` и dispatch `SoftDeleteComponent` ровно один раз — повторный `ConfirmDelete` пока `isDeleting=true` игнорируется (защита от двойного тапа, `concept/template_model.md` § Защита cardinality на INSERT).

---

## Effect/IO

Три категории `[guide: mate-framework.md § Конвенции]`:

### `ComponentsManagerDatasourceEffect`

```kotlin
package me.apomazkin.components_manager.logic

import me.apomazkin.lexeme.ComponentTemplate
import me.apomazkin.lexeme.ComponentTypeId
import me.apomazkin.lexeme.Scope

sealed interface ComponentsManagerDatasourceEffect : Effect {

    /** Initial subscribe — `FlowHandler` подписывается на UseCase Flow. */
    data object SubscribeAll : ComponentsManagerDatasourceEffect

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

### `ComponentsManagerUiEffect`

```kotlin
sealed interface ComponentsManagerUiEffect : Effect {
    data class Snackbar(val text: String) : ComponentsManagerUiEffect
}
```

### `ComponentsManagerNavigationEffect`

```kotlin
sealed interface ComponentsManagerNavigationEffect : NavigationEffect {
    // Только Back из базы; добавочных навигационных переходов нет
    // (drill-in в per-dict вид не предусмотрен — см. ui_placement.md § Cross-flow).
}
```

### `MateFlowHandler`-ы

- `AllUserDefinedTypesFlowHandler` — подписывается на `useCase.flowAllUserDefinedTypes()` (`Flow<UserDefinedTypesSnapshot>`) → emit `Msg.TypesLoaded` / `Msg.TypesLoadFailed`. Триггерится `ComponentsManagerDatasourceEffect.SubscribeAll` либо инициируется в `init` Mate.

### `DatasourceEffectHandler` — связь Effect → UseCase

| Effect | UseCase call | Msg back |
|---|---|---|
| `CreateComponent` | `useCase.createUserDefinedComponent(name, template, isMulti, scope)` → `CreateOutcome` | `Msg.CreateResult(outcome)` |
| `RenameComponent` | `useCase.renameComponent(typeId, newName)` → `RenameOutcome` | `Msg.RenameResult(outcome)` |
| `LoadImpact` | `useCase.previewDeletionImpact(typeId)` → `DeletionImpact?` | `Msg.ImpactPreviewLoaded(it)` или `Msg.ImpactPreviewFailed(e)` |
| `SoftDeleteComponent` | `useCase.softDeleteComponent(typeId)` → `DeleteOutcome` | `Msg.DeleteResult(outcome)` |

### `PerDictionaryComponentsScreen` — аналогично

Те же три категории, отличие в Datasource Effect:
- `SubscribeForDictionary(dictionaryId)` — подписка `useCase.flowComponentsForDictionary(dictId)`.
- `CreateComponent(...)`, `RenameComponent(...)`, `LoadImpact(...)`, `SoftDeleteComponent(...)` — те же сигнатуры; `dictionaryId` приходит из `state.dictionaryId` при `CreateComponent` (preselect scope).

---

## UseCase

### Domain типы (новые / изменённые в `modules/domain/lexeme/`)

Все типы ниже живут в `:modules:domain:lexeme` (package `me.apomazkin.lexeme`).

#### `Primitive` (новый sealed)

```kotlin
sealed interface Primitive {
    data class Text(val value: String) : Primitive
    data class Image(val uri: String) : Primitive
    data class Color(val hex: String) : Primitive
}
```

Обоснование: `concept/template_model.md` § Domain types. Только три варианта, расширяется при появлении новых примитивов.

#### `Field` + `PrimitiveType` (новые)

```kotlin
data class Field(
    val name: String,
    val type: PrimitiveType,
)

enum class PrimitiveType { TEXT, IMAGE, COLOR }
```

#### `ComponentTemplate` (изменён в M13)

```kotlin
enum class ComponentTemplate(val key: String) {
    TEXT("text"),
    IMAGE("image"),
    // composite — добавляются в будущих фичах
    ;

    val fields: List<Field> get() = when (this) {
        TEXT -> listOf(Field("value", PrimitiveType.TEXT))
        IMAGE -> listOf(Field("value", PrimitiveType.IMAGE))
    }

    companion object {
        /**
         * Fail-soft парсинг: unknown key → null + caller логирует в Crashlytics.
         * `[guide: logging.md]`.
         */
        fun fromKey(key: String): ComponentTemplate? = entries.firstOrNull { it.key == key }
    }
}
```

Изменение по сравнению с M12: drop `LONG_TEXT`, `fromKey` стал nullable, добавлен `fields` (schema живёт в коде, см. `concept/template_model.md` § ComponentTemplate).

#### `TemplateValues` (новый sealed, замена `ComponentValueData`)

```kotlin
sealed interface TemplateValues

data class TextValues(
    val value: Primitive.Text,
) : TemplateValues

// IMAGE и composite — отдельные data class в этих же модулях:
data class ImageValues(
    val value: Primitive.Image,
) : TemplateValues
```

Обоснование per-template variants: `concept/typed_views.md` § Domain. Compile-time exhaustive check на `when`. Map-based promise (`Map<String, Primitive>`) отвергнут на F-N2 review (string-key + runtime `as?` ломают type safety).

MVP — только `TextValues`; `ImageValues` показан как пример как масштабируется. composite (`QuoteWithSourceValues`, `ImageWithCaptionValues`) — будущие фичи.

#### `ComponentType` (изменён в M13)

```kotlin
data class ComponentType(
    val id: ComponentTypeId,
    val systemKey: BuiltInComponent?,
    val dictionaryId: Long?,
    val name: String?,
    val template: ComponentTemplate,
    val position: Int,
    val isMulti: Boolean = false,                     // NEW (M13)
    val createdAt: Date,                              // NEW (M13)
    val updatedAt: Date,                              // NEW (M13)
    val removedAt: Date? = null,                      // RENAME removeDate → removedAt (M13)
)
```

#### `ComponentValueData` (удалён в M13)

Полностью убран; все callsite (wordcard, quiz/chat, app mapper, тесты) переходят на `TemplateValues`. Breaking change ловится compile-time gate через exhaustive `when` `[guide: state-modeling.md § 4 ADT]`.

#### `DeletionImpact` (новый data class — domain)

```kotlin
/**
 * Preview каскадного soft-delete user-defined component_type.
 * Возвращается из `previewDeletionImpact()` и `softDeleteComponent()` (для UI snackbar).
 *
 * `affectedQuizConfigs` — список (dictId, quizMode) у которых в `component_refs`
 *  есть ref на удаляемый тип; будет вычищен в одной транзакции с soft-delete.
 *
 * `affectedPrefs` — список dictId у которых `quiz_picker_dict_<id>` pref
 *  ссылается на удаляемый ref; будет сброшен after soft-delete.
 *
 * Источник: scope_analysis.md aspect `quiz_configs_cleanup` + `prefs_cleanup_on_soft_delete`.
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

#### `ComponentUsage` (новый data class — domain)

Aggregated usage data, который вместе с `List<ComponentType>` собирается репозиторием в одном snapshot. Нужен в aggregated view для отрисовки `usageCount` + `dictionaryNames` per row без N+1 запросов.

```kotlin
data class ComponentUsage(
    /** typeId → суммарно активных component_values. */
    val valueCountByType: Map<ComponentTypeId, Int>,
    /** typeId → set of dictionary ids в которых компонент применим (для global — все словари). */
    val dictionaryIdsByType: Map<ComponentTypeId, Set<Long>>,
    /** dictionaryId → name. Для отображения badge. */
    val dictionaryNames: Map<Long, String>,
)
```

#### `UserDefinedTypesSnapshot` (новый data class — domain)

Aggregated snapshot который возвращает `flowAllUserDefinedTypes()`. Dedicated data class устраняет неоднозначность `.first/.second` в reducer (Open Q #3, F1 iter1 review).

```kotlin
data class UserDefinedTypesSnapshot(
    val types: List<ComponentType>,
    val usage: ComponentUsage,
)
```

#### Sealed result типы (UseCase outcomes)

Используется sealed-обёртка вместо `T?` там где UseCase возвращает типизированный негативный исход (UI диалог реагирует на тип, не на null) `[guide: state-modeling.md § 6 Sum types]`.

```kotlin
sealed interface CreateOutcome {
    /**
     * Создано N rows. Для `Scope.Global` или `Scope.PerDictionaries(listOf(x))` — list length = 1;
     * для `Scope.PerDictionaries(listOf(d1, d2, ...))` — list length = N (по одному row per dictionary).
     * Reducer получает все созданные entities для optimistic refresh / undo (F2 iter1 review).
     */
    data class Success(val created: List<ComponentType>) : CreateOutcome
    data object NameEmpty : CreateOutcome
    /** Same-scope коллизия (active row с тем же именем уже есть). */
    data object SameScopeCollision : CreateOutcome
    /** Cross-scope коллизия (global ⊥ per-dict invariant, F039). */
    data object CrossScopeCollision : CreateOutcome
    /** Любая другая (IO / SQL / etc) — UI показывает generic error snackbar. */
    data class Failure(val cause: Throwable) : CreateOutcome
}

sealed interface RenameOutcome {
    data class Success(val type: ComponentType) : RenameOutcome
    data object NameEmpty : RenameOutcome
    data object SameScopeCollision : RenameOutcome
    data object CrossScopeCollision : RenameOutcome
    /** Попытка переименовать built-in — запрещено. */
    data object BuiltInProtected : RenameOutcome
    data class Failure(val cause: Throwable) : RenameOutcome
}

sealed interface DeleteOutcome {
    data class Success(val impact: DeletionImpact) : DeleteOutcome    // для snackbar «N values hidden»
    data object BuiltInProtected : DeleteOutcome
    data class Failure(val cause: Throwable) : DeleteOutcome
}
```

### Interface `ComponentsManagerUseCase`

```kotlin
package me.apomazkin.components_manager.deps

import kotlinx.coroutines.flow.Flow
import me.apomazkin.lexeme.ComponentTemplate
import me.apomazkin.lexeme.ComponentType
import me.apomazkin.lexeme.ComponentTypeId
import me.apomazkin.lexeme.ComponentUsage
import me.apomazkin.lexeme.CreateOutcome
import me.apomazkin.lexeme.DeleteOutcome
import me.apomazkin.lexeme.DeletionImpact
import me.apomazkin.lexeme.RenameOutcome
import me.apomazkin.lexeme.Scope
import me.apomazkin.lexeme.UserDefinedTypesSnapshot

interface ComponentsManagerUseCase {

    /**
     * Реактивная подписка на все user-defined component_types (built-in исключены)
     * + aggregated usage по всем словарям. Один snapshot для aggregated view —
     * без N+1 запросов из reducer.
     *
     * Источник: ui_placement.md § Общий view (built-in не показываем).
     *
     * @return [UserDefinedTypesSnapshot] — типы + aggregated usage в одном dedicated snapshot
     *  (устраняет `.first/.second` в reducer, F1 iter1 review).
     */
    fun flowAllUserDefinedTypes(): Flow<UserDefinedTypesSnapshot>

    /**
     * Создать user-defined component_type. Внутри:
     *  1) валидация имени (non-blank);
     *  2) two-prong SELECT (aspect `userdefined_identity_invariant`):
     *     - same-scope active row с тем же именем (per-dict либо global);
     *     - cross-scope (global ⊥ per-dict) inverse check;
     *  3) INSERT row(s) — для `Scope.PerDictionaries(N)` создаётся N rows.
     *
     * @return [CreateOutcome.Success] с list созданных entities (length = N для multi-scope) OR typed error.
     */
    suspend fun createUserDefinedComponent(
        name: String,
        template: ComponentTemplate,
        isMulti: Boolean,
        scope: Scope,
    ): CreateOutcome

    /**
     * Переименовать user-defined component_type. Cascade UPDATE на
     * `quiz_configs.component_refs` в одной транзакции (aspect `quiz_configs_cleanup`(2)).
     * Built-in protected на SQL-уровне (`WHERE system_key IS NULL`).
     */
    suspend fun renameComponent(
        typeId: ComponentTypeId,
        newName: String,
    ): RenameOutcome

    /**
     * Preview каскада soft-delete: count values + words + quiz_configs + prefs.
     * Read-only; не изменяет БД. UI вызывает перед confirm-dialog'ом.
     */
    suspend fun previewDeletionImpact(typeId: ComponentTypeId): DeletionImpact?

    /**
     * Soft-delete user-defined component_type + atomic cleanup:
     *  1) UPDATE component_types SET removed_at = now() WHERE id = ? AND system_key IS NULL;
     *  2) Cleanup `quiz_configs.component_refs` — убрать ref на этот type;
     *  3) Cleanup `quiz_picker_dict_<id>` prefs которые ссылаются на этот ref.
     *
     * `component_values` НЕ trogаются — фильтр по `parent.removed_at IS NULL`
     * через JOIN (aspect `dao_convention`). Recovery вне scope (`deletion_concept.md`).
     */
    suspend fun softDeleteComponent(typeId: ComponentTypeId): DeleteOutcome
}
```

### Interface `PerDictionaryComponentsUseCase`

```kotlin
package me.apomazkin.per_dictionary_components.deps

import kotlinx.coroutines.flow.Flow
import me.apomazkin.lexeme.ComponentTemplate
import me.apomazkin.lexeme.ComponentType
import me.apomazkin.lexeme.ComponentTypeId
import me.apomazkin.lexeme.CreateOutcome
import me.apomazkin.lexeme.DeleteOutcome
import me.apomazkin.lexeme.DeletionImpact
import me.apomazkin.lexeme.PerDictionarySnapshot
import me.apomazkin.lexeme.RenameOutcome
import me.apomazkin.lexeme.Scope

interface PerDictionaryComponentsUseCase {

    /**
     * Подписка на active user-defined component_types применимых к этому словарю
     * (global `dictionaryId IS NULL` + per-dict `dictionaryId = :dictId`).
     * Built-in исключены (ui_placement.md § Per-dictionary view).
     *
     * Также — `valueCountByType` в рамках этого словаря.
     *
     * @return [PerDictionarySnapshot] — типы + valueCountByType + dictionaryName в одном dedicated snapshot (F4 iter1 review).
     */
    fun flowComponentsForDictionary(
        dictionaryId: Long,
    ): Flow<PerDictionarySnapshot>

    /**
     * Те же CRUD методы что в `ComponentsManagerUseCase`. Делегируются на
     * общий impl (или extends), domain-семантика та же.
     */
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

data class PerDictionarySnapshot(
    val dictionaryId: Long,
    val dictionaryName: String,
    val types: List<ComponentType>,
    val valueCountByType: Map<ComponentTypeId, Int>,
)
```

Обоснование: per-dict UseCase это подмножество с другой read-семантикой. Write-методы — те же signatures (один impl класс реализует оба интерфейса либо PerDictUseCaseImpl делегирует на ComponentsManagerUseCaseImpl — финальное решение на business_design_tree).

---

## Контракт с data слоем (CoreDbApi)

Решение F004/F030 (best-guess из scope_analysis.md): **расширить существующий `CoreDbApi.LexemeApi`**, не создавать отдельный `ComponentApi`. Component-методы уже живут там; набор < 10 новых методов укладывается в existing interface. Возможный split на отдельный `ComponentApi` — отдельная backlog-задача после фичи (если набор разрастётся).

### Новые методы `CoreDbApi.LexemeApi`

```kotlin
interface LexemeApi {
    // ===== Existing M12 component methods (signatures меняются под TemplateValues) =====

    // BREAKING CHANGES: 5 методов меняют сигнатуру `ComponentValueData` → `TemplateValues`
    // (F029/F036, см. scope_analysis.md aspect `public_contract_change`):
    //   - addLexemeWithBuiltInComponent(..., data: TemplateValues)
    //   - addLexemeWithUserDefinedComponent(..., data: TemplateValues)
    //   - addLexemeWithComponents(..., data: List<ComponentValueData>) → List<TemplateValues>
    //   - addComponentValue(..., data: TemplateValues)
    //   - updateComponentValue(..., data: TemplateValues)
    // Точные новые сигнатуры — на data_design_tree (data sub-flow).

    // ===== NEW M13 methods для конструктора =====

    /**
     * Реактивная подписка на все user-defined active component_types
     * (`system_key IS NULL AND removed_at IS NULL`) с aggregated usage.
     * Aggregated view: типы + valueCountByType + dictionaryNames.
     */
    fun flowAllUserDefinedTypesWithUsage(): Flow<UserDefinedTypesSnapshot>

    /**
     * Реактивная подписка на active user-defined types применимые к словарю
     * (`(dictionary_id = :dictId OR dictionary_id IS NULL) AND system_key IS NULL AND removed_at IS NULL`)
     * + valueCount внутри словаря.
     */
    fun flowUserDefinedTypesForDictionary(dictionaryId: Long): Flow<DictionaryTypesSnapshot>

    /**
     * Atomic create user-defined component_type:
     *  1) Two-prong SELECT для name collision check (aspect `userdefined_identity_invariant`);
     *  2) INSERT row(s) (для multi-dict scope — N rows в одной транзакции);
     *  3) Возврат typed outcome со ВСЕМИ созданными rows (F2 iter1 review).
     */
    suspend fun createUserDefinedComponent(
        name: String,
        template: ComponentTemplate,
        isMulti: Boolean,
        scope: Scope,
    ): CreateComponentOutcome           // data-layer-shape, mappable в domain `CreateOutcome`

    /**
     * Atomic rename + cascade в `quiz_configs.component_refs`. Built-in защищён
     * через `WHERE system_key IS NULL`. Возврат typed outcome.
     */
    suspend fun renameComponentType(
        typeId: Long,
        newName: String,
    ): RenameComponentOutcome

    /**
     * Read-only preview: valueCount + dictionariesWithValues + affectedQuizConfigs +
     * affectedPrefs. Реализация — JOIN component_values ⋈ lexemes ⋈ words ⋈ dictionaries +
     * scan `quiz_configs.component_refs` JSON + iterate prefs через DictionaryApi.
     */
    suspend fun previewDeletionImpact(typeId: Long): DeletionImpact?

    /**
     * Atomic soft-delete:
     *  1) UPDATE component_types SET removed_at = :now WHERE id = :id AND system_key IS NULL;
     *  2) Cascade UPDATE на `quiz_configs.component_refs` (json_remove либо собрать новый JSON);
     *  3) Возврат `DeletionImpact` для snackbar.
     * Affected prefs (`quiz_picker_dict_<id>`) — сбрасываются ВНЕ транзакции
     * на UseCase-уровне (prefs живут в другом storage — DataStore, не Room).
     */
    suspend fun softDeleteComponentType(typeId: Long): SoftDeleteComponentOutcome
}
```

### Data-layer shape для outcome

```kotlin
sealed interface CreateComponentOutcome {
    /**
     * Список созданных API-entities. Для `Scope.Global` или одного per-dict — length = 1,
     * для `Scope.PerDictionaries(N)` — length = N. Маппится 1:1 в domain `CreateOutcome.Success(created)`.
     */
    data class Success(val types: List<ComponentTypeApiEntity>) : CreateComponentOutcome
    data object SameScopeCollision : CreateComponentOutcome
    data object CrossScopeCollision : CreateComponentOutcome
}

sealed interface RenameComponentOutcome {
    data class Success(val type: ComponentTypeApiEntity) : RenameComponentOutcome
    data object SameScopeCollision : RenameComponentOutcome
    data object CrossScopeCollision : RenameComponentOutcome
    data object BuiltInProtected : RenameComponentOutcome
}

sealed interface SoftDeleteComponentOutcome {
    data class Success(val impact: DeletionImpact) : SoftDeleteComponentOutcome
    data object BuiltInProtected : SoftDeleteComponentOutcome
}
```

### Data DTO (api-entity changes)

- `ComponentTypeApiEntity` — добавить `isMulti: Boolean`, `createdAt: Date`, `updatedAt: Date`; rename `removeDate → removedAt`. `template` — решение по nullable (F019) — best-guess `toApiEntity()` возвращает `ComponentTypeApiEntity?`, skip row при unknown template + Crashlytics-log (см. scope_analysis.md Open question F019).
- `ComponentValueApiEntity` — rebind `data: ComponentValueData` → `data: TemplateValues`; добавить `createdAt/updatedAt: Date`, `removedAt: Date?` (M13 new columns).
- DAO convention: все active queries обязаны иметь `WHERE removed_at IS NULL` либо JOIN на parent (aspect `dao_convention`).

### Зависимость от `PrefsProvider` + `DictionaryApi`

`softDeleteComponentType` impl на UseCase-уровне (best-guess B из scope_analysis F049): после успешного `softDeleteComponentType` (Room транзакция) UseCase iterate через `dictionaryApi.getDictionaryList()`, для каждого dict собирает ключ через `quizPickerPrefKey(dictId)`, читает `prefsProvider.getStringByRawKey(key)`, проверяет match на удаляемый ref (`builtin:<key>` либо `user:<name>`), при совпадении — `setStringByRawKey(key, null)`.

Альтернатива (option A) — добавить `findKeysWithPrefix(prefix)` API в `PrefsProvider` — отложена в backlog (out-of-scope для IS481).

---

## Открытые вопросы

1. **Generic DialogState переиспользуется между двумя screen-модулями или дублируется?** Best-guess: дублируется (минимизация cross-module зависимостей). Финальное решение — на business_design_tree.
2. **`PerDictionaryComponentsUseCase` extends `ComponentsManagerUseCase` или отдельный интерфейс с делегированием?** Best-guess: отдельный интерфейс с делегированием в общий impl (две interface signatures сейчас зависят от двух разных logic-пакетов; cross-module наследование нежелательно). Финальное решение — на business_design_tree.
3. **`flow*` методы возвращают `Pair<...>` / dedicated data class?** Контракт использует dedicated data class (`UserDefinedTypesSnapshot`, `PerDictionarySnapshot`, `ComponentUsage`) — устраняет неоднозначность `.first/.second` в reducer. **Закрыт на iter 2** (F1 fix).
4. **Reducer-эскиз vs полная reducer-логика** — детальная reducer-логика (per-Msg branches) живёт в business_implement; контракт фиксирует только State/Msg/Effect инварианты.
5. **`AffectedQuizConfig.quizMode` enum source** — **Закрыт на iter 3 sync (F078):** используется `String` (data layer convention, БД хранит string). Дискретный enum может появиться в будущем.
6. **`CrossScopeCollision` vs `SameScopeCollision` UI текст** — конкретные strings finalize на UI sub-flow; контракт фиксирует только типы.
7. **`CreateOutcome.NameTaken(scope)` vs дискретные `SameScopeCollision` / `CrossScopeCollision`** — **Закрыт на iter 3 sync (F080):** обобщающий `NameTaken(scope)` удалён как dead branch; reducer использует только distinct `SameScopeCollision` / `CrossScopeCollision`.

---

## История ревью

### iter 1 (2026-06-16) — заархивирован в business_contract_iter1.md

Verdict: changes_requested. 4 findings:
- F1 (блокирующий): flowAllUserDefinedTypes → UserDefinedTypesSnapshot (dedicated data class)
- F2 (блокирующий): CreateOutcome.Success → List<ComponentType>
- F3 (минор-блок): добавлены NameTaken(scope) + NameError.ScopeCollision
- F4 (минор): stale KDoc для flowComponentsForDictionary обновлён

### iter 2 (2026-06-16): 4 findings fixed → contract готов к contract_spec

### iter 3 sync (2026-06-16): contract synced со spec по findings F078-F081

- F078: QuizMode → String
- F079: shared types в me.apomazkin.lexeme package
- F080: CreateOutcome.NameTaken(scope) removed
- F081: NameError.ScopeCollision removed
