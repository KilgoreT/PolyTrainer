<!-- META: spec_filename: null -->

# Quiz Component Picker

## Бизнес-описание

В меню чат-экрана квиза добавляется пункт «Компонент квиза» — radio-выбор одного из `ComponentType` текущего словаря (built-in либо user-defined). Квиз тренирует только выбранный компонент: `QuizGameImpl.fetchData` перед `toQuizItem(...)` сужает `quizConfig.componentRefs` до single-element list `[selectedRef]`.

Picker обобщённый: работает на любой набор `ComponentType` из `LexemeApi.getComponentTypes(dictionaryId)`, без хардкода имён или ключей. Новые built-in и user-defined типы появляются в подменю автоматически по факту наличия в словаре. Выбор persistent per-dictionary — сохраняется между визитами в чат, восстанавливается при возврате в тот же словарь. Если сохранённый `ComponentTypeRef` больше не доступен в `availableTypes` (тип удалён) — fallback на default = первый по `position`.

## User Stories

- Как пользователь, я хочу выбрать один компонент квиза из доступных в словаре, чтобы тренировать конкретно его (например, только переводы либо только определения).
- Как пользователь, я хочу чтобы мой выбор сохранялся per-dictionary и восстанавливался при возврате в словарь, чтобы не выбирать заново каждый раз.
- Как пользователь, я хочу видеть в подменю все компоненты текущего словаря (built-in + user-defined), чтобы переключаться между ними без захода в configurator.
- Как пользователь словаря с единственным компонентом, я хочу видеть пункт checked + disabled, чтобы понять что альтернативы нет.

## State

```kotlin
@Immutable
data class ItemsState(
    val earliest: Earliest = Earliest(),
    val frequentMistakes: FrequentMistakes = FrequentMistakes(),
    val debug: Debug = Debug(),
    val quizComponent: QuizComponent = QuizComponent(),
) {
    data class Earliest(val isOn: Boolean = false)
    data class FrequentMistakes(val isOn: Boolean = false)
    data class Debug(val isOn: Boolean = false)

    data class QuizComponent(
        val availableTypes: List<ComponentType> = emptyList(),
        val selectedRef: ComponentTypeRef? = null,
    )
}
```

| Поле | Тип | Семантика |
|---|---|---|
| `quizComponent.availableTypes` | `List<ComponentType>` | Компоненты текущего словаря, отсортированы по `position`. Пустой список — данные ещё не загружены либо у словаря нет компонентов (degenerate). UI в этом случае скрывает picker. |
| `quizComponent.selectedRef` | `ComponentTypeRef?` | Текущий radio-выбор. `null` валиден только в transient-окне до `QuizComponentTypesLoaded`. После load всегда non-null если `availableTypes` непустой. |

**Computed helper** (не stored, derives из state):

```kotlin
val ItemsState.QuizComponent.isPickerEnabled: Boolean
    get() = availableTypes.size > 1
```

UI использует `isPickerEnabled` для disabled-state при единственном типе (пункт checked + disabled).

**Инварианты:**

1. `availableTypes` загружается one-shot на entry в chat — не реактивный (словарь не меняется в рамках сессии).
2. После `QuizComponentTypesLoaded` всегда `selectedRef ∈ availableTypes.map { it.toRef() }` либо `availableTypes.isEmpty()`. Restored ref не in list → reducer fallback на первый.
3. `selectedRef = null` валиден только в transient-окне между entry и `QuizComponentTypesLoaded`; UI в этом окне скрывает picker.
4. Single value (`selectedRef: ComponentTypeRef?`), не `Map<Ref, Boolean>` — radio выражает «ровно один» типом.

## UI Messages

```kotlin
sealed interface Msg {
    // existing menu Msg unchanged: EarliestOn/Off, FrequentMistakesOn/Off,
    // DebugOn/Off, UpdateMenu, PrepareToStart, и т.д.

    /** Click на radio-пункт. State напрямую не меняется — обновление через flow → QuizComponentTypesLoaded. */
    data class SelectQuizComponent(val ref: ComponentTypeRef) : Msg

    /**
     * Bulk-load: availableTypes из БД + restored selectedRef из prefs.
     * Emit из LoadQuizComponentTypes effect (initial) и из QuizPickerFlowHandler (on persist).
     */
    data class QuizComponentTypesLoaded(
        val types: List<ComponentType>,
        val restoredSelectedRef: ComponentTypeRef?,
    ) : Msg
}
```

| Msg | Trigger | State change | Effects | Guard |
|---|---|---|---|---|
| `SelectQuizComponent(ref)` | Click на radio-пункт в подменю picker'а. | Нет (state приходит через flow). | `setOf(DatasourceEffect.SaveQuizPickerSelection(ref))` | — |
| `QuizComponentTypesLoaded(types, restored)` | Emit из `LoadQuizComponentTypes` effect (initial) либо из `QuizPickerFlowHandler` (on prefs write). | `itemsState.quizComponent = QuizComponent(types, resolveSelection(types, restored))` | `setOf()` | — |

Reducer helper:

```kotlin
private fun resolveSelection(types: List<ComponentType>, restored: ComponentTypeRef?): ComponentTypeRef? {
    if (types.isEmpty()) return null
    val available = types.mapNotNull { it.toRef() }.toSet()
    if (restored != null && restored in available) return restored
    return available.firstOrNull()
}
```

Pattern decision: `SelectQuizComponent` идёт по pattern `Msg.EarliestOn/Off` — click не меняет state directly, write to prefs → flow → state. Single source через flow handler.

## IO

### Effects

```kotlin
sealed interface DatasourceEffect : Effect {
    // existing: EarliestOn/Off, FrequentMistakesOn/Off, DebugOn/Off, …

    /** One-shot fetch на entry. Загружает availableTypes + restored selectedRef → Msg.QuizComponentTypesLoaded. */
    data object LoadQuizComponentTypes : DatasourceEffect

    /** Persist write picker selection. Flow подхватит write и emit Msg.QuizComponentTypesLoaded для UI update. */
    data class SaveQuizPickerSelection(val ref: ComponentTypeRef) : DatasourceEffect
}
```

`dictionaryId` не входит в Msg/Effect — резолвится в `DatasourceEffectHandler` через `useCase.getCurrentDictionaryId()` (соответствует existing pattern `QuizGameImpl.fetchData`).

| Effect | IO в DatasourceEffectHandler |
|---|---|
| `LoadQuizComponentTypes` | `val dictId = useCase.getCurrentDictionaryId() ?: return Empty`; `val types = useCase.getAvailableTypes(dictId)`; `val restored = useCase.getQuizPickerSelection(dictId)`; emit `Msg.QuizComponentTypesLoaded(types, restored)`. |
| `SaveQuizPickerSelection(ref)` | `val dictId = useCase.getCurrentDictionaryId() ?: return Empty`; `useCase.setQuizPickerSelection(dictId, ref)`; emit `Msg.Empty`. |

Initial load — через reducer-branch `Msg.PrepareToStart` (extend): `state.stopLoading().systemMessage(...) to setOf(DatasourceEffect.LoadQuizComponentTypes)`.

### Subscribers

**QuizPickerFlowHandler** — новый flow handler, реактивный на dynamic-key prefs flow:

```kotlin
class QuizPickerFlowHandler @Inject constructor(
    private val useCase: QuizChatUseCase,
    private val prefsProvider: PrefsProvider,
) : MateFlowHandler<Msg, Effect> {
    // val dictId = useCase.getCurrentDictionaryId() ?: return
    // prefsProvider.getStringFlowByRawKey(quizPickerPrefKey(dictId))
    //   .collectLatest { _ -> emit Msg.QuizComponentTypesLoaded(
    //       useCase.getAvailableTypes(dictId),
    //       useCase.getQuizPickerSelection(dictId),
    //   ) }
}
```

Каждый write в pref → flow emit `QuizComponentTypesLoaded` → reducer обновляет `selectedRef`. Single update path: initial load и persist update идут через один Msg. `AppBarFlowHandler` (combine Boolean prefs) не модифицируется — picker'у нужен dynamic per-dictionary String-key flow.

### Quiz session integration

`QuizGameImpl.fetchData()` перед `toQuizItem(...)`:

```kotlin
val selectedRef = quizChatUseCase.getQuizPickerSelection(dictionaryId)
val effectiveRefs = selectedRef?.let { listOf(it) } ?: quizConfig.componentRefs
return list.mapNotNull { it.toQuizItem(componentRefs = effectiveRefs, ...) }
```

`selectedRef` — через UseCase (single source), не через `PrefsProvider` напрямую. При `null` fallback на `quizConfig.componentRefs`. Mid-session change не применяется — selection вступает в силу на следующем `loadData()`.

## UseCase

```kotlin
interface QuizChatUseCase {
    // existing methods unchanged

    /** Domain ComponentType словаря, сортировка по position. Empty list — не null. Прокси над CoreDbApi.LexemeApi.getComponentTypes. */
    suspend fun getAvailableTypes(dictionaryId: Long): List<ComponentType>

    /** Persistent picker ref словаря. null = не сохранён либо corrupted pref. Resolve default — caller (reducer / QuizGameImpl). */
    suspend fun getQuizPickerSelection(dictionaryId: Long): ComponentTypeRef?

    /** Persist picker ref словаря через PrefsProvider raw-string API. */
    suspend fun setQuizPickerSelection(dictionaryId: Long, ref: ComponentTypeRef)
}
```

`getAvailableTypes` — Effect `LoadQuizComponentTypes` + `QuizPickerFlowHandler` on re-emit. `getQuizPickerSelection` — Effect handler, FlowHandler, `QuizGameImpl.fetchData`. `setQuizPickerSelection` — Effect `SaveQuizPickerSelection`.

### ComponentTypeRef encoding

| Variant | Encoded String |
|---|---|
| `BuiltIn(BuiltInComponent.TRANSLATION)` | `builtin:translation` |
| `UserDefined("Definition")` | `user:Definition` |

Parse contract:
- `builtin:<key>` → `BuiltInComponent.fromKey(key)?.let { BuiltIn(it) }` либо `null` (unknown built-in key игнорируется — future-proof).
- `user:<name>` → `UserDefined(name)`. Имя восстанавливается через `substringAfter(':')` — корректно для names с `:`, unicode, любым `String`.
- Любой другой формат → `null`. Caller (reducer / quiz session) выполняет fallback на default.

Encoding/parsing — internal к UseCase impl. `ComponentTypeRef` в domain остаётся pure без serialization-аннотаций. Без JSON / kotlinx.serialization.

### PrefsProvider extensions

```kotlin
suspend fun PrefsProvider.getStringByRawKey(key: String): String?       // null если не установлен
fun PrefsProvider.getStringFlowByRawKey(key: String): Flow<String?>     // reactive для FlowHandler
suspend fun PrefsProvider.setStringByRawKey(key: String, value: String?) // value=null → remove
```

`PrefKey` enum не модифицируется — dynamic per-dictionary ключи живут вне enum (raw-string API). Helper: `internal fun quizPickerPrefKey(dictionaryId: Long): String = "quiz_picker_dict_$dictionaryId"`.

### Domain extension `ComponentType.toRef()`

```kotlin
// modules/domain/lexeme/.../ComponentTypeRef.kt либо рядом с ComponentType.kt
fun ComponentType.toRef(): ComponentTypeRef = when (val sk = systemKey) {
    null -> ComponentTypeRef.UserDefined(name ?: error("user-defined ComponentType без name"))
    else -> ComponentTypeRef.BuiltIn(sk)
}
```

Stable mapping `ComponentType` (DB-id + meta) → `ComponentTypeRef` (stable identity без id). Используется в:
- Invariant 2 (`selectedRef ∈ availableTypes.map { it.toRef() }`).
- Reducer helper `resolveSelection(types, restored)` — для membership check restored ref в available.

Extension живёт в `modules/domain/lexeme` (domain module) — pure-JVM, без Android-зависимостей.

## Тестовые сценарии

- **Initial load, нет pref.** Предусловие: словарь = `[translation, Definition]`, pref не установлен. Действие: enter chat. Ожидание: `selectedRef = BuiltIn(translation)` (default = первый по position).
- **Initial load, restored ref валиден.** Предусловие: pref `quiz_picker_dict_<id> = "user:Definition"`. Действие: enter chat. Ожидание: `selectedRef = UserDefined("Definition")`.
- **Initial load, restored ref недоступен.** Предусловие: pref = `user:Removed`, в `availableTypes` нет `Removed`. Действие: enter chat. Ожидание: fallback на первый из `availableTypes`.
- **Select changes pref + re-emit.** Действие: `Msg.SelectQuizComponent(UserDefined("Definition"))`. Ожидание: `SaveQuizPickerSelection` → write → `QuizPickerFlowHandler` emit `QuizComponentTypesLoaded` → `selectedRef = UserDefined("Definition")`.
- **Single-type degenerate.** Предусловие: `availableTypes.size == 1`. Ожидание: `isPickerEnabled == false`, UI рисует пункт checked + disabled.
- **Quiz session uses selectedRef.** Предусловие: `selectedRef = UserDefined("Definition")`. Ожидание: `toQuizItem` вызывается с `componentRefs = listOf(UserDefined("Definition"))`; лексемы без Definition пропускаются.
- **Quiz session pre-first-selection fallback.** Предусловие: `getQuizPickerSelection` возвращает null. Ожидание: fallback на `quizConfig.componentRefs` (preserves текущая семантика).

_model: claude-opus-4-7[1m]_

## log_messages

- Spec черновик собран из `business_contract.md` + `02_scope.md` + `business_walkthrough.md`; разделы Бизнес-описание / User Stories выведены из scope и task.
- `spec_filename: null` — файл остаётся только в feature dir, на `business_publish_spec` не публикуется (новая фича без записи в `docs/features-spec/`).
- Структура спеки полная: State / UI Messages / IO (Effects + Subscribers) / UseCase + опциональные тестовые сценарии.
