package me.apomazkin.dictionary.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.StateFlow
import io.github.kilgoret.mate.Mate
import io.github.kilgoret.mate.MateStateHolder

class DictionaryListViewModel @AssistedInject constructor(
    @Assisted navigator: ListNavigator,
    datasourceHandler: DictionaryListEffectHandler,
    listSubHandler: DictionaryListSubHandler,
    navHandlerFactory: ListNavigationEffectHandler.Factory,
) : ViewModel(), MateStateHolder<DictionaryListScreenState, DictionaryListMsg> {

    private val stateHolder = Mate(
        initState = DictionaryListScreenState(),
        initEffects = emptySet(),
        coroutineScope = viewModelScope,
        reducer = DictionaryListReducer(),
        effectHandlers = listOf(
            datasourceHandler,
            navHandlerFactory.create(navigator),
        ),
        subscriptions = { it.subscriptions() },
        subscriptionHandlers = listOf(listSubHandler),
    )

    override val state: StateFlow<DictionaryListScreenState>
        get() = stateHolder.state

    override fun accept(message: DictionaryListMsg) = stateHolder.accept(message)

    @AssistedFactory
    interface Factory {
        fun create(navigator: ListNavigator): DictionaryListViewModel
    }
}
