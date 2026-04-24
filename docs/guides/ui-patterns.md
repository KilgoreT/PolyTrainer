# Паттерны UI

## Двухуровневый паттерн composable

Каждый экран состоит из двух composable-функций:

### 1. Публичная точка входа (Dependency Injection)

Создаёт ViewModel, собирает стейт, делегирует внутренней composable.

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
    WordCardScreen(
        state = state,
        onBackPress = onBackPress,
    ) { viewModel.accept(it) }
}
```

### 2. Внутренняя stateless composable (чистый рендеринг)

Получает стейт и callback. Без ссылки на ViewModel. Без сайд-эффектов в теле.

```kotlin
@Composable
internal fun WordCardScreen(
    state: WordCardState,
    onBackPress: () -> Unit,
    sendMessage: (Msg) -> Unit,
) {
    Scaffold(
        topBar = { TopBarWidget(state, sendMessage) },
        floatingActionButton = { ... },
        snackbarHost = { ... },
    ) { paddings ->
        Column(
            modifier = Modifier
                .padding(paddings)
                .verticalScroll(rememberScrollState())
        ) {
            // Контент
        }
    }
}
```

### 3. Preview

```kotlin
@PreviewScreen
@Composable
private fun Preview() {
    AppTheme {
        WordCardScreen(
            state = WordCardState(isLoading = false),
            onBackPress = {},
        ) {}
    }
}
```

## Отправка сообщений из UI

UI-события диспатчатся как сообщения через callback `sendMessage`:

```kotlin
// Клик кнопки → Сообщение
Button(onClick = { sendMessage(Msg.ShowDropdownMenu) })

// Изменение текста → Сообщение с значением
TextField(
    value = state.wordState.edited,
    onValueChange = { sendMessage(Msg.ChangeWordValue(it)) }
)

// Действие с элементом списка → Сообщение с ID
IconButton(
    onClick = { sendMessage(Msg.DeleteLexeme(lexemeState.id)) }
)
```

## Сайд-эффекты в composable

`LaunchedEffect` для одноразовых или state-triggered эффектов:

```kotlin
// Закрытие экрана
LaunchedEffect(state.closeScreen) {
    if (state.closeScreen) onBackPress()
}

// Показ snackbar
LaunchedEffect(state.snackbarState.show) {
    if (state.snackbarState.show) {
        snackbarHostState.showSnackbar(state.snackbarState.title)
        sendMessage(UiMsg.Snackbar(text = "", show = false))
    }
}
```

## Когда выносить composable в отдельный файл

Composable выносится в отдельный `*Widget.kt` если **2+ критериев**:

1. **Переиспользуется** в 2+ местах
2. **Принимает state-объект** + callback'и
3. **Имеет эффекты** (LaunchedEffect, DisposableEffect)
4. **Представляет визуальный блок** со своим стилем
5. **Имеет preview**

Если 0-1 — остаётся inline. Layout-обёртки (Box, Column, Spacer) — всегда inline.

## Три уровня виджетов

| Уровень | Где | State management | Используется из | Пример |
|---------|-----|-----------------|----------------|--------|
| Примитивы | `core/ui/` | Нет | Везде | `PrimaryFabWidget`, `IconBoxed` |
| Модуль-виджет | `modules/widget/` | TEA (если сложный) | Несколько экранов | `dictionaryappbar` |
| Экранный виджет | `screen/*/widget/` | Нет (props + callbacks) | Только свой экран | `TopBarWidget`, `LexemeItemWidget` |

**Правило уровня:**
- Без бизнес-логики, используется везде → `core/ui/`
- Несколько экранов, может иметь свой TEA → `modules/widget/`
- Только один экран → `screen/*/widget/`

Зависимости строго послойно: `core/ui` ← `modules/widget` ← `screen/*/widget`. Обратных зависимостей нет.

## AppBar — всегда отдельный виджет

AppBar **никогда** не пишется inline в `Scaffold`. Каждый экран использует отдельный виджет:

```kotlin
// Правильно:
topBar = { AboutAppBar(onBackPress = onBackPress) }
topBar = { SettingsAppBar() }
topBar = { TopBarWidget(state = state.topBarState, onBackPress = onBackPress, sendMsg = sendMsg) }

// Неправильно:
topBar = {
    TopAppBar(
        navigationIcon = { IconBoxed(...) },
        title = { Text(...) }
    )
}
```

Паттерны AppBar:

| Тип | Виджет | Параметры |
|-----|--------|-----------|
| Только назад + заголовок | `AboutAppBar(onBackPress)` | `onBackPress: () -> Unit` |
| Без навигации | `SettingsAppBar()` | нет |
| С состоянием (меню, действия) | `TopBarWidget(state, onBackPress, sendMsg)` | state + callbacks |
| Через DI (MainUiDeps) | `uiDeps.AppBar(titleResId)` | `@StringRes titleResId` |

Если экран условно показывает AppBar (например, onboarding без AppBar, management с AppBar) — `onBackPress: (() -> Unit)? = null`:

```kotlin
Scaffold(
    topBar = {
        onBackPress?.let { MyAppBar(onBackPress = it) }
    }
)
```

## Структура Scaffold

Типичный скелет экрана:

```kotlin
Scaffold(
    topBar = { TopBarWidget(state, sendMessage) },
    floatingActionButton = {
        PrimaryFabWidget(iconRes = R.drawable.ic_add) { sendMessage(Msg.AddLexeme) }
    },
    snackbarHost = { SnackbarHost(snackbarHostState) },
    containerColor = MaterialTheme.colorScheme.background,
    contentWindowInsets = WindowInsets(0.dp),
) { paddings ->
    Box(modifier = Modifier.padding(paddings)) {
        // Основной контент
    }
}
```

## Условные оверлеи

Диалоги и bottom sheet по флагам стейта:

```kotlin
if (state.wordState.showWarningDialog) {
    ConfirmDeleteWordWidget(
        onConfirm = { sendMessage(Msg.DeleteWord(state.wordState.id)) },
        onDismiss = { sendMessage(Msg.HideDeleteWordDialog) }
    )
}

if (state.addLexemeBottomState.show) {
    AddLexemeBottomWidget(
        state = state.addLexemeBottomState,
        sendMessage = sendMessage
    )
}
```

## Рендеринг списков

`key()` для стабильной рекомпозиции:

```kotlin
state.lexemeList.forEach { lexemeState ->
    key(lexemeState.id) {
        LexemeItemWidget(
            state = lexemeState,
            sendMessage = sendMessage,
        )
    }
}
```

## Библиотека общих виджетов

Расположена в `modules/core/ui`. Паттерн **base + themed wrapper**.

### Базовый слой

```kotlin
// Базовая кнопка с любым цветом
@Composable
fun LexemeFab(
    @DrawableRes iconRes: Int,
    color: Color,
    enabled: Boolean = true,
    iconColor: Color = whiteColor,
    cornerRadius: Int = DEFAULT_CORNER_RADIUS,
    onClick: () -> Unit,
)

// Базовое текстовое поле с полной кастомизацией
@Composable
fun LexemeTextFieldWidget(
    value: String,
    onValueChange: (String) -> Unit,
    imeAction: ImeAction = ImeAction.Default,
    @StringRes placeHolder: Int?,
    isInputEnabled: Boolean = true,
    onKeyboardActions: () -> Unit,
)
```

### Тематизированные обёртки

```kotlin
// Применяет MaterialTheme.colorScheme.primary
@Composable
fun PrimaryFabWidget(
    @DrawableRes iconRes: Int,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    LexemeFab(
        iconRes = iconRes,
        color = MaterialTheme.colorScheme.primary,
        onClick = onClick,
    )
}
```

### Редактируемый текст (два режима)

```kotlin
@Composable
fun LexemeEditableText(
    originValue: String,        // Сохранённое значение (режим просмотра)
    changedValue: String,       // Редактируемое значение (режим редактирования)
    isEditMode: Boolean,        // Текущий режим
    onTextChange: (String) -> Unit,
    onOpenEditMode: () -> Unit,
    onCloseEditMode: () -> Unit,
    textColor: Color,
)
```

## Конвенции по лейауту

| Элемент | Значение |
|---------|----------|
| Горизонтальный padding экрана | 16.dp |
| Spacing между элементами списка | 8.dp (`Arrangement.spacedBy`) |
| Размер FAB | 56.dp |
| Corner radius FAB | 16.dp |
| Размер иконок | 24.dp (стандарт), 12.dp (малый) |
| Spacing строки ввода | 8.dp |

## System bars

```kotlin
SystemBarsWidget(
    color = Color.Transparent,
    statusBarDarkIcon = true,
    navigationBarDarkIcon = true,
)
```

## Работа с клавиатурой

```kotlin
Box(
    modifier = Modifier
        .padding(paddings)
        .consumeWindowInsets(paddings)
        .imePadding()    // Учёт клавиатуры
)
```

Фокус для текстовых полей:

```kotlin
val focusRequester = remember { FocusRequester() }
LaunchedEffect(Unit) { focusRequester.requestFocus() }
DisposableEffect(Unit) { onDispose { keyboardController?.hide() } }
```
