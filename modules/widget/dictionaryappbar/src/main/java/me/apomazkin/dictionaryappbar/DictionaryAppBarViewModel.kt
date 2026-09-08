package me.apomazkin.dictionaryappbar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.StateFlow
import me.apomazkin.dictionaryappbar.mate.DatasourceEffectHandler
import me.apomazkin.dictionaryappbar.mate.DictionaryAppBarReducer
import me.apomazkin.dictionaryappbar.mate.DictionaryAppBarState
import me.apomazkin.dictionaryappbar.mate.DictionaryAppBarSubHandler
import me.apomazkin.dictionaryappbar.mate.Msg
import me.apomazkin.dictionaryappbar.mate.subscriptions
import me.apomazkin.logger.LexemeLogger
import io.github.kilgoret.mate.Mate
import io.github.kilgoret.mate.MateStateHolder
import io.github.kilgoret.mate.navigation.MateNavigationHandler

/**
 * VM виджета app bar со словарём — точка сборки цикла mate. Навигация
 * идёт через shared nav-handler приложения, per-экранного navigator'а нет.
 */
class DictionaryAppBarViewModel @AssistedInject constructor(
    logger: LexemeLogger,
    datasourceHandler: DatasourceEffectHandler,
    appBarSubHandler: DictionaryAppBarSubHandler,
    navigationHandler: MateNavigationHandler,
) : ViewModel(), MateStateHolder<DictionaryAppBarState, Msg> {

    private val stateHolder = Mate(
        initState = DictionaryAppBarState(),
        initEffects = setOf(),
        coroutineScope = viewModelScope,
        reducer = DictionaryAppBarReducer(logger = logger),
        effectHandlers = listOf(
            datasourceHandler,
            navigationHandler,
        ),
        subscriptions = { it.subscriptions() },
        subscriptionHandlers = listOf(appBarSubHandler),
    )

    override val state: StateFlow<DictionaryAppBarState>
        get() = stateHolder.state

    override fun accept(message: Msg) = stateHolder.accept(message)

    @AssistedFactory
    interface Factory {
        fun create(): DictionaryAppBarViewModel
    }
}
