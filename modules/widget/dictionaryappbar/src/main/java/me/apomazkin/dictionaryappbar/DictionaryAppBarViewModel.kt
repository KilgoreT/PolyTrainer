package me.apomazkin.dictionaryappbar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
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

class DictionaryAppBarViewModel @AssistedInject constructor(
    @Assisted navigator: DictionaryAppBarNavigator,
    logger: LexemeLogger,
    datasourceHandler: DatasourceEffectHandler,
    appBarSubHandler: DictionaryAppBarSubHandler,
    navHandlerFactory: DictionaryAppBarNavigationEffectHandler.Factory,
) : ViewModel(), MateStateHolder<DictionaryAppBarState, Msg> {

    private val stateHolder = Mate(
        initState = DictionaryAppBarState(),
        initEffects = setOf(),
        coroutineScope = viewModelScope,
        reducer = DictionaryAppBarReducer(logger = logger),
        effectHandlers = listOf(
            datasourceHandler,
            navHandlerFactory.create(navigator),
        ),
        subscriptions = { it.subscriptions() },
        subscriptionHandlers = listOf(appBarSubHandler),
    )

    override val state: StateFlow<DictionaryAppBarState>
        get() = stateHolder.state

    override fun accept(message: Msg) = stateHolder.accept(message)

    @AssistedFactory
    interface Factory {
        fun create(navigator: DictionaryAppBarNavigator): DictionaryAppBarViewModel
    }
}
