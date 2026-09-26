package me.apomazkin.stattab

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.StateFlow
import me.apomazkin.logger.LexemeLogger
import io.github.kilgoret.mate.MateStore
import io.github.kilgoret.mate.navigation.MateNavigationHandler
import me.apomazkin.stattab.deps.StatisticUseCase
import me.apomazkin.stattab.mate.Msg
import me.apomazkin.stattab.mate.StatisticState

/**
 * VM вкладки статистики: тонкая обёртка над [StatisticAssembly] —
 * сборка раннера живёт там (общая с харнесом), VM даёт только
 * viewModelScope и продовые зависимости.
 */
class StatisticViewModel @AssistedInject constructor(
    useCase: StatisticUseCase,
    logger: LexemeLogger,
    navigationHandler: MateNavigationHandler,
) : ViewModel(), MateStore<StatisticState, Msg> {

    private val stateHolder = StatisticAssembly.create(
        useCase = useCase,
        logger = logger,
        navigationHandler = navigationHandler,
        coroutineScope = viewModelScope,
    )

    override val state: StateFlow<StatisticState>
        get() = stateHolder.state

    override fun accept(message: Msg) = stateHolder.accept(message)

    @AssistedFactory
    interface Factory {
        fun create(): StatisticViewModel
    }
}
