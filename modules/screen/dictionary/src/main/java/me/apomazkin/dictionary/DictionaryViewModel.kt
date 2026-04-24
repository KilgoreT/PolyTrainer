package me.apomazkin.dictionary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.StateFlow
import me.apomazkin.dictionary.logic.DictionaryReducer
import me.apomazkin.dictionary.logic.DictionaryState
import me.apomazkin.dictionary.logic.DatasourceEffect
import me.apomazkin.dictionary.logic.DatasourceEffectHandler
import me.apomazkin.dictionary.logic.Msg
import me.apomazkin.mate.Mate
import me.apomazkin.mate.MateStateHolder

interface DictionaryUseCase {
    suspend fun getFlagRes(numericCode: Int): Int
    suspend fun addDictionary(numericCode: Int, name: String): Long
    suspend fun saveCurrentDictionary(numericCode: Int)
}

class DictionaryViewModel(
    dictionaryUseCase: DictionaryUseCase,
) : ViewModel(), MateStateHolder<DictionaryState, Msg> {

    private val stateHolder = Mate(
        initState = DictionaryState(),
        initEffects = setOf(DatasourceEffect.LoadDictionaryList),
        coroutineScope = viewModelScope,
        reducer = DictionaryReducer(),
        effectHandlerSet = setOf(
            DatasourceEffectHandler(dictionaryUseCase = dictionaryUseCase),
        )
    )

    override val state: StateFlow<DictionaryState>
        get() = stateHolder.state

    override fun accept(message: Msg) = stateHolder.accept(message)

    class Factory(
        private val dictionaryUseCase: DictionaryUseCase,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DictionaryViewModel(dictionaryUseCase) as T
        }
    }

}