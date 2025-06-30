package me.apomazkin.quiztab.logic

import me.apomazkin.mate.Effect
import me.apomazkin.mate.MateReducer
import me.apomazkin.mate.ReducerResult
import me.apomazkin.quiztab.logic.processor.processUiMessage
import me.apomazkin.ui.logger.LexemeLogger

internal class QuizTabReducer(
    private val logger: LexemeLogger,
) : MateReducer<QuizTabState, Msg, Effect> {
    override fun reduce(
        state: QuizTabState,
        message: Msg
    ): ReducerResult<QuizTabState, Effect> {
        logger.log(message = "Reduce --prevState--: $state ")
        logger.log(message = "Reduce ---message---: $message ")
        return when (message) {
            is UiMsg -> processUiMessage(state, message)
            Msg.Empty -> state to emptySet()
        }.also {
            logger.log(message = "Reduce --newState--: ${it.first} ")
            it.second.forEach { effect ->
                logger.log(message = "Reduce --toEffect--: $effect ")
            }
        }
    }
}