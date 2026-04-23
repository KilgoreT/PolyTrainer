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

## Конвенции

1. **Route-строки** — lowercase, без пробелов: `"vocabulary"`, `"wordCard/{wordId}"`.
2. **Аргументы** — через `navArgument` с явным типом: `NavType.LongType`, `NavType.StringType`.
3. **Навигационные функции** — private extensions на `NavController`: `goToWordCard()`, `backPress()`.
4. **Зависимости** — через `MainUiDeps`, не напрямую из `appComponent` в composable.
5. **popBackStack()** — для возврата назад, не `navigateUp()`.
