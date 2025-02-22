package me.apomazkin.quiz.chat.logic

import androidx.annotation.ArrayRes
import androidx.annotation.StringRes
import me.apomazkin.mate.Effect
import me.apomazkin.mate.MateReducer
import me.apomazkin.mate.ReducerResult
import me.apomazkin.mate.effects
import me.apomazkin.mate.state
import me.apomazkin.quiz.chat.R
import me.apomazkin.ui.logger.LexemeLogger
import me.apomazkin.ui.resource.ResourceManager

internal class ChatReducer(
    private val resourceManager: ResourceManager,
    private val logger: LexemeLogger,
) : MateReducer<ChatScreenState, Msg, Effect> {
    
    private fun resToString(@StringRes id: Int) =
        resourceManager.stringByResId(id)
    
    private fun arrayToString(@ArrayRes id: Int) =
        resourceManager.stringByArrayId(id)
    
    override fun reduce(
        state: ChatScreenState, message: Msg
    ): ReducerResult<ChatScreenState, Effect> {
        logger.log(message = "Reduce --prevState--: $state ")
        logger.log(message = "Reduce ---message---: $message ")
        return when (message) {
            is Msg.PrepareToStart -> state
                .stopLoading()
                .prepareToStart(
                    message = resToString(R.string.chat_quiz_system_start)
                ) to emptySet<Effect>()
            
            is Msg.Start -> state
                .startQuiz(
                    message = resToString(R.string.chat_quiz_user_start)
                ) to setOf(DatasourceEffect.LoadQuiz)
            
            is Msg.QuizData -> state
                .appendQuizData(message.data)
                .ask(header = resToString(R.string.chat_quiz_ask_translate_header)) to emptySet()
            
            is Msg.UserTextChange -> state
                .userTextChange(message.value) to emptySet()
            
            is Msg.UserTextEnter -> state
                .userTextEnter()
                .clearUserInput()
                .checkAnswer(
                    correctHeader = arrayToString(R.array.chat_quiz_system_correct),
                    incorrectHeader = arrayToString(R.array.chat_quiz_system_incorrect),
                )
                .ask(header = resToString(R.string.chat_quiz_ask_translate_header)) to emptySet()
            
            is Msg.Empty -> state to emptySet()
        }.also {
            logger.log(message = "Reduce --newState--: ${it.state()} ")
            it.effects().forEach { effect ->
                logger.log(message = "Reduce --toEffect--: $effect ")
            }
        }
    }
}