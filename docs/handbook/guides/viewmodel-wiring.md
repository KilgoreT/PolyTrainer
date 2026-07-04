# Подключение ViewModel

ViewModel в проекте инжектятся через `@AssistedInject` + `@AssistedFactory`. Это единственный паттерн — даже если runtime аргументов нет, `Navigator` приходит через `@Assisted`.

## ViewModel

```kotlin
class WordCardViewModel @AssistedInject constructor(
    @Assisted wordId: Long,
    @Assisted navigator: WordCardNavigator,
    datasourceHandler: DatasourceEffectHandler,
    uiHandler: UiEffectHandler,
    navHandlerFactory: WordCardNavigationEffectHandler.Factory,
) : ViewModel(), MateStateHolder<WordCardState, Msg> {

    private val stateHolder = Mate(
        initState = WordCardState(),
        initEffects = setOf(DatasourceEffect.LoadWord(wordId)),
        coroutineScope = viewModelScope,
        reducer = WordCardReducer(),
        effectHandlerSet = setOf(
            datasourceHandler,
            uiHandler,
            navHandlerFactory.create(navigator),
        )
    )

    override val state: StateFlow<WordCardState> = stateHolder.state
    override fun accept(message: Msg) = stateHolder.accept(message)

    @AssistedFactory
    interface Factory {
        fun create(wordId: Long, navigator: WordCardNavigator): WordCardViewModel
    }
}
```

- `@Assisted` — для runtime аргументов: Navigator, id экрана, edit-режим. Передаются в `Factory.create(...)`.
- Обычные параметры конструктора — приходят из Dagger графа.
- `navHandlerFactory.create(navigator)` — `MateNavigationEffectHandler` тоже AssistedInject, потому что принимает Navigator. Composable передаёт Navigator один раз — в Factory создания ViewModel.

## Handlers

DataSource / UI / Flow handlers — `@Inject constructor` (обычная инъекция, без `@Assisted`):

```kotlin
class DatasourceEffectHandler @Inject constructor(
    private val useCase: WordCardUseCase,
) : MateTypedEffectHandler<Msg, DatasourceEffect>() {
    override fun filter(effect: Effect): DatasourceEffect? = effect as? DatasourceEffect
    override suspend fun onEffect(effect: DatasourceEffect, consumer: (Msg) -> Unit) { /* ... */ }
}
```

NavigationEffectHandler — `@AssistedInject`, потому что принимает Navigator:

```kotlin
class WordCardNavigationEffectHandler @AssistedInject constructor(
    @Assisted navigator: WordCardNavigator,
) : MateNavigationEffectHandler<Msg>(navigator) {
    override suspend fun onScreenEffect(effect: NavigationEffect) { /* ... */ }

    @AssistedFactory
    interface Factory {
        fun create(navigator: WordCardNavigator): WordCardNavigationEffectHandler
    }
}
```

## AppComponent

Каждая ViewModel экспонирует свою Factory через явный getter:

```kotlin
interface AppComponent {
    fun getWordCardViewModelFactory(): WordCardViewModel.Factory
    fun getChatViewModelFactory(): ChatViewModel.Factory
    // ...
}
```

Биндинга в feature модуле для ViewModel **не нужно** — `@AssistedInject` + `@AssistedFactory` создаёт всё автоматически.

## DI модуль фичи

Только UseCase биндинг:

```kotlin
@Module
interface WordCardModule {
    @Binds
    fun bindUseCase(impl: WordCardUseCaseImpl): WordCardUseCase
}
```

## Composable

Принимает `factory: XxxViewModel.Factory` + `navigator: XxxNavigator` параметрами. ViewModel создаётся через `viewModelFactory { ... }` helper из `core/di`:

```kotlin
@Composable
fun WordCardScreen(
    wordId: Long,
    factory: WordCardViewModel.Factory,
    navigator: WordCardNavigator,
    viewModel: WordCardViewModel = viewModel(
        key = "wordCard_$wordId",
        factory = viewModelFactory { factory.create(wordId, navigator) },
    ),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    WordCardScreen(
        state = state,
        sendMessage = { viewModel.accept(it) },
    )
}
```

- `key = ...` нужен только если экран принимает id-аргумент и должен пересоздаваться при его смене.
- `viewModelFactory { ... }` — helper в `me.apomazkin.di`.
- Composable НЕ принимает `onBackPress` callback. Системная back — через `BackHandler { sendMessage(Msg.RequestBack) }` во внутреннем composable.

## CompositionRootImpl

Единственное место, где Factory из Dagger встречается с навигационными callbacks из RootRouter:

```kotlin
class CompositionRootImpl(
    private val wordCardViewModelFactory: WordCardViewModel.Factory,
    // ... остальные Factory
) : CompositionRoot {
    @Composable
    override fun WordCardScreenDep(wordId: Long, onBackPress: () -> Unit) {
        val navigator = remember(onBackPress) { WordCardNavigatorImpl(onBackPress) }
        WordCardScreen(
            wordId = wordId,
            factory = wordCardViewModelFactory,
            navigator = navigator,
        )
    }
}
```

`remember(onBackPress)` обязателен — без него NavigatorImpl пересоздаётся на каждой рекомпозиции, ломая стабильность параметра `navigator`.

## MainRouter / RootRouter

Composable в navigation graph достаёт Factory из `appComponent` и передаёт в CompositionRootImpl или прямо в screen:

```kotlin
composable(RootPoint.DICTIONARY_LIST.route) {
    val listNavigator = remember(navController) {
        ListNavigatorImpl(navController, onExit = onExitApp)
    }
    DictionaryListScreen(
        factory = context.appComponent.getDictionaryListViewModelFactory(),
        navigator = listNavigator,
    )
}
```

## Добавление нового экрана: чек-лист

1. **Создать UseCase** (если нужен) — интерфейс в screen модуле, реализация в app, `@Binds` в feature модуле.

2. **Создать Navigator** — `interface XxxNavigator : Navigator { fun openYyy(...) }` в screen модуле.

3. **Создать sealed `XxxNavigationEffect : NavigationEffect`** (если есть свои навигационные эффекты помимо `Back`).

4. **Создать handlers:**
   - `DatasourceEffectHandler @Inject constructor` extends `MateTypedEffectHandler<Msg, EffectType>`
   - `XxxNavigationEffectHandler @AssistedInject constructor(@Assisted navigator)` extends `MateNavigationEffectHandler<Msg>(navigator)` + `@AssistedFactory`

5. **ViewModel:**
   - `@AssistedInject constructor(@Assisted navigator, ..., navHandlerFactory: XxxNavigationEffectHandler.Factory)`
   - `@AssistedFactory interface Factory { fun create(navigator: XxxNavigator): XxxViewModel }`

6. **Composable:**
   - Параметры: `factory: XxxViewModel.Factory, navigator: XxxNavigator`
   - `viewModel(factory = viewModelFactory { factory.create(navigator) })`
   - `BackHandler { sendMessage(Msg.RequestBack) }` во внутреннем composable

7. **NavigatorImpl** в `app/.../navigator/`:
   - `class XxxNavigatorImpl(navController, callbacks): XxxNavigator`
   - `navController.navigate(...) { launchSingleTop = true }`

8. **DI:**
   - `@Binds` UseCase в feature module (если есть)
   - `fun getXxxViewModelFactory(): XxxViewModel.Factory` на AppComponent
   - Добавить параметр в `CompositionRootImpl` (если экран в табе) и `MainRouter` (или использовать напрямую в `RootRouter`)
   - В `CompositionRootImpl`: создать Navigator через `remember(...)` и передать в Screen

9. **Тесты:**
   - Reducer: проверять `NavigationEffect.Back` / `XxxNavigationEffect.OpenYyy` через `assertSingleEffect`
   - Handlers: Navigator интерфейс легко мокается

## Что НЕ делать

- **Не создавать ручной `ViewModelProvider.Factory`** — только AssistedFactory от Dagger.
- **Не использовать `@IntoMap @ViewModelKey`** — устаревший паттерн, удалён из проекта.
- **Не принимать `onBackPress: () -> Unit` в screen composable** — навигация через `BackHandler` + `Msg.RequestBack` → reducer → `NavigationEffect.Back`.
- **Не достать `appComponent` внутри screen composable** — Factory приходит параметром через CompositionRoot.
- **Не хранить `closeScreen: Boolean` в state** — навигация это эффект, не флаг.
