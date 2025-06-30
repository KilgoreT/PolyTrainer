package me.apomazkin.stattab.mate

import me.apomazkin.mate.Effect
import me.apomazkin.mate.MateReducer
import me.apomazkin.mate.ReducerResult
import me.apomazkin.ui.logger.LexemeLogger

internal class StatisticReducer(
    private val logger: LexemeLogger,
) : MateReducer<StatisticState, Msg, Effect> {
    override fun reduce(
        state: StatisticState,
        message: Msg
    ): ReducerResult<StatisticState, Effect> {
        logger.log(message = "Reduce --prevState--: $state ")
        logger.log(message = "Reduce ---message---: $message ")
        return when (message) {
            is Msg.UpdateStates -> state
                .hideLoading()
                .updateWordCount(message.wordCount)
                .updateLexemeCount(message.lexemeCount)
                .updateQuizStat(message.quizStat) to setOf<Effect>()

            Msg.Empty -> state to emptySet()
        }.also {
            logger.log(message = "Reduce --newState--: ${it.first} ")
            it.second.forEach { effect ->
                logger.log(message = "Reduce --toEffect--: $effect ")
            }
        }
    }
}