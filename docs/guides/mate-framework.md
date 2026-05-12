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

В проекте handlers обычно наследуют один из базовых классов вместо прямой реализации `MateEffectHandler`:

### MateTypedEffectHandler

Базовый класс для DataSource/UI handlers. Автоматически фильтрует чужие эффекты через `filter()`, дочерний реализует только типизированный `onEffect()`.

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

### Navigator + MateNavigationEffectHandler

`Navigator` — абстракция над `NavController` для screen модуля. Реализация (`XxxNavigatorImpl`) живёт в `app/.../navigator/`.

```kotlin
interface Navigator {
    fun back()
}
```

`MateNavigationEffectHandler` наследует `MateTypedEffectHandler<Msg, NavigationEffect>`, обрабатывает базовый `NavigationEffect.Back` через `navigator.back()`, делегирует специфичные эффекты в `onScreenEffect()`.

Per-screen паттерн: `XxxNavigator : Navigator` + `sealed XxxNavigationEffect : NavigationEffect` + `XxxNavigationEffectHandler @AssistedInject(@Assisted navigator)`.

Подробнее — `docs/guides/effect-handlers.md`, `docs/features-spec/navigation.md`.

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
class ChatViewModel @AssistedInject constructor(
    @Assisted navigator: ChatNavigator,
    resourceManager: ResourceManager,
    logger: LexemeLogger,
    datasourceHandler: DatasourceEffectHandler,
    appBarFlowHandler: AppBarFlowHandler,
    navHandlerFactory: ChatNavigationEffectHandler.Factory,
) : ViewModel(), MateStateHolder<ChatScreenState, Msg> {

    private val stateHolder = Mate(
        initState = ChatScreenState(),
        initEffects = setOf(DatasourceEffect.PrepareToStart),
        coroutineScope = viewModelScope,
        reducer = ChatReducer(logger, resourceManager),
        effectHandlerSet = setOf(
            datasourceHandler,
            appBarFlowHandler,
            navHandlerFactory.create(navigator),
        )
    )

    override val state: StateFlow<ChatScreenState>
        get() = stateHolder.state

    override fun accept(message: Msg) = stateHolder.accept(message)

    @AssistedFactory
    interface Factory {
        fun create(navigator: ChatNavigator): ChatViewModel
    }
}
```

DataSource/UI handlers инжектятся через `@Inject` и приходят готовыми. NavigationHandler — через `AssistedFactory`, так как Navigator runtime параметр.

## Конвенции

1. **Редьюсер всегда чистый.** Без `suspend`, без `withContext`, без сайд-эффектов.
2. **Эффекты — sealed interfaces** расширяющие `Effect`. Три категории: `DatasourceEffect`, `UiEffect`, `NavigationEffect` (базовый `Back` + per-screen sealed: `XxxNavigationEffect.OpenYyy`, `XxxNavigationEffect.ExitApp` для root).
3. **Эффект-хендлеры** работают на `Dispatchers.IO` для операций с данными. Наследуют `MateTypedEffectHandler<Msg, E>` (фильтрация чужих эффектов в базе).
4. **Сообщения** — единый sealed interface `Msg`. Внутренние сообщения (от хендлеров) — вложенные sealed interfaces (например, `UiMsg : Msg`).
5. **initEffects** запускают первую загрузку данных при создании экрана.
6. **FlowHandler'ы** автоматически подписываются при инициализации Mate и отписываются при dispose.
7. **Empty message** (`Msg.Empty`) — для "своих" эффектов без полезного результата. Для чужих эффектов consumer вообще не вызывается — фильтрация в `filter()` базового класса.
8. **`@AssistedInject` для ViewModel** — Navigator и runtime аргументы через `@Assisted`, остальные зависимости через обычный constructor injection.
