# Структура стейта и extension-функции

## Принципы стейта

1. **Иммутабельные data class'ы** с аннотацией `@Stable` или `@Immutable` для оптимизации Compose.
2. **Иерархическая вложенность**: корневой стейт экрана содержит вложенные под-стейты.
3. **Дефолтные значения** для всех полей — безопасное создание через конструктор без аргументов.
4. **Extension-функции** для всех мутаций стейта — редьюсер никогда не вызывает `.copy()` напрямую на сложном стейте.

## Паттерн иерархии стейта

```kotlin
@Stable
data class WordCardState(
    val topBarState: TopBarState = TopBarState(),
    val addLexemeBottomState: AddLexemeBottomState = AddLexemeBottomState(),
    val closeScreen: Boolean = false,
    val isLoading: Boolean = true,
    val wordState: WordState = WordState(),
    val lexemeList: List<LexemeState> = listOf(),
    val snackbarState: SnackbarState = SnackbarState(),
)

@Stable
data class TopBarState(
    val isMenuOpen: Boolean = false,
)

@Stable
data class WordState(
    val id: Long = NOT_IN_DB,
    val added: Date? = null,
    val value: String = "",
    val isEditMode: Boolean = false,
    val edited: String = "",
    val showWarningDialog: Boolean = false,
    val deleteButtonEnabled: Boolean = true,
)
```

**Правила:**
- Корневой стейт — одно поле на каждый UI-регион (topBar, контент, bottomSheet, snackbar)
- Каждый под-стейт — отдельный data class
- Списки: `List<ItemState>`, не `List<DomainModel>`
- `NOT_IN_DB = -1L` для неинициализированных ID
- `TextValueState` — паттерн для toggle edit/view: `origin` (сохранённое) + `edited` (в процессе) + `isEdit` (режим)

## Extension-функции

Чистые, без сайд-эффектов трансформации стейта. **Строительные блоки** для редьюсера.

### Простые расширения

```kotlin
fun ChatScreenState.stopLoading() = copy(loading = false)

fun ChatScreenState.startQuiz() = copy(
    chat = chat.copy(readyToStart = true)
)

fun ChatScreenState.showUserActions() = copy(
    chat = chat.copy(showUserActions = true)
)

fun ChatScreenState.enableUserInput() = copy(
    chat = chat.copy(isUserInputEnable = true)
)

fun ChatScreenState.clearUserInput() = copy(
    chat = chat.copy(inputState = EMPTY_STRING)
)

fun ChatScreenState.exit() = copy(exit = true)
```

### Параметризованные расширения

```kotlin
fun ChatScreenState.userTextChange(value: String) = copy(
    chat = chat.copy(inputState = value),
)

fun ChatScreenState.updateMenu(
    isEarliestOn: Boolean,
    isFrequentMistakesOn: Boolean,
    isDebugOn: Boolean,
): ChatScreenState {
    return this.copy(
        appBarState = this.appBarState.copy(
            itemsState = this.appBarState.itemsState.copy(
                earliest = if (isEarliestOn != this.appBarState.itemsState.earliest.isOn) {
                    ItemsState.Earliest(isOn = isEarliestOn)
                } else this.appBarState.itemsState.earliest,
                // ...
            )
        )
    )
}
```

### Цепочки в редьюсере

```kotlin
// Расширения цепляются fluently:
is Msg.NextQuestion -> state
    .systemMessage(message = message.content)
    .showUserActions()
    .enableUserInput() to emptySet()

is Msg.UserAttempt -> state
    .userTextEnter()
    .clearUserInput()
    .hideUserActions()
    .disableUserInput() to setOf(
        DatasourceEffect.CheckAnswer(message.value)
    )
```

### Расширения для списков

Для модификации элементов в списках используется `modifyFiltered` из `modules/core/tools`:

```kotlin
fun ChatState.addSystemMessage(message: MessageContent) = copy(
    messagesState = messagesState.copy(
        list = messagesState.list + ChatMessage.addSystemMessage(
            message = message.text,
            order = nextOrder(),
            buttons = message.buttons
        )
    )
)
```

## Конвенции

1. **Одна задача на расширение.** `showActionMenu()` делает ровно одну вещь.
2. **Расширения живут в State.kt**, рядом с дата-классами которые они расширяют.
3. **Нейминг:** глагол в начале — `show`, `hide`, `enable`, `disable`, `clear`, `update`, `add`, `remove`.
4. **Без бизнес-логики.** Расширения только модифицируют стейт. Сложные решения — в редьюсере.
5. **Чистые функции.** Один и тот же вход → один и тот же выход. Без сайд-эффектов.
6. **Расширения на корневом стейте** (например, `ChatScreenState.showMenu()`), не на вложенных, если только вложенное расширение не используется несколькими корневыми.
7. **Chain-friendly:** каждое расширение возвращает модифицированный стейт для fluent-цепочек.

## Использование в редьюсере

```kotlin
override fun reduce(state: ChatScreenState, message: Msg): ReducerResult<ChatScreenState, Effect> {
    return when (message) {
        // Только стейт: цепочка расширений + пустые эффекты
        is Msg.ShowMenu -> state.showActionMenu() to setOf()

        // Стейт + эффекты
        is Msg.UserAttempt -> state
            .userTextEnter()
            .clearUserInput()
            .hideUserActions()
            .disableUserInput() to setOf(
                DatasourceEffect.CheckAnswer(message.value)
            )

        // Только эффекты: стейт без изменений
        is Msg.Start -> state to setOf(DatasourceEffect.LoadQuiz)

        // Сложная логика: локальные переменные
        is Msg.QuizLoaded -> {
            val newState = state.startQuiz().userMessage(...)
            val finalState = if (message.content != null) {
                newState.systemMessage(...)
            } else newState
            return finalState to setOf(DatasourceEffect.NextQuestion)
        }
    }
}
```
