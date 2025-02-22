package me.apomazkin.quiz.chat.logic

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import me.apomazkin.mate.EMPTY_STRING
import me.apomazkin.quiz.chat.logic.ChatMessage.MessageValue.Plain
import me.apomazkin.theme.LexemeStyle

private const val DEFAULT_LOAD_DELAY = 0L

/**
 * State
 */
@Immutable
data class ChatScreenState(
    val loading: Boolean = true,
    val loadDelay: Long = DEFAULT_LOAD_DELAY,
    val chat: ChatState = ChatState(),
    val quizState: QuizState = QuizState(),
    val snackbarState: SnackbarState = SnackbarState(),
)

@Immutable
data class ChatState(
    val readyToStart: Boolean = false,
    val messagesState: ChatMessageState = ChatMessageState(),
    val inputState: String = EMPTY_STRING,
)

@Immutable
data class ChatMessageState(
    val list: List<ChatMessage> = listOf(),
)


data class QuizState(
    val step: Int = -1,
    val quizData: List<Pair<String, String>> = listOf(),
)

@Stable
data class ChatMessage(
    val order: Int = -1,
    val isSystemMessage: Boolean,
    val message: MessageValue,
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
    
    companion object {
        
        fun plain(message: String) = Plain(message)
        
        fun addSystemMessage(message: String, order: Int) =
            ChatMessage(
                order = order,
                isSystemMessage = true,
                message = MessageValue.Plain(message),
            )
        
        fun addSystemMessage(message: AnnotatedString, order: Int) =
            ChatMessage(
                order = order,
                isSystemMessage = true,
                message = MessageValue.Rich(message),
            )
        
        fun addUserMessage(message: String, order: Int) = ChatMessage(
            order = order,
            isSystemMessage = false,
            message = MessageValue.Plain(message),
        )
    }
}

@Immutable
data class SnackbarState(
    val title: String = EMPTY_STRING,
    val show: Boolean = false,
)

fun ChatScreenState.stopLoading() = copy(loading = false)

fun ChatScreenState.prepareToStart(
    message: String,
) = copy(
    chat = chat.addSystemMessage(message = message)
)

fun ChatScreenState.startQuiz(
    message: String
) = copy(
    chat = chat
        .copy(readyToStart = true)
        .addUserMessage(message = message)
)

fun ChatScreenState.appendQuizData(data: List<Pair<String, String>>) = copy(
    quizState = quizState.copy(
        quizData = data,
    )
)

fun ChatScreenState.ask(
    header: String,
) = copy(
    chat = chat.addSystemMessage(
        buildAnnotatedString {
            append(header)
            append("\n")
            withStyle(style = LexemeStyle.BodyMBold.toSpanStyle()) {
                append(quizState.quizData[nextStep()].first)
            }
        }
    ),
    quizState = quizState.copy(
        step = nextStep(),
    )
)

fun ChatScreenState.userTextChange(value: String) = copy(
    chat = chat.copy(
        inputState = value
    ),
)

fun ChatScreenState.userTextEnter() = copy(
    chat = chat.addUserMessage(chat.inputState),
)

fun ChatScreenState.clearUserInput() = copy(
    chat = chat.copy(
        inputState = EMPTY_STRING
    )
)

fun ChatScreenState.checkAnswer(
    correctHeader: String,
    incorrectHeader: String,
) = copy(
    chat = chat.addSystemMessage(
        message = if (isAnswerCorrect()) correctHeader else incorrectHeader
    )
)


fun ChatScreenState.nextStep() = quizState.step + 1

fun ChatScreenState.lastMessage() =
    chat.messagesState.list.last().message.asString()

fun ChatScreenState.correctAnswer() = quizState.quizData[quizState.step].second

fun ChatScreenState.isAnswerCorrect() = lastMessage() == correctAnswer()


fun ChatState.nextOrder() = messagesState.list.size
fun ChatState.addUserMessage(message: String) = copy(
    messagesState = messagesState.copy(
        list = messagesState.list + ChatMessage.addUserMessage(
            message = message,
            order = nextOrder()
        )
    )
)

fun ChatState.addSystemMessage(message: AnnotatedString) = copy(
    messagesState = messagesState.copy(
        list = messagesState.list + ChatMessage.addSystemMessage(
            message = message,
            order = nextOrder()
        )
    )
)

fun ChatState.addSystemMessage(message: String) = copy(
    messagesState = messagesState.copy(
        list = messagesState.list + ChatMessage.addSystemMessage(
            message = message,
            order = nextOrder()
        )
    )
)