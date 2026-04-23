# ViewModel и DI подключение

## Структура ViewModel

Каждый TEA-экран имеет ViewModel который:
1. Реализует `MateStateHolder<State, Msg>`
2. Создаёт и хранит инстанс `Mate`
3. Предоставляет `Factory` для инстанцирования

```kotlin
class WordCardViewModel(
    wordId: Long,
    wordCardUseCase: WordCardUseCase,
) : ViewModel(), MateStateHolder<WordCardState, Msg> {

    private val stateHolder = Mate(
        initState = WordCardState(),
        initEffects = setOf(DatasourceEffect.LoadWord(wordId)),
        coroutineScope = viewModelScope,
        reducer = WordCardReducer(),
        effectHandlerSet = setOf(
            DatasourceEffectHandler(wordCardUseCase = wordCardUseCase),
            UiEffectHandler()
        )
    )

    override val state: StateFlow<WordCardState>
        get() = stateHolder.state

    override fun accept(message: Msg) = stateHolder.accept(message)

    class Factory(
        private val wordId: Long,
        private val wordCardUseCase: WordCardUseCase,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return WordCardViewModel(wordId, wordCardUseCase) as T
        }
    }
}
```

## Чеклист инициализации Mate

| Параметр | Описание | Пример |
|----------|----------|--------|
| `initState` | Дефолтный стейт | `WordCardState()` |
| `initEffects` | Эффекты сразу при запуске | `setOf(DatasourceEffect.LoadWord(id))` |
| `coroutineScope` | Scope жизненного цикла | `viewModelScope` |
| `reducer` | Чистый трансформер стейта | `WordCardReducer()` |
| `effectHandlerSet` | Все эффект-хендлеры | `setOf(DatasourceEH, UiEH)` |

## Создание ViewModel в Composable

```kotlin
@Composable
fun WordCardScreen(
    wordId: Long,
    wordCardUseCase: WordCardUseCase,
    onBackPress: () -> Unit,
    viewModel: WordCardViewModel = viewModel(
        factory = WordCardViewModel.Factory(wordId, wordCardUseCase)
    ),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    WordCardScreen(state, onBackPress) { viewModel.accept(it) }
}
```

## Поток зависимостей

```
AppComponent (Dagger)
    |
    | предоставляет UseCase, ResourceManager, PrefsProvider, Logger
    v
Navigation (MainRouter/RootRouter)
    |
    | передаёт зависимости как параметры composable
    v
Screen Composable (публичная точка входа)
    |
    | создаёт ViewModelFactory с зависимостями
    v
ViewModel
    |
    | передаёт зависимости в Mate, Reducer, EffectHandlers
    v
Mate (TEA цикл запущен)
```

## Полный пример: добавление нового экрана

### Шаг 1: Определить State, Messages, Effects

```kotlin
// logic/State.kt
@Stable
data class NewFeatureState(
    val isLoading: Boolean = true,
    val data: String = "",
    val snackbarState: SnackbarState = SnackbarState(),
)

fun NewFeatureState.stopLoading() = copy(isLoading = false)
fun NewFeatureState.setData(value: String) = copy(data = value, isLoading = false)
```

```kotlin
// logic/Message.kt
sealed interface Msg {
    data object LoadData : Msg
    data class DataLoaded(val data: String) : Msg
    data object Empty : Msg
}
```

### Шаг 2: Реализовать Reducer

```kotlin
// logic/Reducer.kt
class NewFeatureReducer : MateReducer<NewFeatureState, Msg, Effect> {
    override fun reduce(state: NewFeatureState, message: Msg): ReducerResult<NewFeatureState, Effect> {
        return when (message) {
            is Msg.LoadData -> state to setOf(DatasourceEffect.FetchData)
            is Msg.DataLoaded -> state.setData(message.data) to setOf()
            is Msg.Empty -> state to emptySet()
        }
    }
}
```

### Шаг 3: Реализовать эффект-хендлеры

```kotlin
// logic/DatasourceEffectHandler.kt
internal sealed interface DatasourceEffect : Effect {
    data object FetchData : DatasourceEffect
}

internal class DatasourceEffectHandler(
    private val useCase: NewFeatureUseCase,
) : MateEffectHandler<Msg, Effect> {
    override suspend fun runEffect(effect: Effect, consumer: (Msg) -> Unit) {
        when (val eff = effect as? DatasourceEffect) {
            is DatasourceEffect.FetchData -> {
                withContext(Dispatchers.IO) {
                    useCase.getData().let { Msg.DataLoaded(it) }
                }
            }
            null -> Msg.Empty
        }.let(consumer)
    }
}
```

### Шаг 4: Создать ViewModel

```kotlin
// NewFeatureViewModel.kt
class NewFeatureViewModel(
    useCase: NewFeatureUseCase,
) : ViewModel(), MateStateHolder<NewFeatureState, Msg> {

    private val stateHolder = Mate(
        initState = NewFeatureState(),
        initEffects = setOf(DatasourceEffect.FetchData),
        coroutineScope = viewModelScope,
        reducer = NewFeatureReducer(),
        effectHandlerSet = setOf(DatasourceEffectHandler(useCase))
    )

    override val state = stateHolder.state
    override fun accept(message: Msg) = stateHolder.accept(message)

    class Factory(private val useCase: NewFeatureUseCase) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            NewFeatureViewModel(useCase) as T
    }
}
```

### Шаг 5: Создать Screen Composable

```kotlin
// NewFeatureScreen.kt
@Composable
fun NewFeatureScreen(
    useCase: NewFeatureUseCase,
    viewModel: NewFeatureViewModel = viewModel(
        factory = NewFeatureViewModel.Factory(useCase)
    ),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    NewFeatureScreen(state) { viewModel.accept(it) }
}

@Composable
internal fun NewFeatureScreen(
    state: NewFeatureState,
    sendMessage: (Msg) -> Unit,
) {
    // Чистый UI
}
```

### Шаг 6: Зарегистрировать в DI

```kotlin
// di/NewFeatureModule.kt
@Module
interface NewFeatureModule {
    @Binds
    fun bindUseCase(impl: NewFeatureUseCaseImpl): NewFeatureUseCase
}
```

Добавить в `AppComponent`:
```kotlin
@Component(modules = [..., NewFeatureModule::class])
interface AppComponent {
    fun getNewFeatureUseCase(): NewFeatureUseCase
}
```

### Шаг 7: Добавить навигационный маршрут

В соответствующем роутере добавить composable destination с зависимостями из `appComponent`.
