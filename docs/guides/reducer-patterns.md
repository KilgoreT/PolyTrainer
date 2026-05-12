# Паттерны редьюсера

## Reference реализация

**ChatReducer** (`modules/screen/quiz/chat`) — текущий reference. Другие ред��юсеры могут использовать старые паттерны и будут мигрированы.

## Структура класса редьюсера

```kotlin
internal class ChatReducer(
    private val logger: LexemeLogger,
    private val resourceManager: ResourceManager,
) : MateReducer<ChatScreenState, Msg, Effect> {

    override fun reduce(
        state: ChatScreenState, message: Msg,
    ): ReducerResult<ChatScreenState, Effect> {
        logger.log(message = "Reduce --prevState--: $state ")
        logger.log(message = "Reduce ---message---: $message ")
        return when (message) {
            is Msg.PrepareToStart -> state
                .stopLoading()
                .systemMessage(welcomeMessage().toMessageContent())
                to setOf()
            // ... больше сообщений
        }.also {
            logger.log(message = "Reduce --newState--: ${it.state()} ")
            it.effects().forEach { effect ->
                logger.log(message = "Reduce --toEffect--: $effect ")
            }
        }
    }
}
```

## Паттерн 1: Только стейт (без эффектов)

Цепочка расширений + пустой набор эффектов:

```kotlin
is Msg.ShowMenu -> state
    .showActionMenu() to setOf()

is Msg.HideMenu -> state
    .hideActionMenu() to setOf()
```

## Паттерн 2: Только эффекты (стейт без изменений)

Стейт проходит как есть, объявляются эффекты:

```kotlin
is Msg.EarliestOn -> state to setOf(DatasourceEffect.EarliestOn)
is Msg.Start -> state to setOf(DatasourceEffect.LoadQuiz)
```

## Паттерн 3: Стейт + эффекты

Цепочка расширений вместе с эффектами:

```kotlin
is Msg.UserAttempt -> state
    .userTextEnter()
    .clearUserInput()
    .hideUserActions()
    .disableUserInput() to setOf(
        DatasourceEffect.CheckAnswer(message.value)
    )
```

## Паттерн 4: Условный стейт

Когда логика ветвится по данным сообщения:

```kotlin
is Msg.QuizLoaded -> {
    val newState = state
        .startQuiz()
        .userMessage(startUserMessage().toMessageContent())

    val finalState = if (message.content != null) {
        newState.systemMessage(MessageContent.create(text = message.content))
    } else newState

    return finalState to setOf(DatasourceEffect.NextQuestion)
}
```

## Паттерн 5: Динамические эффекты

Когда эффекты зависят от содержимого сообщения:

```kotlin
is Msg.UserAction -> {
    val effects: Set<Effect> = when (message.action) {
        UserAction.CONTINUE -> setOf(DatasourceEffect.LoadQuiz)
        UserAction.SUMMARY -> setOf(DatasourceEffect.Summary)
        UserAction.EXIT -> setOf(NavigationEffect.Back)
    }

    val newState = when (message.action) {
        UserAction.CONTINUE -> state
        UserAction.SUMMARY -> state.userMessage(...)
        UserAction.EXIT -> state            // навигация не меняет state
    }

    newState to effects
}
```

EXIT возвращает `NavigationEffect.Back` — навигация выражается эффектом, state не получает флага вроде `exit = true`.

## Паттерн 6: Conditional навигация (ExitApp vs Back)

Для root-экранов выбор между закрытием приложения и обычным back делается **в reducer** на основе state, не в composable:

```kotlin
is Msg.RequestBack -> {
    val effect: Effect = if (state.dictionaries.isEmpty()) {
        ListNavigationEffect.ExitApp
    } else {
        NavigationEffect.Back
    }
    state to setOf(effect)
}
```

`NavigationEffect.Back` — базовый из `core/mate`, обрабатывается `MateNavigationEffectHandler` единообразно. `ExitApp` — per-screen sealed (`ListNavigationEffect.ExitApp`), только в root-экранах. Reducer контролирует переключение.

В composable — простой `BackHandler { viewModel.accept(Msg.RequestBack) }`, без if-условий.

## Паттерн 7: Private методы (старый стиль)

WordCardReducer делегирует в private методы. Это **старый паттерн**, постепенно мигрирует на цепочки расширений:

```kotlin
override fun reduce(state: WordCardState, message: Msg): ReducerResult<WordCardState, Effect> {
    return when (message) {
        is Msg.TermLoading -> onTermLoading(state)
        is Msg.TermLoaded -> onTermLoaded(state, message.term)
        is Msg.ShowDropdownMenu -> onChangeDropdownMenu(state, isShow = true)
    }
}

private fun onTermLoading(state: WordCardState): ReducerResult<WordCardState, Effect> =
    state.copy(isLoading = true) to setOf(DatasourceEffect.LoadWord(state.wordState.id))
```

(`closeScreen` поле из state удалено — навигация назад теперь через `Msg.NavigateBack` → `NavigationEffect.Back`.)

## Предпочитаемый стиль (ChatReducer)

Новый паттерн — **inline цепочки расширений** прямо в `when` блоке:

```kotlin
is Msg.NextQuestion -> state
    .systemMessage(message = message.content)
    .showUserActions()
    .enableUserInput() to emptySet()
```

Преимущества:
- Читается как декларативный рецепт
- Нет промежуточных private методов
- Расширения переиспользуемы между сообщениями
- Редьюсер остаётся компактным

## Конвенция логирования

Логировать предыдущий стейт, сообщение, новый стейт и эффекты:

```kotlin
override fun reduce(state: S, message: M): ReducerResult<S, E> {
    logger.log("Reduce --prevState--: $state ")
    logger.log("Reduce ---message---: $message ")
    return when (message) {
        // ...
    }.also {
        logger.log("Reduce --newState--: ${it.state()} ")
        it.effects().forEach { effect ->
            logger.log("Reduce --toEffect--: $effect ")
        }
    }
}
```

## No-Op сообщение

Всегда обрабатывать `Msg.Empty`:

```kotlin
is Msg.Empty -> state to emptySet()
```

## Зависимости редьюсера

Редьюсер может принимать зависимости для чистых операций (без сайд-эффектов):

| Допустимо | Запрещено |
|-----------|-----------|
| `ResourceManager` (строковые ресурсы) | UseCase / Repository |
| `LexemeLogger` (логирование) | Dispatchers / корутины |
| Чистые утилитарные классы | БД / сетевые вызовы |
| Форматтеры / мапперы | SharedPreferences |

Редьюсер должен оставаться **чистой функцией**: одинаковые входы всегда дают одинаковые выходы.

## Что НЕ делать в редью��ере

- **Бизнес-логика** (фильтрация, поиск, обогащение данных) — в UseCase. Reducer только записывает результат в State
- **Навигация** — не через boolean в State. Reducer порождает `NavigationEffect.Back`, NavigationEffectHandler вызывает callback. Один эффект для любого закрытия экрана (и по кнопке назад, и после сохранения)
- **Зависимые загрузки** — не запускать параллельно в initEffects. Цепочка: первый Msg → Reducer → Effect второго
