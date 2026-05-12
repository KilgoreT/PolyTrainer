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
import me.apomazkin.dictionaryappbar.mate.Msg
import me.apomazkin.logger.LexemeLogger
import me.apomazkin.mate.Mate
import me.apomazkin.mate.MateStateHolder

class DictionaryAppBarViewModel @AssistedInject constructor(
    @Assisted navigator: DictionaryAppBarNavigator,
    logger: LexemeLogger,
    datasourceHandler: DatasourceEffectHandler,
    navHandlerFactory: DictionaryAppBarNavigationEffectHandler.Factory,
) : ViewModel(), MateStateHolder<DictionaryAppBarState, Msg> {

    private val stateHolder = Mate(
        initState = DictionaryAppBarState(),
        initEffects = setOf(),
        coroutineScope = viewModelScope,
        reducer = DictionaryAppBarReducer(logger = logger),
        effectHandlerSet = setOf(
            datasourceHandler,
            navHandlerFactory.create(navigator),
        )
    )

    override val state: StateFlow<DictionaryAppBarState>
        get() = stateHolder.state

    override fun accept(message: Msg) = stateHolder.accept(message)

    @AssistedFactory
    interface Factory {
        fun create(navigator: DictionaryAppBarNavigator): DictionaryAppBarViewModel
    }
}
