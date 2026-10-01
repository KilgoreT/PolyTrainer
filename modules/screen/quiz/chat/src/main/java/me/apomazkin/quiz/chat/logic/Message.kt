package me.apomazkin.quiz.chat.logic

import androidx.compose.ui.text.AnnotatedString
import me.apomazkin.lexeme.ComponentType
import me.apomazkin.lexeme.ComponentTypeRef
import me.apomazkin.quiz.QuizGroupLabel
import me.apomazkin.quiz.chat.quiz.QuizQuestion


sealed interface Msg {
    
    /**
     * Message to prepare the quiz
     */
    data object PrepareToStart: Msg

    data object ShowMenu : Msg
    data object HideMenu : Msg
    data object EarliestOn: Msg
    data object EarliestOff: Msg
    data object FrequentMistakesOn: Msg
    data object FrequentMistakesOff: Msg
    data object DebugOn: Msg
    data object DebugOff: Msg
    data class UpdateMenu(
            val isEarliestOn: Boolean,
            val isFrequentMistakesOn: Boolean,
            val isDebugOn: Boolean,
    ): Msg

    /**
     * Message to load quiz data
     */
    data object Start : Msg
    data class QuizLoaded(val content: AnnotatedString?) : Msg

    /**
     * Подпись набора групп, по которому собрана сессия (сабтайтл
     * аппбара); null — «Все». Шлёт handler на входе и вместе с загрузкой
     * квиза.
     */
    data class QuizGroupLabelLoaded(val label: QuizGroupLabel?) : Msg
    data class QuizReLoaded(val content: AnnotatedString?) : Msg
    
    /**
     * Message to show next question
     */
    data class NextQuestion(val content: MessageContent) : Msg
    
    /**
     * Message to change the input text from the user
     */
    data class UserTextChange(val value: String) : Msg
    
    /**
     * Message to send the user attempt
     */
    data class UserAttempt(val value: String) : Msg
    
    data object GetAnswer : Msg
    data class ShowAnswer(val value: MessageContent) : Msg
    
    data object Skip : Msg
    data object Skipped : Msg
    
    /**
     * Message to show the assessment of the user attempt
     */
    data class Assessment(val value: MessageContent) : Msg
    
    data class SessionOver(val value: MessageContent) : Msg
    
    /**
     * Message to send user action
     */
    data class UserAction(val action: ChatMessage.Companion.UserAction) : Msg
    
    data class SummaryOptions(val value: MessageContent) : Msg
    data class Summary(val value: List<MessageContent>) : Msg

    /**
     * Очередное сообщение пачки бота, выданное хендлером после паузы
     * (эффект `DatasourceEffect.DeliverSystemMessages`); [rest] — что ещё
     * стоит в очереди, редьюсер повторяет эффект для него; [then] — что
     * запустить, когда очередь опустеет (пробрасывается из эффекта).
     */
    data class SystemMessageDelivered(
            val message: MessageContent,
            val rest: List<MessageContent>,
            val then: DatasourceEffect? = null,
    ) : Msg

    /**
     * Клик по галке ядра в меню: [checked] — новое положение галки.
     * Снятие последней включённой — no-op (инвариант «минимум одно»).
     */
    data class ToggleQuizComponent(
            val ref: ComponentTypeRef,
            val checked: Boolean,
    ) : Msg

    /**
     * Загрузка пикера: ядра словаря из БД + сохранённый набор из prefs
     * (пусто — не сохранено). Шлют `LoadQuizComponentTypes` (на входе) и
     * подписка `ChatSub.QuizPicker` (после каждой записи prefs).
     */
    data class QuizComponentTypesLoaded(
            val types: List<ComponentType>,
            val restoredSelectedRefs: Set<ComponentTypeRef>,
    ) : Msg

    data object Empty : Msg
}

/**
 * Содержимое сообщения, которое редьюсер кладёт в ленту: [value] — текст
 * или структурный вопрос, [buttons] — кнопки под системным сообщением.
 */
data class MessageContent(
    val value: ChatMessage.MessageValue,
    val buttons: List<ChatMessage.ChatButton> = listOf(),
) {
    companion object {

        fun create(
            text: String
        ): MessageContent {
            return MessageContent(
                value = ChatMessage.MessageValue.Plain(text),
            )
        }

        fun create(
            text: AnnotatedString
        ): MessageContent {
            return MessageContent(
                value = ChatMessage.MessageValue.Rich(text),
            )
        }

        fun create(
            text: String,
            buttons: List<ChatMessage.ChatButton>
        ): MessageContent {
            return MessageContent(
                value = ChatMessage.MessageValue.Plain(text),
                buttons = buttons,
            )
        }

        fun create(
            text: AnnotatedString,
            buttons: List<ChatMessage.ChatButton>
        ): MessageContent {
            return MessageContent(
                value = ChatMessage.MessageValue.Rich(text),
                buttons = buttons,
            )
        }

        fun question(
            question: QuizQuestion,
            buttons: List<ChatMessage.ChatButton> = listOf(),
        ): MessageContent {
            return MessageContent(
                value = ChatMessage.MessageValue.Question(question),
                buttons = buttons,
            )
        }
    }
}