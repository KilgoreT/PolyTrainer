package me.apomazkin.dictionary.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.StateFlow
import me.apomazkin.dictionary.DictionaryUseCase
import me.apomazkin.mate.Mate
import me.apomazkin.mate.MateStateHolder

class DictionaryListViewModel(
    dictionaryUseCase: DictionaryUseCase,
) : ViewModel(), MateStateHolder<DictionaryListScreenState, DictionaryListMsg> {

    private val stateHolder = Mate(
        initState = DictionaryListScreenState(),
        initEffects = emptySet(),
        coroutineScope = viewModelScope,
        reducer = DictionaryListReducer(),
        effectHandlerSet = setOf(
            DictionaryListEffectHandler(dictionaryUseCase = dictionaryUseCase),
            DictionaryListFlowHandler(dictionaryUseCase = dictionaryUseCase),
        )
    )

    override val state: StateFlow<DictionaryListScreenState>
        get() = stateHolder.state

    override fun accept(message: DictionaryListMsg) = stateHolder.accept(message)

    class Factory(
        private val dictionaryUseCase: DictionaryUseCase,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DictionaryListViewModel(dictionaryUseCase) as T
        }
    }
}
