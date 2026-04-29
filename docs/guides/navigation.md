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
fun MainScreen(mainUiDeps: MainUiDeps, openAddDict: () -> Unit) {
    val navController = rememberNavController()
    Column(modifier = Modifier.fillMaxSize()) {
        NavHost(
            modifier = Modifier.weight(1F),
            navController = navController,
            startDestination = TabPoint.VOCABULARY.route,
        ) {
            vocabulary(mainUiDeps, navController, openAddDict)
            quiz(mainUiDeps, navController, openAddDict)
            composable(TabPoint.STATS.route) { ... }
            settings(mainUiDeps, navController)
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
    mainUiDeps: MainUiDeps,
    navController: NavController,
    openAddDict: () -> Unit,
) {
    composable(TabPoint.VOCABULARY.route) {
        mainUiDeps.VocabularyTabDep(
            openAddDict = openAddDict,
            openWordCard = { wordId -> navController.goToWordCard(wordId) }
        )
    }
    composable(
        route = "wordCard/{wordId}",
        arguments = listOf(navArgument("wordId") { type = NavType.LongType })
    ) { backStackEntry ->
        val wordId = backStackEntry.arguments?.getLong("wordId") ?: return@composable
        mainUiDeps.WordCardScreenDep(
            wordId = wordId,
            onBackPress = { navController.popBackStack() }
        )
    }
}

private fun NavController.goToWordCard(wordId: Long) {
    navigate("wordCard/$wordId")
}
```

## Передача зависимостей через MainUiDeps

Интерфейс `MainUiDeps` предоставляет composable-провайдеры для каждого экрана:

```kotlin
interface MainUiDeps {
    @Composable fun VocabularyTabDep(openAddDict: () -> Unit, openWordCard: (Long) -> Unit)
    @Composable fun WordCardScreenDep(wordId: Long, onBackPress: () -> Unit)
    @Composable fun QuizTabScreenDep(openAddDict: () -> Unit, openChatQuiz: (String) -> Unit)
    @Composable fun ChatQuizScreenDep(onBackPress: () -> Unit)
    // ...
}
```

Реализация в `MainRouter` получает зависимости из `appComponent` и пробрасывает в `MainUiDeps`.

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

Нет явного `Activity.finish()`. Выход через очистку back stack:

```kotlin
onExit = {
    navController.popBackStack(RootPoint.SPLASH.route, inclusive = true)
}
```

При пустом стеке Android закрывает Activity. Используется когда пользователь удалил все данные и возвращаться некуда.

В composable — `BackHandler` для перехвата системной кнопки "назад":

```kotlin
BackHandler {
    if (state.dictionaries.isEmpty()) {
        onExit()
    } else {
        onBackPress?.invoke()
    }
}
```

## Конвенции

1. **Route-строки** — UPPER_CASE для root routes (`DICTIONARY_CREATE`), lowercase для tab routes (`vocabulary`).
2. **Аргументы** — через `navArgument` с явным типом: `NavType.LongType`, `NavType.StringType`. Не `NavType.BoolType`.
3. **Навигационные функции** — private extensions на `NavController`: `goToWordCard()`, `backPress()`.
4. **Зависимости** — через `MainUiDeps`, не напрямую из `appComponent` в composable.
5. **popBackStack()** — для возврата назад, не `navigateUp()`.
6. **Nullable `onBackPress`** — `null` = без AppBar (onboarding), не null = с AppBar.
7. **Один контекст = один route.** Разное поведение → разные route, не параметры.
