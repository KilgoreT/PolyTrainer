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

```kotlin
internal class DatasourceEffectHandler(
    private val useCase: WordCardUseCase,
) : MateEffectHandler<Msg, Effect> {

    override suspend fun runEffect(effect: Effect, consumer: (Msg) -> Unit) {
        when (val eff = effect as? DatasourceEffect) {
            is DatasourceEffect.LoadWord -> {
                withContext(Dispatchers.IO) {
                    useCase.getTermById(eff.wordId).let { term ->
                        term?.let { Msg.TermLoaded(it) }
                            ?: Msg.TermNotLoaded
                    }
                }
            }
            is DatasourceEffect.DeleteWord -> {
                withContext(Dispatchers.IO) {
                    useCase.deleteWord(eff.wordId).let {
                        Msg.CloseScreen
                    }
                }
            }
            null -> Msg.Empty   // Эффект не для этого хендлера
        }.let(consumer)         // Отправить результат обратно
    }
}
```

**Единый паттерн runEffect:**

```kotlin
override suspend fun runEffect(effect: Effect, consumer: (Msg) -> Unit) {
    val msg = when (val e = effect as? MyEffect) {
        is MyEffect.DoSomething -> {
            // выполнить
            Msg.Result
        }
        null -> Msg.Empty   // не наш эффект
    }
    consumer(msg)
}
```

- **Всегда** `val msg = when { ... }` → `consumer(msg)`. Без `return` из when-веток
- Каждая ветка возвращает Msg. Для эффектов не требующих результата — `Msg.Empty`
- `effect as? MyEffect` — safe cast, `null` для чужих эффектов
- `withContext(Dispatchers.IO)` — для блокирующих операций

## Паттерн UiEffectHandler

Минимальный хендлер, трансформирующий эффекты в сообщения:

```kotlin
internal class UiEffectHandler :
    MateEffectHandler<Msg, Effect> {

    override suspend fun runEffect(effect: Effect, consumer: (Msg) -> Unit) {
        return when (val eff = effect as? UiEffect) {
            is UiEffect.ShowSnackbar -> UiMsg.Snackbar(text = eff.title, show = true)
            null -> Msg.Empty
        }.let(consumer)
    }
}
```

## Паттерн NavigationEffectHandler (навигация через effect)

Для закрытия экрана по результату действия (сохранение, удаление). Навигация — side-effect, не флаг в State.

```kotlin
internal class NavigationEffectHandler(
    private val onBack: () -> Unit,
) : MateEffectHandler<Msg, Effect> {

    override suspend fun runEffect(effect: Effect, consumer: (Msg) -> Unit) {
        val msg = when (effect) {
            is NavigationEffect.Back -> {
                onBack()
                Msg.Empty
            }
            else -> Msg.Empty
        }
        consumer(msg)
    }
}
```

**Зачем:** убирает навигационные флаги (`needClose`, `closeScreen`) из State. ВСЯ навигация идёт через reducer → effect → handler. State не содержит навигационных полей.

**Правило:** UI не вызывает навигационные callback'и напрямую. Любая навигация — через Msg → Reducer → `NavigationEffect.Back` → NavigationEffectHandler:
- Пользователь нажал "назад" → `Msg.Back` → Reducer → `NavigationEffect.Back` → handler вызывает `onBack()`
- Сохранение завершено → `Msg.DictionarySaved` → Reducer → `NavigationEffect.Back` → handler вызывает `onBack()`
- Системная кнопка "назад" → `BackHandler { viewModel.accept(Msg.Back) }`

Один эффект `NavigationEffect.Back` для любого закрытия экрана. Reducer контролирует логику — может решить не закрывать (показать диалог).

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
3. **`null -> Msg.Empty`** фоллбэк в каждом хендлере.
4. **Всегда `withContext(Dispatchers.IO)`** для операций с данными.
5. **FlowHandler для подписок**, обычный хендлер для одноразовых эффектов.
6. **Consumer отправляет результат сразу** — без пост-обработки, без кеширования.
7. **Internal visibility** — эффекты и хендлеры `internal` для feature-модуля.
8. **Без сложной логики в хендлерах** — они выполняют эффект и конвертируют результат в сообщение. Принятие решений остаётся в редьюсере.
