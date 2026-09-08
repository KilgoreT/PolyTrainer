package me.apomazkin.dictionary.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.StateFlow
import io.github.kilgoret.mate.Mate
import io.github.kilgoret.mate.MateStateHolder
import io.github.kilgoret.mate.navigation.MateNavigationHandler

/**
 * VM списка словарей — точка сборки цикла mate. Навигация идёт через
 * shared nav-handler приложения, per-экранного navigator'а нет.
 */
class DictionaryListViewModel @AssistedInject constructor(
    datasourceHandler: DictionaryListEffectHandler,
    listSubHandler: DictionaryListSubHandler,
    navigationHandler: MateNavigationHandler,
) : ViewModel(), MateStateHolder<DictionaryListScreenState, DictionaryListMsg> {

    private val stateHolder = Mate(
        initState = DictionaryListScreenState(),
        initEffects = emptySet(),
        coroutineScope = viewModelScope,
        reducer = DictionaryListReducer(),
        effectHandlers = listOf(
            datasourceHandler,
            navigationHandler,
        ),
        subscriptions = { it.subscriptions() },
        subscriptionHandlers = listOf(listSubHandler),
    )

    override val state: StateFlow<DictionaryListScreenState>
        get() = stateHolder.state

    override fun accept(message: DictionaryListMsg) = stateHolder.accept(message)

    @AssistedFactory
    interface Factory {
        fun create(): DictionaryListViewModel
    }
}
