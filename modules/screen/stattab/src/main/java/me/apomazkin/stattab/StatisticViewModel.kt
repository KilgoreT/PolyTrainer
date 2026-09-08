package me.apomazkin.stattab

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.StateFlow
import me.apomazkin.logger.LexemeLogger
import io.github.kilgoret.mate.Mate
import io.github.kilgoret.mate.MateStateHolder
import io.github.kilgoret.mate.navigation.MateNavigationHandler
import me.apomazkin.stattab.mate.Msg
import me.apomazkin.stattab.mate.StatSubHandler
import me.apomazkin.stattab.mate.StatisticReducer
import me.apomazkin.stattab.mate.StatisticState
import me.apomazkin.stattab.mate.subscriptions

/**
 * VM вкладки статистики — точка сборки цикла mate. Навигация идёт через
 * shared nav-handler приложения, per-экранного navigator'а нет.
 */
class StatisticViewModel @AssistedInject constructor(
    logger: LexemeLogger,
    statSubHandler: StatSubHandler,
    navigationHandler: MateNavigationHandler,
) : ViewModel(), MateStateHolder<StatisticState, Msg> {

    private val stateHolder = Mate(
        initState = StatisticState(),
        initEffects = setOf(),
        coroutineScope = viewModelScope,
        reducer = StatisticReducer(logger = logger),
        effectHandlers = listOf(
            navigationHandler,
        ),
        subscriptions = { it.subscriptions() },
        subscriptionHandlers = listOf(statSubHandler),
    )

    override val state: StateFlow<StatisticState>
        get() = stateHolder.state

    override fun accept(message: Msg) = stateHolder.accept(message)

    @AssistedFactory
    interface Factory {
        fun create(): StatisticViewModel
    }
}
