package me.apomazkin.stattab

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.StateFlow
import me.apomazkin.logger.LexemeLogger
import io.github.kilgoret.mate.Mate
import io.github.kilgoret.mate.MateStateHolder
import me.apomazkin.stattab.mate.DatasourceEffectHandler
import me.apomazkin.stattab.mate.Msg
import me.apomazkin.stattab.mate.StatisticReducer
import me.apomazkin.stattab.mate.StatisticState

class StatisticViewModel @AssistedInject constructor(
    @Assisted navigator: StatisticNavigator,
    logger: LexemeLogger,
    datasourceHandler: DatasourceEffectHandler,
    navHandlerFactory: StatisticNavigationEffectHandler.Factory,
) : ViewModel(), MateStateHolder<StatisticState, Msg> {

    private val stateHolder = Mate(
        initState = StatisticState(),
        initEffects = setOf(),
        coroutineScope = viewModelScope,
        reducer = StatisticReducer(logger = logger),
        effectHandlers = listOf(
            navHandlerFactory.create(navigator),
        ),
        flowHandlers = listOf(datasourceHandler),
    )

    override val state: StateFlow<StatisticState>
        get() = stateHolder.state

    override fun accept(message: Msg) = stateHolder.accept(message)

    @AssistedFactory
    interface Factory {
        fun create(navigator: StatisticNavigator): StatisticViewModel
    }
}
