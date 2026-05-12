package me.apomazkin.dictionary.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.StateFlow
import me.apomazkin.mate.Mate
import me.apomazkin.mate.MateStateHolder

class DictionaryListViewModel @AssistedInject constructor(
    @Assisted navigator: ListNavigator,
    datasourceHandler: DictionaryListEffectHandler,
    flowHandler: DictionaryListFlowHandler,
    navHandlerFactory: ListNavigationEffectHandler.Factory,
) : ViewModel(), MateStateHolder<DictionaryListScreenState, DictionaryListMsg> {

    private val stateHolder = Mate(
        initState = DictionaryListScreenState(),
        initEffects = emptySet(),
        coroutineScope = viewModelScope,
        reducer = DictionaryListReducer(),
        effectHandlerSet = setOf(
            datasourceHandler,
            flowHandler,
            navHandlerFactory.create(navigator),
        )
    )

    override val state: StateFlow<DictionaryListScreenState>
        get() = stateHolder.state

    override fun accept(message: DictionaryListMsg) = stateHolder.accept(message)

    @AssistedFactory
    interface Factory {
        fun create(navigator: ListNavigator): DictionaryListViewModel
    }
}
