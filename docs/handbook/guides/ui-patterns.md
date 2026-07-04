# Паттерны UI

## Двухуровневый паттерн composable

Каждый экран состоит из двух composable-функций:

### 1. Публичная точка входа (Dependency Injection)

Принимает `factory: XxxViewModel.Factory` + `navigator: XxxNavigator`, создаёт ViewModel через `viewModelFactory { factory.create(navigator, ...) }`, делегирует внутренней composable.

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

- `factory` — `@AssistedFactory` от Dagger, экспонируется через `AppComponent.getXxxViewModelFactory()`.
- `navigator` — runtime параметр, создаётся в `CompositionRootImpl` / навигационном graph через `remember(navController)`.
- `viewModelFactory { ... }` — helper из `core/di`.
- `key = ...` — нужен только если экран принимает аргумент-идентификатор и должен пересоздаваться при его смене.

### 2. Внутренняя stateless composable (чистый рендеринг)

Получает стейт и callback. Без ссылки на ViewModel. Без сайд-эффектов в теле. Без навигационных callback'ов — все события идут через `sendMessage`.

```kotlin
@Composable
internal fun WordCardScreen(
    state: WordCardState,
    sendMessage: (Msg) -> Unit,
) {
    BackHandler { sendMessage(Msg.RequestBack) }
    Scaffold(
        topBar = {
            TopBarWidget(
                topBarState = state.topBarState,
                onBackPress = { sendMessage(Msg.NavigateBack) },
                onOpenMenu = { sendMessage(Msg.OpenTopBarMenu) },
                onCloseMenu = { sendMessage(Msg.CloseTopBarMenu) },
                onDeleteWord = { sendMessage(Msg.OpenDeleteWordDialog) },
            )
        },
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
            sendMessage = {},
        )
    }
}
```

## Отправка сообщений из UI

`sendMessage: (Msg) -> Unit` живёт **только** на верхнем уровне — во внутренней stateless composable экрана (`internal fun WordCardScreen(state, sendMessage)`). На этом уровне `Msg` известны, callback'и для виджетов строятся inline:

```kotlin
internal fun WordCardScreen(
    state: WordCardState,
    sendMessage: (Msg) -> Unit,
) {
    TopBarWidget(
        topBarState = state.topBarState,
        onBackPress = { sendMessage(Msg.NavigateBack) },
        onOpenMenu = { sendMessage(Msg.OpenTopBarMenu) },
        onCloseMenu = { sendMessage(Msg.CloseTopBarMenu) },
        onDeleteWord = { sendMessage(Msg.OpenDeleteWordDialog) },
    )
}
```

## Виджеты получают callbacks, не `sendMessage`

**Правило.** Виджет (любой `internal fun *Widget(...)`) **никогда** не принимает `sendMessage: (Msg) -> Unit`. События идут наружу через плоские callback'и (`onClick`, `onValueChange`, `onConfirm` и т.п.). Заполняются callback'и на верхнем уровне экрана — там, где Msg известны.

**Почему.**

- Виджет, принимающий `sendMessage`, **завязан на MVI-инфраструктуру** (импорт `me.apomazkin.mate.*`, знание о существовании конкретного `Msg`). Перенести в другой экран — нельзя.
- На сайте вызова **не видно ЧТО произойдёт** при клике. `sendMessage` — чёрный ящик; `onConfirm` — очевидное намерение.
- Preview/screenshot-тесты ломаются: подсовывать `sendMessage = {}` можно, но виджет всё равно должен **знать** про `Msg`-тип, который к Preview отношения не имеет.
- Логика "при клике X → отправить Msg.Y" размазана между виджетом и экраном. Через callbacks — собрана в одном месте (главный composable).

**Без исключений по числу callback'ов.** Если callback'ов накапливается много (5+) — это сигнал что виджет слишком жирный, **декомпозируй** (выдели sub-section'ы), не ставь `sendMessage` как эвакуационный люк.

```kotlin
// ❌ Плохо — sendMessage внутри виджета
@Composable
internal fun ConfirmDeleteWordWidget(
    loaded: WordState.Loaded,
    sendMessage: (Msg) -> Unit,
) {
    AlarmDialogWidget(
        onAlarmClick = { sendMessage(Msg.RemoveWord(loaded.id)) },
        onDismissRequest = { sendMessage(Msg.CloseDeleteWordDialog) },
    ) { ... }
}

// ✅ Хорошо — плоские callbacks, id уезжает наверх в WordCardScreen
@Composable
internal fun ConfirmDeleteWordWidget(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlarmDialogWidget(
        onAlarmClick = onConfirm,
        onDismissRequest = onDismiss,
    ) { ... }
}

// Сайт вызова — WordCardScreen
ConfirmDeleteWordWidget(
    onConfirm = { sendMessage(Msg.RemoveWord(state.wordState.id)) },
    onDismiss = { sendMessage(Msg.CloseDeleteWordDialog) },
)
```

**Не передавать "лишних" параметров для callback'ов.** Если параметр (`lexemeId`, `loaded`) используется только чтобы построить Msg — он не нужен виджету. id поедет наверх через callback-builder. Виджет остаётся чисто пресентационным.

## Сайд-эффекты в composable

`LaunchedEffect` — для state-triggered UI-эффектов (фокус, анимации). **Не для навигации** — навигация выражается через `NavigationEffect` в reducer, обрабатывается `NavigationEffectHandler` через `Navigator`. **Не для snackbar/toast/vibration** — см. отдельный раздел «Snackbar и one-shot UI-уведомления (UiHost)» ниже.

```kotlin
// Фокус на поле ввода при входе в edit-mode
LaunchedEffect(state.isEditMode) {
    if (state.isEditMode) focusRequester.requestFocus()
}

// Системная back-кнопка — через Msg, не через LaunchedEffect(closeScreen)
BackHandler { sendMessage(Msg.RequestBack) }
```

Антипаттерн (удалён в IS471):

```kotlin
// Так делать НЕЛЬЗЯ:
LaunchedEffect(state.closeScreen) {
    if (state.closeScreen) onBackPress()
}
```

Закрытие экрана — это эффект, не state. Reducer возвращает `NavigationEffect.Back` → `Navigator.back()` → `navController.popBackStack()`.

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
topBar = {
    TopBarWidget(
        topBarState = state.topBarState,
        onBackPress = onBackPress,
        onOpenMenu = { sendMessage(Msg.OpenTopBarMenu) },
        onCloseMenu = { sendMessage(Msg.CloseTopBarMenu) },
        onDeleteWord = { sendMessage(Msg.OpenDeleteWordDialog) },
    )
}

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
| С состоянием (меню, действия) | `TopBarWidget(state, onBackPress, onOpenMenu, ...)` | state + плоские callbacks |
| Через DI (CompositionRoot) | `uiDeps.AppBar(titleResId)` | `@StringRes titleResId` |

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
    topBar = {
        TopBarWidget(
            topBarState = state.topBarState,
            onBackPress = { sendMessage(Msg.NavigateBack) },
            onOpenMenu = { sendMessage(Msg.OpenTopBarMenu) },
            onCloseMenu = { sendMessage(Msg.CloseTopBarMenu) },
            onDeleteWord = { sendMessage(Msg.OpenDeleteWordDialog) },
        )
    },
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

## Snackbar и one-shot UI-уведомления (UiHost)

Snackbar / toast / vibration — **one-shot side-effects**, не часть data state. Канон проекта — `UiHost` abstraction через `@AssistedInject`. Handler вызывает `uiHost.showSnackbar(message)` **напрямую**, без круга через Msg + State.

**Почему не state-based.** Раньше snackbar хранился в `state.snackbarState: SnackbarState(title, show)`, composable наблюдал его через `LaunchedEffect` и слал `Msg.DismissNotification` для сброса. Проблемы:

- State захламляется транзитивным UI-фидбеком, который по природе one-shot.
- Лишний раунд `Effect → Handler → Msg → State change` для fire-and-forget уведомления.
- Composable должен помнить про reset-msg, иначе snackbar дублируется при recomposition.
- Снэкбар survive'ит config change в state — иногда нежелательно для one-shot.

**Канон.**

```kotlin
// 1. Абстракция-host (в screen модуле или общая в core/ui)
interface UiHost {
    suspend fun showSnackbar(message: String)
    fun showToast(message: String)
    // future: vibration, system UI, etc.
}

// 2. Handler получает UiHost через @AssistedInject
class UiEffectHandler @AssistedInject constructor(
    @Assisted private val uiHost: UiHost,
) : MateTypedEffectHandler<Msg, UiEffect>() {

    override fun filter(effect: Effect): UiEffect? = effect as? UiEffect

    override suspend fun onEffect(effect: UiEffect, consumer: (Msg) -> Unit) {
        when (effect) {
            is UiEffect.ShowNotification -> uiHost.showSnackbar(effect.message)
        }
        // consumer(Msg.NoOperation) — если контракт MateTypedEffectHandler требует return Msg.
    }

    @AssistedFactory
    interface Factory {
        fun create(uiHost: UiHost): UiEffectHandler
    }
}

// 3. Реализация UiHost на стороне Composable
class UiHostImpl(
    private val snackbarHostState: SnackbarHostState,
    private val context: Context,
) : UiHost {
    override suspend fun showSnackbar(message: String) {
        snackbarHostState.showSnackbar(message)
    }
    override fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}

// 4. В корневом Composable
val snackbarHostState = remember { SnackbarHostState() }
val context = LocalContext.current
val uiHost = remember(snackbarHostState, context) { UiHostImpl(snackbarHostState, context) }
val viewModel: WordCardViewModel = hiltViewModel { factory ->
    factory.create(uiHost) // через AssistedInject viewmodel-factory
}

Scaffold(
    snackbarHost = { SnackbarHost(snackbarHostState) },
    // ...
)
```

**Симметрия с Navigation.** Handler `NavigationEffect` получает `Navigator` тем же путём (`@AssistedInject`); реализация `NavigatorImpl` живёт на стороне Composable. UiHost — точная аналогия для UI-фидбека.

**Legacy state-based snackbar** (`SnackbarState(title, show)` в state + `LaunchedEffect(state.snackbarState.show)` + `Msg.DismissNotification` reset) — встречается в существующем коде, постепенно мигрируется. Не добавлять новый код по legacy-паттерну. Если фича уже использует state-based — миграция отдельной задачей (см. `docs/Backlog.md` → «UiEffect: убрать круг Effect → Msg → State, показывать snackbar/toast напрямую через UiHost» + «Централизованная система ошибок и снекбаров»).

## Условные оверлеи

Диалоги и bottom sheet по флагам стейта:

```kotlin
if (state.wordState.showWarningDialog) {
    ConfirmDeleteWordWidget(
        onConfirm = { sendMessage(Msg.DeleteWord(state.wordState.id)) },
        onDismiss = { sendMessage(Msg.HideDeleteWordDialog) },
    )
}

if (state.addLexemeBottomState.show) {
    AddLexemeBottomWidget(
        state = state.addLexemeBottomState,
        onAddLexeme = { sendMessage(Msg.AddLexeme(it)) },
        onDismiss = { sendMessage(Msg.HideAddLexemeBottom) },
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
            enabled = !state.isPendingDbOp,
            onRemove = { sendMessage(Msg.OpenDeleteLexemeDialog(lexemeState.id)) },
            // ... остальные callbacks с захватом lexemeState.id
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

## Выделение логически отдельных элементов в виджеты

Логически самостоятельный UI-элемент выносить в отдельный `*Widget.kt` — даже при однократном использовании. Это не про переиспользование, а про читаемость: экран/форма не должны содержать inline-вёрстку для каждого элемента.

**Выносить:** placeholder, карточка элемента, диалог, панель ввода, кастомная кнопка — всё что имеет свой смысл и состав.

**Не выносить:** один `Text()`, один `Spacer()`, простой `Row` без логики.

```kotlin
// Плохо — inline в форме
Box(
    modifier = Modifier.size(48.dp).clip(CircleShape).background(gray),
    contentAlignment = Alignment.Center,
) {
    Text(text = letter, style = LexemeStyle.BodyL, color = grayTextColor)
}

// Хорошо — отдельный виджет
FlagPlaceholderWidget(letter = name.firstOrNull()?.toString() ?: "")
```
