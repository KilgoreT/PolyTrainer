package me.apomazkin.quiz.chat.logic

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import me.apomazkin.lexeme.ComponentType
import me.apomazkin.lexeme.ComponentTypeRef
import me.apomazkin.mate.EMPTY_STRING
import me.apomazkin.quiz.chat.logic.ChatMessage.MessageValue.Plain
import me.apomazkin.quiz.chat.quiz.QuizQuestion

private const val DEFAULT_LOAD_DELAY = 0L

/**
 * State
 */
@Immutable
data class ChatScreenState(
        val loading: Boolean = true,
        val loadDelay: Long = DEFAULT_LOAD_DELAY,
        val appBarState: AppBarState = AppBarState(),
        val chat: ChatState = ChatState(),
        val snackbarState: SnackbarState = SnackbarState(),
)

/**
 * IS500: [quizGroupName] — имя группы тренировки сабтайтлом аппбара;
 * null — «Все» (сабтайтл показывается ВСЕГДА, для null UI берёт
 * ресурс `group_all_title`).
 */
@Immutable
data class AppBarState(
        val isActionMenuOpen: Boolean = false,
        val itemsState: ItemsState = ItemsState(),
        val quizGroupName: String? = null,
)

fun ChatScreenState.updateQuizGroupName(name: String?) = copy(
        appBarState = appBarState.copy(
                quizGroupName = name
        )
)

fun ChatScreenState.showActionMenu() = this.copy(
        appBarState = appBarState.copy(
                isActionMenuOpen = true
        )
)

fun ChatScreenState.hideActionMenu() = this.copy(
        appBarState = appBarState.copy(
                isActionMenuOpen = false
        )
)

@Immutable
data class ItemsState(
        val earliest: Earliest = Earliest(),
        val frequentMistakes: FrequentMistakes = FrequentMistakes(),
        val debug: Debug = Debug(),
        val quizComponent: QuizComponent = QuizComponent(),
) {
    data class Earliest(
            val isOn: Boolean = false,
    )
    data class FrequentMistakes(
            val isOn: Boolean = false,
    )

    data class Debug(
            val isOn: Boolean = false,
    )

    /**
     * Пикер ядер квиза. `availableTypes` — ядра текущего словаря, которыми
     * квиз умеет спросить, по `position`. `selectedRefs` — включённые
     * галки; до загрузки пусто (transient окно), после загрузки непусто,
     * если `availableTypes` непуст (редьюсер держит инвариант).
     */
    data class QuizComponent(
            val availableTypes: List<ComponentType> = emptyList(),
            val selectedRefs: Set<ComponentTypeRef> = emptySet(),
    )
}

/** Подменю выбора есть только когда есть из чего выбирать: два ядра и больше. */
val ItemsState.QuizComponent.isPickerVisible: Boolean
    get() = availableTypes.size > 1

fun ChatScreenState.updateQuizComponent(
        types: List<ComponentType>,
        selectedRefs: Set<ComponentTypeRef>,
): ChatScreenState = copy(
        appBarState = appBarState.copy(
                itemsState = appBarState.itemsState.copy(
                        quizComponent = ItemsState.QuizComponent(
                                availableTypes = types,
                                selectedRefs = selectedRefs,
                        ),
                ),
        ),
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
                            frequentMistakes = if (isFrequentMistakesOn != this.appBarState.itemsState.frequentMistakes.isOn) {
                                ItemsState.FrequentMistakes(isOn = isFrequentMistakesOn)
                            } else this.appBarState.itemsState.frequentMistakes,
                            debug = if (isDebugOn != this.appBarState.itemsState.debug.isOn) {
                                ItemsState.Debug(isOn = isDebugOn)
                            } else this.appBarState.itemsState.debug,
                    )
            )
    )
}

@Immutable
data class ChatState(
        val readyToStart: Boolean = false,
        val showUserActions: Boolean = false,
        val messagesState: ChatMessageState = ChatMessageState(),
        val inputState: String = EMPTY_STRING,
        val isUserInputEnable: Boolean = false,
)

@Immutable
data class ChatMessageState(
        val list: List<ChatMessage> = listOf(),
)

fun ChatMessageState.isPreviousHasSameType(
        index: Int,
): Boolean {
    if (index == 0) return false
    val previous = list[index - 1]
    val current = list[index]
    return previous.isSystemMessage == current.isSystemMessage
}

/**
 * Откуда пришло сообщение юзера: UI превращает породившую его системную
 * кнопку в пузырь на месте, а не показывает пузырь как новый.
 * [START_BUTTON] — широкая кнопка «Начать» по центру (пузырь сужается и
 * уезжает вправо); [SKIP_CHIP] — правый чип «Пропустить» (перекраска на
 * месте); [SHOW_ANSWER_CHIP] — левый чип «Показать ответ» (перекраска +
 * сдвиг вправо на место «Пропустить»); [INPUT] — набрано в поле ввода.
 */
enum class UserMessageOrigin { INPUT, START_BUTTON, SKIP_CHIP, SHOW_ANSWER_CHIP }

@Stable
data class ChatMessage(
        val order: Int = -1,
        val isSystemMessage: Boolean,
        val message: MessageValue,
        val buttons: List<ChatButton> = listOf(),
        val origin: UserMessageOrigin = UserMessageOrigin.INPUT,
) {

    /**
     * Содержимое пузыря. [Plain] и [Rich] — текст; [Question] — вопрос
     * раунда частями, лента собирает его сама (заголовок, метка,
     * значение), `asText()`/`asString()` для него — отчёт, логи, тесты.
     */
    sealed class MessageValue {
        fun asString(): String = when (this) {
            is Plain -> value
            is Rich -> value.text
            is Question -> question.header + ":\n" + question.value
        }

        fun asText(): AnnotatedString = when (this) {
            is Plain -> buildAnnotatedString { append(value) }
            is Rich -> value
            is Question -> buildAnnotatedString {
                question.debugHeader?.let {
                    append(it)
                    append("\n")
                }
                append(asString())
            }
        }

        data class Plain(val value: String) : MessageValue()
        data class Rich(val value: AnnotatedString) : MessageValue()
        data class Question(val question: QuizQuestion) : MessageValue()
    }

    data class ChatButton(
            @StringRes val title: Int,
            val action: UserAction,
    )

    companion object {

        enum class UserAction {
            CONTINUE,
            EXIT,
            SUMMARY,
        }

        fun addSystemMessage(
                message: String,
                order: Int,
        ) = ChatMessage(
                order = order,
                isSystemMessage = true,
                message = Plain(message),
        )

        fun addSystemMessage(
                message: AnnotatedString,
                order: Int,
                buttons: List<ChatButton> = listOf(),
        ) = ChatMessage(
                order = order,
                isSystemMessage = true,
                message = MessageValue.Rich(message),
                buttons = buttons,
        )

        fun addSystemMessage(
                message: MessageValue,
                order: Int,
                buttons: List<ChatButton> = listOf(),
        ) = ChatMessage(
                order = order,
                isSystemMessage = true,
                message = message,
                buttons = buttons,
        )

        fun addUserMessage(
                message: MessageContent,
                order: Int,
                origin: UserMessageOrigin = UserMessageOrigin.INPUT,
        ) = ChatMessage(
                order = order,
                isSystemMessage = false,
                message = Plain(message.value.asString()),
                origin = origin,
        )
    }
}

@Immutable
data class SnackbarState(
        val title: String = EMPTY_STRING,
        val show: Boolean = false,
)

fun ChatScreenState.stopLoading() = copy(loading = false)

fun ChatScreenState.startQuiz() = copy(
        chat = chat.copy(readyToStart = true)
)

fun ChatScreenState.showUserActions() = copy(
        chat = chat.copy(
                showUserActions = true
        )
)

fun ChatScreenState.hideUserActions() = copy(
        chat = chat.copy(
                showUserActions = false
        )
)

fun ChatScreenState.enableUserInput() = copy(
        chat = chat.copy(
                isUserInputEnable = true
        )
)

fun ChatScreenState.disableUserInput() = copy(
        chat = chat.copy(
                isUserInputEnable = true //TODO
        )
)

fun ChatScreenState.userTextChange(value: String) = copy(
        chat = chat.copy(
                inputState = value
        ),
)

fun ChatScreenState.userTextEnter() = copy(
        chat = chat.addUserMessage(
                message = MessageContent.create(
                        text = chat.inputState,
                )
        ),
)

fun ChatScreenState.clearUserInput() = copy(
        chat = chat.copy(
                inputState = EMPTY_STRING
        )
)

fun ChatScreenState.userMessage(
        message: MessageContent,
        origin: UserMessageOrigin = UserMessageOrigin.INPUT,
) = copy(
        chat = chat.addUserMessage(
                message = message,
                origin = origin,
        )
)

fun ChatScreenState.systemMessage(
        message: MessageContent,
) = copy(
        chat = chat.addSystemMessage(
                message = message
        )
)

fun ChatScreenState.systemMessage(
        result: List<MessageContent>,
) = result.fold(this) { accState, msg ->
    accState.copy(chat = accState.chat.addSystemMessage(message = msg))
}

fun ChatState.nextOrder() = messagesState.list.size

fun ChatState.addUserMessage(
        message: MessageContent,
        origin: UserMessageOrigin = UserMessageOrigin.INPUT,
) = copy(
        messagesState = messagesState.copy(
                list = messagesState.list + ChatMessage.addUserMessage(
                        message = message,
                        order = nextOrder(),
                        origin = origin,
                )
        )
)

fun ChatState.addSystemMessage(
        message: MessageContent,
) = copy(
        messagesState = messagesState.copy(
                list = messagesState.list + ChatMessage.addSystemMessage(
                        message = message.value,
                        order = nextOrder(),
                        buttons = message.buttons
                )
        )
)

