# Business contract: IS481 Quiz Component Picker

Picker компонента квиза в chat-меню. Persistent per-dictionary radio-выбор `ComponentTypeRef` → переопределяет фильтр в `QuizGameImpl.fetchData` перед `toQuizItem(...)`. Existing domain types (`ComponentTypeRef`, `ComponentType` из `modules/domain/lexeme`); новых enum/sealed не вводится.

## State

Nested `quizComponent: QuizComponent` поле в `ItemsState` (pattern walkthrough §1).

```kotlin
@Immutable
data class ItemsState(
    val earliest: Earliest = Earliest(),
    val frequentMistakes: FrequentMistakes = FrequentMistakes(),
    val debug: Debug = Debug(),
    val quizComponent: QuizComponent = QuizComponent(),  // NEW
) {
    // existing nested: Earliest, FrequentMistakes, Debug — без изменений

    data class QuizComponent(
        val availableTypes: List<ComponentType> = emptyList(),
        val selectedRef: ComponentTypeRef? = null,
    )
}
```

| Поле | Тип | Семантика |
|---|---|---|
| `availableTypes` | `List<ComponentType>` | Компоненты словаря из `getComponentTypes(dictionaryId)`, отсортированы по `position`. Пустой → не загружено / у словаря нет компонентов (degenerate, UI скрывает item). |
| `selectedRef` | `ComponentTypeRef?` | Radio-выбор. `null` до load. После `QuizComponentTypesLoaded` — restored из prefs либо default = первый из availableTypes. |

**Инварианты:**

1. `availableTypes` загружается one-shot на entry в chat — не реактивный (словарь не меняется в рамках сессии, см. scope §4).
2. После `QuizComponentTypesLoaded` всегда `selectedRef ∈ availableTypes.map { it.toRef() }` либо `availableTypes.isEmpty()`. Reducer обеспечивает invariant — restored ref не in list → fallback на первый.
3. `selectedRef = null` валиден только в transient-окне между entry и `QuizComponentTypesLoaded`. UI в этом окне скрывает picker.
4. **Single value, не Map<Ref, Boolean>** — radio выражает invariant «ровно один» типом. Multi-select → backlog рефактор `selectedRef → Map` с reducer-инвариантом.

**Computed helper** (не stored, derives из state):

```kotlin
val ItemsState.QuizComponent.isPickerEnabled: Boolean
    get() = availableTypes.size > 1
```

UI использует для disabled state при `availableTypes.size == 1` (checklist case).

## Msg

Два новых Msg в существующий `sealed interface Msg`:

```kotlin
sealed interface Msg {
    // ... existing

    /** Click на radio-пункт. Triggers persist; state — через flow → QuizComponentTypesLoaded. */
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

**Reducer reactions** (extend `ChatReducer.reduce`):

| Msg | State change | Effects |
|---|---|---|
| `SelectQuizComponent(ref)` | **no state change** (state приходит через flow — pattern walkthrough §3) | `setOf(DatasourceEffect.SaveQuizPickerSelection(message.ref))` |
| `QuizComponentTypesLoaded(types, restored)` | `itemsState.quizComponent = QuizComponent(types, resolveSelection(types, restored))` | `setOf()` |

```kotlin
private fun resolveSelection(types: List<ComponentType>, restored: ComponentTypeRef?): ComponentTypeRef? {
    if (types.isEmpty()) return null
    val available = types.mapNotNull { it.toRef() }.toSet()
    if (restored != null && restored in available) return restored
    return available.firstOrNull()  // default = первый по position
}
```

`ComponentType.toRef()` — маппер `systemKey`/`name → ComponentTypeRef`. Owner маппера — sub-flow `business_design_tree` (likely `modules/domain/lexeme`, рядом с типами).

**dictionaryId для SaveQuizPickerSelection** — резолвится в `DatasourceEffectHandler` (через `UseCase.getCurrentDictionaryId()`), **не** в reducer. Reducer не делает IO.

**Pattern decision:** `SelectQuizComponent` идёт по pattern'у `Msg.EarliestOn` — click не меняет state directly, write to prefs → flow → state. Single source через flow.

## Effect/IO

### DatasourceEffect (extend existing sealed interface)

```kotlin
sealed interface DatasourceEffect : Effect {
    // ... existing

    /** One-shot fetch на entry. Загружает availableTypes + restored selectedRef → QuizComponentTypesLoaded. */
    data object LoadQuizComponentTypes : DatasourceEffect

    /** Persist write; flow подхватит write и emit QuizComponentTypesLoaded для UI update. */
    data class SaveQuizPickerSelection(val ref: ComponentTypeRef) : DatasourceEffect
}
```

**dictionaryId не в Msg/Effect** — `DatasourceEffectHandler` извлекает через `useCase.getCurrentDictionaryId()`. Это соответствует existing pattern (см. `QuizGameImpl.fetchData` line 177 — current dict резолвится in-place, не передаётся в effect).

### DatasourceEffectHandler реакции

| Effect | IO |
|---|---|
| `LoadQuizComponentTypes` | `val dictId = useCase.getCurrentDictionaryId() ?: return Empty; val types = useCase.getAvailableTypes(dictId); val restored = useCase.getQuizPickerSelection(dictId); Msg.QuizComponentTypesLoaded(types, restored)` |
| `SaveQuizPickerSelection(ref)` | `val dictId = useCase.getCurrentDictionaryId() ?: return Empty; useCase.setQuizPickerSelection(dictId, ref); Msg.Empty` |

### FlowHandler — новый QuizPickerFlowHandler

Существующий `AppBarFlowHandler` combine трёх Boolean-prefs. Picker'у нужен dynamic-key String flow (`quiz_picker_dict_<id>`) — несимметрично. Отдельный handler:

```kotlin
class QuizPickerFlowHandler @Inject constructor(
    private val useCase: QuizChatUseCase,
    private val prefsProvider: PrefsProvider,
) : MateFlowHandler<Msg, Effect> {
    // subscribe:
    //   val dictId = useCase.getCurrentDictionaryId() ?: return
    //   prefsProvider.getStringFlowByRawKey(quizPickerPrefKey(dictId))
    //     .collectLatest { _ -> emit Msg.QuizComponentTypesLoaded(
    //         types = useCase.getAvailableTypes(dictId),
    //         restoredSelectedRef = useCase.getQuizPickerSelection(dictId),
    //     ) }
}
```

Каждый write в pref → flow emit `QuizComponentTypesLoaded` → reducer обновляет `selectedRef`. **Single update path** — initial load и persist update идут через один Msg.

### Trigger initial load

Initial load — через `Msg.PrepareToStart` reducer-branch + emit `LoadQuizComponentTypes`:

```kotlin
is Msg.PrepareToStart -> state.stopLoading().systemMessage(...) to
    setOf(DatasourceEffect.LoadQuizComponentTypes)
```

Альтернатива: полагаться на FlowHandler subscribe для first-emit (datastore выдаёт current value на subscribe). Тогда отдельный effect не нужен. Sub-flow `business_design_tree` выбирает. Контракт допускает оба — Msg schema одинакова.

### Quiz session integration

`QuizGameImpl.fetchData()` (line 222-226):

```kotlin
val selectedRef = quizChatUseCase.getQuizPickerSelection(dictionaryId)
val effectiveRefs = selectedRef?.let { listOf(it) } ?: quizConfig.componentRefs
return list.mapNotNull { it.toQuizItem(componentRefs = effectiveRefs, ...) }
```

`selectedRef` — через UseCase (single source), **не** через `PrefsProvider` directly (decision §5 walkthrough). При `null` fallback на `quizConfig.componentRefs` (preserves текущая семантика до первого выбора).

## UseCase

Extend `QuizChatUseCase`:

```kotlin
interface QuizChatUseCase {
    // ... existing methods

    /** Domain ComponentType словаря, сортировка по position. Empty list — не null. Прокси над CoreDbApi.LexemeApi.getComponentTypes. */
    suspend fun getAvailableTypes(dictionaryId: Long): List<ComponentType>

    /** Persistent ref словаря. null = не сохранён / corrupted pref. Resolve default — caller (reducer / QuizGameImpl). */
    suspend fun getQuizPickerSelection(dictionaryId: Long): ComponentTypeRef?

    /** Persist ref словаря через PrefsProvider raw-string API (encoding ниже). */
    suspend fun setQuizPickerSelection(dictionaryId: Long, ref: ComponentTypeRef)
}
```

### ComponentTypeRef encoding (decisions §1)

| Variant | Encoded |
|---|---|
| `BuiltIn(BuiltInComponent.TRANSLATION)` | `builtin:translation` |
| `UserDefined("Definition")` | `user:Definition` |

**Parse contract:**
- `builtin:<key>` → `BuiltInComponent.fromKey(key)?.let { BuiltIn(it) }` либо null (unknown built-in key — future-proof для новых).
- `user:<name>` → `UserDefined(name)`. Name восстанавливается через `substringAfter(':')` — корректно для names с `:`, unicode, любым `String`.
- Любой другой format → null (caller fallback на default).

Encoding/parsing — **internal к UseCase impl** (либо отдельный mapper-объект). Не leak'ит в domain (`ComponentTypeRef` остаётся pure без serialization-аннотаций). Без JSON / kotlinx.serialization (decisions §1).

### PrefsProvider extensions

`modules/datasource/prefs/PrefsProvider.kt`:

```kotlin
class PrefsProvider(...) {
    // existing unchanged

    /** Read raw String pref by dynamic key. null если не установлен. */
    suspend fun getStringByRawKey(key: String): String?

    /** Reactive variant для FlowHandler. */
    fun getStringFlowByRawKey(key: String): Flow<String?>

    /** Write raw String pref. value=null → remove. */
    suspend fun setStringByRawKey(key: String, value: String?)
}
```

`PrefKey` **enum НЕ модифицируется** — dynamic per-dictionary ключи живут вне enum (raw-string API, walkthrough §10). Helper:

```kotlin
internal fun quizPickerPrefKey(dictionaryId: Long): String = "quiz_picker_dict_$dictionaryId"
```

Owner helper'а — `QuizChatUseCaseImpl` либо `:datasource:prefs` util (sub-flow design tree).

### Data layer

**Без изменений.** `CoreDbApi.LexemeApi.getComponentTypes(dictionaryId)` уже есть (walkthrough §6). Existing маппер `ComponentTypeApiEntity → ComponentType` переиспользуется (owner — sub-flow design tree).

---

## Cross-check разделов

- **State.quizComponent.availableTypes** ← `QuizComponentTypesLoaded.types` ← `LoadQuizComponentTypes` (либо FlowHandler initial emit) ← `UseCase.getAvailableTypes` ← `CoreDbApi.LexemeApi.getComponentTypes`.
- **State.quizComponent.selectedRef** ← `QuizComponentTypesLoaded.restoredSelectedRef` после `resolveSelection` ← `UseCase.getQuizPickerSelection` ← `PrefsProvider.getStringByRawKey` + decode.
- **Persist path:** UI → `SelectQuizComponent(ref)` → reducer → `SaveQuizPickerSelection` effect → `UseCase.setQuizPickerSelection` → `PrefsProvider.setStringByRawKey` + encode. FlowHandler подхватывает write → `QuizComponentTypesLoaded` → state.
- **Quiz session apply:** `QuizGameImpl.fetchData` → `UseCase.getQuizPickerSelection(dictId)` → filter `quizConfig.componentRefs` до `listOf(selectedRef)` → `toQuizItem`.

Цикл замкнут: persist через UseCase, restore через тот же UseCase в двух точках (FlowHandler для UI / QuizGameImpl для quiz logic). UseCase — single source; PrefsProvider — implementation detail.

_model: claude-opus-4-7[1m]_

## log_messages

- Contract: `ItemsState.quizComponent(availableTypes, selectedRef?)` + 2 Msg (`SelectQuizComponent`, `QuizComponentTypesLoaded`) + 2 Effect (`LoadQuizComponentTypes`, `SaveQuizPickerSelection`) + новый `QuizPickerFlowHandler` + 3 UseCase методов.
- PrefsProvider extended raw-string dynamic-key API (`getStringByRawKey` / `Flow` / `set`); enum `PrefKey` не трогается; encoding `builtin:<key>` / `user:<name>` без JSON.
- QuizGameImpl integration через UseCase (`getQuizPickerSelection`) — не PrefsProvider directly; fallback на `quizConfig.componentRefs` при null.
