package me.apomazkin.quiz.chat.logic

import androidx.compose.ui.text.AnnotatedString
import me.apomazkin.lexeme.ComponentType
import me.apomazkin.lexeme.ComponentTypeRef


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
     * IS481 quiz picker. Click на radio-пункт. State напрямую не меняется —
     * обновление приходит через `QuizPickerFlowHandler` re-emit после write.
     */
    data class SelectQuizComponent(val ref: ComponentTypeRef) : Msg

    /**
     * IS481 quiz picker. Bulk-load: availableTypes из БД + restored selectedRef
     * из prefs. Emit из `LoadQuizComponentTypes` effect (initial) и из
     * `QuizPickerFlowHandler` (on persist).
     */
    data class QuizComponentTypesLoaded(
            val types: List<ComponentType>,
            val restoredSelectedRef: ComponentTypeRef?,
    ) : Msg

    data object Empty : Msg
}

data class MessageContent(
    val text: AnnotatedString,
    val buttons: List<ChatMessage.ChatButton> = listOf(),
) {
    companion object {
        
        fun create(
            text: String
        ): MessageContent {
            return MessageContent(
                text = AnnotatedString(text = text),
            )
        }
        
        fun create(
            text: AnnotatedString
        ): MessageContent {
            return MessageContent(
                text = text,
            )
        }
        
        fun create(
            text: String,
            buttons: List<ChatMessage.ChatButton>
        ): MessageContent {
            return MessageContent(
                text = AnnotatedString(text = text),
                buttons = buttons,
            )
        }
        
        fun create(
            text: AnnotatedString,
            buttons: List<ChatMessage.ChatButton>
        ): MessageContent {
            return MessageContent(
                text = text,
                buttons = buttons,
            )
        }
    }
}