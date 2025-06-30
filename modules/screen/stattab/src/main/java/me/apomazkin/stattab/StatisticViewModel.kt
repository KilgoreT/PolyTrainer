package me.apomazkin.stattab

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.StateFlow
import me.apomazkin.mate.Mate
import me.apomazkin.mate.MateStateHolder
import me.apomazkin.stattab.deps.StatisticUseCase
import me.apomazkin.stattab.mate.DatasourceEffectHandler
import me.apomazkin.stattab.mate.Msg
import me.apomazkin.stattab.mate.StatisticReducer
import me.apomazkin.stattab.mate.StatisticState
import me.apomazkin.ui.logger.LexemeLogger

class StatisticViewModel(
        statisticUseCase: StatisticUseCase,
        logger: LexemeLogger,
) : ViewModel(), MateStateHolder<StatisticState, Msg> {

    private val stateHolder = Mate(
            initState = StatisticState(),
            initEffects = setOf(),
            coroutineScope = viewModelScope,
            reducer = StatisticReducer(logger = logger),
            effectHandlerSet = setOf(
                    DatasourceEffectHandler(
                            useCase = statisticUseCase,
                    ),
            )
    )

    override val state: StateFlow<StatisticState>
        get() = stateHolder.state

    override fun accept(message: Msg) = stateHolder.accept(message)

    class Factory(
            private val statisticUseCase: StatisticUseCase,
            private val logger: LexemeLogger,
            ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return StatisticViewModel(
                    statisticUseCase = statisticUseCase,
                    logger = logger,
            ) as T
        }
    }
}
