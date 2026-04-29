# Решение — два режима экрана + переименование

**Выбран вариант B: два отдельных route.**

## Обоснование

- Соответствует существующей конвенции проекта: Settings → AboutApp = отдельный route + `onBackPress` callback
- В проекте нет `NavType.BoolType` — не вводим новый паттерн
- Management режим: `popBackStack()` — проще и надёжнее чем `popUpTo(SPLASH)` + if/else
- Process death: симметричное поведение для обоих route

## Routes

```kotlin
enum class RootPoint(val route: String) {
    SPLASH("SPLASH"),
    DICTIONARY_SETUP("DICTIONARY_SETUP"),
    DICTIONARY_MANAGEMENT("DICTIONARY_MANAGEMENT"),
    MAIN_ROUTER("MAIN_ROUTER"),
}

// Setup (из splash):
composable(RootPoint.DICTIONARY_SETUP.route) {
    DictionaryScreen(
        onClose = { navigator?.openMainScreen() }
    )
}

// Management (из настроек/dropdown):
composable(RootPoint.DICTIONARY_MANAGEMENT.route) {
    DictionaryScreen(
        onClose = { navController.popBackStack() },
        onBackPress = { navController.popBackStack() }
    )
}

// Навигация из splash:
navigator?.openDictionarySetup()  // popUpTo(SPLASH) inclusive

// Навигация из mainRouter:
openDictionaryManagement = { navController.navigate(DICTIONARY_MANAGEMENT) }
```

## Рефакторинг экрана

### Два callback'а

```kotlin
@Composable
fun DictionaryScreen(
    dictionaryUseCase: DictionaryUseCase,
    onClose: () -> Unit,
    onBackPress: (() -> Unit)? = null,
)
```

- `onClose` — действие завершено (словарь создан, `state.needClose == true`)
- `onBackPress` — пользователь нажал назад без создания
- Наличие `onBackPress` определяет наличие AppBar

### Виджет AppBar

Отдельный виджет по конвенции проекта:

```kotlin
// widget/DictionaryAppBar.kt
@Composable
fun DictionaryAppBar(
    onBackPress: () -> Unit,
) {
    TopAppBar(
        navigationIcon = {
            IconBoxed(
                iconRes = R.drawable.ic_back,
                enabled = true,
                colorEnabled = enableIconColor,
                size = 44,
                onClick = onBackPress,
            )
        },
        title = {
            Text(
                text = stringResource(R.string.dictionary_selection_title),
                style = LexemeStyle.H5,
            )
        }
    )
}
```

### Internal composable

```kotlin
@Composable
internal fun DictionaryScreen(
    state: DictionaryState,
    onClose: () -> Unit,
    onBackPress: (() -> Unit)?,
    sendMsg: (Msg) -> Unit,
) {
    Scaffold(
        topBar = {
            onBackPress?.let { DictionaryAppBar(onBackPress = it) }
        }
    ) { paddings ->
        Box(modifier = Modifier.padding(paddings)) {
            // существующий контент
        }
    }
}
```

## Переименования

### Модуль

`modules/screen/createdictionary` → `modules/screen/dictionary`

Затрагивает: `settings.gradle.kts`, namespace в `build.gradle.kts`, все `project(":modules:screen:createdictionary")` зависимости.

### Классы

| Было | Стало |
|------|-------|
| `CreateDictionaryScreen` | `DictionaryScreen` |
| `CreateDictionaryViewModel` | `DictionaryViewModel` |
| `CreateDictionaryReducer` | `DictionaryReducer` |
| `CreateDictionaryState` | `DictionaryState` |
| `CreateDictionaryUseCase` | `DictionaryUseCase` |
| `CreateDictionaryUseCaseImpl` | `DictionaryUseCaseImpl` |
| `CreateDictionaryModule` (DI) | `DictionaryModule` |

### Callback в навигации

`openAddDict` → `openDictionaryManagement` по всей цепочке:
- `RootRouter.kt` → `mainRouter(openDictionaryManagement = ...)`
- `MainRouter.kt` → `openDictionaryManagement: () -> Unit`
- `MainScreen.kt` → `openDictionaryManagement`
- `Vocabulary.kt`, `Quiz.kt`, `Settings.kt` → параметр `openDictionaryManagement`

### Route

`CREATE_DICTIONARY` → удалить, заменить двумя: `DICTIONARY_SETUP` + `DICTIONARY_MANAGEMENT`
