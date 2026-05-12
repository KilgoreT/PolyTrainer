# Эффект-хендлеры

## Обзор

Эффект-хендлеры выполняют сайд-эффекты, объявленные редьюсером. Они конвертируют эффекты в новые сообщения, которые возвращаются в TEA-цикл.

## Две категории эффектов

### DatasourceEffect

Операции с БД, сетью, preferences. Всегда на `Dispatchers.IO`.

```kotlin
internal sealed interface DatasourceEffect : Effect {
    data class LoadWord(val wordId: Long) : DatasourceEffect
    data class DeleteWord(val wordId: Long) : DatasourceEffect
    data class SaveWord(val wordId: Long, val value: String) : DatasourceEffect
    data class AddLexeme(val wordId: Long) : DatasourceEffect
}
```

### UiEffect

UI-эффекты: snackbar, toast, диалоги инициированные системой.

```kotlin
internal sealed interface UiEffect : Effect {
    data class ShowSnackbar(val title: String) : UiEffect
}
```

## Паттерн DatasourceEffectHandler

Все handlers создаются через Dagger — `@Inject constructor` берёт UseCase'ы из графа. Наследуются от `MateTypedEffectHandler<Msg, E>` — фильтрация чужих эффектов в базовом классе.

```kotlin
class DatasourceEffectHandler @Inject constructor(
    private val useCase: WordCardUseCase,
) : MateTypedEffectHandler<Msg, DatasourceEffect>() {

    override fun filter(effect: Effect): DatasourceEffect? = effect as? DatasourceEffect

    override suspend fun onEffect(effect: DatasourceEffect, consumer: (Msg) -> Unit) {
        val msg = when (effect) {
            is DatasourceEffect.LoadWord -> withContext(Dispatchers.IO) {
                useCase.getTermById(effect.wordId)
                    ?.let { Msg.TermLoaded(it) }
                    ?: Msg.TermNotLoaded
            }
            is DatasourceEffect.DeleteWord -> withContext(Dispatchers.IO) {
                useCase.deleteWord(effect.wordId)
                Msg.CloseScreen
            }
        }
        consumer(msg)
    }
}
```

- `filter()` — `as?` cast в одну строку. Чужие эффекты → `null` → handler выходит без вызова `consumer`
- `onEffect(effect: E)` — параметр типизирован, `when` exhaustive по sealed без `else -> Empty`
- `withContext(Dispatchers.IO)` — для блокирующих операций
- `@Inject constructor` — UseCase'ы из DI графа

## Паттерн UiEffectHandler

Тот же шаблон, без зависимостей:

```kotlin
class UiEffectHandler @Inject constructor() :
    MateTypedEffectHandler<Msg, UiEffect>() {

    override fun filter(effect: Effect): UiEffect? = effect as? UiEffect

    override suspend fun onEffect(effect: UiEffect, consumer: (Msg) -> Unit) {
        val msg = when (effect) {
            is UiEffect.ShowSnackbar -> UiMsg.Snackbar(text = effect.title, show = true)
        }
        consumer(msg)
    }
}
```

## Паттерн NavigationEffectHandler (навигация через effect)

Навигация — side-effect, не флаг в State. Базовый `MateNavigationEffectHandler` обрабатывает `NavigationEffect.Back` через `Navigator`. Per-screen handler наследует базовый, обрабатывает свои навигационные эффекты в `onScreenEffect`.

### Per-screen Navigator + sealed effect

```kotlin
// Navigator интерфейс (в screen модуле, чистый Kotlin)
interface ListNavigator : Navigator {     // back наследуется
    fun exit()                            // root экран — может закрыть приложение
    fun openEdit(id: Long)
}

// Per-screen sealed NavigationEffect
sealed interface ListNavigationEffect : NavigationEffect {
    data object ExitApp : ListNavigationEffect
    data class OpenEdit(val id: Long) : ListNavigationEffect
}

// Handler — наследует базовый, обрабатывает только свои эффекты через onScreenEffect
class ListNavigationEffectHandler @AssistedInject constructor(
    @Assisted private val listNavigator: ListNavigator,
) : MateNavigationEffectHandler<DictionaryListMsg>(listNavigator) {

    override suspend fun onScreenEffect(effect: NavigationEffect) {
        when (effect) {
            is ListNavigationEffect.ExitApp -> listNavigator.exit()
            is ListNavigationEffect.OpenEdit -> listNavigator.openEdit(effect.id)
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(navigator: ListNavigator): ListNavigationEffectHandler
    }
}
```

`Back` обрабатывается один раз в базовом `MateNavigationEffectHandler` — дочерний не пишет про него. `@AssistedInject` потому что `Navigator` — runtime параметр (захватывает `NavController` через NavigatorImpl в app).

### NavigatorImpl

`NavigatorImpl` живёт в `app/.../navigator/` — он знает про `NavController` и Compose Navigation. Screen модули его не видят.

```kotlin
class ListNavigatorImpl(
    private val navController: NavController,
    private val onExit: () -> Unit,
) : ListNavigator {
    override fun back() {
        navController.popBackStack()
    }
    override fun exit() = onExit()
    override fun openEdit(id: Long) {
        navController.navigate("DICTIONARY_CREATE?editId=$id") {
            launchSingleTop = true
        }
    }
}
```

### Правило

UI не вызывает навигационные callback'и напрямую. Любая навигация — через Msg → Reducer → screen-specific NavigationEffect → handler → Navigator:

- Пользователь нажал "назад" → `Msg.RequestBack` → Reducer → `NavigationEffect.Back` → `navigator.back()`
- Сохранение завершено → `Msg.Saved` → Reducer → `NavigationEffect.Back` → `navigator.back()`
- Открытие нового экрана → `Msg.OpenEdit(id)` → Reducer → `ListNavigationEffect.OpenEdit(id)` → `navigator.openEdit(id)`
- Системная кнопка "назад" → `BackHandler { viewModel.accept(Msg.RequestBack) }`

Reducer контролирует логику — может решить не закрывать (показать диалог) или conditional навигацию:

```kotlin
is Msg.RequestBack -> if (state.dictionaries.isEmpty()) {
    state to setOf(ListNavigationEffect.ExitApp)
} else {
    state to setOf(NavigationEffect.Back)
}
```

### Cross-graph (tab → root)

Tab экран может навигировать на двух уровнях:
- Внутри своего таба (внутри `tabsNavController`) — NavigatorImpl дёргает `tabsNavController.navigate(...)`
- На root уровень (открыть DictionaryCreate) — NavigatorImpl получает callback от RootRouter

```kotlin
class VocabularyNavigatorImpl(
    private val tabsNavController: NavController,
    private val onOpenDictionaryCreate: () -> Unit,   // callback от RootRouter
) : VocabularyNavigator {
    override fun back() = tabsNavController.popBackStack()
    override fun openDictionaryCreate() = onOpenDictionaryCreate()
    override fun openWordCard(id: Long) {
        tabsNavController.navigate("wordCard/$id") {
            launchSingleTop = true
        }
    }
}
```

MainScreen инкапсулирован — не знает про `rootNavController`, только callbacks.

## Паттерн FlowHandler (долгоживущие подписки)

### Зачем

Обычный EffectHandler — одноразовый: получил Effect, выполнил, вернул Message. Для реактивных источников (Room Flow, DataStore, StateFlow) нужна долгоживущая подписка: один раз подписался — полу��аешь обновления пока экран жив.

FlowHandler решает эту задачу: подписывается на Flow при старте, каждый emit конвертирует в Message, отправляет в Reducer. Flow не просачивается дальше FlowHandler — UI видит только State.

### Путь данных

```
Источник (Room/DataStore/UseCase)
    ↓
    Flow эмитит новое значение
    ↓
FlowHandler (collect/collectLatest)
    ↓
    send(Msg.SomeUpdate(value))     ← Flow → Message конвертация
    ↓
Reducer
    ↓
    state.copy(field = value)       ← обычная чистая функция
    ↓
State (StateFlow в Mate)
    ↓
UI (collectAsStateWithLifecycle)    ← UI видит только State, не знает про Flow
```

### Интерфейс MateFlowHandler

```kotlin
interface MateFlowHandler<Message, Effect> : MateEffectHandler<Message, Effect> {
    var job: Job?
    fun subscribe(scope: CoroutineScope, send: (Message) -> Unit)
    fun unsubscribe() {
        job?.cancel()
        job = null
    }
}
```

- Наследует `MateEffectHandler` — может обрабатывать и одноразовые эффекты через `runEffect()`
- `subscribe()` — вызывается Mate автоматически при инициализации
- `unsubscribe()` — вызывается при `Mate.dispose()` (через `ViewModel.onCleared()`)
- `job` — корутина подписки, отменяется при unsubscribe

### Жизненный цикл

```
1. ViewModel создаётся
2. Mate инициализируется с effectHandlerSet
3. Mate вызывает subscribe() на каждом FlowHandler
4. FlowHandler запускает корутину: scope.launch { flow.collect { send(Msg) } }
5. Flow эмитит → send(Msg) → Reducer → State → UI
6. ... (повторяется при каждом emit)
7. ViewModel.onCleared() → Mate.dispose() → unsubscribe() → job.cancel()
8. Flow перестаёт собираться
```

### Пример: подписка на DataStore

```kotlin
internal class AppBarFlowHandler(
    private val prefsProvider: PrefsProvider,
) : MateFlowHandler<Msg, Effect> {

    override var job: Job? = null

    override fun subscribe(scope: CoroutineScope, send: (Msg) -> Unit) {
        job = scope.launch {
            combine(
                prefsProvider.getBooleanFlow(EARLIEST_KEY),
                prefsProvider.getBooleanFlow(FREQUENT_MISTAKES_KEY),
                prefsProvider.getBooleanFlow(DEBUG_KEY),
            ) { earliest, mistakes, debug ->
                Msg.UpdateMenu(
                    isEarliestOn = earliest,
                    isFrequentMistakesOn = mistakes,
                    isDebugOn = debug,
                )
            }.collect { send(it) }
        }
    }

    override suspend fun runEffect(effect: Effect, consumer: (Msg) -> Unit) {
        // Этот FlowHandler не обрабатывает одноразовые эффекты
    }
}
```

Три Flow из DataStore combine'ятся в один. При изменении любой настройки — emit → Message → Reducer обновляет State.

### Пример: подписка на Room Flow

```kotlin
class DictionaryListFlowHandler(
    private val dictionaryUseCase: DictionaryUseCase,
) : MateFlowHandler<DictionaryListMsg, Effect> {

    override var job: Job? = null

    override fun subscribe(scope: CoroutineScope, send: (DictionaryListMsg) -> Unit) {
        job = scope.launch {
            dictionaryUseCase.flowDictionaryList()
                .collectLatest { list ->
                    send(DictionaryListMsg.DictionariesLoaded(list))
                }
        }
    }

    override suspend fun runEffect(effect: Effect, consumer: (DictionaryListMsg) -> Unit) {
        // effects handled by DictionaryListEffectHandler
    }
}
```

Room Flow эмитит при каждом изменении таблицы. FlowHandler ловит → Message → список в UI обновляется автоматически.

### FlowHandler + одноразовые эффекты

FlowHandler может одновременно подписываться на Flow И обрабатывать эффекты. Пример: FlowHandler подписан на UseCase Flow (фильтрованные данные), а через `runEffect()` принимает Effect для изменения параметров фильтрации в UseCase.

```kotlin
class FlagFilterFlowHandler(
    private val useCase: DictionaryUseCase,
) : MateFlowHandler<Msg, Effect> {

    override var job: Job? = null

    override fun subscribe(scope: CoroutineScope, send: (Msg) -> Unit) {
        job = scope.launch {
            useCase.flagsFlow()
                .collectLatest { list ->
                    send(Msg.FlagsUpdated(list))
                }
        }
    }

    override suspend fun runEffect(effect: Effect, consumer: (Msg) -> Unit) {
        when (effect) {
            is Effect.FilterFlags -> useCase.updateFilter(effect.query)
            else -> consumer(Msg.Empty)
        }
    }
}
```

- `subscribe()` — подписан на `useCase.flagsFlow()`, при каждом emit → Message
- `runEffect()` — принимает `Effect.FilterFlags(query)`, дёргает `useCase.updateFilter(query)`
- UseCase внутри: `updateFilter()` пишет в MutableStateFlow → Flow пересчитывает → emit → FlowHandler → Message

### Ключевые правила

- Flow **останавливается** в FlowHandler — дальше только Messages
- UI **не знает** про Flow — видит только State через `collectAsStateWithLifecycle`
- Один FlowHandler = одна подписка (один `job`)
- `collectLatest` если новый emit должен отменять обработку предыдущего, `collect` если все emit'ы важны
- Debounce, distinctUntilChanged и другие Flow-операторы — в UseCase или в FlowHandler, НЕ в Reducer и НЕ в UI

## Паттерн: разделение первого emit (Ready vs Updated)

Когда FlowHandler подписывается на Flow, первый emit может иметь особый смысл (данные загрузились, можно инициализировать). Последующие — обычные обновления (фильтрация, изменение данных).

Вместо boolean-флага в State — два разных Message:

```
FlagsReady(list)   — первый emit. FlowHandler отправляет один раз
FlagsUpdated(list) — последующие emit'ы
```

FlowHandler различает:
```kotlin
var isFirstEmit = true
useCase.flagsFlow().collectLatest { list ->
    if (isFirstEmit) {
        send(Msg.FlagsReady(list))
        isFirstEmit = false
    } else {
        send(Msg.FlagsUpdated(list))
    }
}
```

Reducer обрабатывает по-разному: FlagsReady может триггерить цепочку (LoadDictionary), FlagsUpdated — только обновляет State.

**Когда использовать:** первый emit инициирует зависимую загрузку, последующие — нет. Без разделения нужен boolean-флаг в State (isFormPrefilled) — грязнее.

## Паттерн: цепочка эффектов (гарантированный порядок)

Когда два эффекта зависят друг от друга — не запускать одновременно через initEffects (Set неупорядочен). Вместо этого — цепочка: первый эффект завершается, его Message в Reducer порождает второй эффект.

```
initEffects: setOf(Effect.LoadA)          // только первый

LoadA → Handler → Msg.ALoaded(data)       // первый завершён
Reducer: ALoaded → state + setOf(Effect.LoadB)  // второй запускается
LoadB → Handler → Msg.BLoaded(data)       // второй завершён, A уже в State
```

**Когда использовать:** LoadB нуждается в данных из LoadA (например, поиск флага по numericCode в списке флагов). Без цепочки — race condition: LoadB может завершиться раньше LoadA, данных ещё нет.

**Когда НЕ использовать:** эффекты независимы — запускать одновременно через initEffects.

## Подключение хендлеров

В инициализации ViewModel:

```kotlin
private val stateHolder = Mate(
    initState = ChatScreenState(),
    initEffects = setOf(DatasourceEffect.PrepareToStart),
    coroutineScope = viewModelScope,
    reducer = ChatReducer(logger, resourceManager),
    effectHandlerSet = setOf(
        DatasourceEffectHandler(quizGame, prefsProvider),   // одноразовые эффекты
        AppBarFlowHandler(prefsProvider)                     // долгоживущие flow
    )
)
```

Все хендлеры в сете получают **каждый эффект**. Каждый хендлер проверяет свой тип через safe cast и игнорирует чужие эффекты.

## Маппинг Effect → Message

```
DatasourceEffect.LoadWord     → Msg.TermLoaded / Msg.TermNotLoaded
DatasourceEffect.DeleteWord   → Msg.CloseScreen
DatasourceEffect.SaveWord     → Msg.TermLoading (запускает перезагрузку)
DatasourceEffect.AddLexeme    → Msg.LexemeUpdate
DatasourceEffect.DeleteLexeme → Msg.TermLoading (запускает перезагрузку)
UiEffect.ShowSnackbar         → UiMsg.Snackbar
```

Паттерн: деструктивные операции часто запускают полную перезагрузку (`Msg.TermLoading`) вместо локального патча стейта.

## Реактивное обновление данных через FlowHandler

Если экран должен автоматически обновляться при изменении данных (другой экран добавил/удалил запись) — используй `MateFlowHandler` с подпиской на Room Flow, а не ручную перезагрузку при lifecycle events.

```kotlin
class DictionaryListFlowHandler(
    private val useCase: DictionaryUseCase,
) : MateFlowHandler<Msg, Effect> {

    override var job: Job? = null

    override fun subscribe(scope: CoroutineScope, send: (Msg) -> Unit) {
        job = scope.launch {
            useCase.flowDictionaryList()
                .collectLatest { list -> send(Msg.DictionariesLoaded(list)) }
        }
    }

    override suspend fun runEffect(effect: Effect, consumer: (Msg) -> Unit) {
        // Flow handler — одноразовые эффекты в другом хендлере
    }
}
```

В ViewModel — `initEffects = emptySet()`, данные придут через подписку:

```kotlin
Mate(
    initState = State(),
    initEffects = emptySet(),  // НЕ загружаем вручную
    effectHandlerSet = setOf(
        EffectHandler(useCase),      // одноразовые эффекты
        FlowHandler(useCase),        // реактивная подписка
    )
)
```

Room DAO автоматически эмитит новый список при INSERT/DELETE. FlowHandler ловит и отправляет `Msg.DataLoaded(list)` в reducer.

**Когда использовать:**
- Экран показывает список из БД, который может измениться из другого экрана
- Данные должны быть актуальны при возврате (popBackStack)

**Когда НЕ использовать:**
- Данные загружаются один раз и не меняются (языки из Locale)
- Данные не из Room (сетевой запрос без кеша)

## Конвенции

1. **Sealed interface** для каждой категории эффектов, расширяющий `Effect`.
2. **Эффект + хендлер в одном файле** для DatasourceEffect и UiEffect соответственно.
3. **Consumer вызывается только при полезном msg.** Для чужих эффектов — `return` без вызова consumer. Для своих эффектов без результата (навигация) — тоже не вызывать. Контракт `MateEffectHandler.runEffect` не обязывает вызывать `consumer`. Лишний `consumer(Empty)` = бесполезный reducer.reduce.
4. **Всегда `withContext(Dispatchers.IO)`** для операций с данными.
5. **FlowHandler для подписок**, обычный хендлер для одноразовых эффектов.
6. **Видимость** — handler и его sealed effect `public` (Dagger требует public для @Inject в графе и для assisted параметров ViewModel из других модулей). Внутренние типы (Msg, State) остаются `internal`.
7. **Без сложной логики в хендлерах** — они выполняют эффект и конвертируют результат в сообщение. Принятие решений остаётся в редьюсере.

---

## Typed base паттерн: MateTypedEffectHandler

Mate вызывает `runEffect()` на КАЖДОМ handler с КАЖДЫМ эффектом. Чтобы handler не обрабатывал чужие эффекты, есть базовый класс который автоматически фильтрует.

### Базовый класс (core/mate)

```kotlin
abstract class MateTypedEffectHandler<Msg, E : Effect> : MateEffectHandler<Msg, Effect> {

    final override suspend fun runEffect(effect: Effect, consumer: (Msg) -> Unit) {
        val typed = filter(effect) ?: return
        onEffect(typed, consumer)
    }

    protected abstract fun filter(effect: Effect): E?
    protected abstract suspend fun onEffect(effect: E, consumer: (Msg) -> Unit)
}
```

- `filter(effect)` — приводит `Effect` к нужному типу через `as?`. Возвращает `null` для чужих
- При `null` handler выходит — `consumer` НЕ вызывается, reducer не дёргается
- `onEffect(effect: E)` — типизированный метод, дочерний реализует только свою логику
- `runEffect` — `final`, дочерний класс не может его переопределить

### Что должно быть для применения паттерна

1. **Sealed interface для эффектов handler'а:**
   ```kotlin
   internal sealed interface DatasourceEffect : Effect {
       data class LoadX(...) : DatasourceEffect
       data class SaveX(...) : DatasourceEffect
   }
   ```
   Если эффектов нет — handler не нужен, удалить.

2. **Handler — наследует `MateTypedEffectHandler<Msg, EffectType>`:**
   - `filter()` — однострочный override через `as?`
   - `onEffect()` — обработка эффектов через `when`, без `else -> Empty` ветки (компилятор требует exhaustive, sealed гарантирует)

3. **Reducer** — без изменений. Возвращает эффекты в `Set<Effect>` как раньше.

4. **Msg** — `Empty` всё ещё нужен для эффектов которые не меняют state (например после `SaveDictionary` нет смысла менять state).

### Пример: DatasourceEffectHandler для WordCard

#### Эффекты

```kotlin
internal sealed interface DatasourceEffect : Effect {
    data class LoadWord(val wordId: Long) : DatasourceEffect
    data class DeleteWord(val wordId: Long) : DatasourceEffect
    data class SaveWord(val wordId: Long, val value: String) : DatasourceEffect
}
```

#### Handler

```kotlin
internal class DatasourceEffectHandler @Inject constructor(
    private val useCase: WordCardUseCase,
) : MateTypedEffectHandler<Msg, DatasourceEffect>() {

    override fun filter(effect: Effect): DatasourceEffect? = effect as? DatasourceEffect

    override suspend fun onEffect(effect: DatasourceEffect, consumer: (Msg) -> Unit) {
        val msg = withContext(Dispatchers.IO) {
            when (effect) {
                is DatasourceEffect.LoadWord -> {
                    useCase.getTermById(effect.wordId)
                        ?.let { Msg.TermLoaded(it) }
                        ?: Msg.TermNotLoaded
                }
                is DatasourceEffect.DeleteWord -> {
                    useCase.deleteWord(effect.wordId)
                    Msg.CloseScreen
                }
                is DatasourceEffect.SaveWord -> {
                    useCase.saveWord(effect.wordId, effect.value)
                    Msg.Empty
                }
            }
        }
        consumer(msg)
    }
}
```

Сравни с старым паттерном:
- Нет `as? DatasourceEffect` в начале `when` (он в `filter`)
- Нет `null -> Msg.Empty` ветки (фильтр в базовом классе)
- Нет `else -> Msg.Empty` (sealed гарантирует exhaustive)
- `when` типизирован — IDE подсказывает только `DatasourceEffect` варианты

#### Reducer (без изменений)

```kotlin
class WordCardReducer : MateReducer<State, Msg, Effect> {
    override fun reduce(state: State, msg: Msg): ReducerResult<State, Effect> = when (msg) {
        is Msg.DeleteRequest -> state to setOf(DatasourceEffect.DeleteWord(msg.id))
        // ...
    }
}
```

### NavigationEffectHandler — частный случай

Базовый `MateNavigationEffectHandler` наследует тот же паттерн. Фильтрует `NavigationEffect`, обрабатывает базовый `Back` через `Navigator.back()`, делегирует специфичное в `onScreenEffect`. `ExitApp` — НЕ в базовом эффекте: не все экраны могут закрыть приложение. Per-screen экран добавляет `ExitApp` в свой sealed (Splash, DictionaryList) и `exit()` в свой Navigator. Подробнее — секция "Паттерн NavigationEffectHandler" выше.

### Когда НЕ применим

- **FlowHandler с пустым `runEffect`** — обрабатывает только подписки через `subscribe()`. Typed-фильтр не нужен. Наследует обычный `MateFlowHandler`, `runEffect` остаётся пустым (Mate всё равно дёргает его, но handler ничего не делает)
- **Handler без sealed interface** — если эффекты приходят из чужого sealed (как FlagFilterFlowHandler ловит `DictionaryFormEffect.FilterFlags`) — конфликт с другим handler'ом. Нужно вынести в свой sealed interface

### Преимущества

- **Нет boilerplate** — `filter` это одна строка, `when` не требует `else -> Empty`
- **Типизация** — `onEffect(effect: E)` параметр, компилятор гарантирует тип
- **Нет лишних reducer вызовов** — чужие эффекты не доходят до consumer
- **Единый паттерн** — все handlers выглядят одинаково
