# Навигация

## Иерархия навигации

```
RootRouter (NavHost)
├── SPLASH
├── CREATE_DICTIONARY
└── MAIN_ROUTER
    └── MainScreen (внутренний NavHost)
        ├── VOCABULARY (таб)
        │   ├── vocabulary        — список слов
        │   └── wordCard/{wordId} — карточка слова
        ├── QUIZ (таб)
        │   ├── quiz              — выбор квиза
        │   └── quiz/{quizType}   — чат-квиз
        ├── STATS (таб)
        │   └── statistic
        └── SETTINGS (таб)
            ├── settings
            └── about_app
```

## Два уровня NavHost

### Уровень 1: RootRouter (app модуль)

Управляет flow: splash → создание словаря → основной экран.

```kotlin
// route/RootRouter.kt
NavHost(
    navController = navController,
    startDestination = RootPoint.SPLASH.route,
) {
    composable(RootPoint.SPLASH.route) {
        SplashScreen(onComplete = { openMainScreen() })
    }
    composable(RootPoint.CREATE_DICTIONARY.route) {
        CreateDictionaryScreen(onComplete = { openMainScreen() })
    }
    composable(RootPoint.MAIN_ROUTER.route) {
        MainRouter(appComponent)
    }
}
```

### Уровень 2: MainScreen (модуль main)

Табы + вложенная навигация внутри табов.

```kotlin
// MainScreen.kt
@Composable
fun MainScreen(
    compositionRoot: CompositionRoot,
    openDictionaryCreate: () -> Unit,
    openDictionaryList: () -> Unit,
) {
    val navController = rememberNavController()
    Column(modifier = Modifier.fillMaxSize()) {
        NavHost(
            modifier = Modifier.weight(1F),
            navController = navController,
            startDestination = TabPoint.VOCABULARY.route,
        ) {
            vocabulary(navController, compositionRoot, openDictionaryCreate)
            quiz(navController, compositionRoot, openDictionaryCreate)
            composable(TabPoint.STATS.route) {
                compositionRoot.StatisticTabScreenDep(openDictionaryCreate)
            }
            settings(navController, compositionRoot, openDictionaryList)
        }
        BottomBarWidget(navController = navController)
    }
}
```

## Определение маршрутов

Маршруты определяются через enum:

```kotlin
enum class TabPoint(val route: String) {
    VOCABULARY("vocabulary"),
    QUIZ("quiz"),
    STATS("statistic"),
    SETTINGS("settings"),
}
```

## Вложенная навигация внутри таба

Каждый таб регистрирует свои маршруты через extension-функции на `NavGraphBuilder`:

```kotlin
// Vocabulary.kt
fun NavGraphBuilder.vocabulary(
    compositionRoot: CompositionRoot,
    navController: NavController,
    openDictionaryCreate: () -> Unit,
) {
    composable(TabPoint.VOCABULARY.route) {
        compositionRoot.VocabularyTabDep(
            openDictionaryCreate = openDictionaryCreate,
            openWordCard = { wordId -> navController.goToWordCard(wordId) }
        )
    }
    composable(
        route = "wordCard/{wordId}",
        arguments = listOf(navArgument("wordId") { type = NavType.LongType })
    ) { backStackEntry ->
        val wordId = backStackEntry.arguments?.getLong("wordId") ?: return@composable
        compositionRoot.WordCardScreenDep(
            wordId = wordId,
            onBackPress = { navController.popBackStack() }
        )
    }
}

private fun NavController.goToWordCard(wordId: Long) {
    navigate("wordCard/$wordId") {
        launchSingleTop = true
    }
}
```

`CompositionRoot` хост (см. `CompositionRootImpl` в app модуле) принимает callbacks и оборачивает их в `XxxNavigatorImpl`, который передаётся в `XxxScreen` параметром `navigator`. Сам screen уже не видит callbacks — только Navigator.

## Передача зависимостей через CompositionRoot

Интерфейс `CompositionRoot` предоставляет composable-провайдеры для каждого экрана:

```kotlin
interface CompositionRoot {
    @Composable fun VocabularyTabDep(openAddDict: () -> Unit, openWordCard: (Long) -> Unit)
    @Composable fun WordCardScreenDep(wordId: Long, onBackPress: () -> Unit)
    @Composable fun QuizTabScreenDep(openAddDict: () -> Unit, openChatQuiz: (String) -> Unit)
    @Composable fun ChatQuizScreenDep(onBackPress: () -> Unit)
    // ...
}
```

Реализация в `MainRouter` получает зависимости из `appComponent` и пробрасывает в `CompositionRoot`.

## Управление back stack при переключении табов

```kotlin
// BottomBarWidget
navController.navigate(tab.route) {
    popUpTo(navController.graph.findStartDestination().id) {
        saveState = true
    }
    launchSingleTop = true
    restoreState = true
}
```

`saveState / restoreState` сохраняет scroll-позицию и состояние каждого таба.

## Несколько route на один экран

Когда один composable используется в разных контекстах с разным поведением — отдельный route на каждый контекст. Не boolean параметр в route.

```kotlin
DICTIONARY_SETUP("DICTIONARY_SETUP"),    // onboarding — без AppBar, onClose = openMainScreen
DICTIONARY_CREATE("DICTIONARY_CREATE"),  // из dropdown/списка — с AppBar, onClose = popBackStack
```

Каждый route — свой composable с разными callback'ами:

```kotlin
composable(RootPoint.DICTIONARY_SETUP.route) {
    DictionaryFormScreen(
        onClose = { navigator?.openMainScreen() },
        // onBackPress = null → без AppBar
    )
}
composable(RootPoint.DICTIONARY_CREATE.route) {
    DictionaryFormScreen(
        onClose = { navController.popBackStack() },
        onBackPress = { navController.popBackStack() },
    )
}
```

## Разделение callback'ов навигации

Если разные точки входа ведут на разные экраны — отдельные callback'ы через всю цепочку:

```kotlin
// RootRouter → MainRouter → MainScreen → табы
openDictionaryCreate: () -> Unit,  // dropdown в AppBar → экран создания
openDictionaryList: () -> Unit,    // настройки → экран списка
```

Не один `openDictionaryManagement` на всё.

## Выход из приложения

`ExitApp` — НЕ в базовом `NavigationEffect`. Не все экраны могут закрыть приложение. Per-screen экран (Splash, DictionaryList) добавляет `ExitApp` в свой sealed effect и `exit()` в свой Navigator.

`NavigatorImpl` принимает `onExit: () -> Unit` callback, который дёргает `activity.finish()` (берётся из `LocalActivity.current` в navigation graph):

```kotlin
class ListNavigatorImpl(
    private val navController: NavController,
    private val onExit: () -> Unit,
) : ListNavigator {
    override fun exit() = onExit()
    // ...
}
```

Conditional навигация — в reducer, а не в composable. Reducer проверяет state и решает, какой effect эмитить:

```kotlin
is Msg.RequestBack -> if (state.dictionaries.isEmpty()) {
    state to setOf(ListNavigationEffect.ExitApp)
} else {
    state to setOf(NavigationEffect.Back)
}
```

`BackHandler` в composable отправляет один `Msg.RequestBack` — без логики:

```kotlin
BackHandler { viewModel.accept(DictionaryListMsg.RequestBack) }
```

## Navigator паттерн

Screen модуль не получает navigation callbacks напрямую. Видит интерфейс `XxxNavigator : Navigator`, реализацию (`XxxNavigatorImpl`) создаёт `app/.../navigator/`.

```kotlin
// modules/screen/dictionary/list/ListNavigator.kt
interface ListNavigator : Navigator {
    fun exit()
    fun openEdit(id: Long)
    fun openCreate()
}

// app/.../navigator/ListNavigatorImpl.kt
class ListNavigatorImpl(
    private val navController: NavController,
    private val onExit: () -> Unit,
) : ListNavigator {
    override fun back() = navController.popBackStack().let {}
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

В composable Navigator создаётся через `remember(navController)`:

```kotlin
composable(RootPoint.DICTIONARY_LIST.route) {
    val listNavigator = remember(navController) {
        ListNavigatorImpl(navController, onExit = onExitApp)
    }
    DictionaryListScreen(
        factory = appComponent.getDictionaryListViewModelFactory(),
        navigator = listNavigator,
    )
}
```

`remember(navController)` обязателен — без него NavigatorImpl пересоздаётся при каждой рекомпозиции, что делает параметр screen-а нестабильным.

Подробнее — `docs/features-spec/navigation.md` и `docs/guides/effect-handlers.md`.

## Cross-graph навигация (Tab → Root)

Tab экран может навигировать на двух уровнях:
- Внутри своего таба — `tabsNavController.navigate(...)` через NavigatorImpl
- На root уровень — callback от RootRouter (`onOpenDictionaryCreate`, `onOpenDictionaryList`)

```kotlin
class VocabularyNavigatorImpl(
    private val tabsNavController: NavController,
    private val onOpenDictionaryCreate: () -> Unit,
) : VocabularyNavigator {
    override fun back() = tabsNavController.popBackStack().let {}
    override fun openDictionaryCreate() = onOpenDictionaryCreate()
    override fun openWordCard(id: Long) {
        tabsNavController.navigate("wordCard/$id") {
            launchSingleTop = true
        }
    }
}
```

MainScreen инкапсулирован — не знает про rootNavController, только callbacks (как раньше).

### Shared widget Navigator

`DictionaryAppBar` используется в 3 табах с идентичной навигацией. Один `DictionaryAppBarNavigator` на все табы — создаётся в `VocabularyTabDep` / `QuizTabScreenDep` / `StatisticTabScreenDep` каждый раз с одним и тем же `onOpenDictionaryCreate` callback.

## Конвенции

1. **Route-строки** — UPPER_CASE для root routes (`DICTIONARY_CREATE`), lowercase для tab routes (`vocabulary`).
2. **Аргументы** — через `navArgument` с явным типом: `NavType.LongType`, `NavType.StringType`. Не `NavType.BoolType`.
3. **Навигационные функции** — private extensions на `NavController`: `goToWordCard()`, `backPress()`.
4. **Зависимости** — через `CompositionRoot`, не напрямую из `appComponent` в composable.
5. **popBackStack()** — для возврата назад, не `navigateUp()`.
6. **`launchSingleTop = true`** во всех `navigate()` — фиксит баг двойного тапа.
7. **`remember(navController)`** при создании NavigatorImpl — стабильность для Compose.
8. **Один контекст = один route.** Разное поведение → разные route, не параметры.
