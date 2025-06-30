package me.apomazkin.dictionaryappbar.mate

import me.apomazkin.mate.Effect
import me.apomazkin.mate.MateReducer
import me.apomazkin.mate.ReducerResult
import me.apomazkin.ui.logger.LexemeLogger

internal class DictionaryAppBarReducer(
    private val logger: LexemeLogger,
) : MateReducer<DictionaryAppBarState, Msg, Effect> {
    override fun reduce(
            state: DictionaryAppBarState,
            message: Msg
    ): ReducerResult<DictionaryAppBarState, Effect> {
        logger.log(message = "Reduce --prevState--: $state ")
        logger.log(message = "Reduce ---message---: $message ")
        return when (message) {
            is Msg.AvailableDict -> state
                    .hideLoading()
                    .availableDictList(message.list) to setOf<Effect>()

            is Msg.CurrentDict -> state
                    .currentDict(message.current) to setOf()

            is Msg.ChangeDict -> state
                    .dictMenuOff() to setOf(DatasourceEffect.ChangeDict(dict = message.dict))

            is Msg.DictMenuOff -> state
                    .dictMenuOff() to setOf()

            is Msg.DictMenuOn -> state
                    .dictMenuOn() to setOf()

            is Msg.Empty -> state to setOf()
        }.also {
            logger.log(message = "Reduce --newState--: ${it.first} ")
            it.second.forEach { effect ->
                logger.log(message = "Reduce --toEffect--: $effect ")
            }
        }
    }
}