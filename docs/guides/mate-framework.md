# Mate Framework: TEA State Management

## Обзор

Mate — кастомная реализация **The Elm Architecture (TEA)** для Android. Однонаправленный поток данных с чистым управлением состоянием, явными сайд-эффектами и реактивным наблюдением за стейтом.

## Основной цикл

```
UI ---accept(Msg)---> Mate ---reduce(State, Msg)---> Reducer
                        |                                |
                        |                    (NewState, Set<Effect>)
                        |                                |
                   Обновление StateFlow <----------------+
                        |
                   Запуск эффектов
                        |
                   EffectHandler ---consumer(Msg)---> обратно в Mate.accept()
```

## Ключевые типы

### ReducerResult

```kotlin
typealias ReducerResult<State, Effect> = Pair<State, Set<Effect>>
```

Хелперы:
```kotlin
fun <STATE, EFFECTS> ReducerResult<STATE, EFFECTS>.state() = first
fun <STATE, EFFECTS> ReducerResult<STATE, EFFECTS>.effects() = second
```

### MateReducer

Чистая функция. Без сайд-эффектов. Без корутин. Только трансформация стейта + декларация эффектов.

```kotlin
interface MateReducer<State, Message, Effect> {
    fun reduce(state: State, message: Message): ReducerResult<State, Effect>
}
```

### Effect

Маркер-интерфейс для всех эффектов.

```kotlin
interface Effect
```

### MateEffectHandler

Выполняет сайд-эффекты (БД, сеть, преференции). Генерирует новые сообщения через `consumer`.

```kotlin
interface MateEffectHandler<Message, out Effect> {
    suspend fun runEffect(
        effect: @UnsafeVariance Effect,
        consumer: (Message) -> Unit
    )
}
```

### MateFlowHandler

Для долгоживущих подписок (наблюдение за Flow). Расширяет `MateEffectHandler`.

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

### MateStateHolder

Публичный интерфейс к TEA-циклу. Реализуется ViewModel'ом.

```kotlin
interface MateStateHolder<State, Message> {
    val state: StateFlow<State>
    fun accept(message: Message)
}
```

### Mate Class

Центральный оркестратор. Связывает всё вместе.

```kotlin
class Mate<State, Message, Effect>(
    initState: State,                                          // начальный стейт
    reducer: MateReducer<State, Message, Effect>,              // редьюсер
    initEffects: Set<Effect>,                                  // эффекты при инициализации
    private val effectHandlerSet: Set<MateEffectHandler<Message, Effect>>,  // хендлеры
    private val coroutineScope: CoroutineScope,                // скоуп для эффектов
) : MateStateHolder<State, Message>,
    MateReducer<State, Message, Effect> by reducer,
    MateEffectHandler<Message, Effect>
```

**Инициализация:**
1. Создаёт `_state = MutableStateFlow(initState)`
2. Выполняет `initEffects` (например, загрузка начальных данных)
3. Подписывает все `MateFlowHandler` из handler set

**Обработка сообщений (accept):**
1. Вызывает `reduce(_state.value, message)` → `(newState, effects)`
2. Обновляет `_state.value = newState`
3. Запускает каждый эффект в `coroutineScope`
4. Каждый хендлер получает эффект и вызывает `consumer(resultMessage)`
5. `consumer` = `::accept` — цикл продолжается

**Очистка:**
- `dispose()` отменяет все подписки `MateFlowHandler`

## Файловая структура фичи

```
feature/
  logic/
    State.kt                    -- Стейт дата-классы + extension-функции
    Message.kt                  -- Sealed interface Msg
    Reducer.kt                  -- Реализация MateReducer
    DatasourceEffectHandler.kt  -- Эффекты БД/сети
    UiEffectHandler.kt          -- UI эффекты (snackbar и т.д.)
  deps/
    UseCase.kt                  -- Доменный интерфейс
  ui/
    ViewModel.kt                -- Инициализация Mate
    Screen.kt                   -- Composable UI
```

## Пример подключения (reference: ChatQuiz)

```kotlin
class ChatViewModel(
    quizChatUseCase: QuizChatUseCase,
    resourceManager: ResourceManager,
    prefsProvider: PrefsProvider,
    logger: LexemeLogger,
) : ViewModel(), MateStateHolder<ChatScreenState, Msg> {

    private val stateHolder = Mate(
        initState = ChatScreenState(),
        initEffects = setOf(DatasourceEffect.PrepareToStart),
        coroutineScope = viewModelScope,
        reducer = ChatReducer(logger, resourceManager),
        effectHandlerSet = setOf(
            DatasourceEffectHandler(quizGame, prefsProvider),
            AppBarFlowHandler(prefsProvider)       // долгоживущий Flow
        )
    )

    override val state: StateFlow<ChatScreenState>
        get() = stateHolder.state

    override fun accept(message: Msg) = stateHolder.accept(message)
}
```

## Конвенции

1. **Редьюсер всегда чистый.** Без `suspend`, без `withContext`, без сайд-эффектов.
2. **Эффекты — sealed interfaces** расширяющие `Effect`. Две категории: `DatasourceEffect` и `UiEffect`.
3. **Эффект-хендлеры** работают на `Dispatchers.IO` для операций с данными.
4. **Сообщения** — единый sealed interface `Msg`. Внутренние сообщения (от хендлеров) — вложенные sealed interfaces (например, `UiMsg : Msg`).
5. **initEffects** запускают первую загрузку данных при создании экрана.
6. **FlowHandler'ы** автоматически подписываются при инициализации Mate и отписываются при dispose.
7. **Empty message** (`Msg.Empty`) — no-op фоллбэк когда эффект-хендлер не обрабатывает тип эффекта.
