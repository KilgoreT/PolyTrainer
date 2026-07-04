# Design tree: IS481 Quiz Component Picker

Spec: `business_contract_spec.md`. DAG узлов для business слоя — Domain → UseCase iface → PrefsProvider → UseCase impl → Mate (State / Msg / Effect / Reducer / FlowHandler) → Widget (dropdown primitives → menu items → ActionsWidget) → QuizGame integration + Strings + DI.

**Корректировка vs spec / план:**
- Tier 1 primitives (узлы 9-10) размещаются в **`modules/core/ui/src/main/java/me/apomazkin/ui/dropdown/`** (package `me.apomazkin.ui.dropdown`). Per зафиксированной convention: Tier 1 = `core/ui` + `core/theme` с `Lexeme*` префиксом. Существующий `modules/widget/iconDropDowned/` (без Lexeme prefix) НЕ trogаем — его migration в design-system convention запланирована отдельным backlog'ом «Migrate `widget/iconDropDowned/` → `core/ui/dropdown/` с Lexeme prefix».
- Spec упоминает `modules/screen/quiz/chat/src/main/res/values/strings.xml` — у chat модуля **нет** `res/` директории, строки live в `core/core-resources/.../values/strings.xml` (где уже лежат `chat_menu_item_*`). Узел 17 — туда.

## Часть 1: Граф

```yaml
- id: 1
  file: modules/domain/lexeme/src/main/java/me/apomazkin/lexeme/ComponentTypeRef.kt
  action: "~"
  depends: []

- id: 2
  file: modules/screen/quiz/chat/src/main/java/me/apomazkin/quiz/chat/deps/QuizChatUseCase.kt
  action: "~"
  depends: [1]

- id: 3
  file: modules/datasource/prefs/src/main/java/me/apomazkin/prefs/PrefsProvider.kt
  action: "~"
  depends: []

- id: 4
  file: app/src/main/java/me/apomazkin/polytrainer/di/module/quizchat/QuizChatUseCaseImpl.kt
  action: "~"
  depends: [1, 2, 3]

- id: 5
  file: modules/screen/quiz/chat/src/main/java/me/apomazkin/quiz/chat/logic/State.kt
  action: "~"
  depends: [1]

- id: 6
  file: modules/screen/quiz/chat/src/main/java/me/apomazkin/quiz/chat/logic/Message.kt
  action: "~"
  depends: [1]

- id: 7
  file: modules/screen/quiz/chat/src/main/java/me/apomazkin/quiz/chat/logic/DatasourceEffectHandler.kt
  action: "~"
  depends: [2, 6]

- id: 8
  file: modules/screen/quiz/chat/src/main/java/me/apomazkin/quiz/chat/logic/ChatReducer.kt
  action: "~"
  depends: [1, 5, 6, 7]

- id: 9
  file: modules/core/ui/src/main/java/me/apomazkin/ui/dropdown/LexemeSubmenuMenuItem.kt
  action: "+"
  depends: []

- id: 10
  file: modules/core/ui/src/main/java/me/apomazkin/ui/dropdown/LexemeRadioMenuItem.kt
  action: "+"
  depends: []

- id: 11
  file: modules/screen/quiz/chat/src/main/java/me/apomazkin/quiz/chat/widget/appbar/menu/ComponentChoiceItem.kt
  action: "+"
  depends: [1, 10]

- id: 12
  file: modules/screen/quiz/chat/src/main/java/me/apomazkin/quiz/chat/widget/appbar/menu/QuizComponentMenuItem.kt
  action: "+"
  depends: [1, 5, 6, 9, 11]

- id: 13
  file: modules/screen/quiz/chat/src/main/java/me/apomazkin/quiz/chat/widget/appbar/ActionsWidget.kt
  action: "~"
  depends: [5, 6, 12]

- id: 14
  file: modules/screen/quiz/chat/src/main/java/me/apomazkin/quiz/chat/logic/QuizPickerFlowHandler.kt
  action: "+"
  depends: [2, 3, 6]

- id: 15
  file: modules/screen/quiz/chat/src/main/java/me/apomazkin/quiz/chat/ChatViewModel.kt
  action: "~"
  depends: [14]

- id: 16
  file: modules/screen/quiz/chat/src/main/java/me/apomazkin/quiz/chat/quiz/QuizGameImpl.kt
  action: "~"
  depends: [2]

- id: 17
  file: core/core-resources/src/main/res/values/strings.xml
  action: "~"
  depends: []
```

Параллельные кластеры: {1, 3, 9, 10, 17}; затем {2}; затем {4, 5, 6, 11}; затем {7, 8, 12, 14, 16}; затем {13, 15}.

## Часть 2: Детали изменений

### #1 ComponentTypeRef.kt [~]

**Было:** `sealed interface ComponentTypeRef { BuiltIn(key: BuiltInComponent); UserDefined(name: String) }` (file-level).

**Стало:** добавляется top-level extension в том же файле (либо в `ComponentType.kt` — owner выбирается impl-step). Pure-JVM, без Android.

```kotlin
fun ComponentType.toRef(): ComponentTypeRef = when (val sk = systemKey) {
    null -> ComponentTypeRef.UserDefined(name ?: error("user-defined ComponentType без name"))
    else -> ComponentTypeRef.BuiltIn(sk)
}
```

### #2 QuizChatUseCase.kt [~]

**Было:** `interface QuizChatUseCase` с `getCurrentDictionaryId`, `updateWriteQuiz`, `getRandomWriteQuizList`, `getQuizConfig`.

**Стало:** + 3 метода:

```kotlin
suspend fun getAvailableTypes(dictionaryId: Long): List<ComponentType>
suspend fun getQuizPickerSelection(dictionaryId: Long): ComponentTypeRef?
suspend fun setQuizPickerSelection(dictionaryId: Long, ref: ComponentTypeRef)
```

### #3 PrefsProvider.kt [~]

**Было:** typed API через enum `PrefKey` (`getBoolean/Long/Int` + `Flow`/`set`).

**Стало:** + raw-string dynamic-key API (для per-dictionary picker prefs); `PrefKey` enum **не трогается**.

```kotlin
suspend fun getStringByRawKey(key: String): String?
fun getStringFlowByRawKey(key: String): Flow<String?>
suspend fun setStringByRawKey(key: String, value: String?)  // value=null → remove
```

Внутри — `stringPreferencesKey(key)` + `dataStore.edit { ... }` симметрично существующим `Long/Boolean` методам.

### #4 QuizChatUseCaseImpl.kt [~]

**Было:** impl 4 методов (deps: `dictionaryApi`, `quizApi`, `lexemeApi`, `prefsProvider`, `logger`).

**Стало:** + 3 импла + private encoder/decoder + `quizPickerPrefKey(dictId)` helper.

```kotlin
override suspend fun getAvailableTypes(dictionaryId: Long): List<ComponentType> =
    lexemeApi.getComponentTypes(dictionaryId).map { it.toDomain() }  // proxy + sort по position

override suspend fun getQuizPickerSelection(dictionaryId: Long): ComponentTypeRef? {
    val raw = prefsProvider.getStringByRawKey(quizPickerPrefKey(dictionaryId)) ?: return null
    return decodeRef(raw)  // null если format невалиден
}

override suspend fun setQuizPickerSelection(dictionaryId: Long, ref: ComponentTypeRef) {
    prefsProvider.setStringByRawKey(quizPickerPrefKey(dictionaryId), encodeRef(ref))
}

private fun quizPickerPrefKey(id: Long): String = "quiz_picker_dict_$id"

private fun encodeRef(ref: ComponentTypeRef): String = when (ref) {
    is BuiltIn      -> "builtin:${ref.key.key}"
    is UserDefined  -> "user:${ref.name}"
}

private fun decodeRef(raw: String): ComponentTypeRef? = when {
    raw.startsWith("builtin:") -> BuiltInComponent.fromKey(raw.substringAfter(':'))?.let(::BuiltIn)
    raw.startsWith("user:")    -> UserDefined(raw.substringAfter(':'))
    else                       -> null
}
```

Маппер `ComponentTypeApiEntity → ComponentType` уже есть в проекте (см. `WordCardUseCaseImpl`) — переиспользовать.

### #5 State.kt [~]

**Было:** `ItemsState(earliest, frequentMistakes, debug)` с nested `Earliest`/`FrequentMistakes`/`Debug` + `updateMenu(...)` extension.

**Стало:** + nested `QuizComponent` + поле в `ItemsState` + computed helper. `updateMenu` не трогаем (Boolean prefs только).

```kotlin
@Immutable
data class ItemsState(
    val earliest: Earliest = Earliest(),
    val frequentMistakes: FrequentMistakes = FrequentMistakes(),
    val debug: Debug = Debug(),
    val quizComponent: QuizComponent = QuizComponent(),  // NEW
) {
    // existing nested...
    data class QuizComponent(
        val availableTypes: List<ComponentType> = emptyList(),
        val selectedRef: ComponentTypeRef? = null,
    )
}

val ItemsState.QuizComponent.isPickerEnabled: Boolean
    get() = availableTypes.size > 1
```

### #6 Message.kt [~]

**Было:** `sealed interface Msg` с existing variants (`PrepareToStart`, `ShowMenu`, `EarliestOn/Off`, ...).

**Стало:** + 2 variant:

```kotlin
data class SelectQuizComponent(val ref: ComponentTypeRef) : Msg
data class QuizComponentTypesLoaded(
    val types: List<ComponentType>,
    val restoredSelectedRef: ComponentTypeRef?,
) : Msg
```

### #7 DatasourceEffectHandler.kt [~]

**Было:** `sealed interface DatasourceEffect` с `PrepareToStart`, `EarliestOn/Off`, ..., `LoadQuiz`, `Skip`, ...; `DatasourceEffectHandler` с `onEffect`.

**Стало:** + 2 effect + 2 ветки в `onEffect`.

```kotlin
sealed interface DatasourceEffect : Effect {
    // existing...
    data object LoadQuizComponentTypes : DatasourceEffect
    data class SaveQuizPickerSelection(val ref: ComponentTypeRef) : DatasourceEffect
}

// в onEffect:
is LoadQuizComponentTypes -> withContext(Dispatchers.IO) {
    val dictId = useCase.getCurrentDictionaryId() ?: return@withContext Msg.Empty
    Msg.QuizComponentTypesLoaded(
        types = useCase.getAvailableTypes(dictId),
        restoredSelectedRef = useCase.getQuizPickerSelection(dictId),
    )
}
is SaveQuizPickerSelection -> withContext(Dispatchers.IO) {
    val dictId = useCase.getCurrentDictionaryId() ?: return@withContext Msg.Empty
    useCase.setQuizPickerSelection(dictId, effect.ref)
    Msg.Empty
}
```

Конструктор хендлера — + `useCase: QuizChatUseCase` (DI).

### #8 ChatReducer.kt [~]

**Было:** `when (message)` с existing branches; `Msg.PrepareToStart -> state.stopLoading().systemMessage(...) to setOf()`.

**Стало:** + 2 branch + private helper `resolveSelection`; `Msg.PrepareToStart` — добавить `LoadQuizComponentTypes` в effects.

```kotlin
is Msg.PrepareToStart -> state.stopLoading().systemMessage(welcomeMessage().toMessageContent()) to
    setOf(DatasourceEffect.LoadQuizComponentTypes)

is Msg.SelectQuizComponent -> state to setOf(DatasourceEffect.SaveQuizPickerSelection(message.ref))

is Msg.QuizComponentTypesLoaded -> state.updateQuizComponent(
    types = message.types,
    selectedRef = resolveSelection(message.types, message.restoredSelectedRef),
) to setOf()

private fun resolveSelection(types: List<ComponentType>, restored: ComponentTypeRef?): ComponentTypeRef? {
    if (types.isEmpty()) return null
    val available = types.map { it.toRef() }.toSet()
    return if (restored != null && restored in available) restored else available.firstOrNull()
}
```

Дополнительно extension `ChatScreenState.updateQuizComponent(types, selectedRef)` — в State.kt (#5) либо здесь, copy в nested `quizComponent`.

### #9 LexemeSubmenuMenuItem.kt [+]

Назначение: generic submenu wrapper над `DropdownMenuItem` для radio-группы — заголовок + опционально trailing chevron + раскрывающаяся inner-`Column` с radio-пунктами. Pure presentational, без logic.

```kotlin
@Composable
fun LexemeSubmenuMenuItem(
    title: StringSource,
    enabled: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,  // radio items
)
```

Поведение: tap на заголовок → toggle isExpanded; `enabled=false` → header показан но клики игнорируются (degenerate case). Internal state `remember { mutableStateOf(false) }` для expansion. Внутри — `Column { content() }` без отдельного `DropdownMenu` (раскрытие inline в существующем dropdown'е).

### #10 LexemeRadioMenuItem.kt [+]

Назначение: generic radio-вариант `MenuItem` (по аналогии с `MenuItemWithCheckbox` из `MenuItem.kt`). Не модифицируем `MenuItem.kt` чтобы не плодить enum-ветвь — отдельная top-level композиция.

```kotlin
@Composable
fun LexemeRadioMenuItem(
    isSelected: Boolean,
    title: StringSource,
    enabled: Boolean = true,
    onSelect: () -> Unit,
)
```

Внутренности: `DropdownMenuItem(leadingIcon = { RadioButton(selected = isSelected, onClick = null, enabled = enabled) }, text = { Text(title.asString(), ...) }, onClick = onSelect, enabled = enabled)`.

### #11 ComponentChoiceItem.kt [+]

Назначение: per-type wrapper над `LexemeRadioMenuItem`. Резолвит title (built-in TRANSLATION → `R.string.chat_menu_item_component_translation`; UserDefined → `name` raw). `selectedRef` сравнивается через `ComponentType.toRef() == selectedRef`. `enabled` приходит сверху (`isPickerEnabled`).

```kotlin
@Composable
internal fun ComponentChoiceItem(
    type: ComponentType,
    isSelected: Boolean,
    enabled: Boolean,
    onSelect: (ref: ComponentTypeRef) -> Unit,
)
```

### #12 QuizComponentMenuItem.kt [+]

Назначение: top-level picker, оборачивает `LexemeSubmenuMenuItem` + список `ComponentChoiceItem` per `availableTypes`. Скрывает себя если `availableTypes.isEmpty()` (invariant 3). `enabled = isPickerEnabled`. Title: `R.string.chat_menu_item_quiz_component`.

```kotlin
@Composable
internal fun QuizComponentMenuItem(
    state: ItemsState.QuizComponent,
    onSelect: (ref: ComponentTypeRef) -> Unit,
) {
    if (state.availableTypes.isEmpty()) return
    LexemeSubmenuMenuItem(
        title = StringSource.fromRes(R.string.chat_menu_item_quiz_component, ...),
        enabled = state.isPickerEnabled,
    ) {
        state.availableTypes.forEach { type ->
            ComponentChoiceItem(
                type = type,
                isSelected = type.toRef() == state.selectedRef,
                enabled = state.isPickerEnabled,
                onSelect = onSelect,
            )
        }
    }
}
```

### #13 ActionsWidget.kt [~]

**Было:** `IconDropdownWidget { EarliestReviewedMenuItem(...); MistakesMenuItem(...); if (BuildConfig.DEBUG) { DividerMenuItem(); DebugMenuItem(...) } }`.

**Стало:** + `QuizComponentMenuItem(state.quizComponent, onSelect = { sendMessage(Msg.SelectQuizComponent(it)) })` **после `MistakesMenuItem` и до `if (BuildConfig.DEBUG)`-блока** (контракт layout: между Mistakes и debug-divider). Никаких других изменений.

### #14 QuizPickerFlowHandler.kt [+]

Назначение: новый `MateFlowHandler<Msg, Effect>` — subscribe на dynamic-key string flow `quiz_picker_dict_<id>`; на каждый emit (initial + write) → re-emit `Msg.QuizComponentTypesLoaded`. По аналогии с `AppBarFlowHandler` (структура `subscribe(scope, send)` + `var job: Job?`).

```kotlin
class QuizPickerFlowHandler @Inject constructor(
    private val useCase: QuizChatUseCase,
    private val prefsProvider: PrefsProvider,
) : MateFlowHandler<Msg, Effect> {
    override suspend fun runEffect(effect: Effect, consumer: (Msg) -> Unit) {}
    override var job: Job? = null
    override fun subscribe(scope: CoroutineScope, send: (Msg) -> Unit) {
        job = scope.launch {
            val dictId = useCase.getCurrentDictionaryId() ?: return@launch
            prefsProvider.getStringFlowByRawKey(quizPickerPrefKey(dictId))
                .collectLatest { _ ->
                    send(Msg.QuizComponentTypesLoaded(
                        types = useCase.getAvailableTypes(dictId),
                        restoredSelectedRef = useCase.getQuizPickerSelection(dictId),
                    ))
                }
        }
    }
}
```

`quizPickerPrefKey` — re-use из #4 (либо internal top-level в одном из feature-модулей; sub-flow design выбирает).

### #15 ChatViewModel.kt [~]

**Было:** `@AssistedInject constructor(... datasourceHandler, appBarFlowHandler, navHandlerFactory)`; `effectHandlerSet = setOf(datasourceHandler, appBarFlowHandler, navHandlerFactory.create(navigator))`.

**Стало:** + `quizPickerFlowHandler: QuizPickerFlowHandler` в ctor; добавить в `effectHandlerSet`.

```kotlin
effectHandlerSet = setOf(
    datasourceHandler,
    appBarFlowHandler,
    quizPickerFlowHandler,  // NEW
    navHandlerFactory.create(navigator),
)
```

### #16 QuizGameImpl.kt [~]

**Было:** в `fetchData()` после load `quizConfig`: `it.toQuizItem(componentRefs = quizConfig.componentRefs, ...)`.

**Стало:** перед mapping — резолвим `selectedRef` через UseCase, фильтруем `componentRefs`. Fallback на `quizConfig.componentRefs` если `selectedRef == null`.

```kotlin
val selectedRef = quizChatUseCase.getQuizPickerSelection(dictionaryId)
val effectiveRefs = selectedRef?.let { listOf(it) } ?: quizConfig.componentRefs
// ...
.mapNotNull {
    it.toQuizItem(
        componentRefs = effectiveRefs,
        resourceManager = resourceManager,
        isDebugOn = prefsProvider.getBoolean(PrefKey.CHAT_DEBUG_STATUS_BOOLEAN) ?: false,
    )
}
```

### #17 core-resources strings.xml [~]

**Было:** существующие `chat_menu_item_show_debug`, `chat_menu_item_mistakes`, `chat_menu_item_earliest_reviewed`.

**Стало:** + 2 string:

```xml
<string name="chat_menu_item_quiz_component">Quiz component</string>
<string name="chat_menu_item_component_translation">Translation</string>
```

`values-ru-rRU/strings.xml` — параллельно. UserDefined пункт title не использует strings (raw `name`).

_model: claude-opus-4-7[1m]_

## log_messages

- 17 узлов: 14 в `modules/screen/quiz/chat` + app + lexeme + prefs + core-resources + core/ui (Tier 1 dropdown primitives). 5 параллельных кластеров.
- Узлы 9-10 (Lexeme*MenuItem primitives) — в `modules/core/ui/dropdown/` per Tier 1 convention; iconDropDowned migration → backlog.
- Strings — `core/core-resources/` (не chat-module res, отсутствует).
- ViewModel включён в граф (#15) — `QuizPickerFlowHandler` нужно зарегистрировать в `effectHandlerSet`; иначе subscribe не вызовется.

## checklist_items

Привязка sub-item'ов к корневым checklist'а (`checklist.md`).

- **User открывает chat → видит picker с правильным состоянием** [root]
  - [ ] `LexemeSubmenuMenuItem` + `LexemeRadioMenuItem` primitives созданы (#9, #10)
  - [ ] `QuizComponentMenuItem` рендерит подменю поверх `availableTypes`, скрывает себя при `availableTypes.isEmpty()` (#12)
  - [ ] `ComponentChoiceItem` маркирует selected ref через `type.toRef() == selectedRef`, disabled при `!isPickerEnabled` (#11, #1)
  - [ ] `ActionsWidget` встраивает picker между `MistakesMenuItem` и debug-блоком (#13)
  - [ ] `ItemsState.QuizComponent(availableTypes, selectedRef?)` + computed `isPickerEnabled` (#5)
  - [ ] `LoadQuizComponentTypes` triggered из `Msg.PrepareToStart`; `QuizComponentTypesLoaded` обновляет state через `resolveSelection` (#6, #7, #8)
  - [ ] Default fallback на первый из `availableTypes` если restored ref не in list (reducer helper, #8)
  - [ ] Строки `chat_menu_item_quiz_component` + `chat_menu_item_component_translation` добавлены (#17)
- **User меняет выбор → выбор сохраняется в prefs per-dictionary** [root]
  - [ ] `Msg.SelectQuizComponent(ref)` emit'ит `SaveQuizPickerSelection` effect, state не меняется напрямую (#6, #8)
  - [ ] `DatasourceEffectHandler.SaveQuizPickerSelection` резолвит `dictId` через `getCurrentDictionaryId()` и вызывает `setQuizPickerSelection(dictId, ref)` (#7)
  - [ ] `QuizChatUseCaseImpl.setQuizPickerSelection` encode + write через `PrefsProvider.setStringByRawKey(quizPickerPrefKey(dictId), encoded)` (#4)
  - [ ] `PrefsProvider.setStringByRawKey` + `quizPickerPrefKey(dictId) = "quiz_picker_dict_$id"` (#3, #4)
  - [ ] `QuizPickerFlowHandler` подхватывает write → re-emit `QuizComponentTypesLoaded` → state update (#14, #15)
- **User запускает quiz session → componentRefs отфильтрован по selectedRef** [root]
  - [ ] `QuizGameImpl.fetchData` вызывает `useCase.getQuizPickerSelection(dictionaryId)` (#16)
  - [ ] При non-null `selectedRef` → `effectiveRefs = listOf(selectedRef)`; передаётся в `toQuizItem(componentRefs = ...)` (#16)
  - [ ] При null → fallback на `quizConfig.componentRefs` (preserves текущая семантика) (#16)
  - [ ] `QuizChatUseCase.getQuizPickerSelection` объявлен в iface, impl в `QuizChatUseCaseImpl` (#2, #4)
- **User возвращается в словарь → previous выбор восстановлен из prefs** [root]
  - [ ] Per-dictionary key `quiz_picker_dict_<id>` гарантирует изоляцию выборов между словарями (#4)
  - [ ] Initial load на entry: `LoadQuizComponentTypes` effect → `getQuizPickerSelection(dictId)` decode + emit `QuizComponentTypesLoaded` с restored ref (#7, #4)
  - [ ] `ComponentTypeRef` encoding/parse: `builtin:<key>` / `user:<name>` через `substringAfter(':')`; unknown formats → null (#4)
  - [ ] `ComponentType.toRef()` extension в domain — stable mapping для membership check restored ref в available (#1)
  - [ ] Persistence через DataStore выживает cold start (PrefsProvider использует `preferencesDataStore`, без изменений — #3)
