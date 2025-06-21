package me.apomazkin.quiz.chat.logic

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import me.apomazkin.mate.EMPTY_STRING
import me.apomazkin.quiz.chat.logic.ChatMessage.MessageValue.Plain

private const val DEFAULT_LOAD_DELAY = 0L

/**
 * State
 */
@Immutable
data class ChatScreenState(
        val loading: Boolean = true,
        val exit: Boolean = false,
        val loadDelay: Long = DEFAULT_LOAD_DELAY,
        val appBarState: AppBarState = AppBarState(),
        val chat: ChatState = ChatState(),
        val snackbarState: SnackbarState = SnackbarState(),
)

@Immutable
data class AppBarState(
        val isActionMenuOpen: Boolean = false,
        val itemsState: ItemsState = ItemsState()
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
        val isDebugOn: Boolean = false,
)

fun ChatScreenState.debug(isOn: Boolean) = this.copy(
        appBarState = this.appBarState.copy(
                itemsState = this.appBarState.itemsState.copy(
                        isDebugOn = isOn
                )
        )
)

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

@Stable
data class ChatMessage(
        val order: Int = -1,
        val isSystemMessage: Boolean,
        val message: MessageValue,
        val buttons: List<ChatButton> = listOf(),
) {

    sealed class MessageValue {
        fun asString(): String = when (this) {
            is Plain -> value
            is Rich -> value.text
        }

        fun asText(): AnnotatedString = when (this) {
            is Plain -> buildAnnotatedString { append(value) }
            is Rich -> value
        }

        data class Plain(val value: String) : MessageValue()
        data class Rich(val value: AnnotatedString) : MessageValue()
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
            LAST_SESSION_SUMMARY,
            FULL_QUIZ_SUMMARY,
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

        fun addUserMessage(
                message: MessageContent,
                order: Int,
        ) = ChatMessage(
                order = order,
                isSystemMessage = false,
                message = Plain(message.text.text),
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
) = copy(
        chat = chat.addUserMessage(
                message = message
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

fun ChatState.addUserMessage(message: MessageContent) = copy(
        messagesState = messagesState.copy(
                list = messagesState.list + ChatMessage.addUserMessage(
                        message = message,
                        order = nextOrder(),
                )
        )
)

fun ChatState.addSystemMessage(
        message: MessageContent,
) = copy(
        messagesState = messagesState.copy(
                list = messagesState.list + ChatMessage.addSystemMessage(
                        message = message.text,
                        order = nextOrder(),
                        buttons = message.buttons
                )
        )
)

fun ChatScreenState.exit() = copy(
        exit = true
)