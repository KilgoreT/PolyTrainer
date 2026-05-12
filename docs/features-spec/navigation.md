# Spec: Навигация

## Принципы

### 1. Навигация — это side effect

Навигация — побочный эффект, не state. Описывается через `NavigationEffect`, обрабатывается отдельным NavigationEffectHandler, дёргает `Navigator` интерфейс. Не смешивается с datasource/UI эффектами.

### 2. Reducer возвращает Effect

Reducer — чистая функция. Не вызывает навигацию напрямую. Возвращает `NavigationEffect` который потом обрабатывает handler.

### 3. Navigator — абстракция над NavController

Screen модуль не знает про Compose Navigation. Видит только интерфейс `Navigator` (или его наследника `XxxNavigator`). Реализация — в `app/.../navigator/`.

### 4. Composable не получает navigation callbacks

Composable принимает `factory: XxxViewModel.Factory` + `navigator: XxxNavigator`. Все навигационные msg идут через `viewModel.accept(...)` → Reducer → NavigationEffect → Navigator.

---

## Базовый интерфейс

`modules/core/mate/.../NavigationEffect.kt`:

```kotlin
interface NavigationEffect : Effect {
    /** Назад (закрыть текущий экран) */
    data object Back : NavigationEffect
}
```

`Back` — единственный универсальный эффект. `ExitApp` — НЕ в базовом: не все экраны могут закрыть приложение (вложенные не могут). Per-screen экран добавляет `ExitApp` в свой sealed (Splash, DictionaryList).

`modules/core/mate/.../Navigator.kt`:

```kotlin
interface Navigator {
    fun back()
}
```

Базовый Navigator. Screen модуль расширяет:

```kotlin
interface ListNavigator : Navigator {     // back() наследуется
    fun exit()
    fun openEdit(id: Long)
}
```

---

## Структура для экрана

### Шаг 1: Sealed effect (если есть свои эффекты помимо Back)

`modules/screen/dictionary/.../list/ListNavigationEffect.kt`:

```kotlin
sealed interface ListNavigationEffect : NavigationEffect {
    data object ExitApp : ListNavigationEffect           // root экран
    data class OpenEdit(val id: Long) : ListNavigationEffect
}
```

### Шаг 2: Navigator интерфейс

`modules/screen/dictionary/.../list/ListNavigator.kt`:

```kotlin
interface ListNavigator : Navigator {
    fun exit()
    fun openEdit(id: Long)
    fun openCreate()
}
```

### Шаг 3: NavigationEffectHandler — наследует базовый

`modules/screen/dictionary/.../list/ListNavigationEffectHandler.kt`:

```kotlin
class ListNavigationEffectHandler @AssistedInject constructor(
    @Assisted private val listNavigator: ListNavigator,
) : MateNavigationEffectHandler<DictionaryListMsg>(listNavigator) {

    override suspend fun onScreenEffect(effect: NavigationEffect) {
        when (effect) {
            is ListNavigationEffect.ExitApp -> listNavigator.exit()
            is ListNavigationEffect.OpenEdit -> listNavigator.openEdit(effect.id)
            is ListNavigationEffect.OpenCreate -> listNavigator.openCreate()
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(navigator: ListNavigator): ListNavigationEffectHandler
    }
}
```

`Back` обрабатывается один раз в базовом `MateNavigationEffectHandler`. Дочерний пишет только `onScreenEffect` со своими эффектами.

### Шаг 4: Reducer возвращает эффект

```kotlin
is DictionaryListMsg.OpenNewDictionary -> state to setOf(
    ListNavigationEffect.OpenCreate
)

is DictionaryListMsg.EditDictionary -> state to setOf(
    ListNavigationEffect.OpenEdit(message.id)
)

is DictionaryListMsg.RequestBack -> if (state.dictionaries.isEmpty()) {
    state to setOf(ListNavigationEffect.ExitApp)
} else {
    state to setOf(NavigationEffect.Back)
}
```

Reducer контролирует логику — может выбирать `ExitApp` vs `Back` по state.

### Шаг 5: ViewModel — Navigator через `@Assisted`

```kotlin
class DictionaryListViewModel @AssistedInject constructor(
    @Assisted navigator: ListNavigator,
    datasourceHandler: DictionaryListEffectHandler,
    flowHandler: DictionaryListFlowHandler,
    navHandlerFactory: ListNavigationEffectHandler.Factory,
) : ViewModel(), MateStateHolder<...> {

    private val stateHolder = Mate(
        reducer = DictionaryListReducer(),
        effectHandlerSet = setOf(
            datasourceHandler,
            flowHandler,
            navHandlerFactory.create(navigator),
        )
    )

    @AssistedFactory
    interface Factory {
        fun create(navigator: ListNavigator): DictionaryListViewModel
    }
}
```

DatasourceHandler/FlowHandler — `@Inject`, приходят готовыми. Navigator — `@Assisted` runtime параметр.

### Шаг 6: Composable — factory + navigator

```kotlin
@Composable
fun DictionaryListScreen(
    factory: DictionaryListViewModel.Factory,
    navigator: ListNavigator,
    viewModel: DictionaryListViewModel = viewModel(
        factory = viewModelFactory { factory.create(navigator) },
    ),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    BackHandler { viewModel.accept(DictionaryListMsg.RequestBack) }
    // ...
}
```

### Шаг 7: NavigatorImpl — в `app/.../navigator/`

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
    override fun openCreate() {
        navController.navigate("DICTIONARY_CREATE") {
            launchSingleTop = true
        }
    }
}
```

### Шаг 8: Navigation graph создаёт NavigatorImpl

```kotlin
composable(RootPoint.DICTIONARY_LIST.route) {
    val listNavigator = remember(navController) {
        ListNavigatorImpl(
            navController = navController,
            onExit = onExitApp,
        )
    }
    DictionaryListScreen(
        factory = context.appComponent.getDictionaryListViewModelFactory(),
        navigator = listNavigator,
    )
}
```

`remember(navController)` обязателен — иначе `NavigatorImpl` пересоздаётся при каждой рекомпозиции, Compose видит параметр `navigator` нестабильным, screen перерисовывается зря.

---

## Поток данных

```
UI event (click)
  → ViewModel.accept(Msg.EditDictionary(id))
  → Reducer returns ListNavigationEffect.OpenEdit(id)
  → ListNavigationEffectHandler.onScreenEffect(effect)
  → listNavigator.openEdit(id)
  → ListNavigatorImpl: navController.navigate("DICTIONARY_CREATE?editId=$id")
```

---

## Конвенции

1. **Один Navigator на экран.** `XxxNavigator : Navigator` — все методы только для одного экрана.
2. **Sealed NavigationEffect** в screen модуле. Не использовать "общий" sealed на несколько экранов.
3. **NavigatorImpl без логики.** Одна строка на метод — `navController.navigate(...)` или callback. Логика (debounce, conditional) — в reducer.
4. **`launchSingleTop = true`** во всех `navigate()` — фиксит баг двойного тапа.
5. **`remember(navController)`** при создании NavigatorImpl в composable — стабильность для Compose.
6. **AssistedInject для handler и ViewModel** — Navigator runtime параметр.
7. **`@Inject` для DatasourceHandler/FlowHandler/UiHandler** — UseCase'ы из графа.
8. **Удалять `@IntoMap @ViewModelKey` биндинг** мигрированных ViewModel — теперь только через `AssistedFactory`.

---

## Cross-graph навигация (Tab → Root)

Tab экран может навигировать на двух уровнях:
- Внутри своего таба — `tabsNavController.navigate(...)` через NavigatorImpl
- На root уровень (открыть DictionaryCreate) — callback от RootRouter

```kotlin
interface VocabularyNavigator : Navigator {
    fun openWordCard(id: Long)              // internal — tabsNavController
    fun openDictionaryCreate()              // cross-graph — callback от root
}

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

MainScreen инкапсулирован — не знает про `rootNavController`, только callbacks (как раньше).

### AppBar shared widget

`DictionaryAppBar` используется в 3 табах с идентичной навигацией. Один Navigator на всё:

```kotlin
interface DictionaryAppBarNavigator : Navigator {
    fun openDictionaryCreate()
}

class DictionaryAppBarNavigatorImpl(
    private val onOpenDictionaryCreate: () -> Unit,
) : DictionaryAppBarNavigator {
    override fun back() {}                                   // no-op для shared widget
    override fun openDictionaryCreate() = onOpenDictionaryCreate()
}
```
