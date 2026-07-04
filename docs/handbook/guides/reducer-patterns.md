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

---

## Rules — машинно-проверяемые правила

Формализованные правила раздела для прогона в чек-листе ревьюером. Формат — `R-NNN` (id) / `Severity` / `Applies to` / `Check` (как проверить). Reviewer (см. промпты `agents/custom/senior.md` и др.) обязан пройти по каждому R-NNN из подложенных гайдов и подтвердить применимость / нарушение.

### R-RP-001. State мутируется только через extension chain

- **Severity:** critical.
- **Applies to:** тело reducer-функции (всё что внутри `is Msg.X -> ...`).
- **Check:** в reducer'е нет ручных `state.copy(...)` / `state.nested.copy(...)`. Любое изменение state — через extension-функции, объединённые в цепочку (`state.foo().bar().baz()`). См. § Паттерн 1-3.
- **Зачем:** атомарность изменений, единое место для invariant'ов, отсутствие случайного «забыл скопировать поле».

### R-RP-002. Каждая extension меняет ровно одну сущность

- **Severity:** critical.
- **Applies to:** extension-функции мутации state (живущие в `State.kt` — см. `state-and-extensions.md`).
- **Check:** одна extension изменяет ровно один nested-объект (или одно его поле). Не два, не «всё подряд». Если нужно изменить несколько сущностей — это chain из нескольких extension'ов в reducer'е, не одна «жирная» extension.
- **Зачем:** читабельность, переиспользуемость, явный шаг каждого изменения.

### R-RP-003. Переходы edit-mode через атомарную extension

- **Severity:** critical.
- **Applies to:** типы с edit-mode invariant'ом (паттерн `origin / edited / isEdit` или аналог).
- **Check:** переход `isEdit: false → true` обязан копировать `origin → edited` в одной extension-функции (например `enableEdit()`). Reducer вызывает только эту функцию — ручной `copy(edited = origin, isEdit = true)` запрещён.
- **Зачем:** invariant закреплён в коде, не в дисциплине reducer'ов. Забыть копирование физически невозможно.

### R-RP-004. State не содержит навигационных флагов

- **Severity:** critical.
- **Applies to:** определения data class State / nested.
- **Check:** в State нет полей вида `exit: Boolean`, `closeScreen: Boolean`, `navigateTo: Screen`, `shouldFinish: Boolean`. Навигация выражается **эффектами** (`NavigationEffect.Back`, `ExitApp` и т.п.).
- **Зачем:** State описывает данные экрана, не намерения навигации. Composable не зависит от флагов «когда закрыть».

### R-RP-005. Conditional навигация решается в reducer, не в composable

- **Severity:** critical.
- **Applies to:** обработка событий типа `Msg.RequestBack` / `Msg.SaveAndExit` в reducer + соответствующий composable.
- **Check:** в composable — простой `BackHandler { viewModel.accept(Msg.RequestBack) }` без if-условий. Выбор между `ExitApp` / `Back` / другими навигационными эффектами — в reducer на основе state.
- **Зачем:** логика навигации тестируется через reducer (чистая функция), а не через UI.

### R-RP-006. Reducer-логика — inline chain, не private методы

- **Severity:** minor.
- **Applies to:** ветки `when` в reducer-функции.
- **Check:** ветка обрабатывается inline chain extension'ов прямо в `when` (см. § Предпочитаемый стиль / ChatReducer). Делегирование в private методы (`onTermLoading(state)`, `onChangeDropdownMenu(...)`) — legacy паттерн, новый код не пишет так.
- **Зачем:** читается как декларативный рецепт; нет промежуточных приватных методов; extension'ы переиспользуемы между сообщениями.

### R-RP-007. Reducer обязан логировать prevState / message / newState / effects

- **Severity:** minor.
- **Applies to:** функция `reduce(state, message)` в каждом reducer-классе.
- **Check:** в начале функции — `logger.log("Reduce --prevState--: $state")` + `logger.log("Reduce ---message---: $message")`. В конце — `.also { logger.log("Reduce --newState--: ${it.state()}"); it.effects().forEach { logger.log("Reduce --toEffect--: $effect") } }`.
- **Зачем:** консистентный лог TEA-цикла для диагностики. Все reducer'ы пишут одинаково — диагностика становится механической.

### R-RP-008. `Msg.Empty` обработан явно

- **Severity:** minor.
- **Applies to:** sealed-иерархия `Msg` экрана.
- **Check:** в reducer-функции есть ветка `is Msg.Empty -> state to emptySet()` (no-op). Не «забыли», не отсутствует.
- **Зачем:** консистентный no-op для сценариев когда reducer вызывается без изменений.

### R-RP-009. Reducer принимает только pure dependencies

- **Severity:** critical.
- **Applies to:** primary-constructor reducer-класса (`internal class XReducer(...) : MateReducer<...>`).
- **Check:** допустимы только pure-зависимости: `ResourceManager`, `LexemeLogger`, форматтеры/мапперы, чистые утилиты. Запрещены: `UseCase`, `Repository`, `Dispatchers`, корутины, БД/сетевые клиенты, `SharedPreferences`.
- **Зачем:** reducer = чистая функция; сайд-эффекты живут в `EffectHandler`. Reducer тестируется без mock'ов.

### R-RP-010. Бизнес-логика — в UseCase, не в reducer

- **Severity:** critical.
- **Applies to:** тело reducer-функции и extension'ов state.
- **Check:** в reducer'е и extension'ах нет фильтрации / поиска / обогащения данных / сортировок над коллекциями state. Reducer **только записывает результат** UseCase в State (приходит через Msg).
- **Зачем:** разделение «что данные» (UseCase) и «как они хранятся в state» (reducer). Бизнес-правила покрываются UseCase-тестами.

### R-RP-011. Навигация — только через эффекты

- **Severity:** critical.
- **Applies to:** reducer + State.
- **Check:** закрытие экрана — `NavigationEffect.Back` (или per-screen вариант типа `ExitApp`). Один эффект для всех сценариев закрытия (back-кнопка, после сохранения, отмена). Boolean-флаги навигации в State отсутствуют (см. R-RP-004).
- **Зачем:** единая точка обработки навигации (`MateNavigationEffectHandler`). Reducer не зависит от того кто слушает эффект.

### R-RP-012. Зависимые загрузки — цепочкой Msg → Effect, не параллельным запуском

- **Severity:** minor.
- **Applies to:** `initEffects` reducer'а и обработка нескольких подряд идущих data-загрузок.
- **Check:** если загрузка B зависит от результата загрузки A — не запускать обе параллельно в `initEffects`. Цепочка: `Msg.A → Reducer → Effect.LoadA → ... → Msg.ALoaded → Reducer → Effect.LoadB → ...`. Параллельные эффекты только для **независимых** загрузок.
- **Зачем:** избегать race conditions; явная зависимость между загрузками в reducer-логике, не в timing'е.
